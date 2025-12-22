import { createPrivateKey, createPublicKey, createSign } from 'crypto';
import { getPool, ensureSchema } from './_db.js';

function nowSec() {
  return Math.floor(Date.now() / 1000);
}

function signPayload(payloadJson) {
  const pem = process.env.NV_PRIVATE_KEY_PEM;
  if (!pem) throw new Error('missing_private_key');

  const priv = createPrivateKey(pem);
  const sign = createSign('RSA-SHA256');
  sign.update(Buffer.from(payloadJson, 'utf8'));
  sign.end();
  return sign.sign(priv).toString('base64');
}

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'method_not_allowed' });
    return;
  }

  const code = (req.body?.code || '').trim();
  const uuid = (req.body?.uuid || '').trim();
  const hwid = (req.body?.hwid || '').trim();

  if (!code || !uuid || !hwid) {
    res.status(400).json({ error: 'missing_fields' });
    return;
  }

  const pool = getPool();
  const client = await pool.connect();

  try {
    await client.query('BEGIN');
    await ensureSchema(client);

    const rowRes = await client.query(
      'SELECT code, used, bound_uuid, bound_hwid, expires_at, revoked, duration_days, duration_minutes FROM codes WHERE code = $1 FOR UPDATE',
      [code]
    );

    if (rowRes.rowCount === 0) {
      await client.query('ROLLBACK');
      res.status(404).json({ error: 'code_not_found' });
      return;
    }

    const row = rowRes.rows[0];

    // Проверка: ключ отозван
    if (row.revoked) {
      await client.query('ROLLBACK');
      res.status(403).json({ error: 'code_revoked' });
      return;
    }

    // Проверка: ключ истёк (если уже активирован и есть expires_at)
    if (row.used && row.expires_at) {
      const now = nowSec();
      if (now > row.expires_at) {
        await client.query('ROLLBACK');
        res.status(403).json({ error: 'code_expired' });
        return;
      }
    }

    let expiresAt = row.expires_at;

    if (!row.used) {
      // Первая активация - вычисляем expires_at
      const now = nowSec();
      if (row.duration_minutes && row.duration_minutes > 0) {
        // Минуты (для тестов)
        expiresAt = now + (row.duration_minutes * 60);
      } else if (row.duration_days && row.duration_days > 0) {
        // Дни
        expiresAt = now + (row.duration_days * 24 * 60 * 60);
      }

      await client.query(
        'UPDATE codes SET used = TRUE, bound_uuid = $1, bound_hwid = $2, used_at = $3, expires_at = $4 WHERE code = $5',
        [uuid, hwid, now, expiresAt, code]
      );
    } else {
      // Повторная активация - проверяем uuid/hwid
      if ((row.bound_uuid || '') !== uuid || (row.bound_hwid || '') !== hwid) {
        await client.query('ROLLBACK');
        res.status(403).json({ error: 'code_already_used' });
        return;
      }
    }

    await client.query('COMMIT');

    // Формируем payload с exp если есть срок
    const payload = { uuid, hwid };
    if (expiresAt) {
      payload.exp = expiresAt;
    }

    const payloadJson = JSON.stringify(payload);
    const signature = signPayload(payloadJson);

    res.status(200).json({
      license: {
        payload: payloadJson,
        signature
      },
      expires_at: expiresAt || null
    });
  } catch (e) {
    try { await client.query('ROLLBACK'); } catch {}
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

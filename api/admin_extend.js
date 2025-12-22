import { getPool, ensureSchema } from './_db.js';

function nowSec() {
  return Math.floor(Date.now() / 1000);
}

function getAuthToken(req) {
  const hdr = req.headers?.authorization || req.headers?.Authorization || '';
  const m = /^Bearer\s+(.+)$/i.exec(hdr);
  return m ? m[1] : '';
}

/**
 * POST /api/admin_extend
 * Body: { code: "NV-XXXX-XXXX-XXXX", days: 7 }
 * 
 * Продлевает срок действия ключа на указанное количество дней
 */
export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'method_not_allowed' });
    return;
  }

  const expected = (process.env.NV_ADMIN_TOKEN || '').trim();
  const got = (getAuthToken(req) || '').trim();

  if (!expected) {
    res.status(500).json({ error: 'missing_admin_token_env' });
    return;
  }

  if (got !== expected) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }

  const code = (req.body?.code || '').trim();
  const days = parseInt(req.body?.days, 10);

  if (!code) {
    res.status(400).json({ error: 'missing_code' });
    return;
  }

  if (isNaN(days) || days <= 0) {
    res.status(400).json({ error: 'invalid_days' });
    return;
  }

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);

    // Получаем текущий ключ
    const rowRes = await client.query(
      'SELECT expires_at FROM codes WHERE code = $1',
      [code]
    );

    if (rowRes.rowCount === 0) {
      res.status(404).json({ error: 'code_not_found' });
      return;
    }

    const row = rowRes.rows[0];
    const now = nowSec();
    const addSeconds = days * 24 * 60 * 60;

    // Если expires_at уже есть - продлеваем от него (или от сейчас, если уже истёк)
    // Если нет - считаем от сейчас
    let newExpiresAt;
    if (row.expires_at) {
      const base = row.expires_at > now ? row.expires_at : now;
      newExpiresAt = base + addSeconds;
    } else {
      newExpiresAt = now + addSeconds;
    }

    await client.query(
      'UPDATE codes SET expires_at = $1 WHERE code = $2',
      [newExpiresAt, code]
    );

    res.status(200).json({
      success: true,
      code,
      new_expires_at: newExpiresAt,
      expires_date: new Date(newExpiresAt * 1000).toISOString()
    });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

import { getPool, ensureSchema } from './_db.js';

function nowSec() {
  return Math.floor(Date.now() / 1000);
}

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'method_not_allowed' });
    return;
  }

  const uuid = (req.body?.uuid || '').trim();
  const hwid = (req.body?.hwid || '').trim();

  if (!uuid || !hwid) {
    res.status(400).json({ error: 'missing_fields' });
    return;
  }

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);

    // Ищем ключ по uuid и hwid
    const rowRes = await client.query(
      'SELECT code, used, bound_uuid, bound_hwid, expires_at, revoked, revoke_reason FROM codes WHERE bound_uuid = $1 AND bound_hwid = $2',
      [uuid, hwid]
    );

    if (rowRes.rowCount === 0) {
      res.status(404).json({ 
        valid: false, 
        error: 'license_not_found' 
      });
      return;
    }

    const row = rowRes.rows[0];

    // Проверка: ключ отозван
    if (row.revoked) {
      res.status(200).json({ 
        valid: false, 
        error: 'code_revoked',
        reason: row.revoke_reason || 'revoked'
      });
      return;
    }

    // Проверка: ключ истёк
    if (row.expires_at) {
      const now = nowSec();
      if (now > row.expires_at) {
        res.status(200).json({ 
          valid: false, 
          error: 'code_expired',
          expired_at: row.expires_at
        });
        return;
      }
    }

    // Лицензия валидна
    res.status(200).json({
      valid: true,
      code: row.code,
      expires_at: row.expires_at || null
    });

  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

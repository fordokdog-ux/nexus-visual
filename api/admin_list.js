import { getPool, ensureSchema } from './_db.js';

function getAuthToken(req) {
  const hdr = req.headers?.authorization || req.headers?.Authorization || '';
  const m = /^Bearer\s+(.+)$/i.exec(hdr);
  return m ? m[1] : '';
}

/**
 * GET /api/admin_list
 * 
 * Возвращает список всех ключей с информацией
 */
export default async function handler(req, res) {
  if (req.method !== 'GET' && req.method !== 'POST') {
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

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);

    const result = await client.query(
      `SELECT code, used, bound_uuid, bound_hwid, created_at, used_at, expires_at, revoked, revoked_at, revoke_reason, duration_days, note
       FROM codes ORDER BY created_at DESC`
    );

    const now = Math.floor(Date.now() / 1000);

    const codes = result.rows.map(row => ({
      code: row.code,
      used: row.used,
      bound_uuid: row.bound_uuid,
      bound_hwid: row.bound_hwid,
      created_at: row.created_at,
      used_at: row.used_at,
      expires_at: row.expires_at,
      expired: row.expires_at ? now > row.expires_at : false,
      revoked: row.revoked,
      revoked_at: row.revoked_at,
      revoke_reason: row.revoke_reason,
      duration_days: row.duration_days,
      note: row.note,
      status: row.revoked ? 'revoked' : (row.expires_at && now > row.expires_at ? 'expired' : (row.used ? 'active' : 'unused'))
    }));

    res.status(200).json({
      total: codes.length,
      codes
    });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

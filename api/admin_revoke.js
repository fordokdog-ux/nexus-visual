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
 * POST /api/admin_revoke
 * Body: { code: "NV-XXXX-XXXX-XXXX", reason?: "string" }
 * 
 * Отзывает ключ - игрок больше не сможет его использовать
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
  const reason = req.body?.reason || null;

  if (!code) {
    res.status(400).json({ error: 'missing_code' });
    return;
  }

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);

    const result = await client.query(
      'UPDATE codes SET revoked = TRUE, revoked_at = $1, revoke_reason = $2 WHERE code = $3 RETURNING *',
      [nowSec(), reason, code]
    );

    if (result.rowCount === 0) {
      res.status(404).json({ error: 'code_not_found' });
      return;
    }

    res.status(200).json({
      success: true,
      code,
      revoked: true,
      reason
    });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

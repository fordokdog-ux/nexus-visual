import { getPool, ensureSchema } from './_db.js';

function getAuthToken(req) {
  const hdr = req.headers?.authorization || req.headers?.Authorization || '';
  const m = /^Bearer\s+(.+)$/i.exec(hdr);
  return m ? m[1] : '';
}

/**
 * POST /api/admin_reset_hwid
 * Body: { code: "NV-XXXX-XXXX-XXXX" }
 * 
 * Сбрасывает привязку HWID - игрок сможет активировать на другом ПК
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

  if (!code) {
    res.status(400).json({ error: 'missing_code' });
    return;
  }

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);

    const result = await client.query(
      'UPDATE codes SET used = FALSE, bound_uuid = NULL, bound_hwid = NULL, used_at = NULL WHERE code = $1 RETURNING *',
      [code]
    );

    if (result.rowCount === 0) {
      res.status(404).json({ error: 'code_not_found' });
      return;
    }

    res.status(200).json({
      success: true,
      code,
      message: 'HWID reset - key can be activated again'
    });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

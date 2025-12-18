import { randomBytes } from 'crypto';
import { getPool, ensureSchema } from './_db.js';

function nowSec() {
  return Math.floor(Date.now() / 1000);
}

function createCode() {
  const raw = randomBytes(6).toString('hex').toUpperCase();
  return `NV-${raw.slice(0, 4)}-${raw.slice(4, 8)}-${raw.slice(8, 12)}`;
}

function getAuthToken(req) {
  const hdr = req.headers?.authorization || req.headers?.Authorization || '';
  const m = /^Bearer\s+(.+)$/i.exec(hdr);
  return m ? m[1] : '';
}

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'method_not_allowed' });
    return;
  }

  const expected = process.env.NV_ADMIN_TOKEN || '';
  const got = getAuthToken(req);

  if (!expected || got !== expected) {
    res.status(401).json({ error: 'unauthorized' });
    return;
  }

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);
    const code = createCode();
    await client.query(
      'INSERT INTO codes(code, used, bound_uuid, bound_hwid, created_at, used_at) VALUES($1, FALSE, NULL, NULL, $2, NULL)',
      [code, nowSec()]
    );
    res.status(200).json({ code });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

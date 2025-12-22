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

  // Параметры из тела запроса
  const durationDays = req.body?.duration_days ?? req.body?.durationDays ?? null;
  const durationMinutes = req.body?.duration_minutes ?? req.body?.durationMinutes ?? null;
  const note = req.body?.note ?? null;

  const pool = getPool();
  const client = await pool.connect();

  try {
    await ensureSchema(client);
    const code = createCode();
    const createdAt = nowSec();

    // Вычисляем время истечения
    // Если указаны минуты - истекает сразу (для тестов)
    // Если указаны дни - expires_at считается при активации
    let expiresAt = null;
    if (durationMinutes !== null && durationMinutes > 0) {
      expiresAt = createdAt + (durationMinutes * 60);
    }

    await client.query(
      `INSERT INTO codes(code, used, bound_uuid, bound_hwid, created_at, used_at, expires_at, revoked, revoked_at, revoke_reason, duration_days, note)
       VALUES($1, FALSE, NULL, NULL, $2, NULL, $3, FALSE, NULL, NULL, $4, $5)`,
      [code, createdAt, expiresAt, durationDays, note]
    );

    res.status(200).json({
      code,
      duration_days: durationDays,
      duration_minutes: durationMinutes,
      expires_at: expiresAt,
      note,
      created_at: createdAt
    });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  } finally {
    client.release();
  }
}

import pg from 'pg';

const { Pool } = pg;

let pool;

export function getPool() {
  if (!pool) {
    const connectionString =
      process.env.POSTGRES_URL_NON_POOLING ||
      process.env.POSTGRES_URL ||
      process.env.POSTGRES_PRISMA_URL ||
      process.env.DATABASE_URL ||
      process.env.POSTGRES_CONNECTION_STRING;

    if (!connectionString) {
      throw new Error('missing_db_url');
    }

    pool = new Pool({
      connectionString,
      ssl: process.env.PGSSLMODE === 'disable' ? false : { rejectUnauthorized: false }
    });
  }
  return pool;
}

export async function ensureSchema(client) {
  // Таблица кодов активации
  await client.query(`
    CREATE TABLE IF NOT EXISTS codes (
      code TEXT PRIMARY KEY,
      used BOOLEAN NOT NULL DEFAULT FALSE,
      bound_uuid TEXT,
      bound_hwid TEXT,
      created_at BIGINT NOT NULL,
      used_at BIGINT,
      expires_at BIGINT,
      revoked BOOLEAN NOT NULL DEFAULT FALSE,
      revoked_at BIGINT,
      revoke_reason TEXT,
      duration_days INTEGER,
      duration_minutes INTEGER,
      note TEXT
    );
  `);

  // Добавляем новые колонки если их нет (для миграции)
  const cols = ['expires_at', 'revoked', 'revoked_at', 'revoke_reason', 'duration_days', 'duration_minutes', 'note'];
  for (const col of cols) {
    try {
      if (col === 'revoked') {
        await client.query(`ALTER TABLE codes ADD COLUMN IF NOT EXISTS ${col} BOOLEAN NOT NULL DEFAULT FALSE`);
      } else if (col === 'duration_days' || col === 'duration_minutes') {
        await client.query(`ALTER TABLE codes ADD COLUMN IF NOT EXISTS ${col} INTEGER`);
      } else if (col === 'expires_at' || col === 'revoked_at') {
        await client.query(`ALTER TABLE codes ADD COLUMN IF NOT EXISTS ${col} BIGINT`);
      } else {
        await client.query(`ALTER TABLE codes ADD COLUMN IF NOT EXISTS ${col} TEXT`);
      }
    } catch (e) {
      // Колонка уже существует - игнорируем
    }
  }
}

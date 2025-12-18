import pg from 'pg';

const { Pool } = pg;

let pool;

export function getPool() {
  if (!pool) {
    const connectionString =
      process.env.POSTGRES_URL ||
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
  await client.query(`
    CREATE TABLE IF NOT EXISTS codes (
      code TEXT PRIMARY KEY,
      used BOOLEAN NOT NULL DEFAULT FALSE,
      bound_uuid TEXT,
      bound_hwid TEXT,
      created_at BIGINT NOT NULL,
      used_at BIGINT
    );
  `);
}

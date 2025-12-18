export default async function handler(_req, res) {
  res.status(200).json({
    ok: true,
    vercel_env: process.env.VERCEL_ENV || null,
    git_sha: process.env.VERCEL_GIT_COMMIT_SHA || null,
    git_ref: process.env.VERCEL_GIT_COMMIT_REF || null
  });
}

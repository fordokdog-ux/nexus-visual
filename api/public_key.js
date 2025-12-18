import { createPrivateKey, createPublicKey } from 'crypto';

function getPublicKeyX509Base64() {
  const pem = process.env.NV_PRIVATE_KEY_PEM;
  if (!pem) throw new Error('missing_private_key');

  const priv = createPrivateKey(pem);
  const pub = createPublicKey(priv);
  const der = pub.export({ type: 'spki', format: 'der' });
  return Buffer.from(der).toString('base64');
}

export default async function handler(req, res) {
  try {
    res.status(200).json({ public_key_x509_base64: getPublicKeyX509Base64() });
  } catch (e) {
    res.status(500).json({ error: e?.message || 'error' });
  }
}

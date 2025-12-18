import { generateKeyPairSync, createPublicKey } from 'crypto';

const { privateKey } = generateKeyPairSync('rsa', {
  modulusLength: 2048,
  publicExponent: 0x10001,
  publicKeyEncoding: { type: 'spki', format: 'pem' },
  privateKeyEncoding: { type: 'pkcs8', format: 'pem' }
});

const pubDer = createPublicKey(privateKey).export({ type: 'spki', format: 'der' });
const pubB64 = Buffer.from(pubDer).toString('base64');

console.log('=== NV_PRIVATE_KEY_PEM (put into Vercel env) ===');
console.log(privateKey.trim());
console.log();
console.log('=== PUBLIC_KEY_X509_BASE64 (paste into mod) ===');
console.log(pubB64);

const enc = new TextEncoder();
const decoder = new TextDecoder();
const random = (length: number) => crypto.getRandomValues(new Uint8Array(length));
const toB64 = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes));
const fromB64 = (text: string) => Uint8Array.from(atob(text), c => c.charCodeAt(0));

export type Vault = { salt: string; recoverySalt: string; passEnvelope: string; recoveryEnvelope: string };
export type Sealed = { cipher: Blob; wrappedKey: string; iv: string };

async function derive(secret: string, salt: Uint8Array) {
  const material = await crypto.subtle.importKey('raw', enc.encode(secret), 'PBKDF2', false, ['deriveKey']);
  return crypto.subtle.deriveKey({ name: 'PBKDF2', salt, iterations: 600_000, hash: 'SHA-256' }, material, { name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']);
}

async function wrapKey(key: CryptoKey, wrappingKey: CryptoKey) {
  const raw = await crypto.subtle.exportKey('raw', key);
  const iv = random(12);
  const encrypted = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, wrappingKey, raw);
  return `${toB64(iv)}.${toB64(new Uint8Array(encrypted))}`;
}

async function unwrapKey(envelope: string, wrappingKey: CryptoKey) {
  const [iv, cipher] = envelope.split('.');
  const raw = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: fromB64(iv) }, wrappingKey, fromB64(cipher));
  return crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt', 'decrypt']);
}

export async function createVault(password: string) {
  if (password.length < 10) throw new Error('Use at least 10 characters for your vault password.');
  const master = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt']);
  const salt = random(16); const recoverySalt = random(16); const recoveryKey = toB64(random(32));
  const vault: Vault = { salt: toB64(salt), recoverySalt: toB64(recoverySalt), passEnvelope: await wrapKey(master, await derive(password, salt)), recoveryEnvelope: await wrapKey(master, await derive(recoveryKey, recoverySalt)) };
  return { vault, master, recoveryKey };
}

export async function unlockVault(password: string, vault: Vault) {
  return unwrapKey(vault.passEnvelope, await derive(password, fromB64(vault.salt)));
}

export async function unlockRecovery(recoveryKey: string, vault: Vault) {
  return unwrapKey(vault.recoveryEnvelope, await derive(recoveryKey.replaceAll('-', '').trim(), fromB64(vault.recoverySalt)));
}

export async function deriveVaultPassword(password: string, master: CryptoKey) {
  if (password.length < 10) throw new Error('Use at least 10 characters for your new vault password.');
  const salt = random(16); const recoverySalt = random(16); const recoveryKey = toB64(random(32));
  const vault: Vault = { salt: toB64(salt), recoverySalt: toB64(recoverySalt), passEnvelope: await wrapKey(master, await derive(password, salt)), recoveryEnvelope: await wrapKey(master, await derive(recoveryKey, recoverySalt)) };
  return { vault, recoveryKey };
}

export async function encryptFile(file: File, master: CryptoKey): Promise<Sealed> {
  if (file.size === 0) throw new Error('This file is empty. Choose a file with content.');
  if (file.size > 80 * 1024 * 1024) throw new Error('Files larger than 80 MB are not supported yet.');
  const contentKey = await crypto.subtle.generateKey({ name: 'AES-GCM', length: 256 }, true, ['encrypt', 'decrypt']);
  const iv = random(12); const bytes = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, contentKey, await file.arrayBuffer());
  return { cipher: new Blob([bytes], { type: 'application/octet-stream' }), wrappedKey: await wrapKey(contentKey, master), iv: toB64(iv) };
}

export async function decryptFile(cipher: Blob, wrappedKey: string, iv: string, master: CryptoKey, mime: string, name: string) {
  const key = await unwrapKey(wrappedKey, master);
  const bytes = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: fromB64(iv) }, key, await cipher.arrayBuffer());
  return new File([bytes], name, { type: mime });
}

export function recoveryWords(key: string) { return key.match(/.{1,4}/g)?.join('-') ?? key; }

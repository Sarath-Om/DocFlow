# Share viewer on Firebase Hosting

The static viewer is in `public/n/`. Its URL will be `https://gen-lang-client-0540286763.web.app/n/` after deployment. Hosting serves the share page only; it does not store note data or need the Firebase JavaScript SDK. The public Firebase web config values are not used here because the page makes no Firebase client requests.

## Deploy from Windows PowerShell

Run from the DocFlow repository directory:

```powershell
npm install -g firebase-tools
firebase login
firebase deploy --only hosting
```

The repository already has `.firebaserc` pointing to `gen-lang-client-0540286763` and `firebase.json` configured for the `public` directory. A separate `firebase init` is unnecessary and may offer to replace those files. If you need to initialize on a fresh checkout, choose **Hosting**, select the existing project `gen-lang-client-0540286763`, set the public directory to `public`, and do not configure a single-page-app rewrite.

## Custom domain

The configured share links use `https://docflow.free.je/n/`. Firebase Hosting must first have that domain added in the Firebase Console under Hosting, and its DNS records must be updated as Firebase directs. Deployment alone does not move a domain from another host. Until domain verification and DNS changes are complete, test with the `web.app` address.

## Share payload format expected by the page

The URL fragment is `#v1.<base64url(deflate(UTF-8 JSON))>`. For an unencrypted note, JSON may be `{ "v": 1, "n": { "t": "title", "b": "body", "c": "ISO date" } }`. `e` is an optional expiry timestamp in milliseconds (use a value greater than the current time; omit it for no expiry). For password-protected notes, use `{ "v": 1, "x": "A256GCM", "s": "base64url salt", "i": "base64url 12-byte IV", "d": "base64url AES-GCM ciphertext" }`. The ciphertext is the JSON note object `{ "t", "b", "c", "e" }`, encrypted with AES-256-GCM using PBKDF2-HMAC-SHA256, 250,000 iterations, and the password supplied separately. Use at least 16 random salt bytes. The viewer limits compressed link data to 120 KB and inflated JSON to 210 KB; the app should enforce the requested 6,000-character sharing threshold before constructing a link.

## Still required for full app-link integration

The viewer intentionally has no “Open in DocFlow” control until the Android package name, signing-certificate SHA-256 fingerprint, and app-side note-import route are known. To add verified Android App Links, publish `.well-known/assetlinks.json` with those exact values and implement validation/import in the app. Those values must come from the signed Android build; do not guess them.

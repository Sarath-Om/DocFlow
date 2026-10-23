# reCAPTCHA Enterprise setup

The assessment endpoint is `POST /api/recaptcha-assessment`. It accepts only the `view_note`, `unlock_note`, and `download_note` actions, validates the token action and hostname, and requires a score of at least 0.5. It never receives the note body or password.

Before deployment:

1. Enable the reCAPTCHA Enterprise API in Google Cloud for `gen-lang-client-0540286763` and make sure the project has billing enabled. Firebase Cloud Functions deployment requires a billing-enabled project.
2. Confirm the site key `6LdLSP0tAAAAAKmEkF_8tcs9H3DlXSobms41to3_` is an Enterprise score-based key enabled for the actual Hosting domains: `docflow.free.je`, `gen-lang-client-0540286763.web.app`, and `gen-lang-client-0540286763.firebaseapp.com`.
3. Grant the deployed function's runtime service account `roles/recaptchaenterprise.agent` on the project. The function uses Google Application Default Credentials; do not create or commit a service-account key.
4. Deploy with `firebase deploy --only functions,hosting` after installing dependencies from `functions/package.json`.

The browser integration requests a fresh token for `view_note`, `unlock_note`, and `download_note`, sends only the token and action to the same-origin assessment endpoint, and proceeds only when it returns `{ "ok": true }`. Assessment tokens are single-use and expire after two minutes. The owner approved loading Google's script on the share page; the note is encoded in `location.hash`, which page scripts can access. The assessment Function itself receives no note body or password.

The current workspace command runner is unavailable, so the API enablement, IAM grant, installation, local emulator check, and deployment have not been performed.

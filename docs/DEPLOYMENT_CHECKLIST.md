# DocFlow Static Website — Deployment & Pre-Publish Checklist

## 1. Deliverables Overview
- `/docs/index.html` — Home page (`/`) with Hero, CSS/SVG Phone Mockups (Recent, Tools, Manage), 8-Card Features Grid, Local Storage Privacy Section, 4-Step Android Sideload Guide, Conditional SHA-256 Checksum Box, Accessible `<details>` FAQ Accordion, and Final CTA.
- `/docs/privacy.html` & `/docs/privacy/index.html` — Privacy Policy (`/privacy`) with all 14 plain-language sections and Table of Contents.
- `/docs/terms.html` & `/docs/terms/index.html` — Terms of Service (`/terms`) with all 14 sections and Table of Contents.
- `/docs/config.js` — Single centralized configuration file holding all release metadata and legal placeholders.
- `/docs/styles.css` — Flat, minimal stylesheet (`#F0F1F3` light / `#0F1113` dark, `#FFFFFF` / `#1B1D20` cards with `24px` radius, `#1F6BFF` accent blue, zero glassmorphism/blur, responsive from `320px` to `1280px+`).
- `/docs/app.js` — Config renderer, unfilled placeholder highlighter banner, SHA-256 copy button, and `localStorage` dark mode toggle.
- `/docs/favicon.svg`, `/docs/sitemap.xml`, `/docs/robots.txt`.

---

## 2. GitHub Pages Deployment Steps
1. **Commit and push** the `/docs` directory to the `main` branch of your GitHub repository (`https://github.com/Sarath-Om/DocFlow`).
2. Open your repository on GitHub and navigate to **Settings → Pages** (in the left sidebar under "Code and automation").
3. Under **Build and deployment → Source**, select **Deploy from a branch**.
4. Under **Branch**, select `main` and choose the **`/docs`** folder from the folder dropdown, then click **Save**.
5. Wait ~60 seconds for GitHub Actions `pages-build-deployment` to complete. Your site will be live at:
   - Home: `https://sarath-om.github.io/DocFlow/`
   - Privacy Policy: `https://sarath-om.github.io/DocFlow/privacy/` (and `/privacy.html`)
   - Terms of Service: `https://sarath-om.github.io/DocFlow/terms/` (and `/terms.html`)

---

## 3. Pre-Publish Verification Checklist (`docs/config.js`)
Before making your site public, open `docs/config.js` and fill in every placeholder below. Any unfilled `[...]` value is automatically highlighted in yellow on the page and listed in the top build audit banner:

- [ ] `VERSION`: Replace `"[VERSION, e.g. 2.4.0]"` with the exact APK `versionName` from `app/build.gradle.kts`.
- [ ] `SIZE_MB`: Replace `"[SIZE_MB, e.g. 18.4 MB]"` with the built APK size (e.g., `ls -lh app/build/outputs/apk/release/DocFlow.apk`).
- [ ] `RELEASE_DATE`: Replace `"[RELEASE_DATE, e.g. October 22, 2026]"` with the release date.
- [ ] `SHA256_CHECKSUM`: Run `sha256sum DocFlow.apk` on your signed release APK and paste the 64-character hex string into `SHA256_CHECKSUM`. (While empty `""`, the checksum box stays hidden automatically.)
- [ ] `DEVELOPER_NAME`: Replace `"[DEVELOPER / ORGANIZATION NAME]"` with your legal name or organization name.
- [ ] `CONTACT_EMAIL`: Replace `"[EMAIL]"` with your support/privacy contact email address.
- [ ] `EFFECTIVE_DATE` & `LAST_UPDATED_DATE`: Replace `"[DATE]"` with the effective date of the Privacy Policy and Terms.
- [ ] `GOVERNING_CITY_STATE`: Replace `"[CITY, STATE]"` with your city and state in India for judicial jurisdiction.
- [ ] `OPEN_SOURCE_LICENSE`: Replace `"[OPEN-SOURCE LICENSE, if any]"` with your license (e.g., `"Apache License 2.0"`, `"MIT License"`, or `"Proprietary — All Rights Reserved"`).
- [ ] Verify that `https://github.com/Sarath-Om/DocFlow/releases/download/DocFlow/DocFlow.apk` resolves to the latest uploaded APK asset on GitHub Releases.

---

## 4. Real App Behavior vs. Policy Audit Report
| Policy Area | Real Android App Behavior (`app/src/main/...`) | Audit Action Taken |
| :--- | :--- | :--- |
| **Android Backup (`allowBackup`)** | `AndroidManifest.xml` previously had `android:allowBackup="true"`, which could allow automatic OS cloud backups without user initiation. | Updated `AndroidManifest.xml` to `android:allowBackup="false"` so backups/exports occur strictly when initiated by the user. |
| **Permissions** | Declares `CAMERA` (`required="false"`), `USE_BIOMETRIC`, `INTERNET`, and `ACCESS_NETWORK_STATE`. Zero broad storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE` absent). | Verified 100% match with Privacy Policy Section 5 (Camera, System Picker SAF, Biometrics). |
| **On-Device OCR & Processing** | Uses bundled `com.google.mlkit:text-recognition:16.0.1` and local `PdfDocument` / `PdfRenderer` / Room FTS4 (`docflow_local_cache.db`). | Verified 100% match with Privacy Policy Sections 4, 6, and 7. |
| **Encryption** | `CryptoUtils.kt` uses AES-256 (`AES/CBC/PKCS5Padding` with `PBKDF2WithHmacSHA256`, 65,536 iterations, random 16-byte salt & IV) + `androidx.biometric`. | Verified 100% match with Privacy Policy Section 8. |

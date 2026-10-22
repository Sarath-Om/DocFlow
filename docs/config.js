/**
 * DocFlow Static Website Configuration & Placeholders
 * ====================================================
 * Update all values in this single file before publishing to GitHub Pages.
 * Any value still wrapped in square brackets (e.g. "[EMAIL]") or left empty
 * will be automatically highlighted in the page audit banner and logged in
 * the console so you cannot accidentally publish unfilled placeholders.
 */
window.DOCFLOW_CONFIG = {
  // 1. App Release Metadata (Displayed on Home Hero & Install Section)
  APP_NAME: "DocFlow",
  DOWNLOAD_URL: "https://github.com/Sarath-Om/DocFlow/releases/download/DocFlow/DocFlow.apk",
  GITHUB_RELEASES_URL: "https://github.com/Sarath-Om/DocFlow/releases",
  GITHUB_REPO_URL: "https://github.com/Sarath-Om/DocFlow",
  MIN_ANDROID: "Android 8.0+",
  VERSION: "[VERSION, e.g. 2.4.0]",
  SIZE_MB: "[SIZE_MB, e.g. 18.4 MB]",
  RELEASE_DATE: "[RELEASE_DATE, e.g. October 22, 2026]",

  // 2. APK Integrity Checksum (If empty "", the SHA-256 verification box is hidden)
  // Replace with the output of: sha256sum DocFlow.apk
  SHA256_CHECKSUM: "",

  // 3. Legal, Privacy Policy & Terms of Service Placeholders
  DEVELOPER_NAME: "[DEVELOPER / ORGANIZATION NAME]",
  CONTACT_EMAIL: "[EMAIL]",
  EFFECTIVE_DATE: "[DATE]",
  LAST_UPDATED_DATE: "[DATE]",
  GOVERNING_CITY_STATE: "[CITY, STATE]",
  OPEN_SOURCE_LICENSE: "[OPEN-SOURCE LICENSE, if any]"
};

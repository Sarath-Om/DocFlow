"use strict";

const { onRequest } = require("firebase-functions/v2/https");
const { RecaptchaEnterpriseServiceClient } = require("@google-cloud/recaptcha-enterprise");

const SITE_KEY = "6LdLSP0tAAAAAKmEkF_8tcs9H3DlXSobms41to3_";
const PROJECT_ID = process.env.GCLOUD_PROJECT || "gen-lang-client-0540286763";
const ALLOWED_HOSTS = new Set([
  "docflow.free.je",
  "gen-lang-client-0540286763.web.app",
  "gen-lang-client-0540286763.firebaseapp.com",
]);
const ALLOWED_ACTIONS = new Set(["view_note", "unlock_note", "download_note"]);
const MIN_SCORE = Number(process.env.RECAPTCHA_MIN_SCORE || "0.5");
const client = new RecaptchaEnterpriseServiceClient();

exports.recaptchaAssessment = onRequest(
  { region: "us-central1", timeoutSeconds: 15, memory: "256MiB", maxInstances: 10 },
  async (req, res) => {
    res.set("Cache-Control", "no-store");
    res.set("X-Content-Type-Options", "nosniff");
    if (req.method !== "POST") return res.status(405).json({ ok: false, error: "method_not_allowed" });

    const origin = req.get("origin");
    if (!origin) return res.status(403).json({ ok: false, error: "origin_not_allowed" });
    let originHost;
    try {
      const parsed = new URL(origin);
      if (parsed.protocol !== "https:" && parsed.hostname !== "localhost") {
        return res.status(403).json({ ok: false, error: "origin_not_allowed" });
      }
      originHost = parsed.hostname.toLowerCase();
    } catch {
      return res.status(403).json({ ok: false, error: "origin_not_allowed" });
    }
    if (!ALLOWED_HOSTS.has(originHost) && originHost !== "localhost") {
      return res.status(403).json({ ok: false, error: "origin_not_allowed" });
    }

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const token = typeof body.token === "string" ? body.token : "";
    const action = typeof body.action === "string" ? body.action : "";
    if (!token || token.length > 8192 || !ALLOWED_ACTIONS.has(action)) {
      return res.status(400).json({ ok: false, error: "invalid_request" });
    }

    try {
      const [assessment] = await client.createAssessment({
        parent: client.projectPath(PROJECT_ID),
        assessment: {
          event: {
            token,
            siteKey: SITE_KEY,
            expectedAction: action,
          },
        },
      });
      const properties = assessment.getTokenProperties();
      const expectedHost = ALLOWED_HOSTS.has(originHost) ? originHost : "localhost";
      const valid = properties.getValid() &&
        properties.getAction() === action &&
        (ALLOWED_HOSTS.has(properties.getHostname()) || properties.getHostname() === "localhost") &&
        properties.getHostname() === expectedHost;
      const score = assessment.getRiskAnalysis()?.getScore() ?? 0;
      if (!valid || score < MIN_SCORE) {
        return res.status(403).json({ ok: false, error: "verification_failed" });
      }
      return res.status(200).json({ ok: true });
    } catch (error) {
      console.error("reCAPTCHA assessment failed", error?.message || "unknown error");
      return res.status(503).json({ ok: false, error: "verification_unavailable" });
    }
  }
);

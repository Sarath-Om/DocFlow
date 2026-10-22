/**
 * DocFlow Static Website Script
 * - Hydrates centralized config values from window.DOCFLOW_CONFIG
 * - Highlights any unfilled [PLACEHOLDER] values so they cannot be overlooked
 * - Handles dark/light mode toggle persisted in localStorage
 * - Manages SHA-256 checksum visibility and copy-to-clipboard
 */
(function () {
  "use strict";

  var cfg = window.DOCFLOW_CONFIG || {};
  var THEME_STORAGE_KEY = "docflow_site_theme";

  function isUnfilledPlaceholder(val) {
    if (val === null || val === undefined) return true;
    var str = String(val).trim();
    return str === "" || (str.charAt(0) === "[" && str.charAt(str.length - 1) === "]");
  }

  // 1. Theme Initialization & Manual Toggle
  function getEffectiveTheme() {
    try {
      var saved = localStorage.getItem(THEME_STORAGE_KEY);
      if (saved === "dark" || saved === "light") return saved;
    } catch (e) {
      // Ignore storage access errors in strict privacy browsers
    }
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function applyTheme(theme, persist) {
    document.documentElement.setAttribute("data-theme", theme);
    if (persist) {
      try {
        localStorage.setItem(THEME_STORAGE_KEY, theme);
      } catch (e) {}
    }
    var toggleBtn = document.getElementById("theme-toggle-btn");
    if (toggleBtn) {
      var labelSpan = toggleBtn.querySelector(".theme-toggle-label");
      var isDark = theme === "dark";
      toggleBtn.setAttribute("aria-pressed", isDark ? "true" : "false");
      if (labelSpan) {
        labelSpan.textContent = isDark ? "Light mode" : "Dark mode";
      }
    }
  }

  // Apply stored theme immediately if set
  try {
    var storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
    if (storedTheme === "dark" || storedTheme === "light") {
      document.documentElement.setAttribute("data-theme", storedTheme);
    }
  } catch (e) {}

  document.addEventListener("DOMContentLoaded", function () {
    // Wire theme toggle button
    var toggleBtn = document.getElementById("theme-toggle-btn");
    if (toggleBtn) {
      applyTheme(getEffectiveTheme(), false);
      toggleBtn.addEventListener("click", function () {
        var current = getEffectiveTheme();
        var next = current === "dark" ? "light" : "dark";
        applyTheme(next, true);
      });
    }

    // 2. Render Config Values & Track Unfilled Placeholders
    var unfilledKeys = [];
    var configNodes = document.querySelectorAll("[data-config]");
    for (var i = 0; i < configNodes.length; i++) {
      var node = configNodes[i];
      var key = node.getAttribute("data-config");
      if (!key || !(key in cfg)) continue;

      var rawValue = cfg[key];
      if (isUnfilledPlaceholder(rawValue)) {
        if (unfilledKeys.indexOf(key) === -1) unfilledKeys.push(key);
        node.textContent = rawValue || "[" + key + "]";
        node.classList.add("placeholder-flag");
        node.setAttribute("title", "Unfilled placeholder in config.js: " + key);
      } else {
        node.textContent = String(rawValue);
        node.classList.remove("placeholder-flag");
      }
    }

    // Update mailto links if CONTACT_EMAIL is filled
    var mailtoNodes = document.querySelectorAll("[data-config-mailto]");
    for (var m = 0; m < mailtoNodes.length; m++) {
      var mailNode = mailtoNodes[m];
      var emailVal = cfg.CONTACT_EMAIL || "[EMAIL]";
      if (!isUnfilledPlaceholder(emailVal)) {
        mailNode.setAttribute("href", "mailto:" + emailVal);
        mailNode.textContent = emailVal;
        mailNode.classList.remove("placeholder-flag");
      } else {
        if (unfilledKeys.indexOf("CONTACT_EMAIL") === -1) unfilledKeys.push("CONTACT_EMAIL");
        mailNode.removeAttribute("href");
        mailNode.textContent = emailVal;
        mailNode.classList.add("placeholder-flag");
      }
    }

    // Ensure all download buttons point to the exact DOWNLOAD_URL
    if (cfg.DOWNLOAD_URL) {
      var dlLinks = document.querySelectorAll("[data-download-link]");
      for (var d = 0; d < dlLinks.length; d++) {
        dlLinks[d].setAttribute("href", cfg.DOWNLOAD_URL);
      }
    }

    // 3. SHA-256 Checksum Box Visibility & Copy Button
    var checksumContainer = document.getElementById("checksum-container");
    var checksumValueEl = document.getElementById("checksum-value");
    var copyChecksumBtn = document.getElementById("copy-checksum-btn");
    var rawChecksum = cfg.SHA256_CHECKSUM ? String(cfg.SHA256_CHECKSUM).trim() : "";

    if (checksumContainer) {
      if (rawChecksum !== "" && !isUnfilledPlaceholder(rawChecksum)) {
        checksumContainer.removeAttribute("hidden");
        if (checksumValueEl) {
          checksumValueEl.textContent = rawChecksum;
        }
        if (copyChecksumBtn && navigator.clipboard) {
          copyChecksumBtn.addEventListener("click", function () {
            navigator.clipboard.writeText(rawChecksum).then(function () {
              copyChecksumBtn.textContent = "Copied!";
              setTimeout(function () {
                copyChecksumBtn.textContent = "Copy SHA-256";
              }, 2000);
            });
          });
        }
      } else {
        checksumContainer.setAttribute("hidden", "hidden");
        if (unfilledKeys.indexOf("SHA256_CHECKSUM (hidden until set)") === -1) {
          unfilledKeys.push("SHA256_CHECKSUM (hidden until set)");
        }
      }
    }

    // 4. Show Build Audit Banner if any placeholder is unfilled
    var auditBanner = document.getElementById("placeholder-audit-banner");
    if (auditBanner) {
      if (unfilledKeys.length > 0) {
        auditBanner.removeAttribute("hidden");
        var listEl = document.getElementById("placeholder-audit-list");
        if (listEl) {
          listEl.textContent = unfilledKeys.join(", ");
        }
      } else {
        auditBanner.setAttribute("hidden", "hidden");
      }
    }
  });
})();

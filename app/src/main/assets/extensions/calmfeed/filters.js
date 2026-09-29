(() => {
  "use strict";

  // =========================================================================
  // 1. TIKTOK BROWSER STREAMING ENHANCEMENT: PERMANENT APP PROMPT ELIMINATOR
  // =========================================================================
  const isTikTok = location.hostname.includes("tiktok.com");

  if (isTikTok) {
    const injectTikTokStyles = () => {
      if (document.getElementById("calmfeed-tiktok-killer")) return;
      const style = document.createElement("style");
      style.id = "calmfeed-tiktok-killer";
      style.textContent = `
        /* Permanently hide all TikTok Open App banners, dialogs, modals, and login overlays */
        [data-e2e*="open-app"],
        [data-e2e*="app-banner"],
        [data-e2e*="modal"],
        .tiktok-open-app,
        .open-app-banner,
        .open-in-app,
        .download-bar,
        .app-download-bar,
        div[class*="DivModalContainer"],
        div[class*="DivOverlay"],
        div[class*="DivDialogContainer"],
        div[class*="DivAppBanner"],
        div[class*="DivBottomBannerContainer"],
        div[class*="DivBottomContainer"],
        div[class*="DivOpenAppButton"],
        div[class*="DivLoginContainer"],
        div[class*="DivPromptContainer"],
        div[class*="DivFloatingCard"],
        div[class*="DivTopBannerContainer"],
        div[class*="guide-box"],
        div[class*="mask-layer"],
        div[class*="tiktok-web-player-popup"],
        div[class*="DivModalWrapper"],
        a[href*="snssdk"],
        a[href*="tiktok.com/download"],
        a[href*="open_app"],
        button[class*="openApp"],
        div[role="dialog"] button[class*="openApp"],
        div[role="dialog"]:has(a[href*="download"]) {
          display: none !important;
          opacity: 0 !important;
          visibility: hidden !important;
          pointer-events: none !important;
          height: 0 !important;
          width: 0 !important;
          max-height: 0 !important;
          overflow: hidden !important;
          position: absolute !important;
          z-index: -99999 !important;
        }

        /* Guarantee html and body never get locked or frozen by TikTok */
        html, body {
          overflow: auto !important;
          position: static !important;
          -webkit-overflow-scrolling: touch !important;
        }
      `;
      (document.head || document.documentElement).appendChild(style);
    };

    injectTikTokStyles();

    const nukeTikTokPopups = () => {
      injectTikTokStyles();

      // Unlock scrolling if TikTok locked it
      if (document.body) {
        if (document.body.style.overflow === "hidden") {
          document.body.style.removeProperty("overflow");
        }
        if (document.body.style.position === "fixed") {
          document.body.style.removeProperty("position");
        }
      }
      if (document.documentElement && document.documentElement.style.overflow === "hidden") {
        document.documentElement.style.removeProperty("overflow");
      }

      // Find and remove modal containers and overlays
      const modalSelectors = [
        "[data-e2e='modal-container']",
        "div[class*='DivModalContainer']",
        "div[class*='DivOverlay']",
        "div[class*='DivDialogContainer']",
        "div[class*='DivAppBanner']",
        "div[class*='DivBottomBannerContainer']",
        "div[class*='DivPromptContainer']",
        "div[class*='DivLoginContainer']",
        "div[class*='DivModalWrapper']",
        "div[class*='mask-layer']",
        "div[class*='guide-box']"
      ];
      for (const sel of modalSelectors) {
        const els = document.querySelectorAll(sel);
        for (const el of els) {
          // If modal contains a close button, auto-click it
          const closeBtn = el.querySelector("[data-e2e='modal-close-icon'], button[class*='close'], button[aria-label*='Close'], div[class*='Close']");
          if (closeBtn) {
            try { closeBtn.click(); } catch (_) {}
          }
          try { el.remove(); } catch (_) {}
        }
      }

      // Check text of fixed/absolute elements for 'Open app' / 'Open TikTok' / 'Watch in app'
      const candidates = document.querySelectorAll("div, section");
      for (const el of candidates) {
        if (el.children.length <= 4) {
          const txt = (el.innerText || "").trim().toLowerCase();
          if (
            (txt.includes("open app") || txt.includes("open tiktok") || txt.includes("watch in app") || txt.includes("get the app")) &&
            (txt.length < 80)
          ) {
            try {
              const pos = window.getComputedStyle(el).position;
              if (pos === "fixed" || pos === "absolute" || el.className.includes("Modal") || el.className.includes("Banner")) {
                el.remove();
              }
            } catch (_) {}
          }
        }
      }
    };

    window.setInterval(nukeTikTokPopups, 350);
    document.addEventListener("DOMContentLoaded", nukeTikTokPopups);
    window.addEventListener("load", nukeTikTokPopups);

    // Block synthetic redirects to native app schemes (snssdk://, tiktok://, intent://)
    const originalAssign = window.location.assign;
    if (typeof originalAssign === "function") {
      window.location.assign = function(url) {
        if (typeof url === "string" && (url.startsWith("snssdk") || url.startsWith("tiktok:") || url.startsWith("intent:"))) {
          console.info("CalmFeed suppressed TikTok app intent:", url);
          return;
        }
        return originalAssign.call(window.location, url);
      };
    }

    // Intercept clicks on links attempting to trigger app downloads or app schemes
    document.addEventListener("click", (e) => {
      const anchor = e.target.closest("a");
      if (anchor && anchor.href) {
        const href = anchor.href.toLowerCase();
        if (href.startsWith("snssdk") || href.startsWith("tiktok:") || href.startsWith("intent:") || href.includes("tiktok.com/download")) {
          e.preventDefault();
          e.stopPropagation();
          console.info("CalmFeed suppressed TikTok app link click:", anchor.href);
        }
      }
    }, true);
  }

  // =========================================================================
  // 2. MINDFUL CALM FEED FILTERS FOR YOUTUBE & INSTAGRAM
  // =========================================================================
  const reelSelectors = [
    "ytd-reel-shelf-renderer",
    "ytd-rich-section-renderer:has(ytd-reel-shelf-renderer)",
    "ytd-guide-entry-renderer a[title='Shorts']",
    "a[href^='/shorts/']",
    "ytd-compact-video-renderer:has(a[href^='/shorts/'])",
    "yt-lockup-view-model:has(a[href^='/shorts/'])",
    "ytm-rich-item-renderer:has(a[href^='/shorts/'])",
    "ytm-video-with-context-renderer:has(a[href^='/shorts/'])",
    "ytm-reel-item-renderer",
    "ytm-reel-shelf-renderer",
    "ytm-pivot-bar-item-renderer[tab-title='Shorts']",
    ".pivot-bar-item-tab[href^='/shorts']",
    "ytm-pivot-bar-item-renderer[tab-identifier='FEshorts']"
  ];
  const recommendationSelectors = [
    "#related",
    "ytd-watch-next-secondary-results-renderer",
    "ytd-compact-autoplay-renderer",
    "ytd-browse[page-subtype='home'] ytd-rich-grid-renderer",
    "ytm-watch-next-secondary-results-renderer",
    "ytm-single-column-watch-next-results-renderer",
    "ytm-item-section-renderer[section-identifier='related-items']",
    "#watch-more-related",
    "ytm-compact-autoplay-renderer",
    "ytm-browse[page-subtype='home'] ytm-rich-grid-renderer",
    "ytm-browse[page-subtype='home'] ytm-item-section-renderer"
  ];
  let lastScrollAt = 0;
  let touchStartY = null;
  let limitOverlay = null;
  let shortVideosPage = false;
  let accessBlocked = false;
  let lastShortVideoId = null;
  let lastGestureAt = 0;

  function hideMatches(root, selectors) {
    if (!(root instanceof Element || root instanceof Document)) return;
    for (const selector of selectors) {
      try {
        const matches = [];
        if (root instanceof Element && root.matches(selector)) matches.push(root);
        matches.push(...root.querySelectorAll(selector));
        for (const node of matches) {
          const target = node.closest(
            "ytd-reel-shelf-renderer, ytd-guide-entry-renderer, ytd-rich-item-renderer, ytm-rich-item-renderer, ytm-pivot-bar-item-renderer, .pivot-bar-item-tab, article, [role='article'], [role='listitem']"
          ) || node;
          target.style.setProperty("display", "none", "important");
        }
      } catch (error) {
        console.warn("CalmFeed filter warning:", selector, error);
      }
    }
  }

  // Social Sites Check: YouTube, Facebook, TikTok, Instagram
  function isSocialSite() {
    const host = location.hostname.toLowerCase();
    return host.includes("youtube.com") || host.includes("facebook.com") || host.includes("tiktok.com") || host.includes("instagram.com");
  }

  // Daily short-video limits strictly enforced on YouTube, Facebook, TikTok, Instagram!
  // General Browsing Mode is 100% unlimited.
  function isShortVideosPage() {
    if (!isSocialSite()) return false;
    const host = location.hostname.toLowerCase();
    const path = location.pathname.toLowerCase();
    if (host.includes("youtube.com")) {
      return path.startsWith("/shorts");
    }
    if (host.includes("instagram.com")) {
      return path.startsWith("/reels") || path.startsWith("/reel");
    }
    if (host.includes("facebook.com")) {
      return path.includes("/reel") || path.includes("/watch");
    }
    if (host.includes("tiktok.com")) {
      return path.includes("/video/") || path.includes("/foryou") || path === "/" || path === "";
    }
    return false;
  }

  function showLimitOverlay(count, unavailable = false) {
    if (limitOverlay || !document.body) return;
    accessBlocked = true;

    limitOverlay = document.createElement("div");
    limitOverlay.setAttribute("role", "dialog");
    limitOverlay.setAttribute("aria-modal", "true");
    Object.assign(limitOverlay.style, {
      position: "fixed",
      inset: "0",
      zIndex: "2147483647",
      display: "flex",
      flexDirection: "column",
      justifyContent: "center",
      alignItems: "center",
      boxSizing: "border-box",
      padding: "32px",
      background: "#0c1418",
      color: "#eff6f0",
      fontFamily: "system-ui, sans-serif",
      textAlign: "center"
    });
    limitOverlay.style.touchAction = "none";

    const title = document.createElement("h1");
    title.textContent = unavailable
      ? "Daily social video limit unavailable."
      : "That’s enough social videos for today.";
    Object.assign(title.style, { fontSize: "26px", lineHeight: "1.2", maxWidth: "420px" });

    const detail = document.createElement("p");
    detail.textContent = unavailable
      ? "CalmFeed couldn’t verify today’s social video limit. Return Home and try again."
      : `You’ve reached your daily limit for short videos on social sites (${count} videos). Take a breath, or switch to Browsing Mode for articles and search.`;
    Object.assign(detail.style, {
      color: "#b4c5bb",
      fontSize: "15px",
      lineHeight: "1.5",
      maxWidth: "360px"
    });

    const browseBtn = document.createElement("button");
    browseBtn.textContent = "🌐 Switch to Browsing Mode";
    Object.assign(browseBtn.style, {
      marginTop: "20px",
      padding: "12px 24px",
      background: "#10b981",
      color: "#ffffff",
      border: "none",
      borderRadius: "24px",
      fontSize: "14px",
      fontWeight: "bold",
      cursor: "pointer"
    });
    browseBtn.onclick = () => {
      location.href = "https://www.google.com";
    };

    const tip = document.createElement("p");
    tip.textContent = "Browsing Mode (search, news, encyclopedia) is unlimited.";
    Object.assign(tip.style, { color: "#91e0b1", fontSize: "13px", marginTop: "16px" });

    limitOverlay.append(title, detail, browseBtn, tip);
    document.body.append(limitOverlay);
  }

  function requestScrollCount(increment) {
    if (!shortVideosPage) return;
    browser.runtime.sendMessage({ type: "calmfeed:reel-scroll", increment })
      .then((result) => {
        if (!result || typeof result.count !== "number" || typeof result.limit !== "number") {
          showLimitOverlay(0, true);
          return;
        }
        if (result.count >= result.limit) showLimitOverlay(result.limit);
      })
      .catch((error) => {
        console.error("CalmFeed limit check failed:", error);
      });
  }

  function countScrollGesture() {
    if (!shortVideosPage || accessBlocked) return;
    const now = Date.now();
    if (now - lastScrollAt < 350) return;
    lastScrollAt = now;
    lastGestureAt = now;
    requestScrollCount(true);
  }

  function currentShortVideoId() {
    const host = location.hostname.toLowerCase();
    if (host.includes("tiktok.com")) {
      const match = location.pathname.match(/\/video\/(\d+)/i);
      return match ? match[1] : location.pathname;
    }
    const match = location.pathname.match(/^\/(?:shorts|reels?)\/([^/?#]+)/i);
    return match ? match[1] : null;
  }

  function syncShortVideosPage() {
    const active = isShortVideosPage();
    if (active !== shortVideosPage) {
      shortVideosPage = active;
      if (active) {
        lastShortVideoId = currentShortVideoId();
        requestScrollCount(false);
      } else {
        lastShortVideoId = null;
        if (limitOverlay) {
          limitOverlay.remove();
          limitOverlay = null;
          accessBlocked = false;
        }
      }
      return;
    }

    if (!active) return;
    const videoId = currentShortVideoId();
    if (videoId && lastShortVideoId && videoId !== lastShortVideoId
        && Date.now() - lastGestureAt > 500) {
      requestScrollCount(true);
    }
    if (videoId) {
      lastShortVideoId = videoId;
    }
    if (!lastShortVideoId) {
      lastShortVideoId = videoId;
    }
  }

  function blockInteraction(event) {
    if (!accessBlocked) return;
    event.preventDefault();
    event.stopImmediatePropagation();
  }

  hideMatches(document, reelSelectors);
  hideMatches(document, recommendationSelectors);

  const observer = new MutationObserver((records) => {
    for (const record of records) {
      for (const node of record.addedNodes) {
        hideMatches(node, reelSelectors);
        hideMatches(node, recommendationSelectors);
      }
    }
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });

  syncShortVideosPage();
  window.addEventListener("popstate", syncShortVideosPage);
  window.addEventListener("yt-navigate-finish", syncShortVideosPage);
  window.setInterval(syncShortVideosPage, 350);

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden) syncShortVideosPage();
  });
  const originalPushState = history.pushState;
  history.pushState = function (...args) {
    originalPushState.apply(this, args);
    syncShortVideosPage();
  };
  const originalReplaceState = history.replaceState;
  history.replaceState = function (...args) {
    originalReplaceState.apply(this, args);
    syncShortVideosPage();
  };

  document.addEventListener("wheel", (event) => {
    if (event.deltaY > 10) countScrollGesture();
  }, { passive: true, capture: true });
  document.addEventListener("touchstart", (event) => {
    touchStartY = event.changedTouches.length ? event.changedTouches[0].clientY : null;
  }, { passive: true, capture: true });
  document.addEventListener("touchend", (event) => {
    if (touchStartY === null || !event.changedTouches.length) return;
    const endY = event.changedTouches[0].clientY;
    if (touchStartY - endY > 35) countScrollGesture();
    touchStartY = null;
  }, { passive: true, capture: true });
  document.addEventListener("touchcancel", () => {
    touchStartY = null;
  }, { passive: true, capture: true });
  document.addEventListener("keydown", (event) => {
    if (event.key === "ArrowDown" || event.key === "PageDown") countScrollGesture();
  }, true);
  document.addEventListener("touchmove", blockInteraction, { passive: false, capture: true });
  document.addEventListener("wheel", blockInteraction, { passive: false, capture: true });
  document.addEventListener("keydown", blockInteraction, true);
})();

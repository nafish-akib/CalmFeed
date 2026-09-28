(() => {
  "use strict";

  const reelSelectors = [
    "ytd-reel-shelf-renderer",
    "ytd-rich-section-renderer:has(ytd-reel-shelf-renderer)",
    "ytd-guide-entry-renderer a[title='Shorts']",
    "a[href^='/shorts/']",
    "a[href^='/reel/']",
    "a[href*='/reels/']",
    "ytd-compact-video-renderer:has(a[href^='/shorts/'])",
    "yt-lockup-view-model:has(a[href^='/shorts/'])",
    "ytm-rich-item-renderer:has(a[href^='/shorts/'])",
    "ytm-video-with-context-renderer:has(a[href^='/shorts/'])",
    "[aria-label='Reels']",
    "[aria-label='Short videos']",
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
        console.warn("CalmFeed could not apply a page filter:", selector, error);
      }
    }
  }

  function isReelsOrShortsPage() {
    return location.pathname.split("/").some((part) =>
      ["reel", "reels", "shorts"].includes(part.toLowerCase())
    );
  }

  function showLimitOverlay(count) {
    if (limitOverlay || !document.body) return;

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

    const title = document.createElement("h1");
    title.textContent = "That’s enough Reels for today.";
    Object.assign(title.style, { fontSize: "28px", lineHeight: "1.2", maxWidth: "420px" });

    const detail = document.createElement("p");
    detail.textContent = `You’ve reached your ${count}-scroll daily limit. Take a breath and choose what you’d like to do next.`;
    Object.assign(detail.style, {
      color: "#b4c5bb",
      fontSize: "16px",
      lineHeight: "1.5",
      maxWidth: "360px"
    });

    const tip = document.createElement("p");
    tip.textContent = "Use CalmFeed’s Home button to leave this site.";
    Object.assign(tip.style, { color: "#91e0b1", fontSize: "14px", marginTop: "20px" });

    limitOverlay.append(title, detail, tip);
    document.body.append(limitOverlay);
  }

  function requestScrollCount(increment) {
    if (!isReelsOrShortsPage()) return;

    browser.runtime.sendMessage({ type: "calmfeed:reel-scroll", increment })
      .then((result) => {
        if (result && result.count >= result.limit) showLimitOverlay(result.limit);
      })
      .catch((error) => console.error("CalmFeed could not update the Reels limit:", error));
  }

  function countScrollGesture() {
    const now = Date.now();
    if (now - lastScrollAt < 850) return;
    lastScrollAt = now;
    requestScrollCount(true);
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

  requestScrollCount(false);
  document.addEventListener("wheel", (event) => {
    if (event.deltaY > 10) countScrollGesture();
  }, { passive: true });
  document.addEventListener("touchstart", (event) => {
    touchStartY = event.changedTouches.length ? event.changedTouches[0].clientY : null;
  }, { passive: true });
  document.addEventListener("touchend", (event) => {
    if (touchStartY === null || !event.changedTouches.length) return;
    const endY = event.changedTouches[0].clientY;
    if (touchStartY - endY > 35) countScrollGesture();
    touchStartY = null;
  }, { passive: true });
  document.addEventListener("keydown", (event) => {
    if (event.key === "ArrowDown" || event.key === "PageDown") countScrollGesture();
  });
})();

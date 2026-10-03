"use strict";

const DAILY_REEL_LIMIT = 20;
const USAGE_KEY = "dailyReelScrolls";
let scrollCountQueue = Promise.resolve();

function localDateKey() {
  const date = new Date();
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0")
  ].join("-");
}

async function readTodayCount() {
  const stored = await browser.storage.local.get(USAGE_KEY);
  const usage = stored[USAGE_KEY];
  return usage && usage.date === localDateKey() ? usage.count : 0;
}

// =========================================================================
// AD & TRACKER BLOCKER (NETWORK LEVEL)
// =========================================================================
const AD_TRACKER_PATTERNS = [
  "*://*.doubleclick.net/*",
  "*://*.googlesyndication.com/*",
  "*://*.googleadservices.com/*",
  "*://*.adservice.google.com/*",
  "*://*.adnxs.com/*",
  "*://*.taboola.com/*",
  "*://*.outbrain.com/*",
  "*://*.scorecardresearch.com/*",
  "*://*.quantserve.com/*",
  "*://*.criteo.com/*",
  "*://*.criteo.net/*",
  "*://*.adroll.com/*",
  "*://*.advertising.com/*",
  "*://*.adcolony.com/*",
  "*://*.unityads.unity3d.com/*",
  "*://*.popads.net/*",
  "*://*.propellerads.com/*",
  "*://*.zedo.com/*",
  "*://*.revcontent.com/*",
  "*://*.chartbeat.net/*",
  "*://*.hotjar.com/*",
  "*://*.clarity.ms/*",
  "*://*.moatads.com/*",
  "*://*.ad-delivery.net/*"
];

let totalBlockedAds = 0;

if (typeof browser !== "undefined" && browser.webRequest && browser.webRequest.onBeforeRequest) {
  try {
    browser.webRequest.onBeforeRequest.addListener(
      (details) => {
        totalBlockedAds++;
        return { cancel: true };
      },
      { urls: AD_TRACKER_PATTERNS },
      ["blocking"]
    );
  } catch (err) {
    console.warn("CalmFeed could not initialize webRequest blocker:", err);
  }
}

browser.runtime.onMessage.addListener((message) => {
  if (!message || message.type !== "calmfeed:reel-scroll") return undefined;

  scrollCountQueue = scrollCountQueue.then(async () => {
    const count = await readTodayCount();
    if (message.increment !== true || count >= DAILY_REEL_LIMIT) {
      return { count, limit: DAILY_REEL_LIMIT };
    }

    const nextCount = count + 1;
    await browser.storage.local.set({
      [USAGE_KEY]: { date: localDateKey(), count: nextCount }
    });
    return { count: nextCount, limit: DAILY_REEL_LIMIT };
  });
  return scrollCountQueue;
});

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

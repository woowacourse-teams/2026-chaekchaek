import type { StrangerExperiment } from "./stranger-experiment";

const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;
const SEOUL_OFFSET_MS = 9 * HOUR_MS;

export type TrafficBucket = { key: string; label: string; arrivals: number; cumulative: number };

function seoulDay(timestamp: number) {
  return Math.floor((timestamp + SEOUL_OFFSET_MS) / DAY_MS);
}

function hourLabel(timestamp: number) {
  const local = new Date(timestamp + SEOUL_OFFSET_MS);
  return `${String(local.getUTCMonth() + 1).padStart(2, "0")}.${String(local.getUTCDate()).padStart(2, "0")} ${String(local.getUTCHours()).padStart(2, "0")}:00`;
}

function dayLabel(day: number) {
  const local = new Date(day * DAY_MS);
  return `${String(local.getUTCMonth() + 1).padStart(2, "0")}.${String(local.getUTCDate()).padStart(2, "0")}`;
}

export function summarizeReadingTraffic(data: StrangerExperiment, initialAuthorId: string, now = Date.now()) {
  const selected = data.participants.filter((person) => person.id !== initialAuthorId && person.readingStatus !== null);
  const timestamps = selected.map((person) => Date.parse(person.createdAt ?? ""))
    .filter((timestamp) => Number.isFinite(timestamp) && timestamp <= now);
  const hourlyStart = Math.floor(now / HOUR_MS) - 71;
  const hourCounts = new Map<number, number>();
  const dayCounts = new Map<number, number>();
  for (const timestamp of timestamps) {
    const hour = Math.floor(timestamp / HOUR_MS);
    const day = seoulDay(timestamp);
    hourCounts.set(hour, (hourCounts.get(hour) ?? 0) + 1);
    dayCounts.set(day, (dayCounts.get(day) ?? 0) + 1);
  }
  let hourlyCumulative = timestamps.filter((timestamp) => Math.floor(timestamp / HOUR_MS) < hourlyStart).length;
  const hourly: TrafficBucket[] = Array.from({ length: 72 }, (_, index) => {
    const hour = hourlyStart + index;
    const arrivals = hourCounts.get(hour) ?? 0;
    hourlyCumulative += arrivals;
    return { key: String(hour), label: hourLabel(hour * HOUR_MS), arrivals, cumulative: hourlyCumulative };
  });
  const today = seoulDay(now);
  const earliest = timestamps.reduce((first, timestamp) => Math.min(first, seoulDay(timestamp)), today);
  const firstDay = Math.min(earliest, today - 6);
  let dailyCumulative = 0;
  const daily: TrafficBucket[] = Array.from({ length: today - firstDay + 1 }, (_, index) => {
    const day = firstDay + index;
    const arrivals = dayCounts.get(day) ?? 0;
    dailyCumulative += arrivals;
    return { key: String(day), label: dayLabel(day), arrivals, cumulative: dailyCumulative };
  });
  return { visitors: selected.length, timedVisitors: timestamps.length, hourly, daily };
}

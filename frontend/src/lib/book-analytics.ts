import type { Book } from "./experiment";
import type { BookEvent } from "./cloud-experiment";

export type BookAnalyticsRow = {
  book: Book;
  views: number;
  visitors: number;
  totalDurationMs: number;
  averageDurationMs: number;
};

export function summarizeBookAnalytics(books: Book[], events: BookEvent[]): BookAnalyticsRow[] {
  return books.map((book) => {
    const bookEvents = events.filter((event) => event.bookId === book.id);
    const views = bookEvents.filter((event) => event.eventType === "view");
    const totalDurationMs = bookEvents
      .filter((event) => event.eventType === "dwell")
      .reduce((total, event) => total + event.durationMs, 0);
    return {
      book,
      views: views.length,
      visitors: new Set(views.map((event) => event.userId)).size,
      totalDurationMs,
      averageDurationMs: views.length ? Math.round(totalDurationMs / views.length) : 0,
    };
  });
}

export function formatDuration(durationMs: number): string {
  const seconds = Math.round(durationMs / 1000);
  if (seconds < 60) return `${seconds}초`;
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return remainder ? `${minutes}분 ${remainder}초` : `${minutes}분`;
}

export function bookAnalyticsCsv(rows: BookAnalyticsRow[]): string {
  const escape = (value: string | number) => `"${String(value).replaceAll('"', '""')}"`;
  return [
    ["책", "장르", "조회", "고유 방문자", "총 체류시간(초)", "평균 체류시간(초)"],
    ...rows.map((row) => [row.book.title, row.book.genre, row.views, row.visitors,
      Math.round(row.totalDurationMs / 1000), Math.round(row.averageDurationMs / 1000)]),
  ].map((row) => row.map(escape).join(",")).join("\n");
}

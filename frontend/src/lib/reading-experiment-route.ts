import { loadReadingExperiment, saveReadingMutation } from "./stranger-storage";
import { belongsToReadingBook, type ReadingBookId } from "./reading-book-config";
import type { StrangerMutation } from "./stranger-experiment";

const configured = () => Boolean(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY);
const valid = (value: unknown, max: number) => typeof value === "string" && value.trim().length > 0 && value.length <= max;
const id = (value: unknown) => valid(value, 100);
function validMutation(value: unknown, book: ReadingBookId): value is StrangerMutation {
  if (!value || typeof value !== "object") return false;
  const item = value as Record<string, unknown>;
  const scopedId = (value: unknown) => id(value) && belongsToReadingBook(book, String(value));
  if (item.type === "visit") return scopedId(item.userId) && valid(item.nickname, 100)
    && valid(item.source, 64) && /^[\p{L}\p{N}._-]+$/u.test(String(item.source))
    && ["iPhone", "iPad", "Android", "PC", "other"].includes(String(item.device));
  if (item.type === "reading") return scopedId(item.userId) && (item.readingStatus === "read" || item.readingStatus === "unread");
  if (item.type === "page") return scopedId(item.userId) && Number.isInteger(item.page) && Number(item.page) >= 1 && Number(item.page) <= 6;
  if (item.type === "composer") return scopedId(item.userId);
  if (item.type === "attention") {
    const event = item.event as Record<string, unknown> | undefined;
    const target = event?.target;
    const validTarget = typeof target === "string" && (
      /^(page:[1-6]|section:(context|excerpt|feed|composer)|action:page-move)$/.test(target)
      || (target.startsWith("open:") && scopedId(target.slice(5)))
    );
    return Boolean(event && scopedId(event.id) && scopedId(event.userId) && valid(event.createdAt, 40)
      && validTarget
      && (event.eventType === "view" || event.eventType === "dwell")
      && Number.isInteger(event.durationMs) && Number(event.durationMs) >= 0 && Number(event.durationMs) <= 300_000
      && (event.eventType === "view" ? event.durationMs === 0 : Number(event.durationMs) > 0));
  }
  if (item.type === "like") return scopedId(item.reflectionId) && scopedId(item.userId);
  if (item.type === "reflection" || item.type === "reply") {
    const entry = item[item.type] as Record<string, unknown> | undefined;
    return Boolean(entry && scopedId(entry.id) && scopedId(entry.userId) && valid(entry.nickname, 100)
      && valid(entry.body, item.type === "reflection" ? 3000 : 1000) && valid(entry.createdAt, 40)
      && (item.type === "reflection" || scopedId(entry.reflectionId)));
  }
  return false;
}
export async function getReadingExperiment(book: ReadingBookId) {
  if (!configured()) return Response.json({ error: "cloud storage is not configured" }, { status: 503 });
  try { return Response.json(await loadReadingExperiment(book), { headers: { "Cache-Control": "no-store" } }); }
  catch { return Response.json({ error: "failed to load experiment" }, { status: 502 }); }
}
export async function postReadingExperiment(book: ReadingBookId, request: Request) {
  if (!configured()) return Response.json({ error: "cloud storage is not configured" }, { status: 503 });
  try {
    const mutation = await request.json();
    if (!validMutation(mutation, book)) return Response.json({ error: "invalid mutation" }, { status: 400 });
    await saveReadingMutation(book, mutation);
    return Response.json({ ok: true });
  } catch { return Response.json({ error: "failed to save experiment" }, { status: 400 }); }
}

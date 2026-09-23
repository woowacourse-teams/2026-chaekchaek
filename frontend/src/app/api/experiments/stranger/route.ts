import { loadStrangerExperiment, saveStrangerMutation } from "../../../../lib/stranger-storage";
import type { StrangerMutation } from "../../../../lib/stranger-experiment";

export const dynamic = "force-dynamic";
const configured = () => Boolean(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY);
const valid = (value: unknown, max: number) => typeof value === "string" && value.trim().length > 0 && value.length <= max;
const id = (value: unknown) => valid(value, 100);
function validMutation(value: unknown): value is StrangerMutation {
  if (!value || typeof value !== "object") return false;
  const item = value as Record<string, unknown>;
  if (item.type === "visit") return id(item.userId) && valid(item.nickname, 100);
  if (item.type === "reading") return id(item.userId) && (item.readingStatus === "read" || item.readingStatus === "unread");
  if (item.type === "page") return id(item.userId) && Number.isInteger(item.page) && Number(item.page) >= 1 && Number(item.page) <= 6;
  if (item.type === "composer") return id(item.userId);
  if (item.type === "like") return id(item.reflectionId) && id(item.userId);
  if (item.type === "reflection" || item.type === "reply") {
    const entry = item[item.type] as Record<string, unknown> | undefined;
    return Boolean(entry && id(entry.id) && id(entry.userId) && valid(entry.nickname, 100)
      && valid(entry.body, item.type === "reflection" ? 3000 : 1000) && valid(entry.createdAt, 40)
      && (item.type === "reflection" || id(entry.reflectionId)));
  }
  return false;
}
export async function GET() {
  if (!configured()) return Response.json({ error: "cloud storage is not configured" }, { status: 503 });
  try { return Response.json(await loadStrangerExperiment(), { headers: { "Cache-Control": "no-store" } }); }
  catch { return Response.json({ error: "failed to load experiment" }, { status: 502 }); }
}
export async function POST(request: Request) {
  if (!configured()) return Response.json({ error: "cloud storage is not configured" }, { status: 503 });
  try {
    const mutation = await request.json();
    if (!validMutation(mutation)) return Response.json({ error: "invalid mutation" }, { status: 400 });
    await saveStrangerMutation(mutation);
    return Response.json({ ok: true });
  } catch { return Response.json({ error: "failed to save experiment" }, { status: 400 }); }
}

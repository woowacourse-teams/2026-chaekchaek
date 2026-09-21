import { applyCloudMutation, loadCloudExperiment } from "../../../lib/supabase-rest";
import type { ExperimentMutation } from "../../../lib/cloud-experiment";

export const dynamic = "force-dynamic";

function configured() {
  return Boolean(process.env.SUPABASE_URL && process.env.SUPABASE_SERVICE_ROLE_KEY);
}

function validText(value: unknown, maximum: number) {
  return typeof value === "string" && value.trim().length > 0 && value.length <= maximum;
}

function validMutation(value: unknown): value is ExperimentMutation {
  if (!value || typeof value !== "object") return false;
  const mutation = value as Partial<ExperimentMutation>;
  if (mutation.type === "reflection") {
    const note = mutation.reflection;
    return Boolean(note && validText(note.id, 100) && validText(note.bookId, 100) && validText(note.userId, 100)
      && validText(note.nickname, 100) && validText(note.body, 3000) && validText(note.createdAt, 40) && note.isExample === false);
  }
  if (mutation.type === "reply") {
    const reply = mutation.reply;
    return Boolean(reply && validText(reply.id, 100) && validText(reply.reflectionId, 100) && validText(reply.userId, 100)
      && validText(reply.nickname, 100) && validText(reply.body, 1000) && validText(reply.createdAt, 40));
  }
  if (mutation.type === "bookEvent") {
    const event = mutation.event;
    return Boolean(event && validText(event.id, 100) && validText(event.bookId, 100) && validText(event.userId, 100)
      && (event.eventType === "view" || event.eventType === "dwell")
      && Number.isInteger(event.durationMs) && event.durationMs >= 0 && event.durationMs <= 300_000
      && validText(event.createdAt, 40));
  }
  return mutation.type === "like" && validText(mutation.reflectionId, 100) && validText(mutation.userId, 100);
}

export async function GET() {
  if (!configured()) return Response.json({ error: "cloud storage is not configured" }, { status: 503 });
  try {
    return Response.json(await loadCloudExperiment(), { headers: { "Cache-Control": "no-store" } });
  } catch {
    return Response.json({ error: "failed to load experiment" }, { status: 502 });
  }
}

export async function POST(request: Request) {
  if (!configured()) return Response.json({ error: "cloud storage is not configured" }, { status: 503 });
  try {
    const mutation = await request.json();
    if (!validMutation(mutation)) return Response.json({ error: "invalid mutation" }, { status: 400 });
    await applyCloudMutation(mutation);
    return Response.json({ ok: true });
  } catch {
    return Response.json({ error: "failed to save experiment" }, { status: 400 });
  }
}

import "server-only";
import type { StrangerExperiment, StrangerMutation } from "./stranger-experiment";

type Row = Record<string, unknown>;
function config() {
  const url = process.env.SUPABASE_URL;
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !key) throw new Error("Supabase 설정이 없습니다.");
  return { url: url.replace(/\/$/, ""), key };
}
async function request(path: string, init: RequestInit = {}) {
  const { url, key } = config();
  const response = await fetch(url + "/rest/v1/" + path, { ...init, cache: "no-store", headers: {
    apikey: key, Authorization: "Bearer " + key, "Content-Type": "application/json", Prefer: "return=minimal", ...init.headers,
  } });
  if (!response.ok) throw new Error("Supabase 요청 실패: " + response.status);
  return response;
}
async function rows(path: string): Promise<Row[]> {
  return await (await request(path, { headers: { Prefer: "return=representation" } })).json() as Row[];
}
export async function loadStrangerExperiment(): Promise<StrangerExperiment> {
  const [participants, pageViews, reflections, replies, likes] = await Promise.all([
    rows("stranger_participants?select=*&order=created_at.asc"), rows("stranger_page_views?select=user_id,page_number"),
    rows("stranger_reflections?select=*&order=created_at.desc"), rows("stranger_replies?select=*&order=created_at.asc"),
    rows("stranger_likes?select=reflection_id,user_id"),
  ]);
  return {
    participants: participants.map((row) => ({ id: String(row.id), nickname: String(row.nickname),
      readingStatus: row.reading_status === "read" || row.reading_status === "unread" ? row.reading_status : null,
      viewedPages: pageViews.filter((view) => view.user_id === row.id).map((view) => Number(view.page_number)),
      composerStarted: row.composer_started === true })),
    reflections: reflections.map((row) => ({ id: String(row.id), userId: String(row.user_id), nickname: String(row.nickname), body: String(row.body), createdAt: String(row.created_at) })),
    replies: replies.map((row) => ({ id: String(row.id), reflectionId: String(row.reflection_id), userId: String(row.user_id), nickname: String(row.nickname), body: String(row.body), createdAt: String(row.created_at) })),
    likes: likes.map((row) => ({ reflectionId: String(row.reflection_id), userId: String(row.user_id) })),
  };
}
export async function saveStrangerMutation(mutation: StrangerMutation): Promise<void> {
  if (mutation.type === "visit") {
    await request("stranger_participants?on_conflict=id", { method: "POST", headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
      body: JSON.stringify({ id: mutation.userId, nickname: mutation.nickname }) });
  } else if (mutation.type === "reading") {
    await request("stranger_participants?id=eq." + encodeURIComponent(mutation.userId), { method: "PATCH", body: JSON.stringify({ reading_status: mutation.readingStatus }) });
  } else if (mutation.type === "page") {
    await request("stranger_page_views?on_conflict=user_id,page_number", { method: "POST", headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
      body: JSON.stringify({ user_id: mutation.userId, page_number: mutation.page }) });
  } else if (mutation.type === "composer") {
    await request("stranger_participants?id=eq." + encodeURIComponent(mutation.userId), { method: "PATCH", body: JSON.stringify({ composer_started: true }) });
  } else if (mutation.type === "reflection") {
    const { reflection } = mutation;
    await request("stranger_reflections?on_conflict=id", { method: "POST", headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
      body: JSON.stringify({ id: reflection.id, user_id: reflection.userId, nickname: reflection.nickname, body: reflection.body, created_at: reflection.createdAt }) });
  } else if (mutation.type === "reply") {
    const { reply } = mutation;
    await request("stranger_replies?on_conflict=id", { method: "POST", headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
      body: JSON.stringify({ id: reply.id, reflection_id: reply.reflectionId, user_id: reply.userId, nickname: reply.nickname, body: reply.body, created_at: reply.createdAt }) });
  } else {
    const path = "stranger_likes?reflection_id=eq." + encodeURIComponent(mutation.reflectionId) + "&user_id=eq." + encodeURIComponent(mutation.userId);
    if ((await rows(path + "&select=reflection_id")).length) await request(path, { method: "DELETE" });
    else await request("stranger_likes", { method: "POST", body: JSON.stringify({ reflection_id: mutation.reflectionId, user_id: mutation.userId }) });
  }
}

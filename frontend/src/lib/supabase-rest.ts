import "server-only";
import type { Book, Like, Reflection, Reply } from "./experiment";
import type { CloudExperiment, ExperimentMutation } from "./cloud-experiment";

type Row = Record<string, unknown>;

function configuration() {
  const url = process.env.SUPABASE_URL;
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !key) throw new Error("Supabase 환경변수가 설정되지 않았습니다.");
  return { url: url.replace(/\/$/, ""), key };
}

async function request(path: string, init: RequestInit = {}) {
  const { url, key } = configuration();
  const response = await fetch(url + "/rest/v1/" + path, {
    ...init,
    cache: "no-store",
    headers: {
      apikey: key,
      Authorization: "Bearer " + key,
      "Content-Type": "application/json",
      Prefer: "return=minimal",
      ...init.headers,
    },
  });
  if (!response.ok) throw new Error("Supabase 요청 실패: " + response.status);
  return response;
}

const bookFromRow = (row: Row): Book => ({ id: String(row.id), title: String(row.title), author: String(row.author),
  genre: row.genre as Book["genre"], coverUrl: row.cover_url ? String(row.cover_url) : undefined,
  productUrl: row.product_url ? String(row.product_url) : undefined });
const reflectionFromRow = (row: Row): Reflection => ({ id: String(row.id), bookId: String(row.book_id), userId: String(row.user_id),
  nickname: String(row.nickname), title: "", quote: "", source: "", body: String(row.body), createdAt: String(row.created_at), isExample: false });
const replyFromRow = (row: Row): Reply => ({ id: String(row.id), reflectionId: String(row.reflection_id), userId: String(row.user_id),
  nickname: String(row.nickname), body: String(row.body), createdAt: String(row.created_at) });
const likeFromRow = (row: Row): Like => ({ reflectionId: String(row.reflection_id), userId: String(row.user_id) });

async function rows(path: string): Promise<Row[]> {
  return (await (await request(path, { headers: { Prefer: "return=representation" } })).json()) as Row[];
}

export async function loadCloudExperiment(): Promise<CloudExperiment> {
  const [books, reflections, replies, likes] = await Promise.all([
    rows("experiment_books?select=*&order=created_at.asc"),
    rows("experiment_reflections?select=*&order=created_at.desc"),
    rows("experiment_replies?select=*&order=created_at.asc"),
    rows("experiment_likes?select=reflection_id,user_id"),
  ]);
  return { books: books.map(bookFromRow), reflections: reflections.map(reflectionFromRow),
    replies: replies.map(replyFromRow), likes: likes.map(likeFromRow) };
}

export async function applyCloudMutation(mutation: ExperimentMutation): Promise<void> {
  if (mutation.type === "book") {
    throw new Error("책 등록은 배포된 공개 API에서 지원하지 않습니다.");
  }
  if (mutation.type === "reflection") {
    const note = mutation.reflection;
    await request("experiment_reflections", { method: "POST", body: JSON.stringify({ id: note.id, book_id: note.bookId,
      user_id: note.userId, nickname: note.nickname, body: note.body, created_at: note.createdAt }) });
    return;
  }
  if (mutation.type === "reply") {
    const { reply } = mutation;
    await request("experiment_replies", { method: "POST", body: JSON.stringify({ id: reply.id, reflection_id: reply.reflectionId,
      user_id: reply.userId, nickname: reply.nickname, body: reply.body, created_at: reply.createdAt }) });
    return;
  }
  const query = "experiment_likes?reflection_id=eq." + encodeURIComponent(mutation.reflectionId) + "&user_id=eq." + encodeURIComponent(mutation.userId);
  const existing = await rows(query + "&select=reflection_id");
  if (existing.length) await request(query, { method: "DELETE" });
  else await request("experiment_likes", { method: "POST", body: JSON.stringify({ reflection_id: mutation.reflectionId, user_id: mutation.userId }) });
}

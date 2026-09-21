export const GENRES = ["철학", "고전소설", "현대소설", "에세이", "서브컬쳐", "시집"] as const;
export type Genre = (typeof GENRES)[number];
export type Book = { id: string; title: string; author: string; genre: Genre; coverUrl?: string; productUrl?: string };
export type Reflection = {
  id: string; bookId: string; userId: string; nickname: string; title: string;
  quote: string; source: string; body: string; createdAt: string; isExample: boolean; sourceUrl?: string; archived?: boolean;
};
export type Reply = { id: string; reflectionId: string; userId: string; nickname: string; body: string; createdAt: string };
export type Like = { reflectionId: string; userId: string };
export type Experiment = { version: 1; books: Book[]; reflections: Reflection[]; replies: Reply[]; likes: Like[] };
export type Participant = { id: string; nickname: string };
export type Counts = { reflections: number; replies: number; likes: number; participants: number; total: number };

export function toggleLike(experiment: Experiment, reflectionId: string, userId: string): Experiment {
  if (!experiment.reflections.some((note) => note.id === reflectionId)) throw new Error("감상을 찾을 수 없습니다.");
  const exists = experiment.likes.some((like) => like.reflectionId === reflectionId && like.userId === userId);
  return { ...experiment, likes: exists
    ? experiment.likes.filter((like) => !(like.reflectionId === reflectionId && like.userId === userId))
    : [...experiment.likes, { reflectionId, userId }] };
}

export function countParticipation(experiment: Experiment, bookIds: string[]): Counts {
  const notes = experiment.reflections.filter((note) => bookIds.includes(note.bookId));
  const ids = new Set(notes.map((note) => note.id));
  const realNotes = notes.filter((note) => !note.isExample);
  const replies = experiment.replies.filter((reply) => ids.has(reply.reflectionId));
  const likes = experiment.likes.filter((like) => ids.has(like.reflectionId));
  const participants = new Set([...realNotes, ...replies, ...likes].map((entry) => entry.userId)).size;
  return { reflections: realNotes.length, replies: replies.length, likes: likes.length,
    participants, total: realNotes.length + replies.length + likes.length };
}

export function summarizeExperiment(experiment: Experiment) {
  const books = experiment.books.map((book) => ({ book, ...countParticipation(experiment, [book.id]) }));
  const genres = GENRES.filter((genre) => experiment.books.some((book) => book.genre === genre)).map((genre) => ({
    genre, ...countParticipation(experiment, experiment.books.filter((book) => book.genre === genre).map((book) => book.id)),
  }));
  const total = countParticipation(experiment, experiment.books.map((book) => book.id));
  const novel = countParticipation(experiment, experiment.books.filter((book) => ["고전소설", "현대소설"].includes(book.genre)).map((book) => book.id));
  return { books, genres, total, novelShare: total.total ? novel.total / total.total : null };
}

export function metricsCsv(experiment: Experiment): string {
  const escape = (value: string | number) => {
    let text = String(value);
    if (/^[\s]*[=+@-]/.test(text)) text = "'" + text;
    return '"' + text.replaceAll('"', '""') + '"';
  };
  const rows: (string | number)[][] = [["데이터 구분", "책", "장르", "감상", "답글", "좋아요", "총 참여", "참여자"]];
  for (const row of summarizeExperiment(experiment).books) {
    rows.push(["브라우저 미리보기", row.book.title, row.book.genre, row.reflections, row.replies, row.likes, row.total, row.participants]);
  }
  return "\uFEFF" + rows.map((row) => row.map(escape).join(",")).join("\r\n");
}

export function readStoredExperiment(raw: string): Experiment {
  const value = JSON.parse(raw);
  const strings = (entry: unknown, fields: string[]) => typeof entry === "object" && entry !== null
    && fields.every((field) => typeof (entry as Record<string, unknown>)[field] === "string");
  if (!value || value.version !== 1 || ![value.books, value.reflections, value.replies, value.likes].every(Array.isArray)
    || !value.books.every((book: Book) => strings(book, ["id", "title", "author", "genre"]) && GENRES.includes(book.genre))
    || !value.reflections.every((note: Reflection) => strings(note, ["id", "bookId", "userId", "nickname", "title", "quote", "source", "body", "createdAt"]) && typeof note.isExample === "boolean")
    || !value.replies.every((reply: Reply) => strings(reply, ["id", "reflectionId", "userId", "nickname", "body", "createdAt"]))
    || !value.likes.every((like: Like) => strings(like, ["reflectionId", "userId"]))) {
    throw new Error("저장된 미리보기를 읽을 수 없습니다. 다른 브라우저에서 열거나 이 사이트의 저장 데이터를 확인해 주세요.");
  }
  return value as Experiment;
}

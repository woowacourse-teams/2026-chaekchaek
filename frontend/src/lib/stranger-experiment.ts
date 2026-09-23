export type ReadingStatus = "read" | "unread";
export const INITIAL_REFLECTION_AUTHOR_ID = "stranger-initial-author";
export type DevicePlatform = "iPhone" | "iPad" | "Android" | "PC" | "other";
export type StrangerParticipant = {
  id: string; nickname: string; readingStatus: ReadingStatus | null;
  source: string; device: DevicePlatform; viewedPages: number[]; composerStarted: boolean;
};
export type StrangerReflection = { id: string; userId: string; nickname: string; body: string; createdAt: string };
export type StrangerReply = { id: string; reflectionId: string; userId: string; nickname: string; body: string; createdAt: string };
export type StrangerLike = { reflectionId: string; userId: string };
export type StrangerExperiment = {
  participants: StrangerParticipant[]; reflections: StrangerReflection[];
  replies: StrangerReply[]; likes: StrangerLike[];
};
export type StrangerMutation =
  | { type: "visit"; userId: string; nickname: string; source: string; device: DevicePlatform }
  | { type: "reading"; userId: string; readingStatus: ReadingStatus }
  | { type: "page"; userId: string; page: number }
  | { type: "composer"; userId: string }
  | { type: "reflection"; reflection: StrangerReflection }
  | { type: "reply"; reply: StrangerReply }
  | { type: "like"; reflectionId: string; userId: string };

export const emptyStrangerExperiment = (): StrangerExperiment => ({ participants: [], reflections: [], replies: [], likes: [] });

export function applyStrangerMutation(current: StrangerExperiment, mutation: StrangerMutation): StrangerExperiment {
  if (mutation.type === "visit") {
    if (current.participants.some((person) => person.id === mutation.userId)) return current;
    return { ...current, participants: [...current.participants, {
      id: mutation.userId, nickname: mutation.nickname, readingStatus: null, source: mutation.source,
      device: mutation.device, viewedPages: [], composerStarted: false,
    }] };
  }
  if (mutation.type === "reading" || mutation.type === "page" || mutation.type === "composer") {
    return { ...current, participants: current.participants.map((person) => person.id !== mutation.userId ? person : {
      ...person,
      readingStatus: mutation.type === "reading" ? mutation.readingStatus : person.readingStatus,
      viewedPages: mutation.type === "page" && !person.viewedPages.includes(mutation.page)
        ? [...person.viewedPages, mutation.page].sort((a, b) => a - b) : person.viewedPages,
      composerStarted: mutation.type === "composer" ? true : person.composerStarted,
    }) };
  }
  if (mutation.type === "reflection") return current.reflections.some((item) => item.id === mutation.reflection.id)
    ? current : { ...current, reflections: [mutation.reflection, ...current.reflections] };
  if (mutation.type === "reply") return current.replies.some((item) => item.id === mutation.reply.id)
    ? current : { ...current, replies: [...current.replies, mutation.reply] };
  const liked = current.likes.some((item) => item.reflectionId === mutation.reflectionId && item.userId === mutation.userId);
  return { ...current, likes: liked
    ? current.likes.filter((item) => item.reflectionId !== mutation.reflectionId || item.userId !== mutation.userId)
    : [...current.likes, { reflectionId: mutation.reflectionId, userId: mutation.userId }] };
}

export function summarizeStrangerExperiment(data: StrangerExperiment) {
  const participants = data.participants.filter((person) => person.id !== INITIAL_REFLECTION_AUTHOR_ID);
  const groups = (["read", "unread"] as const).map((status) => {
    const people = participants.filter((person) => person.readingStatus === status);
    const ids = new Set(people.map((person) => person.id));
    const submitted = new Set(data.reflections.filter((item) => ids.has(item.userId)).map((item) => item.userId)).size;
    return { status, participants: people.length, pageViews: Array.from({ length: 6 }, (_, index) =>
      people.filter((person) => person.viewedPages.includes(index + 1)).length),
      completedPages: people.filter((person) => person.viewedPages.length === 6).length,
      composerStarted: people.filter((person) => person.composerStarted).length,
      submitted, submissionRate: people.length ? submitted / people.length : null,
      replies: data.replies.filter((item) => ids.has(item.userId)).length,
      likes: data.likes.filter((item) => ids.has(item.userId)).length };
  });
  return { unselected: participants.filter((person) => person.readingStatus === null).length, groups };
}

export function summarizeStrangerPlatforms(data: StrangerExperiment) {
  const submittedIds = new Set(data.reflections.map((item) => item.userId));
  const participants = data.participants.filter((person) => person.id !== INITIAL_REFLECTION_AUTHOR_ID);
  const summarize = (key: "source" | "device") => [...new Set(participants.map((person) => person[key]))]
    .sort((a, b) => a.localeCompare(b))
    .map((value) => {
      const people = participants.filter((person) => person[key] === value);
      return { value, visitors: people.length, read: people.filter((person) => person.readingStatus === "read").length,
        unread: people.filter((person) => person.readingStatus === "unread").length,
        submitted: people.filter((person) => submittedIds.has(person.id)).length };
    });
  return { sources: summarize("source"), devices: summarize("device") };
}

export function strangerMetricsCsv(data: StrangerExperiment) {
  const summary = summarizeStrangerExperiment(data);
  const platforms = summarizeStrangerPlatforms(data);
  const rows: (string | number)[][] = [["읽음 여부", "선택자", "1쪽 열람", "2쪽 열람", "3쪽 열람", "4쪽 열람", "5쪽 열람", "6쪽 열람", "6쪽 모두 열람", "작성 시작", "감상 제출자", "제출률", "답글", "좋아요"],
    ...summary.groups.map((group) => [group.status === "read" ? "읽었어요" : "안 읽었어요", group.participants,
      ...group.pageViews, group.completedPages, group.composerStarted, group.submitted,
      group.submissionRate === null ? "집계 전" : (group.submissionRate * 100).toFixed(1) + "%", group.replies, group.likes]),
    ["미선택 방문자", summary.unselected], [],
    ["유입 경로", "방문자", "읽었어요", "안 읽었어요", "감상 제출자"],
    ...platforms.sources.map((row) => [row.value, row.visitors, row.read, row.unread, row.submitted]), [],
    ["접속 기기", "방문자", "읽었어요", "안 읽었어요", "감상 제출자"],
    ...platforms.devices.map((row) => [row.value, row.visitors, row.read, row.unread, row.submitted])];
  return "\uFEFF" + rows.map((row) => row.map((value) => {
    const cell = String(value);
    return '"' + (/^\s*[=+@-]/.test(cell) ? "'" : "") + cell.replaceAll('"', '""') + '"';
  }).join(",")).join("\r\n");
}

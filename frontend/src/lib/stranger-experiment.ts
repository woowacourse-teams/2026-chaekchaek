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
export type ReadingAttentionEvent = {
  id: string; userId: string; target: string; eventType: "view" | "dwell";
  durationMs: number; createdAt: string;
};
export type StrangerExperiment = {
  participants: StrangerParticipant[]; reflections: StrangerReflection[];
  replies: StrangerReply[]; likes: StrangerLike[]; attentionEvents: ReadingAttentionEvent[];
};
export type StrangerMutation =
  | { type: "visit"; userId: string; nickname: string; source: string; device: DevicePlatform }
  | { type: "reading"; userId: string; readingStatus: ReadingStatus }
  | { type: "page"; userId: string; page: number }
  | { type: "composer"; userId: string }
  | { type: "reflection"; reflection: StrangerReflection }
  | { type: "reply"; reply: StrangerReply }
  | { type: "like"; reflectionId: string; userId: string }
  | { type: "attention"; event: ReadingAttentionEvent };

export const emptyStrangerExperiment = (): StrangerExperiment => ({ participants: [], reflections: [], replies: [], likes: [], attentionEvents: [] });

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
  if (mutation.type === "attention") return current.attentionEvents.some((item) => item.id === mutation.event.id)
    ? current : { ...current, attentionEvents: [...current.attentionEvents, mutation.event] };
  const liked = current.likes.some((item) => item.reflectionId === mutation.reflectionId && item.userId === mutation.userId);
  return { ...current, likes: liked
    ? current.likes.filter((item) => item.reflectionId !== mutation.reflectionId || item.userId !== mutation.userId)
    : [...current.likes, { reflectionId: mutation.reflectionId, userId: mutation.userId }] };
}

export function summarizeStrangerExperiment(data: StrangerExperiment, initialAuthorId = INITIAL_REFLECTION_AUTHOR_ID) {
  const participants = data.participants.filter((person) => person.id !== initialAuthorId);
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

export function summarizeStrangerPlatforms(data: StrangerExperiment, initialAuthorId = INITIAL_REFLECTION_AUTHOR_ID) {
  const submittedIds = new Set(data.reflections.map((item) => item.userId));
  const participants = data.participants.filter((person) => person.id !== initialAuthorId && person.readingStatus !== null);
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

export function summarizeReadingAttention(data: StrangerExperiment, initialAuthorId = INITIAL_REFLECTION_AUTHOR_ID) {
  const people = data.participants.filter((person) => person.id !== initialAuthorId && person.readingStatus !== null);
  const userIds = new Set(people.map((person) => person.id));
  const events = data.attentionEvents.filter((event) => userIds.has(event.userId));
  const dwell = (target: string) => events.filter((event) => event.target === target && event.eventType === "dwell");
  const total = (items: ReadingAttentionEvent[]) => items.reduce((sum, item) => sum + item.durationMs, 0);
  const pages = Array.from({ length: 6 }, (_, index) => {
    const page = index + 1;
    const pageDwell = dwell(`page:${page}`);
    const visitors = people.filter((person) => person.viewedPages.includes(page)).length;
    return { page, visitors, displays: events.filter((event) => event.target === `page:${page}` && event.eventType === "view").length,
      durationMs: total(pageDwell), averageMs: visitors ? Math.round(total(pageDwell) / visitors) : 0 };
  });
  const sections = (["context", "excerpt", "feed", "composer"] as const).map((section) => {
    const sectionDwell = dwell(`section:${section}`);
    const viewers = new Set(sectionDwell.map((event) => event.userId)).size;
    return { section, viewers, durationMs: total(sectionDwell), averageMs: viewers ? Math.round(total(sectionDwell) / viewers) : 0 };
  });
  const reflections = data.reflections.map((reflection) => ({ id: reflection.id, body: reflection.body,
    opens: events.filter((event) => event.target === `open:${reflection.id}` && event.eventType === "view").length,
    likes: data.likes.filter((like) => like.reflectionId === reflection.id && userIds.has(like.userId)).length,
    replies: data.replies.filter((reply) => reply.reflectionId === reflection.id && userIds.has(reply.userId)).length }));
  return { visitors: people.length, pages, sections,
    pageMoves: events.filter((event) => event.target === "action:page-move" && event.eventType === "view").length,
    reflectionOpens: reflections.reduce((sum, reflection) => sum + reflection.opens, 0),
    likes: data.likes.filter((like) => userIds.has(like.userId)).length,
    replies: data.replies.filter((reply) => userIds.has(reply.userId)).length,
    composerStarted: people.filter((person) => person.composerStarted).length,
    submitted: new Set(data.reflections.filter((reflection) => userIds.has(reflection.userId)).map((reflection) => reflection.userId)).size,
    reflections };
}

export function strangerMetricsCsv(data: StrangerExperiment, initialAuthorId = INITIAL_REFLECTION_AUTHOR_ID) {
  const summary = summarizeStrangerExperiment(data, initialAuthorId);
  const platforms = summarizeStrangerPlatforms(data, initialAuthorId);
  const attention = summarizeReadingAttention(data, initialAuthorId);
  const rows: (string | number)[][] = [["읽음 여부", "선택자", "1쪽 열람", "2쪽 열람", "3쪽 열람", "4쪽 열람", "5쪽 열람", "6쪽 열람", "6쪽 모두 열람", "작성 시작", "감상 제출자", "제출률", "답글", "좋아요"],
    ...summary.groups.map((group) => [group.status === "read" ? "읽었어요" : "안 읽었어요", group.participants,
      ...group.pageViews, group.completedPages, group.composerStarted, group.submitted,
      group.submissionRate === null ? "집계 전" : (group.submissionRate * 100).toFixed(1) + "%", group.replies, group.likes]),
    ["이전 방식 미선택 기록(방문자 합계 제외)", summary.unselected], [],
    ["유입 경로", "방문자", "읽었어요", "안 읽었어요", "감상 제출자"],
    ...platforms.sources.map((row) => [row.value, row.visitors, row.read, row.unread, row.submitted]), [],
    ["접속 기기", "방문자", "읽었어요", "안 읽었어요", "감상 제출자"],
    ...platforms.devices.map((row) => [row.value, row.visitors, row.read, row.unread, row.submitted]), [],
    ["발췌 페이지", "고유 열람자", "표시 횟수", "화면 노출 합계(초)", "열람자당 평균(초)"],
    ...attention.pages.map((row) => [row.page, row.visitors, row.displays,
      Math.round(row.durationMs / 1000), Math.round(row.averageMs / 1000)]), [],
    ["화면 영역", "노출자", "화면 노출 합계(초)", "노출자당 평균(초)"],
    ...attention.sections.map((row) => [row.section, row.viewers,
      Math.round(row.durationMs / 1000), Math.round(row.averageMs / 1000)]), [],
    ["페이지 이동", attention.pageMoves], ["감상 답글 영역 열기", attention.reflectionOpens],
    ["좋아요", attention.likes], ["답글", attention.replies],
    ["작성 시작자", attention.composerStarted], ["제출자", attention.submitted], [],
    ["감상 ID", "감상 본문", "답글 영역 열기", "좋아요", "답글"],
    ...attention.reflections.map((row) => [row.id, row.body, row.opens, row.likes, row.replies])];
  return "\uFEFF" + rows.map((row) => row.map((value) => {
    const cell = String(value);
    return '"' + (/^\s*[=+@-]/.test(cell) ? "'" : "") + cell.replaceAll('"', '""') + '"';
  }).join(",")).join("\r\n");
}

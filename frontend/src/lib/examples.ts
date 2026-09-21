import { withCollectedBooks } from "./books";
import type { Experiment, Reflection } from "./experiment";

// 공개 독자 리뷰에서 원문을 확인한 짧은 발췌. 출판사 소개나 창작 감상이 아니다.
// 확인일: 2026-09-21. 출처별 인용은 25단어 이내로 제한한다.
const excerpts = [
  { bookId: "odyssey", body: "사건의 이면과 배경을 들려주고 지금 우리의 상황에 대입하여 서술하는 방식이 글을 술술 읽히게 만든다.",
    source: "YES24 · y*******i · 리뷰 발췌", sourceUrl: "https://sarak.yes24.com/review/22351596" },
  { bookId: "cocktail-love-zombies", body: "친구가 재밌다고 추천해 줘서 읽게 됐는데 뒤로 갈수록 더 재밌어지는 것 같다.",
    source: "느리지만 차근차근 · 리뷰 발췌", sourceUrl: "https://clarte-everyday.tistory.com/7" },
  { bookId: "justice", body: "편협한 관점으로만 세상을 보고 이해하는 것이 아닌, 다양한 관점으로 보고 익히며, 생각하는 힘을 길러야하겠다는 반성도 하게 되었다.",
    source: "YES24 · r*****3 · 리뷰 발췌", sourceUrl: "https://sarak.yes24.com/review/16792786" },
  { bookId: "my-funeral", body: "책 내용을 차분히 읽다 보면 마음 한편이 편해집니다.",
    source: "잡다무니 · 리뷰 발췌", sourceUrl: "https://jobdamuny.tistory.com/38" },
  { bookId: "alya-1", body: "마사치카의 행동이 너무 공감이 가고 유키가 귀엽고",
    source: "Google Play 도서 · 홍서진 · 리뷰 일부 발췌", sourceUrl: "https://play.google.com/store/books/details?id=TeGuEAAAQBAJ" },
  { bookId: "love-on-the-rock", body: "고선경의 시에서 사랑은 감정이기 전에 감각이다.",
    source: "YES24 · j*****5 · 리뷰 발췌", sourceUrl: "https://sarak.yes24.com/review/22149991" },
];

export const sourcedReflections: Reflection[] = excerpts.map((excerpt) => ({
  ...excerpt, id: excerpt.bookId + "-web-review-1", userId: "external-review", nickname: "",
  title: "", quote: "", isExample: true, createdAt: "2026-09-21T00:00:00.000Z",
}));

export function withSourcedReflections(experiment: Experiment): Experiment {
  const ids = new Set(sourcedReflections.map((note) => note.id));
  // 이전 창작 감상은 화면에서 내리되 기존 답글과 좋아요의 연결은 보존한다.
  const existing = experiment.reflections.filter((note) => !ids.has(note.id)).map((note) =>
    note.isExample && note.id.startsWith("odyssey-example-") ? { ...note, archived: true } : note);
  return { ...experiment, reflections: [...existing, ...sourcedReflections.map((note) => ({ ...note }))] };
}

export function initialExperiment(): Experiment {
  return withSourcedReflections(withCollectedBooks({ version: 1, books: [], reflections: [], replies: [], likes: [] }));
}

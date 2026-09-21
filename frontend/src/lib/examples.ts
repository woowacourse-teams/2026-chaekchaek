import type { Experiment, Reflection } from "./experiment";

// 실험용 창작 감상. 출판본 문장을 인용하지 않는다.
const examples = [
  "아무리 좋은 곳이어도 결국 내 집에 가고 싶은 마음은 알 것 같다.",
  "고향을 잊게 하는 로토스, 시간 가는 줄 모르고 보는 짧은 영상 같았다.",
  "‘아무도 아닌 자’라는 이름으로 살아남는 장면이 제일 기억에 남는다.",
  "버티는 오디세우스를 보니 나도 지나온 힘든 날들이 떠올랐다.",
  "떠나려는 손님을 잘 보내주는 것도 배려라는 생각이 들었다.",
];

export function initialExperiment(): Experiment {
  const reflections: Reflection[] = examples.map((body, index) => ({
    id: "odyssey-example-" + (index + 1), bookId: "odyssey", userId: "example", nickname: "첵췍",
    title: "", quote: "", source: "", body, isExample: true, createdAt: "2026-09-21T00:00:00.000Z",
  }));
  return { version: 1, books: [{ id: "odyssey", title: "오디세이아", author: "호메로스", genre: "고전소설" }], reflections, replies: [], likes: [] };
}

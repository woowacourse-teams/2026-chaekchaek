import { INITIAL_REFLECTION_AUTHOR_ID, type StrangerReflection } from "./stranger-experiment";

export const initialStrangerReflections: StrangerReflection[] = [
  {
    id: "stranger-initial-reflection-1",
    userId: INITIAL_REFLECTION_AUTHOR_ID,
    nickname: "책췍",
    body: "가끔 삶이 거대한 어항 같다는 생각이 듭니다. 인간이 만든 법과 도덕 안에서 살고, 감정까지 정해진 방식으로 보여줘야 한다는 점에서 우리는 어항 속 금붕어와 크게 다르지 않은지도 모릅니다. 세상에 정해진 의미가 없다면 허무할 수도 있지만 무의미에서 시작되는 의미만큼은 누구도 강요할 수 없는 나만의 것이기도 합니다. 이방인은 당연하다고 믿어온 삶의 규칙과 감정이 정말 내 것인지 묻게 만드는 책입니다.",
    createdAt: "2026-09-23T00:00:00+09:00",
  },
  {
    id: "stranger-initial-reflection-2",
    userId: INITIAL_REFLECTION_AUTHOR_ID,
    nickname: "책췍",
    body: `의욕이 없고 권태를 느끼는 주인공
소시오패스와 다를 바 없지만 사실 누구나 가져봤을 만한 감정과 생각들
결국 사회적 규범이라는 가면 아래에 있는 나의 모습이었다
감정과 사건의 본질보다 도덕성에 집착하는 등장인물들은 현실과 다를 바 없어보였고
사제 앞에서 속마음을 쏟아내는 마지막 장면은 한마디 한마디가 인상적이었다
모두가 가면을 쓴 세상에서 민낯의 주인공은 이방인이었다`,
    createdAt: "2026-09-23T00:01:00+09:00",
  },
];

export const isInitialStrangerReflection = (id: string) => initialStrangerReflections.some((item) => item.id === id);

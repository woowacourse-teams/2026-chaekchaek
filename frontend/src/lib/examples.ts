import { withCollectedBooks } from "./books";
import type { Experiment, Reflection } from "./experiment";

// 공개 독자 리뷰에서 원문을 확인한 짧은 발췌. 출판사 소개나 창작 감상이 아니다.
// 확인일: 2026-09-21. 출처별 인용은 25단어 이내로 제한한다.
const excerpts = [
  {"bookId": "odyssey", "body": "오디세우스가 폴리페모스를 이긴 뒤 굳이 자신의 이름을 밝히면서 이후의 재앙을 자초하는 장면은 그래서 꽤 현대적으로 읽혔습니다.", "source": "YES24 · h*******5", "sourceUrl": "https://sarak.yes24.com/review/22378291", "sequence": 22378291},
  {"bookId": "odyssey", "body": "인상 깊었던 것은 오디세우스가 칼립소를 떠날 수 있었던 것은 신들의 명령이 내려진 이후였다.", "source": "YES24 · s****2", "sourceUrl": "https://sarak.yes24.com/review/22378128", "sequence": 22378128},
  {"bookId": "odyssey", "body": "세이렌을 유튜브와 숏폼 중독으로 읽어내고, 칼립소를 안락함과 현실 도피로 읽어내고, 폴리페모스를 자만과 절제의 결여로 읽어낸다. 억지스럽지 않았다.", "source": "YES24 · h******7", "sourceUrl": "https://sarak.yes24.com/review/22377567", "sequence": 22377567},
  {"bookId": "odyssey", "body": "칼립소가 불멸을 제안하며 영원한 사랑을 약속했지만, 결국 불사, 영원한 젊음 그 모든 것을 포기하고 유한한 인간의 삶을 선택한다.", "source": "YES24 · w********6", "sourceUrl": "https://sarak.yes24.com/review/22335947", "sequence": 3},
  {"bookId": "odyssey", "body": "이 장에서는 오디세우스와 전쟁을 치른 아킬레우스의 이야기가 소개되는데 그리스 로마 신화의 등장인물들의 이면을 깊게 살펴볼 수 있었다.", "source": "YES24 · y****7", "sourceUrl": "https://sarak.yes24.com/review/22378635", "sequence": 5},
  {"bookId": "cocktail-love-zombies", "body": "그러니까 가시는 누구에게나 있는 아픈 기억. 남에겐 하찮고 나에겐 치명적이라 더 무서운 그런 존재를 대변한다. 트라우마, 그런 단어 말이다.", "source": "YES24 · w*******0", "sourceUrl": "https://sarak.yes24.com/review/16232035", "sequence": 16232035},
  {"bookId": "cocktail-love-zombies", "body": "인상깊었던 부분은 어머니의 변화였다. 처음과 끝의 어머님의 생각과 태도가 많이 바뀌었는데 독립적 주체가 되어가는 모습이 좋았다.", "source": "YES24 · d*****p", "sourceUrl": "https://sarak.yes24.com/review/16524256", "sequence": 16524256},
  {"bookId": "cocktail-love-zombies", "body": "뱀술을 마시고 좀비가 된다는 설정은 참 한국적이다라는 생각과 어떻게 이런 이야기를 쓸 수 있을까 감탄을 했습니다", "source": "YES24 · y********4", "sourceUrl": "https://sarak.yes24.com/review/12471906", "sequence": 12471906},
  {"bookId": "cocktail-love-zombies", "body": "가장 섬뜩한 이야기였다. 세호는 아버지에게 살해당한 어머니를 마주하고 과거로 돌아갈 수 있는 세 번의 기회를 갖게 되는데 어머니를 살릴 수 있는 시점은 어디일까?", "source": "YES24 · r*****1", "sourceUrl": "https://sarak.yes24.com/review/18039024", "sequence": 2},
  {"bookId": "cocktail-love-zombies", "body": "좀비는 형식일 뿐이고 어째 지지고 볶으며 살아가는 우리네 가족들의 현실 삶을 보여주는 듯 했다.", "source": "YES24 · m********g", "sourceUrl": "https://sarak.yes24.com/review/12477073", "sequence": 12477073},
  {"bookId": "justice", "body": "결과를 중시하는 공리주의를 거부하고 동기를 중시하는 칸트가 자살을 해석하는 시각은 흥미롭다.", "source": "YES24 · m*****i", "sourceUrl": "https://sarak.yes24.com/review/13956616", "sequence": 4},
  {"bookId": "justice", "body": "부정적인 결과를 야기할 게 뻔한데도 진실을 이야기하는 것이 하얀 거짓말을 하는 것보다 정의롭다는 칸트의 입장은 우리로서는 완전히 수용하기 어렵다.", "source": "YES24 · p*******0", "sourceUrl": "https://sarak.yes24.com/review/10629843", "sequence": 10629843},
  {"bookId": "justice", "body": "자유주의 부터 공리주의! 아리스토텔레스와 존 롤스, 칸트까지 교과서 윤리책에나 나올법한 분들의 치열한 논쟁이 눈앞에서 벌어지기 시작했다.", "source": "YES24 · s***8", "sourceUrl": "https://sarak.yes24.com/review/10465230", "sequence": 10465230},
  {"bookId": "justice", "body": "그렇다면 앞의 철도의 딜레마의 정답은 무엇일까? 구명보트의 사람들의 행위는 어떻게 판단해야 할까? 개인의 소유와 부의 불균형은 어느 정도까지 허용될까?", "source": "YES24 · i*******3", "sourceUrl": "https://sarak.yes24.com/review/7921956", "sequence": 2},
  {"bookId": "justice", "body": "이마누엘 칸트가 말하는 자유와 도덕의 가치를 정립하는 기본테제인 ‘옳음’이 '좋음'보다 우선할 경우에 다수보다는 개인의 권리를 옹호함으로 공리주의와 상충된다.", "source": "YES24 · k********2", "sourceUrl": "https://sarak.yes24.com/review/7880974", "sequence": 7880974},
  {"bookId": "my-funeral", "body": "에어컨 이야기에서 느껴지는 엄마에 대한 마음이 내게도 전해져 마음이 짠 했다.", "source": "YES24 · l******4", "sourceUrl": "https://sarak.yes24.com/review/12297893", "sequence": 4},
  {"bookId": "my-funeral", "body": "‘모두 없어져 버렸으면 좋겠다.’는 문장은 저자의 것이었지만, 이를 읽기가 무섭게 난 내 마음을 들킨 것만 같아 작아졌다.", "source": "YES24 · q*****2", "sourceUrl": "https://sarak.yes24.com/review/16247750", "sequence": 16247750},
  {"bookId": "my-funeral", "body": "남들과 비교하기보다는 내가 좋아하는 것을 하면서 살아가고, 남의 시선이 아닌 나의 가치와 기준으로 살아가는 것이 더 중요하다는 것을 알려 주고 있다.", "source": "YES24 · s****2", "sourceUrl": "https://sarak.yes24.com/review/12303021", "sequence": 12303021},
  {"bookId": "my-funeral", "body": "장례식에 고인을 추모하기 위해 찾아와서 생전에 관계를 맺었던 고인과의 추억을 이야기하며 풀어내는 그 시간이 일종의 치유가 될 수도 있다는 걸 이번에 깨닫게 되었기 때문이다.", "source": "YES24 · o*******1", "sourceUrl": "https://sarak.yes24.com/review/12305627", "sequence": 12305627},
  {"bookId": "my-funeral", "body": "그리고 생각해보조 내가 죽으면 장례식에 누가 와줄까? 물론 가족은 와주겠지만 그외의 사람은? 자신의 관계르를한번 돌아보게하는 책이었스니다 산것에 후회는 없었습니다.", "source": "YES24 · s*****4", "sourceUrl": "https://sarak.yes24.com/review/13002754", "sequence": 13002754},
  {"bookId": "alya-1", "body": "그리고 애니에서 없던던 마사치카의 독백이 잘 담겨있어 읽는맛이나네요.", "source": "YES24 · h************5", "sourceUrl": "https://sarak.yes24.com/review/20798238", "sequence": 4},
  {"bookId": "alya-1", "body": "그래서 아랴가 무심코 툭툭 뱉는 낮간지러운 러시아어 고백의 말을 모두 다 이해할 수 있다. 이러한 독특한 소재를 활용하여 내용을 전개해 나가니 재미있을 수밖에 ..", "source": "YES24 · s******n", "sourceUrl": "https://sarak.yes24.com/review/20076105", "sequence": 20076105},
  {"bookId": "alya-1", "body": "특히 아랴양 가끔씩 러시아어로 말한다는 것 자체가 일단 와... 그냥 설정이 미쳤습니다.", "source": "YES24 · p****i", "sourceUrl": "https://sarak.yes24.com/review/20011637", "sequence": 20011637},
  {"bookId": "alya-1", "body": "아리사는 열정이 불타오르는 편에 비해 마사치카는 열정이 사그라진 상태. 하지만 그런 마사치카도 아리사의 열정적인 모습을 보고 존경한다고 말할 정도였다.", "source": "YES24 · k******0", "sourceUrl": "https://sarak.yes24.com/review/17897746", "sequence": 17897746},
  {"bookId": "alya-1", "body": "은발에 이쁘고 공부도 잘하면서 츤츤거리는 캐릭터라 싫어할수가 없었다.", "source": "YES24 · b********5", "sourceUrl": "https://sarak.yes24.com/review/19023494", "sequence": 19023494},
  {"bookId": "love-on-the-rock", "body": "특히 <건대입구역 4번 출구 앞에 모러 누워 있었다>라는 시에서... 정말 감격...", "source": "YES24 · g**********8", "sourceUrl": "https://sarak.yes24.com/review/22363283", "sequence": 4},
  {"bookId": "love-on-the-rock", "body": "제목이 칵테일을 비유한 듯 하다 한 잔 안에는 과연 어떠한 사랑 이야기가 담겨있을까 사랑이란 그러하다 어떠한 류의 칵테일처럼 달기도, 쓰기도 하다", "source": "YES24 · e******8", "sourceUrl": "https://sarak.yes24.com/review/22263367", "sequence": 22263367},
  {"bookId": "love-on-the-rock", "body": "이 시집에서 사랑은 피곤한 일상, 불안한 미래, 생계를 위한 노동과 뒤섞여 있으며, 그래서 더욱 현실적이고 생생하게 다가오는 것 같아요.", "source": "YES24 · v******s", "sourceUrl": "https://sarak.yes24.com/review/22229842", "sequence": 22229842},
  {"bookId": "love-on-the-rock", "body": "《러브 온 더 락》은 일상의 사랑과 이별, 외로움을 고선경 시인만의 유쾌하면서도 솔직한 언어로 풀어낸 시집이다.", "source": "YES24 · s*****5", "sourceUrl": "https://sarak.yes24.com/review/22263600", "sequence": 5},
  {"bookId": "love-on-the-rock", "body": "고선경의 시에서 사랑은 감정이기 전에 감각이다.", "source": "YES24 · j*****5", "sourceUrl": "https://sarak.yes24.com/review/22149991", "sequence": 1},
];

export const sourcedReflections: Reflection[] = excerpts.map(({ sequence, ...excerpt }) => ({
  ...excerpt, id: excerpt.bookId + "-web-review-" + sequence, userId: "external-review", nickname: "",
  title: "", quote: "", isExample: true, createdAt: "2026-09-21T00:00:00.000Z",
}));

export function withSourcedReflections(experiment: Experiment): Experiment {
  const ids = new Set(sourcedReflections.map((note) => note.id));
  // 교체된 준비 감상은 화면에서 내리되 기존 답글과 좋아요의 연결은 보존한다.
  const existing = experiment.reflections.filter((note) => !ids.has(note.id)).map((note) =>
    note.isExample && (note.id.startsWith("odyssey-example-") || note.id.includes("-web-review-")) ? { ...note, archived: true } : note);
  return { ...experiment, reflections: [...existing, ...sourcedReflections.map((note) => ({ ...note }))] };
}

export function initialExperiment(): Experiment {
  return withSourcedReflections(withCollectedBooks({ version: 1, books: [], reflections: [], replies: [], likes: [] }));
}

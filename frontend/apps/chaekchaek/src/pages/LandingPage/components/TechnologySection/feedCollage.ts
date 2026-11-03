// Covers matching the supplied references; kept locally to avoid runtime image requests.
// https://image.yes24.com/goods/74971092/XL
// https://image.yes24.com/goods/2312211/XL
// https://image.yes24.com/goods/194315610/XL
import city from './imgs/feed-city.jpg';
import cosmos from './imgs/feed-cosmos.jpg';
import olive from './imgs/feed-olive.jpg';

export const feedCovers = [
  { src: city, title: '대도시의 사랑법', width: 816, height: 1200 },
  { src: cosmos, title: '코스모스', width: 802, height: 1200 },
  { src: olive, title: '태양 아래 올리브', width: 821, height: 1200 },
];

// Reused verbatim from PerspectivesSection; these are a collection of reader
// thoughts, not reviews attributed to the individual covers in this collage.
export const feedThoughts = [
  '사회가 정해한 사랑이라는 단어에 내가 느끼는 감정을 억지로 끼워 맞추려는 시도가 꼭 필요한가 싶다.',
  '무심해 보였다는 클라마르 소녀에게서 나르게 바라는 물음은 없다고 생각하고, 그 사람만이 가진 철학을 이해하며 같이 살아가면 좋지 않을까 생각했다.',
];

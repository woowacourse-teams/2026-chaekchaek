import type { TechnologySlide } from './TechnologySection.types';

export const technologySlides: TechnologySlide[] = [
  {
    id: 'cache',
    label: '효율적인 책 검색',
    category: 'BACKEND · CACHE',
    title: '반복되는 계산은 줄이고, 더 효율적인 책 검색으로',
    problem:
      '책 검색 시 감상 수와 답글 수를 조회하기 위해 동일한 집계 쿼리가 반복적으로 실행되었습니다.',
    solution: 'Caffeine을 활용해 책별 감상 수와 답글 수를 메모리에 캐싱했습니다.',
    details: [
      '검색 결과 전체가 아닌 감상 수와 답글 수만 캐싱',
      '캐시 미스가 발생한 책만 모아서 일괄 조회',
      'TTL 30분 · 최대 500개 항목',
    ],
    before: { value: '100회', description: '감상 수·답글 수 집계 쿼리 각각 실행' },
    after: { value: '0회', description: '감상 수·답글 수 집계 쿼리 각각 실행' },
    outcome: '캐시 적중률 100%',
    note: '측정 조건: 로컬 k6 검색 100회, 사전 캐시 적재 후 측정',
  },
  {
    id: 'feed',
    label: '더 넓어진 감상 탐색',
    category: 'BACKEND · FEED',
    title: '10개의 감상에서, 더 많은 독자의 생각으로',
    problem:
      '기존 홈에서는 스포일러가 없는 최신 감상 10개만 확인할 수 있었습니다. 오래된 감상이나 스포일러가 포함된 감상을 계속 탐색하기 어려웠습니다.',
    solution: '홈 API와 별도로 피드 전용 API를 구현했습니다.',
    details: [
      '홈과 피드의 서로 다른 조회 정책 분리 · 감상 정보 조회 로직은 공통으로 재사용',
      '페이지네이션과 무한 스크롤을 연결해 연속적인 탐색 지원',
      '스포일러 여부를 전달해 사용자가 직접 펼쳐 볼 수 있도록 처리',
    ],
    before: { value: '10개', description: '최신 비스포일러 감상' },
    after: { value: '전체 감상', description: '페이지를 이어서 탐색' },
    outcome:
      '피드에서 감상을 읽고 좋아요·답글을 남기거나, 마음에 드는 책을 바로 서재에 담을 수 있습니다.',
  },
  {
    id: 'auth',
    label: '누구나 시작할 수 있는 경험',
    category: 'FRONTEND · AUTH',
    title: '로그인하지 않아도, 독서는 시작될 수 있도록',
    problem:
      '로그인 사용자와 게스트 사용자는 서로 다른 인증 정보를 사용하며, 사용자 상태에 따라 이용 가능한 기능과 API 요청 방식도 달라집니다.',
    solution: '사용자 상태에 맞게 인증 정보와 이용 흐름을 구분했습니다.',
    details: [
      '로그인 사용자에게 OAuth 기반 인증 적용',
      '게스트 사용자에게 별도의 게스트 토큰 사용',
      '사용자 상태에 따라 API 요청에 필요한 인증 정보 처리',
      '인증 상태에 따른 기능 접근과 UI 동작 구분',
    ],
    flows: [
      { label: '로그인 사용자', steps: ['OAuth 인증', '인증 정보로 API 요청'] },
      { label: '게스트 사용자', steps: ['게스트 토큰', '게스트 인증 정보로 API 요청'] },
    ],
    outcome:
      '로그인이나 회원가입을 먼저 요구하지 않고도 책책을 경험할 수 있도록 초기 진입 장벽을 낮췄습니다.',
  },
];

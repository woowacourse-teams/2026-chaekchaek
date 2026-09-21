// 현재 체크아웃에는 서비스의 생성 목록이 없어 같은 명명 규칙으로 구성한다.
export const ADJECTIVES = ["다정한", "느긋한", "호기심 많은", "차분한", "용감한", "부지런한", "명랑한", "사려 깊은", "수줍은", "씩씩한", "반가운", "꿈꾸는"] as const;
export const BIRDS = ["참새", "오목눈이", "박새", "동고비", "제비", "종달새", "물총새", "직박구리", "딱따구리", "꾀꼬리", "부엉이", "파랑새"] as const;

export function assignNickname(randomIndex: (length: number) => number = (length) => {
  const value = new Uint32Array(1);
  crypto.getRandomValues(value);
  return value[0] % length;
}): string {
  return ADJECTIVES[randomIndex(ADJECTIVES.length)] + " " + BIRDS[randomIndex(BIRDS.length)];
}

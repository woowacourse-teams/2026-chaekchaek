# 책췍 웹 실험

- 기존 책·장르 실험: `/`, 집계: `/admin`
- 『이방인』·『사랑의 편린들』 통합 감상 실험: `/experiments/stranger`
- 책별 집계: `/admin/stranger`, `/admin/love-fragments`
- 이전 『사랑의 편린들』 주소 `/experiments/love-fragments`는 통합 페이지로 이동
- 로컬 실행: `npm ci`, `npm run dev`
- 검증: `npm run typecheck`, `npm test`, `npm run test:e2e`, `npm run build`

Vercel 프로젝트의 루트는 이 디렉터리입니다. 운영 환경은 Supabase에 참여 데이터를 저장하며, `supabase/migrations/`의 실험 테이블이 필요합니다. 『이방인』과 『사랑의 편린들』은 서로 다른 식별자와 API로 데이터를 구분하며 같은 읽기 실험 테이블을 사용합니다. Supabase 환경변수가 없으면 입력은 해당 브라우저에만 저장됩니다. 관리자 경로는 현재 공개된 실험 통계 화면입니다.

통합 화면은 책의 읽음 여부를 선택했을 때만 해당 책의 방문자를 등록합니다. 관리자 화면은 책별 고유 선택자, 발췌 페이지와 화면 영역의 노출 시간, 페이지 이동과 감상 반응을 보여줍니다. 노출 시간은 탭이 보이고 영역이 화면에 나타난 시간을 대략 합산하며 실제 독해·집중 시간과 같지 않습니다. 노출과 관심 이벤트는 읽기 실험 전용 `stranger_attention_events` 테이블에 저장합니다.

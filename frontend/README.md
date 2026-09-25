# 책췍 웹 실험

- 기존 책·장르 실험: `/`, 집계: `/admin`
- 『이방인』 감상 실험: `/experiments/stranger`, 집계: `/admin/stranger`
- 『사랑의 편린들』 감상 실험: `/experiments/love-fragments`, 집계: `/admin/love-fragments`
- 로컬 실행: `npm ci`, `npm run dev`
- 검증: `npm run typecheck`, `npm test`, `npm run test:e2e`, `npm run build`

Vercel 프로젝트의 루트는 이 디렉터리입니다. 운영 환경은 Supabase에 참여 데이터를 저장하며, `supabase/migrations/`의 실험 테이블이 필요합니다. 『이방인』과 『사랑의 편린들』은 서로 다른 식별자와 API로 데이터를 구분하며 같은 읽기 실험 테이블을 사용합니다. Supabase 환경변수가 없으면 입력은 해당 브라우저에만 저장됩니다. 관리자 경로는 현재 공개된 실험 통계 화면입니다.

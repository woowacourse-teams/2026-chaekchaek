# 책췍 읽기 실험 핸드오프 - 새 실험 준비

## 프로젝트 위치 / 브랜치
- 경로: `/Users/ujeonghyeon/Desktop/dev/myDev/2026-chaekchaek` (Git 저장소)
- 브랜치: `an-develop-local`. 실험 구현 HEAD `cb6ac784`는 확인 당시 `origin/an-develop`과 같았다.
- 커밋 규칙: 프롬프트 결과를 의미 단위로 즉시 커밋한다. 메시지는 한글 Conventional Commit이다. `an-develop-local`은 원격 브랜치로 push하지 않고 HEAD를 `origin/an-develop`에 fast-forward로 반영한다. `main` 직접 push·merge는 금지한다.

## 무엇을 만드는가
기존 `/`는 장르·책 관심을 살핀 1차 실험이다. 현재 `/experiments/stranger`는 감상을 남기기 어려운지 탐색하려고 『이방인』과 『사랑의 편린들』의 발췌문·필기와 다른 사람의 감상을 함께 제공하는 2차 실험이다. 사용자는 책마다 읽음 여부를 선택하고 감상·답글·좋아요를 남길 수 있다. **다음에 만들 새 실험의 가설, 대상 콘텐츠, 변인, 성공 지표는 아직 정하지 않았다.**

## 이번 작업에서 확정한 결정 (중요)
- 장르 선호를 묻는 1차 실험에서, 감상 작성의 어려움을 탐색하는 두 책 통합 페이지로 방향을 바꿨다. 두 책은 한 화면에 보이되 읽음 여부와 상세 내용·집계는 책별로 나뉜다.
- 읽음 여부를 선택한 순간 해당 책의 방문자로 센다. 재방문은 브라우저 식별자로 중복 집계하지 않는다. 과거 방식의 미선택 방문 기록은 책별 선택 방문자 수에서 제외한다.
- 각 책에 PDF 웹 이미지 6장과 초기 감상을 제공한다. 초기 감상은 함께 나눈 감상에 표시하며 답글·좋아요가 가능하다. 모든 페이지 열람을 글 작성 조건으로 강제하지 않는다.
- 페이지·화면 영역 노출 시간, 페이지 이동, 감상 답글 영역 열기, 작성·제출·답글·좋아요, 유입 경로와 기기를 책별로 본다. 노출은 실제 독해나 집중의 증거가 아니며 제출은 감상의 질을 뜻하지 않는다.
- 관리자 `/admin/stranger`, `/admin/love-fragments`에는 최근 72시간의 시간별 유입과 전체 일별 유입·누적 방문 그래프가 있다. 기존 참여자 생성 시각을 읽어 그리며 DB 행은 수정하지 않는다. 예전 수집 방식의 첫 기록 시각은 책 선택보다 앞설 수 있다. 최신 수치는 관리자 화면에서 새로고침한다.
- 이번 2차 실험은 발췌문과 초기 감상을 함께 제공한 상태를 관찰한다. 정보와 감상의 개별 효과나 1차 대비 개선의 인과관계를 이 데이터만으로 단정하지 않는다.

## 재사용할 코어 / 건드리지 말 것
- `frontend/src/components/reading-experiment-page.tsx`, `frontend/src/lib/reading-book-config.ts` - 두 책 선택, 발췌 이미지, 감상 UI와 책별 설정. 새 실험 설계가 정해지기 전 기존 흐름을 바꾸지 않는다.
- `frontend/src/lib/stranger-experiment.ts`, `frontend/src/lib/use-stranger-experiment.ts`, `frontend/src/lib/stranger-storage.ts` - 참여자·반응 모델, 브라우저 식별자, Supabase 저장. 기존 참여 기록을 삭제·초기화하지 않는다.
- `frontend/src/lib/use-reading-attention.ts`, `frontend/src/lib/reading-traffic.ts`, `frontend/src/components/reading-experiment-admin.tsx` - 노출·행동 이벤트와 책별 관리자 집계·그래프. 새 실험에서 지표 의미를 먼저 정한 뒤 재사용한다.
- `frontend/supabase/migrations/` - 기존 실험 테이블 정의. 1차 실험과 읽기 실험 데이터는 분리돼 있다. 두 책은 읽기 실험 테이블을 공유하되 식별자 접두사로 구별한다.
- `frontend/public/experiments/stranger/`, `frontend/public/experiments/love-fragments/` - 각 6장 이미지. 원본 PDF는 `/Users/ujeonghyeon/Downloads/stranger.pdf`, `/Users/ujeonghyeon/Downloads/사랑의편린들_6_19.pdf`에 있다.

## 현재 상태
- 변경: 실험 구현과 그래프는 `cb6ac784`까지 커밋·원격 반영·운영 배포됨. 이 핸드오프 작성 전 추적 파일의 미커밋 변경은 없었다. 이 요청에서는 앱 코드를 바꾸지 않는다.
- 운영: `https://chaekchaek.vercel.app/experiments/stranger`와 두 책 관리자 경로가 2026-09-29 확인 시 HTTP 200이었다. 운영 DB는 Supabase이며 `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`가 필요하다. 비밀값은 문서에 쓰지 않는다.
- 검증: 2026-09-29 `frontend`에서 `npm test` 19개 통과, `npm run typecheck` 통과. 마지막 앱 변경 때 브라우저 테스트 15개와 `npm run build`가 통과했다. 새 실험 자체는 미구현·미검증이다.
- 의존성: 이번 핸드오프에서 새 의존성 없음. 웹은 Next.js 16.3.5, React 19.2.0을 사용한다.

## 다음에 할 일 (구현 단계)
1. 사용자와 새 실험의 한 문장 가설, 참여자 행동, 바꿀 요인, 비교 조건, 성공·실패 지표를 확정한다. 기존 2차 실험의 후속인지 독립 실험인지 구분한다.
2. 기존 `/experiments/stranger`와 운영 기록을 보존하는 경로·저장·집계 분리 방식을 결정하고 화면 초안을 검토한다.
3. 적용 브랜치와 GitHub 이슈·PR 절차를 확인한 뒤 구현, 타입 검사, 단위·브라우저 테스트, 빌드를 수행한다.
4. 배포가 요청되면 대상 URL에 반영하고 실제 진입점에서 저장·집계를 검증한다. Vercel CLI는 이전 작업에서 `--scope commit3921-5889s-projects`를 명시해야 배포됐다.

## 제약 / 함정
- `frontend/AGENTS.md`는 Next.js 16의 관련 `node_modules/next/dist/docs/` 문서를 코드 수정 전에 읽도록 요구한다.
- Git 훅은 새 브랜치 이름에 이슈 번호를 요구했다. GitHub 이슈 또는 PR 생성은 제목·본문 초안을 먼저 제시하고 사용자가 독립된 메시지로 정확히 `슛`이라고 답해야 각 1건씩 가능하다. `an-develop` 직접 push는 프로젝트 규칙상 허용된다.
- Supabase 환경변수가 없으면 웹 입력은 브라우저 로컬 미리보기에만 저장된다. 운영 데이터 검증과 로컬 미리보기 검증을 구별한다.
- 새 실험의 내용과 배포 범위는 미확정이다. 이번 핸드오프는 새 실험을 만들거나 운영 데이터를 변경하지 않았다.

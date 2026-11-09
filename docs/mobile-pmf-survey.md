# 모바일 PMF 설문

## 승인 기준

- 2026-11-06 사용자 승인: [SVG 기반 HTML 시안](../android/visual-contracts/prototypes/pmf-survey/index.html), 커밋 `a688295b`.
- 홈 하단 이어서 읽기 바를 설문 바로 교체한다. 홈의 다른 콘텐츠와 4탭은 유지한다.
- 바 문구는 `책췍 설문`, `여러분의 의견을 들려주세요`, `밀어서 하루 동안 숨기기`다. 질문 개수와 텍스트 구분점은 표시하지 않는다.
- Figma MCP Starter 호출 한도로 조회·등록에 실패했다. 사용자 지시에 따라 AGENTS.md와 디자인 시스템 스킬에 접근 불가 시 승인 시안을 사용하는 예외를 추가했다. Figma 등록·팀 라이브러리 게시는 미실행이다.

## 동작

| 행동과 조건 | 결과 |
| --- | --- |
| 인증된 회원 또는 기존 게스트가 홈 진입 | 서버가 제공한 대상 여부·기존 응답 여부로 바 표시 결정 |
| 대상 아님, 제출 완료, 게스트 자격 없음 | 바 숨김 |
| 좌우로 바 너비의 22% 이상 스와이프 | 이 기기에서 24시간 숨김. Android SharedPreferences와 iOS UserDefaults에 만료 시각 보존 |
| 숨김 만료 후 홈 표시 또는 재진입 | 다시 조회하여 대상이면 표시 |
| 바 탭 | 한 문항씩 표시. 답변 선택 전 다음 비활성 |
| 홈 재조회 중 바 탭 | 기존 문항과 제출용 토큰으로 응답 시작. 이전 조회가 늦게 끝나도 문항·선택·제출 완료 상태를 변경하지 않음 |
| 이전·다음, 다이얼로그 닫기·다시 열기 | 같은 세션·응답자의 선택과 문항 위치 보존 |
| 복수 문항의 몰랐던 기능 없음 선택 | 나머지 선택 해제. 다른 항목을 고르면 없음 해제 |
| 제출 중 | 중복 제출·선택 변경·닫기 방지. 500ms 이후 로딩 표시 |
| 제출 실패 | 공통 ErrorDialog. 선택·문항 보존 후 다시 시도 |
| 제출 성공 또는 서버의 이미 응답함 충돌 | 완료 안내. 바를 다시 표시하지 않음. 재실행 후에는 서버의 기존 응답 여부 사용 |
| 같은 회원 토큰 갱신 | 답변 보존 |
| 로그아웃·다른 회원 또는 다른 게스트로 전환 | 이전 응답자의 토큰·선택·오류 제거. 늦은 응답 무시 |

선택 초안은 앱 세션 안에서만 보존한다. 인증값과 제출용 토큰은 메모리에만 보관하며 숨김 저장소에 기록하지 않는다. 숨김은 계정별이 아닌 기기별 24시간이다.

## 서버 계약과 환경

- [development Swagger](https://dev-api.chaekchaek.com/docs/swagger-ui.html)와 [development OpenAPI](https://dev-api.chaekchaek.com/docs/openapi3.yaml), 서버 `be-dev`의 `PmfSurveyService`를 2026-11-06 확인했다.
- `GET /api/v1/pmf-survey`: `eligible`, `alreadySubmitted`, `submissionToken`, `questions`. 문항·선택지 코드와 문구는 서버 응답 사용.
- `POST /api/v1/pmf-survey/responses`: `submissionToken`, 문항별 `questionCode`와 `answerCodes`. actorId를 클라이언트가 보내지 않는다.
- 회원은 Authorization Bearer, 게스트는 X-Guest-Token만 전송한다. 조회 당시의 제출용 토큰을 같은 응답자에게 사용한다.
- `409` 중 `PMF_SURVEY_ALREADY_RESPONDED`만 완료로 복구한다. 저장 후 응답 유실에 따른 재시도도 같은 정책이다.
- 같은 날 확인한 [production OpenAPI](https://api.chaekchaek.com/docs/openapi3.yaml)에는 설문 경로가 없다. `android/openapi3.yaml`은 이 production 원본으로 갱신했다. 앱 전체 서버 설정은 변경하지 않았으며, 설문 조회가 404이면 바를 숨긴다.
- 실제 development 응답 제출과 production 배포 여부는 인증된 실사용자 요청으로 검증하지 않았다. 테스트 fixture의 성공은 서버 저장 증거가 아니다.

## 구현 경계

- Android·iOS는 공통 `HomeScreen`, `PmfSurveyBar`, `PmfSurveyViewModel`, `PmfSurveyRemoteRepository`를 사용한다. 숨김 저장만 플랫폼별로 구현한다.
- `PmfSurveyPreviewScreen`은 실제 공통 홈·설문 UI를 사용하는 로컬 fixture다. iOS는 Debug 전용 실행 인자에서만 진입한다. 운영 인증·응답·숨김 상태를 사용하지 않고 첫 제출을 실패시킨 뒤 재시도를 성공시킨다.
- 기존 `presentation/common/ErrorRecovery.kt`가 data의 `MobileLoginException`과 Ktor의 `ResponseException`에 직접 의존한다. 이는 앱 아키텍처의 presentation→domain 의존 방향 및 의존성 역전 원칙 위반이다. 이번 작업은 공통 오류 UI 정책을 재사용하며 해당 구조 개편은 포함하지 않는다.

## 검증

- 원격 Android는 접수 전 연결 조회와 SSH가 시간 초과됐다. 원격 작업을 접수하지 않은 상태에서 허용된 Jev 로컬 경로로 전환했다.
- 소스 `7ca8933f`: 공통 JVM 테스트 498개 통과, 실패 0개. ViewModel 8개와 HTTP 계약 3개가 설문 전용이다. 24시간 경계·재실행, 재조회와 숨김의 경합, 답변 보존, 배타 선택, 실패·재시도, 회원 전환과 늦은 응답 차단, 헤더·payload·409·404를 검증한다.
- 같은 소스 Android Debug 앱·테스트 APK 빌드와 화면 테스트 6개 통과. 설문 2개와 기존 홈 재진입 4개다. API 36 AVD `chaekchaek_public_607`, serial `emulator-5584`를 임대했다. 설치본과 대상의 versionCode 10 및 인증서가 같아 `install -r`로 데이터를 보존했다. APK SHA-256은 `20b652102d4293c205d64628b0fde55888f868c403268af56dd5ad24456dd677`이다.
- iOS 소스 `88b5ba7d`: 앱·테스트 빌드와 UI 테스트 2개 통과. iOS 26.2 iPhone 17 Pro Simulator `74464F96-99AB-4F02-BFC2-A8D7C082ACE1`에서 문항 선택·이전·닫고 재진입·복수 선택·실패 후 재시도·완료·스와이프를 확인했다. 개인 스킴을 변경하지 않고 같은 커밋의 임시 detached worktree와 공유 스킴을 사용했다.
- Jev 고정 설치본 `0.2.0`, 소스 `a43a0e917e1aed0eef3d1b46dcbf4e100055ae60`, Droidrun·Portal 0.7.27로 추가 확인했다. 첫 실행은 탭 가능한 바의 레이블을 식별하지 못해 0단계에서 차단됐다. 식별자를 추가한 두 번째 실행은 탭 1회가 전달됐지만 후속 트리 변화가 없어 확신도 기준에서 중단됐다. Jev 제출 흐름은 미검증이며 확신도 기준을 낮추거나 ADB 조작으로 대체하지 않았다. 두 실행 합계 API 3회, 입력 15,276·출력 132토큰이다. 원본 트리·세션은 커밋하지 않는다.
- Jev용 Android 전용 속성을 공통 UI에 추가했을 때 iOS 빌드가 실패했다. 해당 속성을 제거했다. 긴 문항 스크롤이 진행 표시 위로 그려지던 문제는 스크롤 영역 clipping으로 수정했다.
- 최종 소스 `d41499d9`: Android 앱·테스트 APK 빌드, 공통 JVM 498개와 Android 화면 6개 모두 통과했다. APK SHA-256은 `30faba7aa0b3251c843498b78cd12a6ab8f3371ee9006c1ef1eebb6b28e254dc`다. 설치본과 대상의 인증서 SHA-256은 모두 `fe4db416e0bba10939c293a1124b25954c5d3f75882d134447c81a86bc6490d9`이며 데이터 보존 설치했다. 스크롤 영역 밖 그리기를 제한한 캡처를 확인했다.
- 최종 코드의 [GitHub 빌드·테스트·Lint](https://github.com/woowacourse-teams/2026-chaekchaek/actions/runs/42202970264)는 통과했다.
- 같은 소스 `d41499d9`의 iOS 앱·테스트 빌드와 UI 테스트 2개도 통과했다. 이전과 동일한 iPhone 17 Pro Simulator에서 재검증했다. Android AVD 종료·임대 반환과 iOS 원래 글자 크기 복원·Simulator 종료를 완료했다.

로컬 근거 경로:

- 공통 JVM: `android/shared/build/test-results/testAndroidHostTest/`
- Android 최초 화면: `/tmp/chaekchaek-survey-android-ui/instrumentation.log`
- Android 경합 수정 화면·Jev: `/tmp/chaekchaek-survey-android-final/`
- iOS 최초 UI 결과: `/tmp/chaekchaek-survey-ios-shared-result.xcresult`
- 최종 Android 화면: `/tmp/chaekchaek-survey-android-clipping/`
- 최종 iOS UI 결과: `/tmp/chaekchaek-survey-ios-complete-result.xcresult`

검증은 로컬 fixture의 동작 근거다. 실사용자 대상 판정과 실제 서버 저장, production 설문 배포, 물리기기는 미검증이다.

## PR #688 병합 전 리뷰

- 2026-11-09 홈 재조회 중 설문을 열면 늦은 응답이 문항·선택을 지우거나 제출 완료 후 바를 다시 표시하는 경합을 발견했다. 두 회귀 테스트가 수정 전 각각 실패하는 것을 확인했다.
- 설문을 열 때 기존 조회를 취소하고 요청 세대를 변경하여 취소를 무시한 응답도 차단한다. 이미 연 다이얼로그의 중복 열기와 완료한 설문의 다시 열기도 차단한다.
- 수정 후 공통 JVM 테스트 500개가 실패·오류·건너뜀 없이 통과했다. 새 경합 회귀 2개를 포함해 ViewModel 테스트는 10개다.
- 같은 날 production OpenAPI는 저장소의 `android/openapi3.yaml`과 SHA-256이 일치하며 여전히 설문 경로가 없다. development 조회·제출 계약은 현재 구현과 일치한다.

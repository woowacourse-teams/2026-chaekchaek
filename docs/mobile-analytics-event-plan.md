# 모바일 사용자 이벤트 로깅 설계

작성일: 2026-10-02. 상태: 설계 초안. 연결 작업: #439.

Android와 iOS에 같은 의미의 이벤트를 기록하여 탐색, 독서 기록, 감상 교류와 재방문 사이의 연관관계를 분석한다. 이 문서는 이벤트 선정과 측정 계약이며 SDK 구현, 콘솔 설정, 실제 수집 완료를 뜻하지 않는다. 기존 #439의 기본 수집 범위에 추가할 설계로, 커스텀 이벤트 구현 및 PR 범위는 아직 확정하지 않았다.

## 1. 먼저 답할 질문

| 질문 | 선행 행동 | 후행 결과와 관찰 창 |
| --- | --- | --- |
| 어디서 발견한 책이 서재 등록으로 이어지는가? | 홈, 피드, 발견, 검색에서 책 노출과 선택 | 같은 탐색 흐름에서 직접 서재 등록 성공 |
| 검색에서 어디서 이탈하는가? | 검색 제출, 결과 표시, 책 선택 | 같은 검색에서 상세 열람 또는 직접 서재 등록 |
| 로그인 요청이 기록을 막는가? | 특정 행동 시도 후 로그인 안내 | 같은 행동 흐름에서 로그인 성공과 원래 행동 성공 |
| 다른 사람의 감상을 본 뒤 내 감상을 쓰는가? | 타인 감상 노출 | 같은 책, 같은 세션에서 감상 작성 성공 |
| 첫날 어떤 행동이 다음 주 재방문과 관련되는가? | 첫 실행 후 24시간 내 직접 등록, 페이지 기록, 감상 작성, 좋아요 | 첫 실행 후 7일 이상 14일 미만의 재방문 |
| 지연과 오류가 행동 완료율에 영향을 주는가? | 작업별 지연, 실패 종류 | 같은 작업의 재시도와 최종 성공, 후속 재방문 |

위 질문은 관찰 자료의 연관관계를 다룬다. 적극적인 사용자가 기록과 재방문을 모두 많이 할 수 있으므로, 기록 행동이 재방문을 유발했다고 단정하지 않는다. 인과 효과는 별도 무작위 실험 등이 필요하다.

## 2. 이벤트 목록

공통 속성은 3절을 따른다. P0는 첫 계측에 필요한 항목, P1은 세부 사용성과 체류 분석용 항목이다. 아래 `cc_` 이벤트는 직접 정의하는 커스텀 이벤트다.

| 우선순위 | 이벤트 | 정확한 발생 시점 | 추가 속성 |
| --- | --- | --- | --- |
| P0 | SDK 자동 이벤트 | `first_open`, `session_start`, `user_engagement`를 SDK에 맡긴다. 수동 중복 전송하지 않는다 | SDK 제공 시각, 플랫폼, 앱 버전, 세션 정보 |
| P0 | `screen_view` | 홈, 피드, 발견, 서재, 책 상세, 마이페이지의 논리적 화면이 전면에 표시될 때 | `screen_name`, `screen_class`, `view_id`, `previous_screen` |
| P0 | `cc_content_impression` | 책 카드 또는 감상 콘텐츠 면적의 50% 이상이 전면 화면에서 연속 1초 이상 보일 때 | `content_type`, `content_id`, `book_key`, `list_id`, `position`, `is_own` |
| P0 | `cc_book_select` | 사용자가 책을 눌러 상세 진입을 요청할 때 | `book_key`, `list_id`, `position`, `search_id` |
| P0 | `cc_book_detail_ready` | 선택한 책의 상세 로딩이 성공하고 콘텐츠가 표시될 때 | `book_key`, `has_my_record`, `review_count_bucket`, `duration_ms` |
| P0 | `cc_search_submit` | 공백이 아닌 검색을 명시적으로 실행할 때. 키 입력마다 전송하지 않는다 | `search_id`, `query_length_bucket`, `sort`, `trigger` |
| P0 | `cc_search_result` | 해당 검색 요청 결과가 현재 화면에 반영될 때 | `search_id`, `result_count`, `outcome`, `duration_ms`, `error_category` |
| P0 | `cc_auth_prompt` | 보호된 행동 때문에 로그인 안내가 실제 표시될 때 | `action_id`, `trigger_action` |
| P0 | `cc_auth_start` | Apple 또는 Google 로그인 버튼을 눌렀을 때 | `auth_attempt_id`, `action_id`, `method`, `trigger_action` |
| P0 | `cc_auth_result` | 앱 서버 인증까지 완료되거나 로그인 실패, 명시적 취소가 확정될 때 | `auth_attempt_id`, `action_id`, `method`, `outcome`, `duration_ms`, `error_category` |
| P0 | `cc_auth_dismiss` | 로그인 안내를 사용자가 닫을 때 | `action_id`, `trigger_action` |
| P0 | `cc_composer_open` | 감상 또는 답글 작성 UI가 실제 열렸을 때 | `composer_id`, `content_type`, `mode`, `book_key` |
| P0 | `cc_action_start` | 사용자가 저장, 등록, 삭제, 좋아요 등의 실행을 요청할 때. 인증 및 검증 이전 | `action_id`, `action`, `book_key`, `composer_id`, `target_type`, `item_count` |
| P0 | `cc_action_result` | 해당 행동의 결과가 확정될 때. 성공은 서버 확인 후이며 낙관적 UI 변경만으로 전송하지 않는다 | `action_id`, `action`, `outcome`, `duration_ms`, `error_category`, `attempt_count`, `completed_count`, `effect`, 행동별 속성 |
| P0 | `cc_library_auto_add` | 페이지, 상태, 별점 저장 과정에서 서재 항목이 실제 새로 생성된 것이 확인될 때 | `action_id`, `book_key`, `caused_by_action` |
| P0 | `cc_load_result` | 검색 이외 홈 콘텐츠, 서재, 상세, 감상, 답글의 최초 로딩 또는 재시도가 완료될 때 | `load_id`, `resource`, `trigger`, `outcome`, `duration_ms`, `error_category` |
| P1 | `cc_composer_dismiss` | 작성 화면을 저장 없이 명시적으로 닫을 때 | `composer_id`, `content_type`, `has_input`, `active_ms` |
| P1 | `cc_filter_change` | 현재 값과 다른 필터 또는 정렬을 선택할 때 | `control`, `old_value`, `new_value` |
| P1 | `cc_view_engagement` | 전면 체류 중 30초마다, 그리고 화면 이탈 또는 백그라운드 전환 시 남은 구간을 전송 | `view_id`, `segment_index`, `active_ms`, `book_key` |

`cc_action_start/result`의 `action`은 아래 허용 목록만 쓴다. 단일 범용 로그에 임의 문자열을 넣는 API 대신 구현에서는 `LibraryAddRequested`, `ReadingPageSaveSucceeded`처럼 의도를 드러내는 타입이나 함수로 매핑한다.

| 행동 값 | 의미 | 결과에 붙일 속성 |
| --- | --- | --- |
| `library_add` | 검색 또는 상세에서 직접 서재 등록 | `effect=created/already_present` |
| `library_remove` | 서재에서 삭제, 일괄 삭제 포함 | `item_count`, `completed_count` |
| `reading_status_change` | 읽고 싶은, 읽는 중, 다 읽은 상태 변경 | `previous_status`, `new_status`, `item_count`, `completed_count` |
| `reading_page_save` | 읽은 페이지 저장 | `progress_bucket`, `change_direction=increase/decrease/unchanged/unknown` |
| `rating_save` | 별점 저장 | `rating`, `mode=create/update` |
| `review_create/update/delete` | 감상 생성, 수정, 삭제. 각각 별도 action 값 | `mode` |
| `reply_create/update/delete` | 답글 생성, 수정, 삭제. 각각 별도 action 값 | `mode`, `target_type=review/reply` |
| `review_like/reply_like` | 감상 또는 답글 좋아요 변경. 각각 별도 action 값 | `new_state=liked/unliked` |

감상 작성 시작과 제출, 서버 저장 성공을 구분한다. 일괄 작업은 항목마다 성공 이벤트를 복제하지 않고 작업 1건과 완료 개수를 기록한다. 여러 책에 걸친 작업은 `book_key`를 생략한다. 실제 변화 없는 성공은 `effect=unchanged`로 기록해 활성 행동 전환에서 제외한다.

## 3. 연결 속성과 값 규칙

| 속성 | 용도와 수명 |
| --- | --- |
| `schema_version` | 모든 수동 이벤트에 정수 `1`. 의미가 바뀌면 버전을 올린다 |
| `environment` | `production/test`. 개발자 테스트는 별도 Firebase 프로젝트 또는 분석 필터로 분리 |
| `auth_state` | 이벤트 당시 `signed_in/signed_out`. 계정 ID는 보내지 않는다 |
| `screen_name` | `home/feed/discover/library/book_detail/my_page` 등 고정 이름 |
| `view_id` | 화면 방문마다 새 임의 ID. 재구성 시 유지, 재진입 시 새로 생성 |
| `flow_id` | 탐색에서 상세 및 후속 행동까지 연결하는 임의 ID. 새 검색, 새 진입 경로, 세션 종료 시 갱신. 로그인 왕복 중 유지 |
| `origin` | 흐름 시작 위치 `home/feed/discover/search/library/detail/unknown`. 현재 화면과 별도로 유지 |
| `event_id` | 수동 이벤트마다 임의 ID. 같은 이벤트 재전송 시 유지하여 중복 제거 |
| `event_seq` | 프로세스 실행 안의 증가 순번. 시각이 같은 이벤트 순서 보조용 |
| `run_id` | 프로세스 실행마다 임의 ID. `event_seq`의 범위를 지정 |
| `action_id` | 한 번의 사용자 행동부터 인증과 자동 재시도, 최종 결과까지 유지. 명시적 재시도는 새 ID와 `retry_of_action_id` 사용 |
| `search_id` | 검색 요청 단위 임의 ID. 새 제출이나 정렬 변경으로 재검색할 때 갱신 |
| `composer_id` | 작성 UI를 연 뒤 닫거나 저장할 때까지 유지 |
| `book_key` | 두 플랫폼 공통으로 `isbn13:<정규화 ISBN13>`, 없으면 `catalog:<서버 도서 ID>`. 개인 서재 항목 ID를 쓰지 않는다. 둘 다 없으면 생략하고 연결 분석에서 제외 |
| `content_id` | 공개 감상 ID 또는 book_key. 작성자 ID, 본문은 제외 |
| `list_id`, `position` | 고정 섹션 코드와 당시 표시 순서(1부터). 검색, 홈, 피드 간 노출 차이를 구분 |

SDK가 제공하는 플랫폼, 앱 버전, 타임스탬프를 중복 파라미터로 보내지 않는다. 속성은 관련 이벤트에만 붙이고 불명확한 값은 임의로 추정하지 않는다. `unknown`과 누락을 분석에서 별도 집계한다.

`outcome`: `success/empty/failure/cancelled/blocked/partial`. 검색 결과 없음은 `empty`, 로컬 유효성 검증 실패는 `blocked`, 사용자가 직접 취소하면 `cancelled`다. 프로세스 종료로 결과가 없을 때 실패를 만들어 보내지 않는다.

`error_category`: `network/timeout/auth/validation/not_found/conflict/server/unknown`. 원문 오류, 응답 본문, URL 쿼리, 토큰은 보내지 않는다. `duration_ms`는 시작부터 결과까지 단조 시계로 계산한다. 행동은 인증 대기를 포함하고 API 지연은 load 이벤트와 구분한다.

구간: `query_length_bucket=1_5/6_15/16_plus`, `review_count_bucket=0/1_5/6_plus`, `progress_bucket=0/1_25/26_50/51_75/76_99/100/unknown`. 진행률은 전체 페이지를 알 때만 계산한다. 상태 값은 `want_to_read/reading/finished/unknown`으로 통일한다.

검색어, 감상, 답글 원문과 이메일, 닉네임, 인증 토큰, 사용자 계정 ID, 생년월일은 전송하지 않는다. 도서 및 감상 ID도 행동 이력을 연결하는 데이터이므로 완전 익명 자료로 표현하지 않는다.

## 4. 이벤트를 오해 없이 기록하는 규칙

- Compose 재구성, 데이터 재조회, 뒤로가기 애니메이션만으로 이벤트가 중복 발생하지 않게 한다. `screen_view`는 논리 화면 기준 수동 계측을 사용하고 플랫폼 자동 화면 계측과 중복되지 않도록 설정한다.
- 노출은 다운로드 완료나 목록 포함 여부가 아니라 실제 가시성으로 판단한다. `(view_id, list_id, content_type, content_id)`마다 한 번만 전송한다. 새 검색 결과는 새 view_id로 구분한다.
- 감상 노출은 읽었다는 증거가 아니다. `is_own=false`인 노출만 타인 감상 노출 분석에 쓴다. 피드에서 표시만 되는 좋아요 수를 좋아요 클릭으로 기록하지 않는다.
- 화면 체류는 포그라운드 시간만 구간별로 합산한다. 앱 종료로 마지막 구간이 유실될 수 있으므로 정확한 독서 시간이라고 부르지 않는다.
- `cc_search_result`는 최초 결과에만 사용한다. 페이지 추가 로딩은 `cc_load_result(resource=search_page, trigger=pagination)`로 구분한다. 정렬 재검색은 `trigger=sort_change`로 기록한다.
- `cc_book_detail_ready`는 방문당 한 번이며 로딩 성공 뒤 실제 콘텐츠 표시를 뜻한다. `cc_load_result`는 네트워크 결과이므로 사용자 열람과 별도로 집계한다.
- 성공 반환만으로 신규 서재 등록을 추정하지 않는다. 이미 등록된 책은 `already_present`, 실제 생성 여부를 알 수 없으면 `unknown`으로 기록한다.
- 자동 서재 등록 이후 페이지 저장에 실패하면 자동 등록 성공과 페이지 저장 실패를 각각 기록한다. 직접 등록 전환율에 자동 등록을 섞지 않는다.
- 자동 API 재시도는 같은 action_id의 최종 결과 한 번과 attempt_count로 기록한다. 사용자 재시도는 별도 행동이며 retry_of_action_id로 연결한다.
- 동일 `(event_id)` 재전송을 제거하고 최종 결과는 action_id당 한 번만 기록한다. 사용자 취소와 네트워크 취소를 혼동하지 않는다.

## 5. 연관관계 분석 정의

분석 단위는 우선 계정이 아니라 앱 설치다. BigQuery의 `(stream_id, user_pseudo_id)`를 설치 키로, 여기에 `ga_session_id`를 더해 세션 키로 사용한다. 재설치, 데이터 초기화, 다른 기기, Android와 iOS 사이를 동일인으로 연결하지 않는다. 계정 기반 연결은 별도 수집 정책과 구현이 필요하며 이번 설계에서 제외한다.

| 지표 | 분모 | 분자 및 연결 조건 |
| --- | --- | --- |
| 노출 후 책 선택률 | 책별 실제 노출이 있는 설치·방문 수 | 같은 설치·방문·목록·책의 선택 수를 중복 제거. 노출 기준 1초 전에 누른 선택은 별도 집계 |
| 검색 해결률 | trigger=submit인 search_id 수 | 같은 search_id에서 책 선택 또는 library_add 성공이 발생한 수 |
| 상세 후 직접 등록률 | 상세 ready가 있고 has_my_record=false인 설치·flow·책 수 | 같은 세션·flow·책에서 library_add, success, effect=created가 발생한 수 |
| 인증 후 행동 완료율 | action_id가 있는 auth_prompt 수 | 같은 action_id의 auth_result 성공 후 action_result 성공이 있는 수 |
| 작성 완료율 | composer_id별 composer_open 수 | 같은 composer_id에서 review_create 또는 reply_create 성공 수 |
| 타인 감상 노출 후 작성률 | 상세 방문 설치·책을 타인 감상 선행 노출 유무로 구분 | 노출군은 노출 이후, 비노출군은 상세 진입 이후 같은 세션·책의 review_create 성공. 노출 조건 충족 전 작성 성공은 제외 |
| 행동별 첫날 활성화율 | first_open이 관측되고 24시간 관찰이 끝난 설치 수 | 첫 24시간 내 특정 행동 성공이 한 번 이상인 설치 수 |
| 행동별 W1 재방문율 | first_open 후 14일 관찰이 끝난 설치를 첫 24시간 행동 유무로 구분 | first_open 후 [7일, 14일)에 수동 screen_view가 한 번 이상 있는 설치 수 |
| 오류 후 복구율 | failure/blocked인 action_id 수 | 같은 세션의 retry_of_action_id 연결 사슬 안에 성공이 발생한 수 |

W1은 여기서는 최초 실행 시점 기준의 경과 시간이다. GA4 달력 주간 코호트와 같은 지표라고 표기하지 않는다. first_open이 없는 기존 설치는 별도 코호트로 분리하며 최초로 관측한 이벤트를 신규 설치로 간주하지 않는다.

관찰 중 행동한 모든 사람과 안 한 사람을 사후 비교하지 않고 첫 24시간을 선행 행동 창으로 고정한다. 후행 결과를 보고 그룹을 나누면 재방문자가 행동할 기회도 더 많아지는 편향이 생긴다.

분석 결과에는 기간, 분자·분모, 전환율, 퍼센트포인트 차이, 신뢰구간을 함께 표시한다. 표본이 작으면 수치와 불확실성을 그대로 보여준다. 플랫폼, 앱 버전, 유입 경로, 기존 서재 여부, 초기 활동량 차이를 분리해서 확인하되 관찰 데이터만으로 인과를 주장하지 않는다. 여러 행동을 동시에 비교해 나온 우연한 차이는 후속 코호트로 재확인한다.

## 6. 저장 및 계측 완료 조건

- 복수 세션과 이벤트 순서를 연결하는 분석을 위해 Firebase Analytics의 BigQuery 내보내기를 구성해야 한다. 이 문서는 실제 연결을 수행하지 않았다. 연결 시작 시점, 내보내기 데이터의 실제 시작일, 저장 지역, 보관 기간, 접근 권한 및 비용은 활성화 전에 확인한다.
- GA4 사용자 정의 측정기준에는 `origin`, `action`, `outcome`, `auth_state`, `error_category`, `schema_version` 같은 고정 범주를 우선 등록한다. event_id, flow_id, action_id, book_key 등 고유값이 많은 키는 BigQuery 연결용으로 사용하고 일반 보고서 측정기준 등록에서 제외한다.
- 수동 이벤트 하나당 공통 속성과 전용 속성을 합쳐 25개 이하로 유지한다. 자동 이벤트에 이 공통 속성 세트를 무조건 적용하지 않는다.
- Android와 iOS에서 같은 시나리오의 이벤트명, 속성 타입, 발생 순서가 같아야 한다. 핵심 시나리오는 검색→등록, 로그인→중단 행동 재개, 감상 노출→작성, 자동 서재 등록→페이지 저장, 오류→재시도다.
- 재구성 중복, 백그라운드 노출 제외, 이미 등록된 책, 좋아요 해제, 일괄 변경, 취소, 서버 실패를 검증한다. Analytics 전송 실패는 사용자 기능의 성공 여부를 바꾸지 않는다.
- DebugView 수신과 운영 BigQuery 내보내기 확인은 별도 검증으로 기록한다. 현재 공식 문서상 디버그 모드 이벤트도 기본적으로 일일 내보내기에 포함될 수 있으므로 개발자 트래픽 필터와 테스트 환경 분리를 확인한다. 운영 전환율에 테스트 이벤트가 섞이지 않아야 한다.
- 앱 코드, SDK 기본 수집, 콘솔 사용자 정의 항목, BigQuery 연결, 실제 이벤트 수신을 각각 완료 상태로 관리한다.

## 7. 코드 근거와 공식 문서

현재 코드에서 확인한 연결 지점:

- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/RootScreen.kt`: 홈, 피드, 발견, 서재 탭과 로그인 안내
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/Navigation.kt`: 상세 진입과 로그인 후 행동 재개
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/search/SearchViewModel.kt`: 검색, 정렬, 페이지 추가, 등록 요청
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/register/BookRegistrationViewModel.kt`: 검증과 인증 대기, 서버 등록 결과
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/bookdetail/BookDetailViewModel.kt`: 서재, 독서 기록, 감상, 답글, 좋아요
- `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/feed/FeedScreen.kt`: 감상 노출과 책 선택

외부 사양 확인일: 2026-10-02.

- [Firebase 이벤트 기록](https://firebase.google.com/docs/analytics/ios/events)
- [BigQuery 내보내기 스키마](https://support.google.com/analytics/answer/7029846?hl=en)
- [Firebase BigQuery 내보내기](https://firebase.google.com/docs/projects/bigquery-export)
- [GA4 고유값 개수와 보고서 제한](https://support.google.com/analytics/answer/12226705?hl=en)
- [Firebase DebugView](https://firebase.google.com/docs/analytics/debugview)

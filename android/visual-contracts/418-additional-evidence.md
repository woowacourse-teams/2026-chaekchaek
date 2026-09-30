# 추가 화면 재디자인 근거

2026-09-30 최신 Figma `tn59Thk2GRcVLkzoO8k9Sr`의 추가 페이지 `913:2`를 Android 공통 UI에 적용한 근거랍니다. 주요 화면 5개 PR과 분리된 추가 화면 작업이며 iOS 실행과 캡처는 사용자 결정에 따라 제외한답니다.

## 원본과 대응

계정 915:3/26/43/61/81, 독서와 감상 916:30/52/74/102/129, 답글과 내 글 918:66/83/100/113/129, 서재 918:491/516/541/557, 빈 상태와 실패 919:135/155/175/195/216, 작은 화면 920:193의 내부 시트 920:196의 최신 컨텍스트를 읽었답니다. 920:180의 별도 상태는 미검증이랍니다. 구현용 내부 노드 916:33, 916:77, 916:105, 920:196은 별도로 고해상도 컨텍스트를 읽었답니다. 구현 기준 원본 스크린샷은 별도 get_screenshot 또는 use_figma 렌더로 확인했답니다. 고운돋움과 Pretendard 문자 아이콘 결정이 과거 가이드보다 우선한답니다.

| 원본 | 구현 |
| --- | --- |
| 915:3, 919:195 | ui/archive/MyPageScreen.kt |
| 915:26, 915:43 | ui/common/LoginRequiredSheet.kt |
| 915:61, 919:135, 920:180 | ui/archive/ArchiveDialogs.kt |
| 915:81 | ui/archive/MyPageScreen.kt |
| 916:33, 919:175 | ui/bookdetail/BookRatingDialog.kt |
| 916:52, 919:155 | ui/bookdetail/BookDetailSheets.kt의 PageInputDialog |
| 916:77, 916:105, 920:196 | ui/bookdetail/BookDetailSheets.kt의 ReviewInputSheet |
| 916:129 | ui/bookdetail/BookDetailSheets.kt의 초안 취소 확인 |
| 918:66, 918:83 | ui/bookdetail/BookDetailSheets.kt의 ReplyInputSheet |
| 918:100, 918:113, 918:129 | ui/bookdetail/BookDetailSheets.kt의 메뉴와 삭제 확인 |
| 918:491, 918:557 | ui/archive/ArchiveScreen.kt의 편집 전용 UI와 오류 |
| 918:516, 918:541 | ui/archive/ArchiveDialogs.kt |
| 919:216 | 기존 BookDetailScreen Snackbar 오류 흐름 유지 |

## 유지한 동작

감상 제출은 기존 BookDetailInputRules를 사용하므로 공백 제출, 1000자 제한, 쪽수 범위, 스포일러와 초안 취소 조건을 유지한답니다. 필수 안내는 초기 안내이며 오류로 표시하지 않는답니다. 닉네임은 기존 Nickname 검증과 최대 10자 입력을 유지한답니다. 익명 해제, 실제 회원 탈퇴, 삭제와 저장 콜백은 기존 호출부를 유지한답니다.

평점은 Rating의 0.5-5.0 범위와 반점 단위를 유지한답니다. 별 좌우 절반 선택 외에 48dp 감소/증가 버튼을 추가했고 경계에서는 해당 버튼을 비활성화한답니다. 비교 기록은 BookDetailViewModel의 listOfNotNull(lower, current, higher)라 최대 세 작품이며, 실제 0/1/2개 기록에 예시를 채우지 않는답니다. 별 자산은 Figma 932:189의 채워진 별 벡터를 SVG 원본으로 보존하고, Android에서 표시할 투명 PNG로 변환했답니다. 처음 받은 932:187 프레임 PNG는 불투명한 흰 배경 때문에 Icon tint 적용 시 사각형으로 표시되어 교체했답니다. 최종 PNG의 알파 범위는 0-255이며 원본 벡터의 형태와 색상을 유지했답니다.

Android GoogleSignInButton actual은 Unit이므로 기존 Android 검은 fallback을 유지한답니다. 공급업체 내부 스타일을 재구현하지 않았고 로그인 콜백과 signingIn 비활성화를 유지한답니다.

## 빌드와 테스트

`./gradlew :app:assembleDebug :shared:testAndroidHostTest testDesignUiHarness testChaekchaekVisualAdapter --console=plain`이 성공했답니다. 공통 호스트 테스트 148개, 디자인 검사기 테스트 28개, 어댑터 테스트 21개가 통과했답니다. 기존 BookDetailRulesTest의 감상 공백/길이/쪽수 범위, 반점 10개와 답글 경계 검증을 사용했답니다. 테스트 통과는 원본 픽셀 일치나 실제 사용자 입력 성공을 의미하지 않는답니다.

## 미검증과 차이

전체 속성 원본 수집은 아직 미완료랍니다. collect_design_figma.js가 913:2에서 659노드와 4,722,064자 압축 데이터를 보고했으나 첫 16,000자만 확보했답니다. 원본 데이터가 잘렸으므로 복원하거나 통과로 판정하지 않았답니다. figma.io.write JSON export는 도구 결과에 파일을 반환하지 않았답니다. 최신 계약은 418-additional.job.json이며 `./gradlew verifyDesignUi -PdesignJob=visual-contracts/418-additional.job.json`을 실행한 결과 source.json 부재로 검사 프로세스가 exit 2로 차단됐답니다. 첫 수집 조각은 ignored build/design-guard/418-additional/source-chunks.partial.json에 보존했답니다.

화면 실행 캡처, 320/390 너비, 키보드 표시, 긴 입력, 실데이터 평점 비교, 빈 기록, 실패/재시도, 작성/수정/취소와 삭제 확인은 최종 stacked APK에서 별도 확인해야 한답니다. 자동 속성 검토는 pending이며 미검증을 pass로 바꾸지 않았답니다. 일반 텍스트는 공통 의미 기반 Typography 역할에 연결했으므로 Figma 일부 21px/23px 값과 코드 역할의 크기가 다를 수 있답니다. 별점 카드 긴 제목 두 줄 말줄임과 서재 편집에서 기존 책표지/정보 유지도 픽셀 일치로 검증되지 않았답니다. 요청 실패는 기존 Snackbar 표시와 소비 흐름을 유지했으므로 Figma의 정적 오류 카드 샘플을 새 기능으로 추가하지 않았답니다.

회원 탈퇴와 삭제 API의 실제 실행은 이 작업의 캡처를 위해 호출하지 않는답니다. iOS는 실행과 Dynamic Type 확인을 수행하지 않았답니다. UI 함수 분리 외에 도메인/저장/API 책임을 옮기지 않았으며 이번 읽은 변경 범위에서 재현 가능한 객체지향 법칙 위반을 확정하지 않았답니다.

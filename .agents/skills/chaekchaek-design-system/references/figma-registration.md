# Figma 등록 확인

## 구현 전 확인 기록

UI 변경마다 아래 실제 조회 결과를 작업 기록에 남긴다. 예전 기록의 링크는 탐색 시작점이며 현재 존재와 상태를 다시 확인한다.

| 확인 대상 | 필요한 근거 |
| --- | --- |
| 대상 화면 | Figma 링크, 필요한 상태, 기준 구현 또는 승인 시안 |
| 공통 메인 컴포넌트 | COMPONENT 또는 COMPONENT_SET 노드 링크, 상태 변형과 텍스트 속성 |
| 공통 레퍼런스 | 레퍼런스 링크, INSTANCE 노드가 위 메인 컴포넌트에 연결된 조회 결과 |
| 검증 | 변수/스타일 재사용, 글꼴, 화면과 레퍼런스 캡처의 잘림 및 겹침 확인 |

검색 결과가 비어 있으면 디자인 시스템 페이지의 로컬 컴포넌트를 직접 조회한다. 검색 결과 없음은 캔버스에 컴포넌트가 없다는 증거가 아니다. 일반 프레임, 분리된 인스턴스, 전체 UI 스크린샷도 메인 컴포넌트로 취급하지 않는다.

- 화면 없음: Figma 시안을 먼저 생성하고 확인한다.
- 메인 컴포넌트 없음: 공통 요소를 컴포넌트로 생성한다.
- 메인 컴포넌트는 있고 레퍼런스 없음: 기존 컴포넌트의 인스턴스만 추가한다.
- 필요한 상태만 없음: 기존 세트와 레퍼런스에 해당 상태를 보강한다.
- 둘 다 있음: 재사용한다. 이름만 다른 중복 컴포넌트를 생성하지 않는다.
- Figma 조회/등록 실패: 해당 UI 코드를 적용하지 않고 실패 원인을 보고한다.

## 선택 컨트롤 확인 사례

2026-10-07에 `origin/an-develop`의 `6c12092e`와 PR #442 실행 캡처를 대조했다. 아래 링크는 파일 내 등록이며 팀 라이브러리 게시 증거가 아니다.

| 구현 | 메인 컴포넌트 | 공통 레퍼런스 |
| --- | --- | --- |
| `ChaekFilterChip` | [FilterChip](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=903-113) | [04 필터](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=905-252) |
| `ChaekDropdown` | [Dropdown](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1002-384) | [09 정렬 드롭다운](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1003-355) |
| 드롭다운 메뉴 항목 | [DropdownOption](https://www.figma.com/design/tn59Thk2GRcVLkzoO8k9Sr?node-id=1002-363) | 펼침 상태 안의 선택/비선택 인스턴스 |

구현은 `android/shared/src/commonMain/kotlin/com/chaekchaek/app/ui/common/ChaekSelectionControls.kt`, 실행 캡처는 `android/visual-contracts/pr-442/screenshots/`를 참조한다.

필터는 이미 등록되어 있어 재사용했다. 드롭다운은 Default, Expanded, Disabled 세 상태와 메뉴 항목 Default, Selected를 추가했다. Figma의 Gowun Dodum Regular와 Android의 합성 SemiBold 및 elevation 렌더링은 같다고 단정하지 않는다. 펼침 시안은 정적 상태이며 실제 선택/닫힘 동작의 디바이스 검증을 대신하지 않는다.

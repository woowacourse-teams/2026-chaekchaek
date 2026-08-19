# iOS App Store 심사 준비

## 현재 구현 범위

- iOS 17 이상, iPhone 전용 SwiftUI 앱
- 로그인 없이 알라딘 Open API 도서 검색
- 독서 상태를 선택해 로컬 서재에 추가, 변경, 삭제
- `UserDefaults`에 서재 기록 저장
- 앱 안에서 개인정보처리방침과 문의 링크 제공
- 번들 ID `com.chamsae.chaekchaek`, 버전 `1.0`, 빌드 `1`

검색과 서재의 KMP 이전은 하지 않았다. 현재 공유 모듈에는 이 기능이 없고 Android 앱에만
구현되어 있어, 첫 심사 빌드에서는 SwiftUI와 Foundation만 사용한다. 제품 기능을 iOS와 Android가
공동으로 확장할 때 공유 모듈 이전을 별도 작업으로 진행한다.

## 로컬 설정

1. `iosApp/Configuration/Secrets.xcconfig.example`을 `Secrets.xcconfig`로 복사한다.
2. `ALADIN_TTB_KEY`에 실제 키를 넣는다.
3. 실제 키가 든 `Secrets.xcconfig`는 커밋하지 않는다.

키가 없으면 앱은 실행되지만 검색 시 설정 안내 오류를 표시한다.

## 개발자 계정 등록 후 할 일

1. Apple Developer Program 등록을 완료한다.
2. Xcode에서 `iosApp/iosApp.xcodeproj`를 연다.
3. Chaekchaek 타깃의 Signing & Capabilities에서 본인 Team을 선택한다.
4. App Store Connect에 번들 ID `com.chamsae.chaekchaek`로 앱 레코드를 만든다.
5. Release Archive를 만들고 Validate App을 통과한 뒤 업로드한다.

현재 프로젝트는 자동 서명을 사용하고 Team ID만 비워 두었다. 2026-08-19 기준 업로드 빌드는
Xcode 26 이상과 iOS 26 SDK를 사용해야 한다.

## 검증 명령

`android/iosApp`에서 실행한다.

```bash
xcodebuild test \
  -project iosApp.xcodeproj \
  -scheme Chaekchaek \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' \
  -derivedDataPath DerivedData \
  CODE_SIGNING_ALLOWED=NO

xcodebuild build \
  -project iosApp.xcodeproj \
  -scheme Chaekchaek \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -derivedDataPath DerivedData \
  CODE_SIGNING_ALLOWED=NO
```

## App Store Connect 초안

- 이름: 첵췍
- 부제: 읽고 싶은 책을 한곳에
- 기본 카테고리: 도서
- 키워드: 독서,책검색,서재,독서기록,책관리
- 개인정보처리방침: `https://app.notion.com/p/3b185850b3e18085b919d108ce7cd4ef?source=copy_link`
- 지원 URL: 미정

설명 초안:

> 첵췍은 읽고 싶은 책과 읽고 있는 책, 다 읽은 책을 한곳에 정리하는 로컬 서재입니다.
> 제목이나 저자로 도서를 검색하고 독서 상태를 선택해 바로 담을 수 있습니다.
> 로그인 없이 사용할 수 있으며 서재 기록은 사용자의 기기에만 저장됩니다.

심사 메모 초안:

> 로그인이나 테스트 계정은 필요하지 않습니다. 검색 탭에서 책을 검색하고 결과 오른쪽의 추가
> 버튼을 누른 뒤 독서 상태를 선택하면 서재 탭에서 확인할 수 있습니다. 상태 변경과 삭제도
> 서재 탭에서 가능합니다. 서재 기록은 기기에만 저장되며 결제 기능은 없습니다.

## 제출 전 필수 확인

- 지원 URL에는 사용자가 연락할 수 있는 실제 이메일, 전화번호 또는 법적 주소가 있어야 한다.
  현재 GitHub 이슈 링크는 앱 안의 임시 문의 경로일 뿐 App Store 지원 URL로 확정하지 않는다.
- 개인정보처리방침을 로그아웃한 브라우저에서 열어 공개 접근을 확인한다.
- 개인정보처리방침에 검색어가 알라딘 Open API로 전송되는 사실과 처리 목적을 명시한다.
- 알라딘이 검색어 또는 IP를 실시간 요청 처리보다 오래 보관하는지 확인한다.
- App Privacy는 보수적으로 Search History, App Functionality, 사용자와 연결 안 됨, 추적 안 함으로
  설정했다. 알라딘의 실제 보관 정책을 확인한 뒤 App Store Connect 답변과 manifest를 일치시킨다.
- 실제 키를 넣은 기기에서 검색, 서재 저장, 앱 재실행 후 유지, 상태 변경, 삭제를 확인한다.
- 앱 아이콘은 기존 512px 자산을 1024px로 확대한 임시본이다. 제출 전 원본 1024px 디자인 자산으로
  교체하면 선명도가 좋아진다.

## 근거

- [Apple Upcoming Requirements](https://developer.apple.com/news/upcoming-requirements/)
- [Apple App Privacy Details](https://developer.apple.com/app-store/app-privacy-details/)
- [Apple Platform Version Information](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information/)
- [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [Apple Privacy Manifest Files](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files)

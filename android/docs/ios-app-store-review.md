# iOS App Store 심사 준비

## 현재 제출 상태

- 제출 ID: `7294cb83-ecee-4ec2-831a-a11ce59afd2a`
- 거절 빌드: `1.1.0 (2)`
- 거절 사유: Guideline 2.1 - Information Needed - New App Submission
- 재제출 빌드: `1.1.1 (4)`, 2026-09-02 App Store Connect 업로드 성공, 처리 확인 대기
- 번들 ID: `com.chamsae.chaekchaek`
- 지원 범위: iOS 17 이상, iPhone 전용
- 이전 TestFlight 업로드: `1.1.1 (3)`, 2026-08-31

`1.1.1 (4)`에는 Apple과 Google 로그인, 서버 서재, 독서 상태와 진행률, 별점, 감상과 답글,
마이페이지, 앱 내부 회원 탈퇴가 포함된다. 결제, 구독, 광고, AI 기능과 카메라, 위치, 연락처,
마이크, 사진 보관함, App Tracking Transparency 권한 요청은 없다.

## 제출 전 확인이 필요한 값

- 실제 테스트한 iPhone 모델과 최신 iOS 버전
- 실제 기기 화면 녹화 파일 또는 심사관이 접근할 수 있는 URL
- App Store Connect의 실제 배포 지역
- Aladin Open API 이용 권한과 도서 정보 및 표지 사용 근거
- 로그아웃한 브라우저에서 접근 가능한 개인정보처리방침과 지원 URL

## 실제 기기 녹화 순서

촬영 전 아래 상태를 준비한다.

- 최신 iOS가 설치된 실제 iPhone에 심사 제출과 동일한 빌드를 설치한다.
- 집중 모드를 켜고 알림 미리보기를 꺼서 개인 정보 노출을 막는다.
- 앱을 로그아웃하고 완전히 종료한다.
- 최초 로그인과 마지막 회원 탈퇴에 사용할 촬영 전용 소셜 계정을 준비한다.
- 검색, 서재 기록, 감상 작성에 사용할 책 한 권과 짧은 테스트 문구를 미리 정한다.

영상은 편집 없이 한 번에 촬영하며 iPhone 홈 화면에서 앱을 실행하는 장면부터 시작한다.

1. 앱 아이콘을 눌러 cold launch하고 홈의 인기 도서와 최신 감상을 짧게 확인한다.
2. 발견에서 준비한 책 제목을 검색하고 검색 결과에서 책 상세를 연다.
3. 책 상세에서 서재 추가를 눌러 로그인 바텀시트를 표시한다.
4. Apple로 계속하기를 선택하고 촬영 전용 계정으로 최초 로그인한다. 최초 성공 시 첵췍 계정이
   자동 생성되며 별도 회원가입 양식은 없다는 흐름이 보이게 한다.
5. 다시 책 상세에서 책을 내 서재에 추가한다.
6. 내 서재에서 독서 상태, 읽은 쪽수와 별점을 기록하고 저장 결과를 확인한다.
7. 책 상세에서 감상을 작성하고 공개 감상 목록에 나타난 결과를 확인한다.
8. 방금 작성한 감상에 답글을 작성하고 답글이 표시되는지 확인한다.
9. 내 서재의 프로필 이미지를 눌러 마이페이지로 이동하고 익명 해제와 계정 관리 영역을 확인한다.
10. 회원 탈퇴를 누르고 공통 확인 다이얼로그에서 탈퇴를 완료한다.
11. 로그아웃 상태로 돌아온 화면을 2초 정도 보여준 뒤 녹화를 종료한다.

회원 탈퇴는 실제 계정을 삭제하므로 반드시 마지막에 수행한다. 결제, 구독, 민감 권한 요청,
신고와 사용자 차단은 현재 앱에 없으므로 영상에서 존재하는 기능처럼 설명하지 않는다. 완성된
영상은 재생해 앱 실행, 로그인, 감상 게시, 답글, 회원 탈퇴가 끊김 없이 보이는지 확인한 뒤
PR #320 본문과 App Store Connect 심사 메시지에 각각 첨부한다.

## App Review Notes 초안

아래 대괄호 항목을 실제 값으로 교체한 뒤 4,000자 제한을 확인한다.

```text
App Review Information for ChaekChaek

1. Screen recording
A screen recording captured on a physical iPhone running the latest available iOS is attached to this review message: [ATTACH RECORDING OR INSERT ACCESSIBLE URL].

The recording begins with a cold launch and demonstrates the typical flow: browsing the Home feed, searching for a book in Discover, opening book details, signing in with Apple, adding the book to My Library, changing reading status and progress, rating the book, creating a review and reply, opening My Page from the profile image in My Library, and completing account deletion.

There are no purchases, subscriptions, paid content, camera, location, contacts, microphone, photo library, or App Tracking Transparency prompts in the app.

The app includes user-created reviews and replies. Authors can edit or delete their own content. The current build does not provide content-reporting or user-blocking controls.

2. Devices and operating systems tested
- [PHYSICAL IPHONE MODEL], iOS [VERSION]
- iPhone 17 Pro Simulator, iOS 26.2, including Large and AX5 accessibility text-size validation

3. App functions and target audience
ChaekChaek is a Korean-language reading companion for readers who want to discover books, organize books by reading status, record reading progress and ratings, and share short reading impressions with other readers.

4. Setup and access instructions
No sample files are required.

Basic browsing:
1. Launch the app.
2. Use Home to browse popular books and recent reviews.
3. Open Discover to search by book title or author.
4. Select a result to view book details and public reviews.

Account-based features:
1. Open My Library and tap the profile image, or attempt to add a book to My Library.
2. On the login sheet, tap Continue with Apple.
3. The reviewer may use their own Apple ID. A ChaekChaek account is created automatically on first successful sign-in, so no separate username, password, or pre-created demo account is required.
4. Google Sign-In is also available as an alternative.
5. After signing in, the reviewer can add books, set reading status and progress, rate books, and manage My Library.

Account deletion:
1. Open My Library.
2. Tap the profile image to open My Page.
3. Under Account Management, tap Delete Account.
4. Review the irreversible deletion notice and tap Delete Account to confirm.
5. The app deletes the account through the ChaekChaek service and clears the local authenticated session.

5. External services, tools, and platforms
- Apple Authentication Services for Sign in with Apple
- Google Sign-In SDK for optional Google authentication
- ChaekChaek backend API at api.chaekchaek.com for authentication, book discovery, library records, ratings, reviews, replies, and account deletion
- Aladin Open API, accessed through the ChaekChaek backend, for bibliographic metadata and book cover images
- Apple Keychain for storing the refresh token on the device

The app does not use a payment processor, subscription service, advertising SDK, or AI service.

6. Regional differences
The app provides the same features in every distributed region. The interface and primary content are Korean. There are no region-specific purchases, subscriptions, or restrictions. Distribution regions: [CONFIRM APP STORE DISTRIBUTION REGIONS].

7. Regulated industry and protected third-party material
ChaekChaek is a general book-discovery, reading-record, and community app. It does not provide medical, financial, gambling, legal, or other highly regulated services.

Book bibliographic metadata and cover images are supplied through the Aladin Open API. The app does not provide full book text or paid reading content. Authorization evidence: [CONFIRM OR ATTACH ALADIN OPEN API USE EVIDENCE IF REQUESTED].
```

## 알려진 심사 위험

공개 감상과 답글은 사용자 생성 콘텐츠지만 현재 신고와 사용자 차단 기능이 없다. 이번 재제출에서는
구현하지 않기로 결정했으며, Apple Guideline 1.2로 추가 거절될 위험을 수용한다. 심사 메모와
화면 녹화에서 존재하지 않는 기능을 제공한다고 설명하지 않는다.

## 검증 명령

`android` 디렉터리에서 실행한다.

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme Chaekchaek \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' \
  CODE_SIGNING_ALLOWED=NO

xcodebuild build \
  -project iosApp/iosApp.xcodeproj \
  -scheme Chaekchaek \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO
```

## 근거

- [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [Apple App Privacy Details](https://developer.apple.com/app-store/app-privacy-details/)
- [Apple Platform Version Information](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information/)
- [Apple Privacy Manifest Files](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files)

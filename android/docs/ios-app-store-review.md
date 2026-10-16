# iOS App Store 심사 준비

## 2026-10-16 배포 빌드 준비

- 준비 버전: `1.3.0 (9)`
- 이전 스토어 사용 버전: `1.2.2 (8)`, 사용자 확인
- 기준 소스 커밋: `6b3d02276516617d755445cf7579fb586f39e9c0`
- 버전 변경과 검증 기록 커밋: `93b7de787c82490c03bd890d9023f6267d3edeb6`
- Archive: `android/iosApp/release/Chaekchaek-1.3.0-9.xcarchive`
- Simulator 검증: iPhone 17 Pro, iOS 26.2, 기본 글자 크기와 AX5에서 각각 8개 통과, 로컬 fixture 테스트 1개 건너뜀, 실패 0개
- 접근성 검증 범위: 기존 홈 캐러셀 표시점의 작은 터치 영역은 테스트에 예외가 있음. 전체 HIG 준수 판정은 하지 않음
- 캡처 확인: 기본 크기와 AX5 로그인 시트, AX5 마이페이지. AX5 시트 배경의 서재 필터 영역에 기존 글자 겹침이 보임
- 검증 환경: 동일 소스와 버전 변경을 적용한 임시 detached worktree의 공유 scheme. 기존 개인 scheme은 변경하지 않음
- Simulator 정리: 원래 글자 크기를 복원하고 이번 작업에서 부팅한 기기 종료 확인
- Release Archive 검증: production 설정으로 생성 성공. 앱과 Archive의 버전 `1.3.0 (9)`, 번들 ID, Apple 로그인 entitlement와 깊은 코드 서명 검증 통과
- 서명 유형: 로컬 Archive는 Apple Development. 이후 Xcode Organizer의 App Store Connect 배포 경로로 업로드 완료
- 앱 실행 파일 SHA-256: `8997f93046eaca6ff0c6bfbaf97948ba3b5b57b0afa6a42d359de317abaa0cfe`
- 앱 실행 파일과 dSYM UUID 일치: `148C8A6F-51E1-361C-9977-2C3633CD1BA3`
- 실기기 release 실행과 공급업체 로그인: 이번 작업에서는 미검증
- App Store Connect 업로드: 2026-10-16 14:01 KST 완료. Xcode Organizer에서 `1.3.0 (9)`, `Uploaded to Apple`, build `9` 확인
- 업로드 경고: FirebaseAnalytics, GoogleAdsOnDeviceConversion, GoogleAppMeasurement, GoogleAppMeasurementIdentitySupport의 dSYM 누락 4건
- TestFlight 처리 상태: 미확인. 업로드 성공을 테스터 배포 가능 상태로 해석하지 않음
- App Store 심사 제출: 수행하지 않음
- 관련 이슈와 PR: #531, #532

아래 제출 ID, 심사 메모와 기기 검증 결과는 과거 제출 기록이다. 이번 버전의 검증이나 현재 심사 상태를 의미하지 않는다.

## 현재 제출 상태

- 제출 ID: `7294cb83-ecee-4ec2-831a-a11ce59afd2a`
- 거절 빌드: `1.1.0 (2)`
- 거절 사유: Guideline 2.1 - Information Needed - New App Submission
- 재제출 빌드: `1.1.1 (5)`, 2026-09-03 TestFlight 처리 완료, 제출 준비 완료
- 번들 ID: `com.chamsae.chaekchaek`
- 지원 범위: iOS 17 이상, iPhone 전용
- 이전 App Store Connect 업로드: `1.1.1 (4)`, 2026-09-02

`1.1.1 (5)`에는 Apple과 Google 로그인, 서버 서재, 독서 상태와 진행률, 별점, 감상과 답글,
마이페이지, 앱 내부 회원 탈퇴가 포함된다. 결제, 구독, 광고, AI 기능과 카메라, 위치, 연락처,
마이크, 사진 보관함, App Tracking Transparency 권한 요청은 없다.

## 재제출 빌드 검증

- 소스 커밋: `94f92d8a`
- Archive: `android/iosApp/release/Chaekchaek-1.1.1-5.xcarchive`
- Archive 크기: 93MB
- 앱 바이너리 SHA-256: `385083f4b99e4f860ea95f861cb80c12cad9c93e1c8e3aa67074deb7a42a37f3`
- iPhone 17 Pro Simulator, iOS 26.2: 단위 테스트 3개와 UI 테스트 3개 통과
- Archive 버전과 빌드: `1.1.1 (5)`
- 서명과 Apple 로그인 entitlement 검증 통과
- App Store Connect 업로드 및 TestFlight 처리 완료, 제출 준비 완료
- iPhone 13에 TestFlight `1.1.1 (5)`를 설치해 주요 흐름 최종 확인

## 제출 전 확인이 필요한 값

- YES24 Open API 사용 권한을 보여주는 화면이 추가로 요청될 경우 API 키를 가린 증빙

App Store 지원 URL은 `https://chaekchaek.com/intro`로 변경하고 재접속해 저장을 확인했다.
개인정보처리방침 URL도 로그아웃 상태에서 정상 접근됨을 확인했다.

## 실제 기기 녹화 순서

### 촬영 전 준비

- 최신 iOS가 설치된 실제 iPhone에 심사 제출과 동일한 빌드를 설치한다.
- 집중 모드를 켜고 알림 미리보기를 꺼서 개인 정보 노출을 막는다.
- 최초 로그인과 마지막 회원 탈퇴에 사용할 촬영 전용 Apple 계정을 준비한다.
- 해당 계정으로 이전 로그인 테스트를 했다면 녹화 전에 앱에서 회원 탈퇴해 책췍 계정을 삭제한다.
- 앱이 로그아웃된 것을 확인하고 완전히 종료한다.
- `데미안` 검색 결과가 나타나는지 확인한 뒤 앱을 다시 완전히 종료한다.

녹화 중 입력값은 아래와 같이 고정한다.

```text
검색어: 데미안
읽은 쪽수: 10
별점: 4
감상: 다시 읽고 싶은 문장이 많은 책이에요.
답글: 저도 인상 깊게 읽었어요.
```

### 녹화 행동 스크립트

영상은 편집 없이 한 번에 촬영한다. 각 저장이나 화면 전환 뒤에는 결과가 보이도록 1초에서
2초 정도 멈춘다.

1. iPhone 제어 센터에서 화면 녹화를 시작하고 홈 화면으로 돌아간다.
2. 3초 카운트다운이 끝나면 책췍 앱 아이콘을 눌러 앱을 실행한다.
3. 홈 화면이 로드되면 인기 도서를 보여주고 아래로 한 번 스크롤해 최신 감상을 보여준다.
4. 하단의 `발견`을 누른다.
5. 검색창을 누르고 `데미안`을 입력한다.
6. 검색 결과에서 `데미안` 한 권을 눌러 책 상세로 이동한다.
7. 책 상세 오른쪽 위의 `서재에 추가` 아이콘을 누른다.
8. 로그인 바텀시트가 나타나면 `Apple로 계속하기`를 누른다.
9. 촬영 전용 Apple 계정으로 인증을 완료한다.
10. 책 상세로 돌아오면 `서재에 추가` 아이콘을 다시 누르고 선택 상태가 바뀌는지 확인한다.
11. 아래로 스크롤해 `내 독서 기록`을 찾는다.
12. `읽는 중`을 누른다.
13. `쪽수 입력`을 누르고 `10`을 입력한 뒤 `저장`을 누른다.
14. `별점 주기`를 누른다. 기본 선택된 별점 4를 확인하고 `별점 저장`을 누른다.
15. 화면 아래의 `이 순간의 감상 남기기`를 누른다.
16. `느낀점`에 `다시 읽고 싶은 문장이 많은 책이에요.`를 입력하고 `감상 남기기`를 누른다.
17. 공개 감상 목록에 방금 작성한 감상이 나타나는지 확인한다.
18. 방금 작성한 감상의 `감상에 답글 작성`을 누른다.
19. `답글`에 `저도 인상 깊게 읽었어요.`를 입력하고 `등록`을 누른다.
20. 방금 작성한 답글이 감상 아래에 나타나는지 확인한다.
21. 하단의 `내 서재`를 누른다.
22. 오른쪽 위 프로필 이미지를 눌러 마이페이지로 이동한다.
23. `익명으로 감상 공개` 영역과 `계정 관리` 영역이 한 화면에 보이도록 천천히 스크롤한다.
24. `회원 탈퇴`를 누른다.
25. 확인 다이얼로그의 안내 문구를 2초 정도 보여준 뒤 `탈퇴하기`를 누른다.
26. 로그아웃 상태로 돌아온 화면을 2초 정도 보여준다.
27. iPhone 제어 센터를 열어 화면 녹화를 종료한다.

회원 탈퇴는 실제 계정을 삭제하므로 반드시 마지막에 수행한다. 결제, 구독, 민감 권한 요청,
신고와 사용자 차단은 현재 앱에 없으므로 영상에서 존재하는 기능처럼 설명하지 않는다. 완성된
영상은 재생해 앱 실행, 로그인, 감상 게시, 답글, 회원 탈퇴가 끊김 없이 보이는지 확인한 뒤
PR #320 본문과 App Store Connect 심사 메시지에 각각 첨부한다.

## App Review Notes 및 회신 최종안

App Review Information의 Notes와 Guideline 2.1 회신에 아래 내용을 사용한다.

```text
Hello App Review Team,

Thank you for your guidance. We addressed the requested information and tested iOS build 1.1.1 (5) through TestFlight. Please find all requested details below.

App Review Information for 책췍, iOS 1.1.1 (5)

1. Screen recording
A screen recording captured on a physical iPhone running the latest available iOS is attached to this review reply as `책췍심사영상-오디오제거.mp4`.

The recording begins with a cold launch and shows Home, book search and details, Sign in with Apple, My Library, reading progress, rating, a review and reply, My Page, and account deletion.

There are no purchases, subscriptions, paid content, camera, location, contacts, microphone, photo library, or App Tracking Transparency prompts in the app.

The app includes user-created reviews and replies. Authors can edit or delete their own content. The current build does not provide content-reporting or user-blocking controls.

2. Devices and operating systems tested
- iPhone 13, iOS 26.6.1, TestFlight build 1.1.1 (5)
- iPhone 17 Pro Simulator, iOS 26.2

3. App functions and target audience
책췍 is a Korean-language reading companion for readers who want to discover books, organize books by reading status, record reading progress and ratings, and share short reading impressions with other readers.

4. Setup and access instructions, No sample files are required.

Basic browsing:
1. Launch the app.
2. Use Home to browse popular books and recent reviews.
3. Open Discover to search by book title or author.
4. Select a result to view book details and public reviews.

Account-based features:
1. Open My Library and tap the profile image, or attempt to add a book to My Library.
2. On the login sheet, tap Continue with Apple.
3. The reviewer may use their own Apple ID. The first successful sign-in automatically creates a 책췍 account, so no demo credentials are required.
4. Google Sign-In is also available as an alternative.
5. After signing in, the reviewer can add books, set reading status and progress, rate books, and manage My Library.

Account deletion:
1. Open My Library.
2. Tap the profile image to open My Page.
3. Under Account Management, tap Delete Account.
4. Review the irreversible deletion notice and tap Delete Account to confirm.
5. The app deletes the account and clears the local authenticated session.

5. External services, tools, and platforms
- Apple Authentication Services for Sign in with Apple
- Google Sign-In SDK for optional Google authentication
- 책췍 backend API at api.chaekchaek.com for authentication, book discovery, library records, ratings, reviews, replies, and account deletion
- YES24 Open API, accessed only through the 책췍 backend, for book search results, bibliographic metadata, and book cover images
- Apple Keychain for storing the refresh token on the device

The app does not use a payment processor, subscription service, advertising SDK, or AI service.

6. Regional differences
The app is distributed only in South Korea and provides the same features throughout that region. The interface and primary content are Korean. There are no region-specific purchases, subscriptions, or restrictions.

7. Regulated industry and protected third-party material
책췍 is a general book-discovery, reading-record, and community app. It does not provide medical, financial, gambling, legal, or other highly regulated services.

Book search results, metadata, and cover images are supplied through the YES24 Open API via the 책췍 backend. Its API key is stored only on the backend. The app does not provide full book text or paid reading content. Documentation: https://developers.yes24.com/, https://developers.yes24.com/api-doc/goods-item-list, and https://developers.yes24.com/api-doc/goods-item-detail. A redacted developer-account screenshot can be provided if requested.

Thank you.
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
- [YES24 Open API](https://developers.yes24.com/)
- [YES24 상품 검색 API](https://developers.yes24.com/api-doc/goods-item-list)
- [YES24 상품 상세 API](https://developers.yes24.com/api-doc/goods-item-detail)

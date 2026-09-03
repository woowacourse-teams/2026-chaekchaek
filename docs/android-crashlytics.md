# Android Crashlytics와 난독화 오류 대응

## 목적

release APK의 R8 코드 축소·난독화 상태에서도 크래시를 수집하고, 사람이 읽을 수 있는
스택 트레이스로 복원한다.

## 현재 구성

- `release`는 `isMinifyEnabled = true`, `isShrinkResources = true`로 R8을 실행한다.
- Firebase Crashlytics SDK와 Firebase BoM은 `releaseImplementation`으로만 포함한다.
- Crashlytics Gradle plugin은 release mapping 파일을 자동 업로드한다.
- `firebase_crashlytics_collection_enabled=true`로 release 앱의 자동 수집을 요청한다.
- release용 실제 `android/app/google-services.json`은 Git에서 제외한다.
- debug와 integration은 Firebase SDK를 포함하지 않으며, 빌드만 가능한 자리표시자
  `google-services.json`을 각 소스 세트에 둔다.

`google-services.json`의 실제 파일은 Firebase Console에서 내려받아
`android/app/google-services.json`에만 둔다. 앱 패키지명과 파일의 Android 클라이언트
패키지명이 일치해야 한다.

## 릴리스 빌드와 mapping

```sh
cd android
./gradlew :app:assembleRelease
```

release APK는 `app/build/outputs/apk/release/app-release.apk`, R8 mapping 파일은
`app/build/outputs/mapping/release/mapping.txt`에 생성된다. Crashlytics plugin이 적용된
release 빌드는 `uploadCrashlyticsMappingFileRelease` 작업으로 mapping을 업로드한다.

업로드만 다시 확인할 때는 다음을 실행한다.

```sh
cd android
./gradlew :app:uploadCrashlyticsMappingFileRelease
```

mapping 파일은 같은 release 산출물과 짝으로 보관해야 한다. 다른 버전의 mapping 파일로는
난독화된 스택 트레이스를 정확히 복원할 수 없다.

## 최초 확인 절차

1. Firebase Console에서 `chaekchaek-d2b85` 프로젝트의 Crashlytics 수집을 활성화한다.
2. 서명된 release APK를 테스트 기기에 설치하고 한 번 실행한다.
3. 테스트용 VM 크래시를 한 번 발생시킨다.
4. 앱을 다시 실행해 보류된 보고서를 전송한다.
5. Crashlytics 대시보드에서 보고서와 원래 클래스·메서드명이 복원됐는지 확인한다.

에뮬레이터에서의 테스트 명령은 다음과 같다.

```sh
adb -s <device> shell am crash com.chamsae.chaekchaek
adb -s <device> shell monkey -p com.chamsae.chaekchaek 1
```

`am crash`는 앱 프로세스에 VM 크래시를 유도한다. 재실행 전에는 Crashlytics가 보류된
크래시 보고서를 전송하지 않는다.

## 확인한 결과와 현재 주의점

2026-09-03에 release APK를 에뮬레이터에 설치하고 VM 크래시와 재실행을 수행했다.
기기 로그에는 `FATAL EXCEPTION`과 Crashlytics 초기화가 기록됐다. 다만 Firebase 서버 설정은
`firebase_crashlytics_enabled: false`를 반환했으므로, Firebase Console에서 프로젝트 수집을
활성화한 뒤 대시보드 수신을 다시 확인해야 한다.

## 민감값 원칙

모바일 앱에 포함된 문자열은 난독화 여부와 관계없이 추출할 수 있다. `BuildConfig`, 리소스,
네이티브 코드, 암호화는 추출 비용을 높일 뿐 비밀 보관 수단이 아니다.

- 서버 권한을 주는 API 키, 서명 키, 서비스 계정 키는 앱에 넣지 않고 백엔드 또는 비밀 관리
  시스템에 둔다.
- 앱에 필요한 공개 식별자는 API 제한을 적용하고, 서버에서 인증·인가와 호출량 제한을 검증한다.
- `google-services.json`, `keystore.properties`, keystore 파일은 Git·PR·공개 채널에 올리지 않는다.

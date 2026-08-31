# Android 버전 및 릴리스 관리

이 문서는 QA와 릴리스 담당자가 현재 Play 상태, 빌드에 포함된 변경, 문제 발생 시 복구 기준을
한곳에서 확인하기 위한 기준 문서다. 버전을 바꾸거나 Play 상태가 바뀔 때 같은 커밋에서 이 문서를
갱신한다.

## 현재 상태

확인일: 2026-08-31

| 구분 | `versionName` | `versionCode` | Git 태그 | 상태 |
| --- | --- | --- | --- | --- |
| 프로덕션 배포 | 없음 | 없음 | 없음 | 아직 배포하지 않음 |
| 최신 Play 심사 | `1.0` | `2` | 없음 | 심사 통과, 관리형 게시 대기 |
| 현재 소스 | `1.1.1` | `5` | 없음 | 서명 AAB 생성, iOS TestFlight 업로드 |

QA는 테스트를 시작하기 전에 앱의 설정 화면이나 설치 파일 정보에서 `versionName`과
`versionCode`를 함께 확인한다. `1.0 (2)`처럼 두 값을 함께 말해야 같은 이름의 다른 빌드를
혼동하지 않는다.

현재 `1.0 (2)`를 만든 정확한 커밋과 상세 변경 내역은 기존 기록이 없어 확인할 수 없다. 따라서
이 버전은 심사 통과 사실만 기준으로 사용하며, 정확한 소스 복구 기준은 다음 릴리스 태그부터
관리한다.

2026-08-28 Play Console 게시 개요에서 프로덕션 `1.0 (2)` 전체 출시와 대한민국 배포를 포함한
13개 변경사항이 `게시 준비됨` 상태인 것을 확인했다. 관리형 게시가 활성화되어 있으므로 심사
통과와 실제 게시를 구분한다. 프로덕션 게시 버튼은 누르지 않으며, 타겟 연령 만 6-17세 설정은
의도한 값으로 유지한다.

## 두 버전 값의 역할

- `versionCode`는 Android와 Google Play가 새 빌드인지 판단하는 양의 정수다. Play에 올린 값보다
  큰 값을 다음 업로드에 사용한다. 이미 사용한 값은 재사용하지 않으며 허용되는 최댓값은
  `2100000000`이다.
- `versionName`은 사용자와 QA에게 보이는 버전 문자열이다. 제품 변경의 크기를 표현하지만 Play의
  업그레이드 순서를 결정하지 않는다.
- Android는 설치된 앱보다 낮은 `versionCode`의 앱 설치를 막는다. 스토어에서 이전 빌드를 그대로
  다시 배포하는 방식의 롤백은 할 수 없다.

근거: [Android 앱 버전 관리](https://developer.android.com/studio/publish/versioning)

## 버전 부여 규칙

현재 심사 빌드 `1.0 (2)`는 그대로 보존한다. 다음 릴리스부터 아래 규칙을 적용한다.

1. 앱 버전의 단일 출처는 `android/app/build.gradle.kts`의 `defaultConfig`에 있는
   `versionCode`와 `versionName`이다. 다른 파일에 같은 값을 중복 선언하지 않는다.
2. `versionCode`는 Play 업로드마다 직전에 사용한 값보다 1 올린다.
3. `versionName`은 `주.부.수정` 세 자리 형식을 사용한다. 변경 성격에 따라 릴리스 작업을 수행하는
   모델이 증가 단위를 판단한다.
   - `수정`: 버그 수정만 포함하면 올린다. 예: `1.0.1`
   - `부`: 기존 사용 흐름을 유지하는 기능을 추가하면 올리고 수정 번호를 0으로 만든다. 예: `1.1.0`
   - `주`: 호환되지 않는 큰 제품 변경이 있으면 올리고 나머지를 0으로 만든다. 예: `2.0.0`
4. QA와 릴리스 대화에서는 항상 `versionName (versionCode)` 형식으로 쓴다. 예: `1.1.0 (3)`.
5. 릴리스 소스에는 `android-v<versionName>-code<versionCode>` 형식의 Git 태그를 붙인다.
   예: `android-v1.1.0-code3`.
6. Play Console에 올리기 직전에 Console의 가장 큰 `versionCode`를 다시 확인한다. 저장소 기록보다
   큰 값이 있으면 그 값 다음 번호를 사용한다.

## 릴리스 기록

### 1.1.1 (5)

- 상태: Google Play 제출용 서명 AAB 생성, iOS TestFlight `1.1.1 (3)` 업로드
- 배포일: Android 미배포, iOS TestFlight 2026-08-31
- 소스 커밋: `dca0219`
- Git 태그: 없음
- AAB SHA-256: `79344684d272d17fd551e4a922a6ec753f406b16122ed0af9566185b408c736d`
- 변경 내역:
  - iOS HIG에 맞춰 공통 화면의 타이포그래피와 접근성 의미를 보완
  - iOS 디자인 시스템 훅과 CI 접근성 감사를 추가
- 검증 결과:
  - PR #264 Android CI와 iOS 접근성 감사 통과
  - Android 단위 테스트와 release AAB 빌드 성공
  - AAB 서명, manifest, 런처 아이콘 검증 성공
  - iPhone 17 Pro, iOS 26.2에서 iOS 단위 테스트 3개와 UI 테스트 1개 통과
  - iOS Release Archive 서명 검증과 App Store Connect 업로드 성공, TestFlight 처리 중
- 이전 정상 기준: Android `1.1.0 (3)`, iOS TestFlight `1.1.0 (2)`

### 1.1.0 (4)

- 상태: Play 재업로드 구성 준비, 업로드 여부 미확인
- 배포일: 미배포
- 소스 커밋: `39f3997`
- Git 태그: 없음
- AAB SHA-256: 기록 없음
- 변경 내역:
  - `versionCode`를 4로 올리고 integration 빌드 타입을 명시
- 검증 결과: 별도 릴리즈 기록 없음
- 이전 정상 기준: Android `1.1.0 (3)`

### 1.1.0 (3)

- 상태: Google Play 심사용 AAB 생성, iOS TestFlight `1.1.0 (2)` 업로드
- 배포일: Android 미배포, iOS TestFlight 2026-08-31
- 소스 커밋: `d8b1681`
- Git 태그: 없음, Play 심사 통과 후 `android-v1.1.0-code3` 생성
- AAB SHA-256: `0b4de43663b2bbc1f34166139e9af4e213ce7b01c94539e4158e8f1a242d0cab`
- 변경 내역:
  - 공통 KMP 화면과 Android, iOS 인증 흐름 통합
  - 검색, 서재, 상세, 감상과 별점 기능 보완
  - QA2차 피드백과 다크 모드 접근성 반영
- 검증 결과:
  - Android 단위 테스트와 release AAB 빌드 성공
  - AAB 서명, manifest, 런처 아이콘 검증 성공
  - iOS Release Archive와 App Store Connect 업로드 성공
- 이전 정상 기준: `1.0 (2)`, 정확한 소스 커밋과 태그 없음

### 1.0 (2)

- 상태: Play 심사 통과, 미배포
- 확인일: 2026-08-21
- 소스 커밋과 태그: 기존 기록 없음
- 변경 내역: 기존 기록 없음
- 검증 근거: Play 심사 통과
- 이전 정상 기준: 없음

다음 릴리스부터 아래 양식을 복사해 최신 항목을 위에 추가한다.

```markdown
### 1.1.0 (3)

- 상태: 준비 중 | 심사 중 | 심사 통과 | 배포 완료 | 중단
- 배포일: YYYY-MM-DD 또는 미배포
- 소스 커밋: 7자리 Git SHA
- Git 태그: android-v1.1.0-code3
- AAB SHA-256: 해시 또는 아직 생성하지 않음
- 변경 내역:
  - 사용자가 확인할 수 있는 변경
- 검증 결과:
  - 실행한 테스트와 QA 결과
- 이전 정상 기준: android-v1.0.1-code2 또는 없음
```

## 릴리스 절차

1. 이전 정상 태그 이후의 변경을 확인하고 사용자에게 보이는 변경만 릴리스 기록에 정리한다.
2. `android/app/build.gradle.kts`의 두 버전을 규칙에 맞게 바꾸고 릴리스 상태를 `준비 중`으로
   기록한다.
3. 변경을 커밋한 뒤 그 커밋에서 아래 명령으로 서명 AAB를 만들고 검증한다. 각 Android 명령을
   실행하기 전에 명령의 목적을 한 줄로 먼저 설명한다. 서명 설정은
   [Android 빌드 및 서명 운영 가이드](android-build-signing.md)를 따른다.

   ```sh
   cd android
   ./gradlew :app:assembleDebug
   ./gradlew :app:verifyReleaseSigning
   ./gradlew :app:bundleRelease
   jarsigner -verify app/build/outputs/bundle/release/app-release.aab
   shasum -a 256 app/build/outputs/bundle/release/app-release.aab
   ```

4. AAB의 SHA-256과 소스 커밋을 릴리스 기록에 적는다.
5. QA는 `versionName (versionCode)`, 변경 내역, 핵심 기능 결과를 대조한다.
6. Play Console의 대상 트랙에 AAB를 올리고 상태를 `심사 중`으로 바꾼다. 통과하면 정확히 AAB를
   만든 소스 커밋에 Git 태그를 붙인다.
7. 실제 프로덕션 배포가 끝난 뒤에만 현재 상태 표를 `프로덕션 배포`로 갱신한다. 심사 통과와
   배포 완료를 같은 상태로 취급하지 않는다.
8. 배포 후 아래 규칙에 따라 release 커밋을 남긴다.

## Release 커밋

에이전트가 release 커밋을 만들 때는 `release-commit` 스킬을 사용한다. 스킬을 사용할 수 없는
팀원도 아래 절차와 형식을 그대로 따른다. 제목은 항상 아래 형식을 사용한다.

```text
chore(release): vX.Y.Z 배포
```

다른 제목 변형을 만들지 않는다. 본문 첫 줄에는 배포 대상, `versionName` 증감과 SemVer 증가 단위,
`versionCode` 증감을 적는다.

```text
대상: Google Play <트랙> / A.B.C -> X.Y.Z (major|minor|patch), versionCode M -> N
```

### 변경 목록 추출

release 커밋을 만들기 전에 직전 release 커밋을 찾는다.

```sh
git log --grep='chore(release)' --format=%H -n 1
```

첫 release라 결과가 없으면 마지막 버전 변경 커밋을 확인하고, 그것도 없으면 최근 태그를 기준으로
삼는다.

```sh
git log -G 'version(Code|Name) =' --format='%H %s' -- app/build.gradle.kts
git describe --tags --abbrev=0
```

기준점 이후의 커밋을 수집한다. 버전 변경 커밋이 현재 `HEAD`라면 `HEAD` 대신 `HEAD^`까지 조회해
단순 버전 변경 커밋을 목록에서 제외한다.

```sh
git log <기준-커밋>..HEAD --oneline
git log <기준-커밋>..HEAD^ --oneline
```

각 커밋을 `- type(scope): 요약` 한 줄로 옮긴다. 원문 의미를 유지하면서 읽기 좋게 다듬을 수 있지만
임의로 누락하지 않는다. release 커밋과 단순 버전 변경 커밋만 목록에서 제외한다. 항목을 성격별로
묶더라도 개별 커밋이 사라지면 안 된다.

### 완성 형식

```text
chore(release): v1.1.0 배포

대상: Google Play internal / 1.0 -> 1.1.0 (minor), versionCode 2 -> 3

- feat(search): 검색 정렬 기능을 추가
- fix(library): 서재 등록 실패 처리를 수정

Play track: internal
AAB SHA-256: <hash>
```

본문은 한국어로 쓰고 서명 트레일러를 넣지 않는다. 실제 배포한 트랙만 적으며 Play Console에
올리지 않은 대상을 포함하지 않는다.

### 기존 release 커밋 보강

본문이 부족한 release 커밋이 아직 push되지 않았다면 아래 명령으로 원격 포함 여부를 먼저
확인하고, 결과가 없을 때만 `git commit --amend`로 메시지를 보강한다.

```sh
git branch -r --contains <release-커밋-해시>
```

amend할 때는 무관한 파일을 stage하지 않는다. 이미 push된 커밋은 이력을 다시 쓰지 않으며, 변경이
꼭 필요하면 force push 전에 사용자에게 승인을 요청한다.


## 문제 발생 시 복구

1. 릴리스 기록에서 `배포 완료`이고 검증 결과가 정상인 가장 가까운 태그를 고른다.
2. 그 태그와 문제 버전의 변경 내역을 비교해 되돌릴 변경을 결정한다.
3. 정상 태그의 코드를 기준으로 수정 빌드를 만들되, `versionCode`는 Play에 올린 모든 값보다 크게
   정한다.
4. `versionName`은 수정 릴리스로 올리고 새 릴리스 기록과 태그를 만든다.
5. 서명, QA, Play 심사를 다시 거쳐 더 높은 버전의 수정 빌드를 배포한다.

아직 프로덕션 배포가 없는 현재 상태에서는 `1.0 (2)`를 롤백 기준으로 단정할 수 없다. 다음
배포부터 소스 태그, AAB 해시, 검증 결과가 모두 있는 항목만 복구 기준으로 선택한다.

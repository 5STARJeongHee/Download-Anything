# Download Anything 개발 규칙

광고 없는 개인용 안드로이드 미디어 다운로더. 공유 링크 → 영상·음악·이미지 저장.

- 조사 근거: [docs/research.md](docs/research.md)
- 설계: [docs/design.md](docs/design.md)
- 진행 상황: [checklist.md](checklist.md)
- 결정 기록: [context-notes.md](context-notes.md)

새 세션은 위 4개 문서를 먼저 읽는다.

## 1. 절대 규칙

1. **광고·분석·추적 SDK 금지.** Firebase Analytics, Crashlytics, AdMob, Facebook SDK 등 일체 넣지 않는다.
2. **네트워크 대상 제한.** 앱이 접속하는 곳은 사용자가 공유한 미디어 플랫폼, 그 CDN, yt-dlp 업데이트(GitHub)뿐이다. 새 도메인을 추가하면 context-notes.md에 이유를 적는다.
3. **라이선스 GPL-3.0.** youtubedl-android가 GPL-3.0이다. GPL과 호환되지 않는 의존성은 쓰지 않는다.
4. **다른 앱 이름·코드 복붙 금지.** Seal, YTDLnis 이름을 쓰지 않는다. 구조를 참고할 때는 출처를 context-notes.md에 남긴다.
5. **비밀값 커밋 금지.** 서명 키(`*.jks`, `keystore.properties`), `local.properties`, 쿠키 파일은 `.gitignore`에 둔다.

## 2. 빌드 환경

| 항목 | 값 |
|---|---|
| JDK | 17 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot`) |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` (`local.properties`의 `sdk.dir`) |
| Gradle / AGP / Kotlin | `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties` 참고 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |
| ABI | `arm64-v8a`만 (개인용, APK 용량 절감) |

자주 쓰는 명령.

```bash
./gradlew test            # 단위 테스트
./gradlew assembleDebug   # 디버그 APK (app/build/outputs/apk/debug)
./gradlew lint
```

버전은 추측하지 말고 `gradle/libs.versions.toml`만 고친다. 올릴 때는 Maven 메타데이터로 최신 안정판을 확인한다.

## 3. 코드 구조 규칙

- 레이어 의존 방향: `ui → work → data → domain`. 역방향 import 금지.
- `domain/`은 순수 Kotlin. `android.*`, `androidx.*`, youtubedl-android import 금지.
- youtubedl-android 호출은 `data/ytdlp/YtDlpEngine` 인터페이스 구현체 한 곳에서만 한다.
- 새 플랫폼 이미지 추출기는 `MediaExtractor`를 구현하고 `ExtractorRouter`에 등록한다. 실패 시 yt-dlp로 폴백되게 한다.
- 파일 하나 400줄 이내 권장, 800줄 상한. 함수 50줄 이내.
- 데이터 클래스는 불변(`val`, `copy()`). 공유 상태는 `StateFlow`.
- 오류는 삼키지 않는다. 사용자에게 보이는 오류는 한국어 문장 + 가능하면 행동(예: "엔진 업데이트")을 함께 준다. 로그에는 원본 yt-dlp 오류를 남긴다.
- `Log`는 `BuildConfig.DEBUG`일 때만. 릴리스에 디버그 출력 금지.

## 4. 코드 스타일

- 새 소스 파일 첫 줄에 한국어 한 줄 주석으로 역할을 적는다(예: `// 공유 텍스트에서 URL을 뽑고 플랫폼을 판별한다`). 설정 파일은 제외.
- 이름: 클래스 `PascalCase`, 함수·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`, Boolean은 `is/has/should/can` 접두.
- 매직 넘버 대신 이름 있는 상수.
- 주석은 "왜"만 쓴다. 코드가 하는 일을 반복하지 않는다.

## 5. 테스트 규칙

- domain·data의 순수 로직(URL 파싱, 프리셋→인자 변환, 파일명, 응답 JSON 파싱, 라우팅)은 **테스트 먼저** 작성한다. 이 범위 커버리지 80% 목표.
- youtubedl-android, MediaStore, WorkManager는 JVM 테스트에서 돌지 않는다. 인터페이스 뒤로 숨기고 가짜 구현으로 테스트한다. 실제 동작은 실기기에서 확인하고 checklist.md에 결과를 적는다.
- 네트워크를 타는 테스트 금지. 응답 JSON은 `app/src/test/resources/`에 샘플로 둔다(개인 정보 제거).
- 테스트 이름은 동작을 설명한다: `` `extracts url from text with surrounding words`() ``.
- "완료"라고 말하기 전에 `./gradlew test assembleDebug`를 실행한다.

## 6. Git 규칙

- 원격: `https://github.com/5STARJeongHee/Download-Anything.git`, 기본 브랜치 `main`.
- 한 문장으로 설명되는 단위로 커밋한다. 형식은 `<type>: <설명>`(feat, fix, refactor, docs, test, chore, perf, ci). 설명은 한국어 가능.
- 빌드·테스트가 깨진 상태로 커밋하지 않는다.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## 7. 작업 절차

1. checklist.md에서 다음 항목을 고른다.
2. 결정이 필요하면 context-notes.md에 "결정 / 이유 / 대안"을 적는다.
3. 테스트 작성 → 구현 → `./gradlew test assembleDebug`.
4. checklist.md 체크, 커밋.

## 8. 법적 전제

개인 용도(본인 콘텐츠, 허락받은 콘텐츠 등) 전용. 앱 설정 화면과 README에 "저작권과 각 서비스 약관을 지킬 책임은 사용자에게 있다"는 고지를 둔다. DRM 우회 기능은 만들지 않는다.

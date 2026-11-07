# Download Anything

광고 없는 개인용 안드로이드 미디어 다운로더. 다른 앱의 **공유** 메뉴에서 링크를 넘기면 영상, 음악, 이미지를 휴대폰에 저장한다.

- 엔진: [yt-dlp](https://github.com/yt-dlp/yt-dlp) ([youtubedl-android](https://github.com/yausername/youtubedl-android)로 앱에 내장)
- 이미지(X 사진, Pinterest 이미지 핀): 자체 경량 추출기
- 광고, 분석, 추적 SDK 없음. 서버 없음.

## 상태

Phase 1(MVP) 구현 완료, 에뮬레이터에서 확인했다. 공유 → 정보 조회 → 형식 선택(영상 최고/720p, 음악 m4a/mp3, 이미지) → 백그라운드 다운로드 → 갤러리·음악 폴더 저장까지 동작한다. arm64 실기기 확인은 남아 있다. 진행 상황은 [checklist.md](checklist.md).

### 알려진 제한
- **16KB 메모리 페이지 기기**에서는 내장 ffmpeg가 동작하지 않는다([youtubedl-android#365](https://github.com/yausername/youtubedl-android/issues/365)). 이때 앱은 영상·음성을 합치지 않는 형식으로 받고 mp3 변환을 숨긴다. 홈 하단에 "ffmpeg 없음"으로 표시된다.
- Instagram, 연령 제한·비공개 콘텐츠는 로그인이 필요해 아직 받을 수 없다(Phase 2).

## 설치와 사용

1. [Releases](https://github.com/5STARJeongHee/Download-Anything/releases)에서 `DownloadAnything-vX.Y.Z.apk`를 받아 휴대폰에 설치한다. 직접 빌드할 때는 `./gradlew assembleDebug`로 만든 `app/build/outputs/apk/debug/app-debug.apk`를 쓴다.
2. 처음 실행하면 yt-dlp 엔진을 준비하고 최신 버전으로 업데이트한다(수십 초).
3. YouTube·X·Pinterest 앱에서 **공유 → Download Anything로 받기**를 누르고 형식을 고른다.
4. 저장 위치: `Movies/DownloadAnything`, `Music/DownloadAnything`, `Pictures/DownloadAnything`.

## 문서

| 문서 | 내용 |
|---|---|
| [docs/research.md](docs/research.md) | 기존 앱·오픈소스 조사, 실측 결과 |
| [docs/design.md](docs/design.md) | 아키텍처, 화면, 단계별 계획 |
| [CLAUDE.md](CLAUDE.md) | 개발 규칙 |
| [context-notes.md](context-notes.md) | 결정 기록 |

## 빌드

JDK 17과 Android SDK(platform 37)가 필요하다. `local.properties`에 `sdk.dir`을 적는다.

```bash
./gradlew test assembleDebug
```

APK는 `app/build/outputs/apk/debug/app-debug.apk`(arm64-v8a 전용).

## 고지

개인 용도(본인 콘텐츠, 허락받은 콘텐츠 등) 전용이다. 저작권과 각 서비스 약관을 지킬 책임은 사용자에게 있다. DRM 우회 기능은 없다.

## 라이선스

[GPL-3.0](LICENSE). youtubedl-android(GPL-3.0)를 포함한다.

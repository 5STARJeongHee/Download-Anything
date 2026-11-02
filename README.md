# Download Anything

광고 없는 개인용 안드로이드 미디어 다운로더. 다른 앱의 **공유** 메뉴에서 링크를 넘기면 영상, 음악, 이미지를 휴대폰에 저장한다.

- 엔진: [yt-dlp](https://github.com/yt-dlp/yt-dlp) ([youtubedl-android](https://github.com/yausername/youtubedl-android)로 앱에 내장)
- 이미지(X 사진, Pinterest 이미지 핀): 자체 경량 추출기
- 광고, 분석, 추적 SDK 없음. 서버 없음.

## 상태

Phase 0(골격) 완료. 공유 링크 인식과 엔진 초기화까지 동작한다. 실제 다운로드는 Phase 1에서 붙인다. 진행 상황은 [checklist.md](checklist.md).

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

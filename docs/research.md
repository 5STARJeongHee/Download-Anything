# 미디어 다운로더 앱 조사 보고서

조사일 2026-11-02. 목적은 "링크를 공유하면 영상·이미지·음악을 받아주는 앱"을 광고 없이 직접 만들기 위한 근거 수집이다.

## 1. 결론 요약

- 광고 앱(Snaptube, VidMate 등)의 핵심 기능은 사실상 전부 오픈소스 **yt-dlp** 위에서 재현할 수 있다.
- 안드로이드에서는 **youtubedl-android** 라이브러리가 Python + yt-dlp + ffmpeg를 앱 안에 넣어준다. Seal, YTDLnis가 이 방식이다.
- yt-dlp는 **영상·오디오 전용**이다. X 사진 트윗, Pinterest 이미지 핀은 직접 확인한 결과 yt-dlp로 받을 수 없다. 이미지는 별도 추출기가 필요하다.
- YouTube는 수시로 막힌다. 앱 안에서 yt-dlp를 스스로 업데이트하는 기능이 필수다.
- 라이선스는 GPL-3.0(youtubedl-android)을 따라야 한다. 지인에게 APK를 주는 것도 배포에 해당한다.

## 2. 광고형 다운로더 앱 분석

| 앱 | 배포 | 문제점 |
|---|---|---|
| Snaptube | APK 직접 배포(Play 스토어 없음) | 보안업체 Upstream(Secure-D)이 2019년에 백그라운드 광고 자동 클릭, 원치 않는 유료 구독 가입을 보고 |
| VidMate | APK 직접 배포 | 같은 보고서에서 Snaptube와 동일한 Mango SDK, 약 1억 3천만 건의 의심 거래 보고. 누적 5억 다운로드 |

한계: 위 내용은 2019년 상업 보안업체 보고이며 Snaptube 측은 반박했다. 현재 버전의 동작은 확인하지 않았다.

시사점: 광고·추적 SDK를 넣지 않는 것 자체가 차별점이다. 우리 앱은 **네트워크 요청 대상을 미디어 플랫폼과 yt-dlp 업데이트 서버로 한정**한다.

## 3. 광고 없는 오픈소스 대안 분석

| 프로젝트 | 형태 | 기술 | 라이선스 | 참고할 점 |
|---|---|---|---|---|
| [Seal](https://github.com/JunkFood02/Seal) | 안드로이드 | Kotlin, Compose, youtubedl-android | GPL-3.0 | 단일 Activity + Compose 네비게이션, 메타데이터·썸네일 삽입, 플레이리스트, 커스텀 명령 템플릿 |
| [YTDLnis](https://github.com/deniscerri/ytdlnis) | 안드로이드 | Kotlin, youtubedl-android, NewPipe 추출기 병행 | GPL-3.0 | 공유 시 하단 카드만 띄우는 빠른 다운로드, 대기열·예약, 쿠키 로그인, PO 토큰, yt-dlp 자동 업데이트 |
| [NewPipe](https://newpipe.net) | 안드로이드 | 자체 추출기 | GPL-3.0 | YouTube 클라이언트 대체. 지원 사이트가 적음 |
| [cobalt](https://github.com/imputnet/cobalt) | 웹(셀프호스트) | Node.js API + Svelte 웹 | AGPL-3.0 | 서버가 프록시로 중계, 캐시 없음. 공개 인스턴스는 YouTube가 막힘(서버 IP 차단) |

Seal과 YTDLnis는 자기 이름을 포크나 다른 다운로더에 쓰지 말라고 명시했다. 우리 앱 이름은 **Download Anything**으로 한다.

## 4. 핵심 라이브러리

### youtubedl-android
- 좌표: `io.github.junkfood02.youtubedl-android:library|ffmpeg|aria2c`, 최신 **0.18.1**(2025-11-16, Maven Central 확인).
- 0.18.0: 16KB 페이지 크기 지원. 0.18.1: QuickJS 추가(YouTube JS 챌린지 대응용으로 추정, 실기기에서 확인 필요).
- 사용법: `YoutubeDL.getInstance().init(ctx)`, `FFmpeg.getInstance().init(ctx)`, `getInfo(url)`, `execute(request, processId) { progress, eta, line -> }`, `destroyProcessById(id)`, `updateYoutubeDL(ctx, UpdateChannel.NIGHTLY)`.
- 패키징: `extractNativeLibs=true`(또는 `jniLibs.useLegacyPackaging = true`). ABI 하나당 APK가 큼.
- 라이선스 GPL-3.0.

### yt-dlp (데스크톱 2026.10.26 기준)
- YouTube 전체 지원에는 JS 런타임(deno 권장, Node/Bun/QuickJS 가능)과 yt-dlp-ejs가 필요하다.
- 영상·음성 병합과 변환에 ffmpeg가 필요하다.
- 업데이트 채널은 stable(월 1회 정도), nightly(권장), master가 있다.

## 5. 실측 결과 (2026-11-02, yt-dlp 2026.10.26)

| 대상 | 결과 | 근거 |
|---|---|---|
| Pinterest 영상 핀 | 성공. formats 6개 | 실제 실행 |
| Pinterest 이미지 핀 | 실패. formats가 비고 이미지가 thumbnails에만 들어감 | `extractor/pinterest.py` `_extract_video` 코드 |
| X 영상 트윗 | 지원 | `extractor/twitter.py` |
| X 사진 트윗 | 불가. 추출기가 `m['type'] != 'photo'`로 사진을 걸러내고 "No video could be found" 오류 | `extractor/twitter.py` 1349, 1377행 |
| X 임베드 API `cdn.syndication.twimg.com/tweet-result` | 사진 트윗에서 `mediaDetails[].type=photo`, `media_url_https` 반환. 로그인 불필요 | 실제 실행 |

결론: **추출기를 2계층으로 둔다.** 영상·음악은 yt-dlp, 이미지는 Kotlin으로 만든 플랫폼별 경량 추출기가 맡는다(X 임베드 API, Pinterest는 yt-dlp `--ignore-no-formats-error` 결과의 원본 이미지 또는 페이지 `og:image`).

## 6. 플랫폼 제약 (안드로이드)

- Google Play는 YouTube 다운로더를 허용하지 않는다. APK 직접 설치로 쓴다.
- Android 14+: 장시간 작업은 foreground service 타입(`dataSync`) 선언이 필요하다. Android 15부터 dataSync는 하루 6시간 제한이 있다.
- Android 13+: 알림 권한(`POST_NOTIFICATIONS`)을 런타임에 요청해야 한다.
- Android 10+: 공용 저장소는 MediaStore로 써야 한다(scoped storage).
- Instagram, 연령 제한 콘텐츠는 로그인 쿠키가 필요하다. YouTube는 PO 토큰이 필요한 경우가 있다.

## 7. 법적 고지

YouTube 등 각 서비스 약관은 무단 다운로드를 금지할 수 있다. 이 앱은 본인·지인의 개인 용도(본인 콘텐츠, 허락받은 콘텐츠, 퍼블릭 도메인 등)를 전제로 하며, 저작권자의 권리를 침해하는 용도로 쓰지 않는다.

## 출처
- [Seal](https://github.com/JunkFood02/Seal), [YTDLnis](https://github.com/deniscerri/ytdlnis), [cobalt](https://github.com/imputnet/cobalt), [yt-dlp](https://github.com/yt-dlp/yt-dlp), [youtubedl-android](https://github.com/yausername/youtubedl-android), [youtubedl-android 릴리스](https://github.com/yausername/youtubedl-android/releases)
- [Upstream: Snaptube 사기 경고](https://upstreamsystems.com/fraud-alert-popular-video-app-snaptube-needs-careful-watching), [TechRadar: VidMate](https://www.techradar.com/news/chinese-vidmate-app-fraud-hits-up-to-half-a-billion-users), [GSMA: Secure-D 보고](https://www.gsma.com/membership/resources/secure-d-exposes-suspicious-android-app-with-500-million-downloads/)
- [Railway cobalt 템플릿(YouTube 차단 언급)](https://railway.com/deploy/cobalt-youtube-downloader), [Techlore 토론](https://discuss.techlore.tech/t/what-is-the-best-youtube-videos-downloader-for-android/3797)

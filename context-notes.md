# 결정 기록

형식: 날짜 / 결정 / 이유 / 버린 대안. 새 결정은 아래에 덧붙인다.

## 2026-11-02

### 안드로이드 네이티브 앱으로 만든다
- 이유: 사용자 선택. 서버 없이 폰에서 바로 받고, 공유 메뉴와 붙이기 쉽다.
- 대안: 셀프호스트 웹앱(cobalt 방식). 서버를 계속 켜 둬야 하고, 서버 IP가 YouTube에 막히기 쉽다.

### 개인·지인용, GitHub 릴리스 APK 배포
- 이유: 사용자 선택. Play 스토어는 YouTube 다운로더를 허용하지 않는다.
- 영향: 로그인·과금·대규모 트래픽 대응 없음. 단 지인 배포도 GPL 배포이므로 소스 공개(이 레포)와 GPL-3.0 LICENSE 유지.

### 엔진은 youtubedl-android 0.18.1
- 이유: yt-dlp + Python + ffmpeg를 앱에 넣어주는 검증된 라이브러리(Seal, YTDLnis 사용). 0.18.1에 QuickJS 포함.
- 대안: NewPipe Extractor(YouTube 위주, 지원 사이트 적음), 직접 파서(유지보수 불가).

### 추출기 2계층(yt-dlp + 이미지 전용 추출기)
- 근거: yt-dlp 2026.10.26 소스에서 X 추출기가 사진을 걸러내고(`twitter.py` 1349행), Pinterest 이미지 핀은 formats가 빈다(`pinterest.py` `_extract_video`). 실제 실행으로 Pinterest 영상 핀은 formats 6개 확인.
- X 이미지: `cdn.syndication.twimg.com/tweet-result` 응답의 `mediaDetails[type=photo].media_url_https` 사용. 로그인 없이 동작 확인. `token` 파라미터는 임의 값(`a`)으로도 응답했다. 공개 임베드 위젯이 쓰는 계산식(id / 1e15 × π를 36진수로, 0과 점 제거)을 쓴다.
- 추가 도메인: `cdn.syndication.twimg.com`, `pbs.twimg.com`(X 이미지 CDN). CLAUDE.md 1-2 규칙에 따른 기록.
- 대안: gallery-dl을 앱 Python에 추가 설치. 패키지 추가 과정이 복잡하고 용량이 커서 보류.

### 빌드 버전 (Maven 메타데이터로 확인)
- Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.21, Compose BOM 2026.09.00, WorkManager 2.12.0, OkHttp 5.5.1, kotlinx-serialization 1.12.0, coroutines 1.11.0, core-ktx 1.19.1, lifecycle 2.11.0, activity-compose 1.13.0.
- 로컬: JDK 17, SDK platforms 35/36/36.1, build-tools 35/36.1/37.

### ABI는 arm64-v8a만
- 이유: youtubedl-android가 ABI마다 Python·ffmpeg 바이너리를 넣어 APK가 크다. 개인용 기기는 거의 arm64.

### minSdk 26
- 이유: 본인·지인 기기 기준 충분. 알림 채널 등 구버전 분기를 없앤다.

### compileSdk 37, targetSdk 36
- 이유: Compose BOM 2026.09.00(Compose 1.12)의 AAR 메타데이터가 compileSdk 37 이상을 요구해 빌드가 실패했다. AGP가 platform 37을 자동 설치했다. targetSdk는 36으로 두어 동작 변경을 늦춘다.

### URL 호스트는 정규식으로 뽑는다
- 이유: `java.net.URI`는 쿼리의 `|`, 공백, 한글에서 예외를 던져 YouTube 링크도 OTHER로 판정됐다(코드 리뷰 지적, 테스트로 재현).
- 추적 파라미터: `utm_*`, `fbclid`, `gclid`만 모든 사이트에서 지운다.

### minSdk 26 → 29로 변경
- 이유: MediaStore `RELATIVE_PATH`가 API 29부터 있다. 26~28용 `WRITE_EXTERNAL_STORAGE`와 파일 경로 저장 분기를 따로 만들지 않는다. 개인용이라 Android 10 이상이면 충분하다.

### 엔진 실측 (에뮬레이터 Pixel_10_Pro, Android 37 x86_64, 16KB 페이지)
- `execute()`는 항상 `--js-runtimes quickjs:<libqjs.so>`와 `--ffmpeg-location`을 붙인다(AAR 바이트코드 확인). `--cache-dir`이 없으면 `--no-cache-dir`을 붙이므로 앱이 캐시 폴더를 지정한다.
- 내장 yt-dlp 2025.11.12에 `yt_dlp_ejs-0.3.1` 포함, `JS runtimes: quickjs` 인식. YouTube 정보 조회 성공(형식 39개, 최대 2160p). 따라서 `--remote-components`는 넣지 않는다.
- 내장 버전으로는 실제 다운로드가 `HTTP 403`. `updateYoutubeDL(NIGHTLY)` 후(2026.10.29) 다운로드 성공. → **첫 실행 시 업데이트는 필수.**
- 에뮬레이터 Doze(`Device idle: true`) 상태에서는 앱 uid의 DNS가 막혀 `[Errno 7]`가 난다. 확인 전 `dumpsys deviceidle disable`.
- **16KB 페이지 기기에서 ffmpeg 실행 불가.** `libffmpeg.zip.so` 안의 libwebp 계열 5개가 4KB 정렬(youtubedl-android #365, 미해결). yt-dlp는 "ffmpeg not found"로 처리하고 앱은 죽지 않는다.
  - 대응: 앱 시작 시 ffmpeg 실행 가능 여부를 검사하고, 없으면 합치기·변환이 없는 형식으로 바꾼다(영상 `-f b`, 음악 m4a 원본). mp3는 숨긴다.
  - ffmpeg가 필요한 기능은 4KB 이미지(android-36 google_apis x86_64)로 확인한다.
- 일부 YouTube 영상(jNQXAC9IVRU, BaW_jenozKc)은 이 네트워크에서 "This video is unavailable". 데스크톱 최신 yt-dlp도 같다. 앱 문제 아님.

### 디버그 전용 확인 도구 EngineProbeReceiver
- `app/src/debug`에만 있어 릴리스에 들어가지 않는다. stderr 전체를 보려고 youtubedl-android를 직접 호출한다(CLAUDE.md 3절 "엔진 호출은 한 곳" 규칙의 디버그 예외).
- 사용: `adb shell am broadcast -f 32 -n com.downloadanything/.EngineProbeReceiver --es url "'<URL>'" --es args "'--verbose --dump-single-json'"` 또는 `--es action update`.

## 2026-11-03 (Phase 1)

### 정보 조회는 youtubedl-android의 getInfo(VideoInfo) 대신 raw JSON을 직접 파싱
- 이유: 실제 응답 샘플로 JVM 테스트가 가능하고, `vcodec`의 `"none"`(없음)과 `null`(모름)을 구분할 수 있다. X의 http 형식은 둘 다 `null`이라 `!= "none"`으로 판정해야 영상으로 잡힌다.
- `--no-playlist`를 줘도 X 다중 영상 트윗은 `_type: playlist` + entries로 온다. 파서가 entries를 합치고, 워커는 결과 파일이 여러 개라고 가정한다(작업별 폴더의 파일 전부 저장).
- `_has_drm`은 `null`로 올 수 있다. `true`면 거부한다(DRM 우회 금지 규칙).

### Pinterest 이미지 핀은 별도 추출기를 만들지 않았다
- yt-dlp `--ignore-no-formats-error` 결과의 thumbnails에 원본(`/originals/`)이 들어 있어서, 파서에서 `extractor_key == "Pinterest"`이고 형식이 없을 때만 가장 큰 썸네일을 이미지로 쓴다. YouTube에서 형식 0개는 엔진 문제이므로 이미지로 바꾸지 않는다.
- 한계: 실제 이미지 핀으로는 확인하지 못했다(영상 핀 샘플에서 formats만 비운 합성 샘플로 테스트).

### X 임베드 API token은 고정값 "a"
- 서버가 2026-11 현재 token을 검사하지 않는다. 공개 위젯의 계산식(JS `Number.toString(36)`)을 정확히 옮기려면 코드가 늘어나서, 검사가 시작되면 그때 바꾼다.

### 설정 화면은 따로 만들지 않고 홈 하단에 엔진 버전·업데이트 버튼·고지를 둔다
- 이유: 설정 항목이 업데이트 버튼 하나뿐이라 화면 이동(navigation 라이브러리)을 넣을 이유가 없다.

### 엔진 상태 흐름
- 앱 시작: Initializing → (하루 지났으면) Updating → Ready(version, isFfmpegAvailable, isUpdateFailed). 정보 조회·워커는 `awaitReadyEngine()`으로 Ready를 기다린다.
- 수동 업데이트 버튼은 누르는 즉시 Updating으로 바꾼다. 직후 다시 조회할 때 옛 Ready를 보고 업데이트 전 엔진으로 실행하지 않게 하려는 것이다.
- 엔진 안에서 실행과 업데이트를 서로 기다리게 했다. 업데이트는 실행 중 작업이 0개가 될 때까지 기다리고, 업데이트 중에는 새 실행이 대기한다(코드 리뷰 지적).
- `awaitReadyEngine()`은 3분 제한. 업데이트 실패는 `Throwable`까지 잡아 Ready(isUpdateFailed)로 돌린다.

### Phase 1 코드 리뷰 반영 (kotlin-reviewer)
- 네트워크 대기 후 백그라운드에서 시작된 워커는 Android 12+에서 foreground 전환이 거부된다. 거부되면 알림 없이 계속 진행한다.
- OkHttp 호출을 코루틴 취소와 연결(`Call.await()`), https가 아닌 이미지 주소 거부, MediaStore 복사는 IO 스레드.
- 공유 시트는 ViewModel이 Idle이면 다시 조회한다(프로세스 재시작 후 빈 화면 방지).
- 남긴 한계(LOW): 여러 파일 중 일부만 저장되고 실패하면 저장된 파일은 남고 작업은 실패로 표시된다. 복사 중 프로세스가 죽으면 IS_PENDING 항목이 남는다(시스템이 약 7일 뒤 정리).

### Phase 1 에뮬레이터 확인 결과
- 16KB(Android 37, ffmpeg 없음): YouTube 720p(단일 파일), YouTube m4a, X 사진 원본, X 영상, Pinterest 영상 저장 성공. 404 페이지는 "볼 수 없음" 오류 화면.
- 4KB(Android 36, ffmpeg 있음): YouTube 720p 영상+음성 합치기(102MB), mp3 변환, m4a(메타데이터·썸네일), 취소 시 Python 프로세스 종료·임시 폴더 삭제. `si`, `feature`, `igsh` 등은 알려진 플랫폼에서만 지운다. 일반 사이트에서는 기능용일 수 있기 때문이다.

## 2026-11-07 (릴리스)

### 릴리스 서명과 GitHub Actions 배포
- 결정: `v*` 태그를 push하면 `.github/workflows/release.yml`이 테스트 → 서명된 release APK 빌드 → GitHub 릴리스 생성(APK + sha256)까지 한다.
- 서명 값은 환경 변수(`SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`)를 먼저 보고, 없으면 로컬 `keystore.properties`를 본다. 둘 다 없으면 release 서명 설정을 만들지 않아 debug 빌드는 그대로 된다.
- CI의 키는 저장소 Secrets(`SIGNING_KEYSTORE_BASE64` 외 3개)에 둔다. 러너 임시 폴더에 복원하고 끝나면 지운다.
- 키 파일은 저장소 밖(`%USERPROFILE%\keys\download-anything.jks`)에 두고 따로 백업한다. 키를 잃으면 설치된 앱을 업데이트할 수 없다.
- 태그와 `versionName`이 다르면 워크플로가 실패한다. 버전을 올릴 때는 `versionCode`(+1)와 `versionName`을 함께 고친 뒤 태그를 단다.
- 메모장이 `keystore.properties.txt`로 저장하는 일이 있어 `.gitignore`를 `keystore.properties*`로 넓혔다.
- 대안: 로컬에서 빌드해 웹으로 수동 업로드. 이 PC에는 Android SDK가 없어 CI 쪽이 간단하다.

## 2026-11-08

### X 영상은 임베드 API의 mp4를 직접 받는다
- 증상: 실기기에서 X 영상을 받으면 "엔진을 업데이트" 안내가 뜨고, 업데이트해도 실패했다. 릴리스 빌드라 원본 yt-dlp 오류는 볼 수 없었다.
- 결정: `cdn.syndication.twimg.com/tweet-result` 응답의 `mediaDetails[].video_info.variants` 중 `video/mp4`(https) 주소를 바로 받는다. 정보 조회 단계에서 mp4가 있으면 yt-dlp를 부르지 않는다. 직접 받기가 `IOException`으로 실패하면 yt-dlp로 한 번 더 받는다.
- 이유: yt-dlp의 X 추출기는 게스트 토큰·GraphQL API에 기대서 X 쪽 변경에 자주 깨진다. 임베드 API는 이미 사진에 쓰고 있고 로그인 없이 동작하며, mp4 변형에는 소리가 들어 있어 ffmpeg 합치기도 필요 없다. 2026-11-08 PC에서 응답과 `video.twimg.com` mp4 200 응답을 확인했다.
- 화질: 최고 화질은 bitrate가 가장 큰 것, 720p는 주소의 `/WxH/` 높이가 720 이하인 것 중 가장 좋은 것(없으면 최고). `FormatSelector`의 `b[height<=720]/b`와 같은 규칙.
- 한 트윗의 영상 중 하나라도 mp4가 없으면 직접 받기를 쓰지 않고 전부 yt-dlp에 맡긴다(일부만 저장되는 것을 막는다).
- 한계: 음악 추출(m4a·mp3)은 여전히 yt-dlp를 쓴다. 민감한 콘텐츠·보호 계정처럼 임베드 API가 미디어를 주지 않는 트윗도 yt-dlp로 간다.
- 추가 도메인: `video.twimg.com`(X 영상 CDN). 이전에도 yt-dlp가 접속하던 곳이지만 이제 앱의 OkHttp가 직접 접속한다. CLAUDE.md 1-2 규칙에 따른 기록.
- 대안: yt-dlp에 `--extractor-args twitter:api=syndication`을 주는 방법. 원인 오류를 확인하지 못한 상태라 yt-dlp 경로 자체를 피하는 쪽을 골랐다.

### 이 PC에 Android SDK 설치
- `%LOCALAPPDATA%\Android\Sdk`에 cmdline-tools(16111833, SHA-1 확인), `platforms;android-37.0`, `build-tools;37.0.0`, `platform-tools`를 설치했다. 이제 로컬에서 `./gradlew test assembleDebug`가 된다.
- `local.properties`의 경로는 `C:/Users/...`처럼 슬래시로 쓴다. 역슬래시는 properties 이스케이프로 사라진다.

### 로그아웃 상태에 숨겨진 X 트윗은 "로그인 필요"로 안내한다
- 증상(v0.1.2 실기기): 특정 트윗에서 "받을 수 있는 영상·음악·이미지를 찾지 못했습니다" + 엔진 업데이트 버튼이 떴다.
- 원인: 임베드 API가 `{"__typename":"TweetTombstone","tombstone":{}}`만 주고, oEmbed는 403 "not authorized to see this status"를 줬다. 민감한 콘텐츠·연령 제한처럼 로그인해야 보이는 트윗이다. yt-dlp도 미디어 없이 끝나 NO_MEDIA로 분류됐다.
- 결정: 파서가 tombstone이면 `LOGIN_REQUIRED`를 던지고, 라우터는 yt-dlp가 미디어를 못 찾거나 실패하면(네트워크 오류 제외) `LOGIN_REQUIRED`로 바꾼다. yt-dlp의 "NSFW tweet requires authentication"도 같은 분류로 넣었다.
- 이런 트윗을 실제로 받으려면 Phase 2의 로그인 쿠키(`--cookies`)가 필요하다. 외부 중계 서비스(fxtwitter 등)는 네트워크 대상 제한 규칙 때문에 쓰지 않는다.

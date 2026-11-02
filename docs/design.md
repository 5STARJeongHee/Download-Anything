# Download Anything 설계 문서

근거 자료는 [research.md](research.md)에 있다.

## 1. 목표와 범위

**목표**: 공유 메뉴로 링크를 넘기면 광고·추적 없이 영상, 음악, 이미지를 휴대폰에 저장한다.

**사용자**: 본인과 지인(개인 용도). Play 스토어 배포 없음, GitHub 릴리스 APK로 설치.

**하지 않는 것**: 광고, 분석·추적 SDK, 계정/서버, 과금, 스트리밍 플레이어.

### 지원 대상 (우선순위)

| 플랫폼 | 영상 | 음악 | 이미지 | 단계 |
|---|---|---|---|---|
| YouTube / YouTube Music | O | O | 썸네일 | MVP |
| X(트위터) | O | - | O(임베드 API) | MVP |
| Pinterest | O | - | O | MVP |
| TikTok, Reddit, SoundCloud, Vimeo 등 yt-dlp 지원 사이트 | O | O | - | MVP(yt-dlp가 되는 만큼) |
| Instagram | O | - | O | Phase 2(로그인 쿠키 필요) |

## 2. 기술 스택

| 영역 | 선택 | 이유 |
|---|---|---|
| 언어/UI | Kotlin, Jetpack Compose, Material 3 | Seal과 같은 검증된 조합 |
| 추출·다운로드 엔진 | youtubedl-android 0.18.1 (yt-dlp + Python + ffmpeg + QuickJS) | 기기 안에서 동작, 서버 불필요 |
| 이미지 추출 | OkHttp + kotlinx.serialization | yt-dlp가 이미지를 못 받음(실측) |
| 백그라운드 작업 | WorkManager + foreground(dataSync) | 앱을 닫아도 다운로드 계속 |
| 저장 | 앱 전용 폴더에 받은 뒤 MediaStore로 복사 | scoped storage 대응 |
| 빌드 | Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.21, compileSdk 36, minSdk 26, JDK 17 | 2026-11 기준 최신 안정판 |
| 테스트 | JUnit4 | domain/data 순수 로직 단위 테스트 |

minSdk 26(Android 8.0)으로 잡는다. 본인·지인 기기 기준으로 충분하고 분기 코드가 줄어든다.

## 3. 아키텍처

단일 `app` 모듈, 패키지로 레이어를 나눈다. 모듈 분리는 필요가 생길 때 한다.

```
com.downloadanything
├─ domain/            순수 Kotlin. 안드로이드 의존성 금지
│  ├─ model/          Platform, MediaKind, MediaInfo, MediaFormat, DownloadPreset
│  ├─ UrlParser       공유 텍스트에서 URL 추출, 플랫폼 판별, 추적 파라미터 제거
│  ├─ FormatSelector  프리셋 → yt-dlp 인자(-f, -x, --audio-format 등)
│  ├─ FileNamer       파일명 정리(금지 문자, 길이 제한)
│  └─ MediaExtractor  추출기 인터페이스
├─ data/
│  ├─ ytdlp/          YtDlpEngine(youtubedl-android 래퍼), YtDlpExtractor
│  ├─ image/          XImageExtractor(임베드 API), PinterestImageExtractor
│  ├─ ExtractorRouter URL → 추출기 선택, 실패 시 다음 추출기로 폴백
│  └─ storage/        MediaStoreSaver
├─ work/              DownloadWorker(WorkManager, foreground 알림, 진행률)
└─ ui/                MainActivity, ShareActivity(하단 시트), Compose 화면
```

### 의존 방향
`ui → work → data → domain`. domain은 아무것도 의존하지 않는다. youtubedl-android 호출은 `YtDlpEngine` 인터페이스 뒤에만 둔다(JVM 테스트에서 돌지 않으므로).

### 추출기 2계층

```
공유 URL
  └─ UrlParser.parse() → Platform
       └─ ExtractorRouter
            ├─ 이미지가 있을 수 있는 플랫폼(X, Pinterest): ImageExtractor 먼저 → 미디어 없으면 YtDlpExtractor
            └─ 그 외: YtDlpExtractor
                 └─ 결과: MediaInfo(title, thumbnail, items[MediaItem(kind, formats)])
```

- X: `cdn.syndication.twimg.com/tweet-result?id={id}&token={token}` 응답의 `mediaDetails` 중 `type=photo`는 `media_url_https?name=orig`로 받는다. 영상은 yt-dlp로 넘긴다.
- Pinterest: yt-dlp `-J --ignore-no-formats-error` 결과에서 formats가 비면 thumbnails 중 가장 큰 이미지(원본)를 이미지로 받는다.

### 다운로드 흐름

1. 공유 → `ShareActivity`(투명 테마 하단 시트)가 텍스트에서 URL 추출.
2. 추출기로 정보 조회(제목, 썸네일, 가능한 형식). 진행 표시.
3. 사용자가 프리셋 선택: `최고 화질 영상`, `720p 영상`, `음악(m4a)`, `음악(mp3)`, `이미지`.
4. `DownloadWorker` 등록 → foreground 알림에 진행률 표시, 취소 버튼.
5. 앱 전용 폴더(`cacheDir/downloads/{id}`)에 받음 → MediaStore로 복사(영상 `Movies/DownloadAnything`, 음악 `Music/DownloadAnything`, 이미지 `Pictures/DownloadAnything`) → 임시 파일 삭제.
6. 완료 알림을 누르면 파일 열기.

### yt-dlp 업데이트
- 앱 시작 시 `init()`을 백그라운드 코루틴에서 실행(수 초 걸림). 끝나기 전 요청은 대기.
- 하루 1회 이하로 `updateYoutubeDL(NIGHTLY)` 자동 실행, 설정 화면에 수동 버튼.
- 추출 실패 메시지에 "엔진 업데이트" 버튼을 함께 보여준다.

## 4. 화면

| 화면 | 내용 |
|---|---|
| 공유 하단 시트 | 썸네일, 제목, 프리셋 버튼, 다운로드 버튼. 앱 본체를 열지 않음 |
| 홈 | 큰 URL 입력칸(클립보드 붙여넣기 버튼), 진행 중 작업 목록 |
| 설정 | yt-dlp 버전·업데이트, 기본 프리셋, 저장 폴더 안내, 라이선스 |

**디자인 방향**: "도구"답게 단단한 느낌. 진한 잉크색 배경에 형광 라임 강조색 하나, 굵은 산세리프 제목과 고정폭 숫자(진행률·용량). 진행률 막대를 화면의 주인공으로 둔다. 다크/라이트 둘 다 지원.

## 5. 단계별 계획

| 단계 | 범위 | 완료 기준 |
|---|---|---|
| Phase 0 | 문서, 프로젝트 골격, domain 로직 + 테스트 | `gradlew test assembleDebug` 통과 |
| Phase 1 (MVP) | 공유 → 정보 조회 → 프리셋 → 다운로드 → MediaStore 저장. X·Pinterest 이미지. yt-dlp 업데이트 | 실기기에서 YouTube 영상/음악, X 사진/영상, Pinterest 이미지/영상 각 1건 저장 성공 |
| Phase 2 | 다운로드 기록(Room), 대기열·동시 다운로드 수, 플레이리스트, Instagram/연령 제한용 WebView 로그인 쿠키, PO 토큰 | 기능별 실기기 확인 |
| Phase 3 | 자막·메타데이터 삽입, SponsorBlock, 구간 자르기, 홈 화면 위젯 | 필요 시 |

## 6. 리스크

| 리스크 | 대응 |
|---|---|
| YouTube 차단·형식 변경 | yt-dlp nightly 자동 업데이트, 실패 시 업데이트 유도 |
| QuickJS로 YouTube JS 챌린지가 안 풀릴 수 있음 | Phase 1에서 실기기로 확인. 안 되면 YTDLnis처럼 Deno/Node 패키지 추가 검토 |
| X 임베드 API 변경 | 이미지 추출기가 실패해도 yt-dlp로 폴백, 오류 메시지를 명확히 |
| APK 용량(ABI당 수십~100MB) | 개인용이므로 `arm64-v8a`만 빌드 |
| Android 15 dataSync 6시간 제한 | 개인 사용 패턴상 문제 없음. 초과 시 작업 재시도 |

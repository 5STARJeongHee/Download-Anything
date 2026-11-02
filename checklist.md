# 체크리스트

단계 정의는 [docs/design.md](docs/design.md) 5절을 따른다.

## Phase 0. 준비
- [x] 기존 앱·오픈소스 조사 (docs/research.md)
- [x] yt-dlp 이미지 지원 실측 (X 사진, Pinterest 이미지 불가 확인)
- [x] 설계 문서 (docs/design.md)
- [x] 개발 규칙 (CLAUDE.md)
- [ ] Gradle 프로젝트 골격 (wrapper, version catalog, app 모듈)
- [ ] youtubedl-android 의존성 추가, 앱 시작 시 백그라운드 init
- [ ] domain: `UrlParser` (URL 추출, 플랫폼 판별, 추적 파라미터 제거) + 테스트
- [ ] domain: `DownloadPreset` → yt-dlp 인자 변환 `FormatSelector` + 테스트
- [ ] domain: `FileNamer` + 테스트
- [ ] 공유 인텐트(`ACTION_SEND text/plain`) 수신 Activity 골격
- [ ] `./gradlew test assembleDebug` 통과
- [ ] GitHub 원격에 push

## Phase 1. MVP
- [ ] `YtDlpEngine` 인터페이스 + youtubedl-android 구현 (getInfo, execute, cancel, update)
- [ ] `YtDlpExtractor`: `VideoInfo` → `MediaInfo` 변환 + 테스트(가짜 엔진)
- [ ] `XImageExtractor`: 임베드 API 토큰 계산·JSON 파싱 + 테스트(샘플 JSON)
- [ ] `PinterestImageExtractor`: formats 없을 때 원본 이미지 선택 + 테스트
- [ ] `ExtractorRouter`: 플랫폼별 순서, 폴백 + 테스트
- [ ] `DownloadWorker`: foreground(dataSync) 알림, 진행률, 취소
- [ ] `MediaStoreSaver`: Movies/Music/Pictures 하위 `DownloadAnything` 폴더에 저장
- [ ] 공유 하단 시트 UI: 정보 조회 → 프리셋 선택 → 다운로드
- [ ] 홈 화면: URL 입력, 진행 중 작업
- [ ] 설정: yt-dlp 버전·수동 업데이트, 하루 1회 자동 업데이트, 법적 고지
- [ ] 알림 권한(Android 13+) 요청
- [ ] 실기기 확인: YouTube 영상
- [ ] 실기기 확인: YouTube 음악(m4a/mp3)
- [ ] 실기기 확인: X 사진 / X 영상
- [ ] 실기기 확인: Pinterest 이미지 / Pinterest 영상
- [ ] QuickJS로 YouTube JS 챌린지가 처리되는지 확인 (안 되면 context-notes에 대응 기록)
- [ ] 릴리스 서명 설정, GitHub 릴리스에 APK 업로드

## Phase 2
- [ ] 다운로드 기록(Room)
- [ ] 대기열, 동시 다운로드 수 설정
- [ ] 플레이리스트
- [ ] WebView 로그인 → 쿠키 파일 → `--cookies` (Instagram, 연령 제한)
- [ ] YouTube PO 토큰

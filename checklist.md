# 체크리스트

단계 정의는 [docs/design.md](docs/design.md) 5절을 따른다.

## Phase 0. 준비
- [x] 기존 앱·오픈소스 조사 (docs/research.md)
- [x] yt-dlp 이미지 지원 실측 (X 사진, Pinterest 이미지 불가 확인)
- [x] 설계 문서 (docs/design.md)
- [x] 개발 규칙 (CLAUDE.md)
- [x] Gradle 프로젝트 골격 (wrapper, version catalog, app 모듈)
- [x] youtubedl-android 의존성 추가, 앱 시작 시 백그라운드 init
- [x] domain: `UrlParser` (URL 추출, 플랫폼 판별, 추적 파라미터 제거) + 테스트
- [x] domain: `DownloadPreset` → yt-dlp 인자 변환 `FormatSelector` + 테스트
- [x] domain: `FileNamer` + 테스트
- [x] 공유 인텐트(`ACTION_SEND text/plain`) 수신 Activity 골격
- [x] `./gradlew test assembleDebug` 통과 (단위 테스트 23개, 디버그 APK 64MB)
- [ ] 실기기에 설치해 엔진 초기화(yt-dlp 버전 표시)와 공유 시트 확인
- [x] GitHub 원격에 push

## Phase 1. MVP
- [x] `YtDlpEngine` + youtubedl-android 구현 (JSON 조회, 다운로드, 취소, nightly 업데이트, ffmpeg 검사)
- [x] `YtDlpExtractor` + `YtDlpInfoParser`: 실제 응답 샘플로 테스트 (VideoInfo 대신 raw JSON 파싱)
- [x] `XImageExtractor` + `XSyndicationParser`: 사진 원본 추출 + 테스트 (token은 고정값, context-notes 참고)
- [x] Pinterest 이미지 핀: 별도 추출기 대신 파서에서 "Pinterest + 형식 없음 → 가장 큰 이미지" 처리
- [x] `ExtractorRouter`: 플랫폼별 순서, 폴백, 병합 + 테스트
- [x] `DownloadWorker`: foreground(dataSync) 알림, 진행률, 취소
- [x] `MediaStoreSaver`: Movies/Music/Pictures 하위 `DownloadAnything` 폴더에 저장
- [x] 공유 하단 시트 UI: 정보 조회 → 프리셋 선택 → 다운로드
- [x] 홈 화면: URL 입력, 다운로드 목록(진행률·취소), 엔진 버전·수동 업데이트, 법적 고지 (설정 화면은 홈에 합침)
- [x] 하루 1회 자동 업데이트, 첫 실행 시 업데이트
- [x] 알림 권한(Android 13+) 요청
- [x] ffmpeg 없는 기기(16KB 페이지)용 대체 형식과 mp3 숨김
- [x] 에뮬레이터(x86_64, 16KB, ffmpeg 없음) 확인: YouTube 영상 720p, YouTube m4a, X 사진, X 영상, Pinterest 영상, 실패 화면, 홈 목록
- [x] 에뮬레이터(x86_64, 4KB, ffmpeg 있음) 확인: YouTube 720p 합치기, mp3 변환, m4a, 다운로드 취소(프로세스 종료·임시 폴더 삭제)
- [x] QuickJS로 YouTube JS 챌린지 처리 확인 (경고 없음, 최대 2160p)
- [ ] **실제 Pinterest 이미지 핀 확인** (테스트는 합성 샘플뿐, 실제 이미지 핀 주소를 못 구함)
- [ ] X 영상을 임베드 API mp4로 직접 받기(yt-dlp X 추출기 실패 우회, 실패 시 yt-dlp 폴백) + 테스트 (`./gradlew test assembleDebug` 통과, 테스트 75개. 실기기 미확인)
- [ ] **arm64 실기기 확인** (사용자 휴대폰): 위 항목 전체, 특히 기기 페이지 크기에 따른 ffmpeg 유무
- [x] 릴리스 서명 설정(Gradle, GitHub Actions 워크플로)
- [ ] GitHub Secrets 등록 후 첫 태그(v0.1.0)로 릴리스 APK 업로드 확인

## Phase 2
- [ ] 다운로드 기록(Room)
- [ ] 대기열, 동시 다운로드 수 설정
- [ ] 플레이리스트
- [ ] WebView 로그인 → 쿠키 파일 → `--cookies` (Instagram, 연령 제한)
- [ ] YouTube PO 토큰

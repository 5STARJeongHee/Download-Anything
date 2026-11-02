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
- 추적 파라미터: `utm_*`, `fbclid`, `gclid`만 모든 사이트에서 지운다. `si`, `feature`, `igsh` 등은 알려진 플랫폼에서만 지운다. 일반 사이트에서는 기능용일 수 있기 때문이다.

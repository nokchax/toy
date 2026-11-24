rootProject.name = "toy"

include(
    // 라이브러리
    "common", // Entity · Repository · Flyway 마이그레이션 · 공통 설정
    "crawl-core", // 크롤 엔진 · 크롤 서비스 (bot · crawler · internal 이 함께 쓴다)
    // 앱 (각각 따로 배포)
    "bot", // 텔레그램 메시지 수신 · 처리 · 발송 (텔레그램으로 보내는 유일한 앱)
    "crawler", // 주기 크롤 실행
    "internal", // REST API · MCP (외부 비공개)
    "external", // 웹 페이지 (리버스 프록시로 외부 공개)
)

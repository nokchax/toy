# toy

등록한 페이지를 주기적으로 크롤해서, 조건에 맞는 새 항목이 생기면 텔레그램으로 알려주는 봇.

Kotlin 2.4 · Spring Boot 4.1 · JDK 25 · MySQL 8 (로컬은 H2) · telegrambots 10 (long polling)

## 흐름

```
(A) 명령어      /cr add …  ─▶ CrawlService ─▶ DB 에 설정 저장 (+ 지금 보이는 항목을 기준값으로)
(B) 스케줄러    매분 ─▶ 실행할 크롤 선점 ─▶ 가져오기 → 뽑기 → 거르기 → 새 항목 판별 ─▶ 알림 큐 저장
(C) 알림 발송   알림 큐 ─▶ NotificationDispatcher ─▶ TelegramSender ─▶ 텔레그램
```

- 세 흐름은 DB 로만 이어진다. crawler 는 텔레그램을 모르고, 메시지는 bot 만 보낸다.
- 이미 본 항목 · 보낼 알림 · 실행 기록은 한 트랜잭션으로 저장해서 알림이 유실되거나 중복되지 않는다.
- 발송이 실패하면 알림 큐에 남아 1, 2, 4 … 분 간격으로 다시 보낸다.
- 크롤이 연속 5번 실패하면 끄고 알린다.

## 모듈

| 모듈 | 역할 |
|---|---|
| `common` | Entity · Repository · Flyway 마이그레이션 (`db/migration`) |
| `crawler` | 크롤 엔진 (`HttpFetcher` → `Extractor` → 필터 → `CrawlResultRecorder`), `CrawlScheduler`, `CrawlService` |
| `bot` | 실행 앱. 텔레그램 수신 (`TelegramBot`), 명령어 (`@BotCommand` / `@SubCommand`), 발송 (`TelegramSender`, `NotificationDispatcher`) |

## 명령어

```
/cr test <url> <selector>                       저장하지 않고 미리보기
/cr add <이름> <url> <selector> [-cron 10m] [-mode new|changed|always] [-xpath]
/cr list · /cr show <id>
/cr filter <id> add contains 특가               조건: contains, not_contains, equals, not_equals,
/cr filter <id> add regex "특가|핫딜"                   starts_with, ends_with, regex
/cr filter <id> add contains /event/ -field url
/cr filter <id> del <번호>
/cr cron <id> 30m                               10m · 1h · "*/10 * * * *" · "0 0 9 * * *"
/cr mode <id> new|changed|always
/cr on|off|run|history|del <id>
```

- 필터는 번호 순서대로 **모두** 통과해야 남는다 (AND). OR 은 `regex "a|b"`.
- 알림 모드: `new` 처음 보는 항목 · `changed` 결과가 바뀌면 · `always` 실행할 때마다.

## 로컬 실행

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25
./gradlew test

cp .env.example .env && chmod 600 .env   # 프로젝트 루트. TOY_TELEGRAM_TOKEN 등을 채운다
./gradlew :bot:bootRun                   # local 프로필: ./data 에 H2 파일 DB
```

- 앱이 프로젝트 루트의 `.env` 를 직접 읽는다 (`spring.config.import: optional:file:.env[.properties]`). 같은 이름의 환경변수가 있으면 환경변수가 우선한다.
- 토큰 없이 띄우면 텔레그램 연결 없이 뜬다.
- `TOY_TELEGRAM_ALLOWED_CHAT_IDS` 를 비워두고 봇에 `/help` 를 보내면 내 chat id 를 알려준다.

## NAS 배포

```sh
ssh nas
cd /volume2/docker && git clone <저장소> toy && cd toy
cp .env.example .env && chmod 600 .env && vi .env   # 토큰, chat id, DB 접속 정보
sudo docker compose up -d --build
sudo docker compose logs -f
```

## 다음 단계

- 브라우저 Fetcher (Selenium 컨테이너): JS 렌더링 · 로그인 페이지
- MCP 입구: Claude CLI 에서 `CrawlService` 를 도구로 호출
- 사이트별 요청 간격 제한, 오래된 `crawl_items` · `crawl_histories` 정리

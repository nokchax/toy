# toy

등록한 페이지를 주기적으로 크롤해서, 조건에 맞는 새 항목이 생기면 텔레그램으로 알려주는 봇.
텔레그램 봇 외에 REST API · MCP(인터널)와 웹 페이지(익스터널)로도 크롤을 다룬다.

Kotlin 2.4 · Spring Boot 4.1 · JDK 25 · MySQL 8 (로컬은 H2) · telegrambots 10 (long polling) · Spring AI MCP 서버 2.0

## 앱

네 앱은 따로 실행 · 배포되고 DB 로만 이어진다.

| 앱 | 하는 일 | 포트 |
|---|---|---|
| `bot` | 텔레그램 메시지 수신 · 명령 처리 · 발송. **텔레그램으로 보내는 유일한 앱** | 없음 |
| `crawler` | 주기 크롤 실행. 결과와 알림을 DB 에 저장 | 없음 |
| `internal` | 크롤 REST API (`/api/crawls`) · MCP 서버 (`/mcp`). API 키 필요, 외부 비공개 | 8090 |
| `external` | 크롤 목록 · 상세 웹 페이지. 로그인 필요, 리버스 프록시로 외부 공개 | 8080 |

## 흐름

```
(A) 명령 · API   bot /cr add … · internal API · MCP ─▶ CrawlService ─▶ DB 에 설정 저장 (+ 지금 보이는 항목을 기준값으로)
(B) 주기 실행     crawler 매분 ─▶ 실행할 크롤 선점 ─▶ 가져오기 → 뽑기 → 거르기 → 새 항목 판별 ─▶ 알림 큐 저장
(C) 알림 발송     bot ─▶ 알림 큐 (5초마다, bot 안에서 생긴 알림은 바로) ─▶ TelegramSender ─▶ 텔레그램
```

- 다른 앱은 알림 큐(`notification_messages`)에 쌓기만 하고 발송은 bot 만 한다. 텔레그램 라이브러리도 `bot` 모듈에만 있다.
- 이미 본 항목 · 보낼 알림 · 실행 기록은 한 트랜잭션으로 저장해서 알림이 유실되거나 중복되지 않는다.
- 발송이 실패하면 알림 큐에 남아 1, 2, 4 … 분 간격으로 다시 보낸다.
- 크롤이 연속 5번 실패하면 끄고 알린다.
- 모든 앱이 시작할 때 Flyway 마이그레이션을 돌린다 (DB 락으로 동시 실행을 막는다). 일부 앱만 새로 배포해도 된다.

## 모듈

| 모듈 | 종류 | 내용 |
|---|---|---|
| `common` | 라이브러리 | Entity · Repository · Flyway 마이그레이션 (`db/migration`) · 공통 설정 `toy-common.yaml` |
| `crawl-core` | 라이브러리 | 크롤 엔진 (`HttpFetcher` → `Extractor` → 필터 → `CrawlResultRecorder`), `CrawlClaimer`, `CrawlService` |
| `bot` | 앱 | 텔레그램 수신 (`TelegramBot`), 명령어 (`@BotCommand` / `@SubCommand`), 발송 (`TelegramSender`, `NotificationDispatcher`) |
| `crawler` | 앱 | `CrawlScheduler` |
| `internal` | 앱 | `CrawlApiController` (REST), `CrawlTools` (MCP 도구), `ApiKeyFilter` |
| `external` | 앱 | `PageController` + Thymeleaf 템플릿, `SecurityConfig` (폼 로그인) |

크롤 엔진 · 서비스는 `crawl-core` 를 bot · crawler · internal 이 함께 쓴다 (봇의 `/cr test` · `/cr run` 은 봇이 직접 가져온다).

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

## 인터널 API · MCP

모든 요청에 `Authorization: Bearer <TOY_INTERNAL_API_KEY>` (또는 `X-Api-Key`) 가 필요하다. `/health` 만 예외.

```
GET    /api/crawls                    목록
GET    /api/crawls/{id}               상세
GET    /api/crawls/{id}/history       최근 실행 기록
POST   /api/crawls/preview            {"url", "selector", "selectorType"?}  저장하지 않고 미리보기
POST   /api/crawls                    {"name", "url", "selector", "selectorType"?, "cron"?, "notifyMode"?}
POST   /api/crawls/{id}/run           지금 실행 (알림은 bot 이 보낸다)
PUT    /api/crawls/{id}/active        {"active": true|false}
PUT    /api/crawls/{id}/cron          {"cron": "1h"}
PUT    /api/crawls/{id}/mode          {"notifyMode": "NEW_ITEMS"|"CHANGED"|"ALWAYS"}
POST   /api/crawls/{id}/filters       {"op": "contains", "pattern": "특가", "field"?: "TEXT"|"URL"}
DELETE /api/crawls/{id}/filters/{seq}
DELETE /api/crawls/{id}
```

MCP 도구 (Streamable HTTP, `POST /mcp`): `list_crawls` · `get_crawl` · `crawl_history` · `preview_crawl` · `register_crawl` · `run_crawl` · `set_crawl_active` · `change_crawl_cron` · `add_crawl_filter` · `remove_crawl_filter`

```sh
# Claude Code 에 등록 (로컬 실행 기준)
claude mcp add --transport http toy http://localhost:8090/mcp --header "Authorization: Bearer <TOY_INTERNAL_API_KEY>"
```

인터널 · 익스터널은 `TOY_OWNER_CHAT_ID` 의 크롤을 다룬다. 봇에서 쓰는 내 chat id 와 같게 두면 봇 · API · 웹이 같은 크롤을 본다.

## 로컬 실행

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25
./gradlew test

cp .env.example .env && chmod 600 .env   # 프로젝트 루트. 필요한 값을 채운다
./gradlew :bot:bootRun                   # local 프로필: ./data 에 H2 파일 DB
./gradlew :crawler:bootRun               # 다른 터미널에서. 네 앱이 같은 H2 파일을 함께 연다
./gradlew :internal:bootRun              # http://localhost:8090
./gradlew :external:bootRun              # http://localhost:8080
```

- 앱이 프로젝트 루트의 `.env` 를 직접 읽는다 (`spring.config.import: optional:file:.env[.properties]`). 같은 이름의 환경변수가 있으면 환경변수가 우선한다.
- 토큰 없이 띄우면 bot 은 텔레그램 연결 없이 뜬다 (알림은 큐에 남는다).
- `TOY_TELEGRAM_ALLOWED_CHAT_IDS` 를 비워두고 봇에 `/help` 를 보내면 내 chat id 를 알려준다.
- 같은 봇 토큰으로 로컬과 NAS 에서 동시에 띄우지 않는다 (텔레그램은 토큰당 연결 하나만 허용). 로컬 개발은 테스트용 봇 토큰으로.

## 다음 단계

- 브라우저 Fetcher (Selenium 컨테이너): JS 렌더링 · 로그인 페이지
- 익스터널 페이지에서 크롤 수정 (지금은 읽기 전용)
- 사이트별 요청 간격 제한, 오래된 `crawl_items` · `crawl_histories` 정리

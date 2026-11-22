create table pages
(
    id         bigint auto_increment primary key,
    url        varchar(2048) not null comment '페이지 url',
    created_at datetime(6)   not null,
    updated_at datetime(6)   not null
);

create table crawls
(
    id            bigint auto_increment primary key,
    page_id       bigint       not null comment '페이지 id',
    chat_id       bigint       not null comment '등록한 텔레그램 chat id (알림 대상)',
    name          varchar(64)  not null comment '크롤 이름',
    selector      varchar(512) not null comment '항목 선택자',
    selector_type varchar(16)  not null comment 'CSS | XPATH',
    cron          varchar(64)  not null comment 'spring cron (6자리)',
    notify_mode   varchar(16)  not null comment 'NEW_ITEMS | CHANGED | ALWAYS',
    active        boolean      not null comment '실행 여부',
    next_run_at   datetime(6)  null comment '다음 실행 시각',
    last_run_at   datetime(6)  null comment '마지막 실행 시각',
    fail_count    int          not null comment '연속 실패 횟수',
    last_hash     varchar(64)  null comment '직전 결과 해시 (CHANGED 모드)',
    created_at    datetime(6)  not null,
    updated_at    datetime(6)  not null,
    constraint fk_crawls_page foreign key (page_id) references pages (id)
);

create index idx_crawls_due on crawls (active, next_run_at);
create index idx_crawls_chat on crawls (chat_id);

create table crawl_filters
(
    id         bigint auto_increment primary key,
    crawl_id   bigint       not null comment '크롤 id',
    seq        int          not null comment '적용 순서',
    op         varchar(16)  not null comment '조건',
    pattern    varchar(256) not null comment '비교 값',
    field      varchar(16)  not null comment 'TEXT | URL',
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    constraint fk_crawl_filters_crawl foreign key (crawl_id) references crawls (id) on delete cascade,
    constraint uk_crawl_filters_seq unique (crawl_id, seq)
);

create table crawl_items
(
    id         bigint auto_increment primary key,
    crawl_id   bigint        not null comment '크롤 id',
    item_hash  varchar(64)   not null comment 'sha256(url 또는 text)',
    text       varchar(1024) not null comment '항목 텍스트',
    url        varchar(2048) null comment '항목 링크',
    created_at datetime(6)   not null comment '처음 본 시각',
    updated_at datetime(6)   not null,
    constraint uk_crawl_items_hash unique (crawl_id, item_hash)
);

create table crawl_histories
(
    id          bigint auto_increment primary key,
    crawl_id    bigint        not null comment '크롤 id',
    started_at  datetime(6)   not null comment '시작 시각',
    duration_ms bigint        not null comment '소요 시간',
    status      varchar(16)   not null comment 'SUCCESS | FAILURE',
    item_count  int           not null comment '추출한 항목 수',
    new_count   int           not null comment '알린 항목 수',
    error       varchar(1000) null comment '실패 사유',
    created_at  datetime(6)   not null,
    updated_at  datetime(6)   not null
);

create index idx_crawl_histories_crawl on crawl_histories (crawl_id, id);

create table notification_messages
(
    id              bigint auto_increment primary key,
    chat_id         bigint        not null comment '받는 chat id',
    crawl_id        bigint        null comment '원인 크롤 id',
    body            varchar(4096) not null comment '메시지 본문',
    status          varchar(16)   not null comment 'PENDING | SENT | FAILED',
    retry_count     int           not null comment '재시도 횟수',
    next_attempt_at datetime(6)   not null comment '다음 발송 시도 시각',
    sent_at         datetime(6)   null comment '발송 시각',
    last_error      varchar(1000) null comment '마지막 실패 사유',
    created_at      datetime(6)   not null,
    updated_at      datetime(6)   not null
);

create index idx_notification_messages_pending on notification_messages (status, next_attempt_at);

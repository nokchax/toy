alter table crawls
    add column fetch_method varchar(16) not null default 'HTTP' comment '페이지를 가져오는 방법 HTTP | BROWSER';

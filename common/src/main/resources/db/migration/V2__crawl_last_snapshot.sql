alter table crawls
    add column last_snapshot varchar(4000) null comment '직전 결과 텍스트 (CHANGED 모드에서 이전 값 표시용, 줄 단위)';

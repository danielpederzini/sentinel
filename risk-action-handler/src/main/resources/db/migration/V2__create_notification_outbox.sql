create table if not exists notification_outbox (
    id bigint generated always as identity primary key,
    transaction_id varchar(64) not null,
    email_subject varchar(512) not null,
    email_content text not null,
    status varchar(16) not null default 'PENDING',
    created_at timestamp not null default now(),
    sent_at timestamp,
    failure_reason text,
    attempt_count integer not null default 0,
    constraint fk_notification_outbox_transaction foreign key (transaction_id) references transactions (id)
);

create index if not exists idx_notification_outbox_status on notification_outbox (status);
create index if not exists idx_notification_outbox_transaction_id on notification_outbox (transaction_id);

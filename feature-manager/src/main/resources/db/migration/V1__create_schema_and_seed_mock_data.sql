create table if not exists users (
    id varchar(64) primary key,
    email varchar(255) not null,
    birth_date date not null,
    home_country_code varchar(2) not null,
    creation_date_time timestamp not null
);

create table if not exists merchants (
    id varchar(64) primary key,
    email varchar(255) not null,
    risk_score real not null,
    category varchar(32) not null,
    creation_date_time timestamp not null
);

create table if not exists cards (
    id varchar(64) primary key,
    type varchar(32) not null,
    creation_date_time timestamp not null
);

create table if not exists trusted_devices (
    id varchar(64) primary key,
    name varchar(255),
    type varchar(32) not null,
    creation_date_time timestamp not null
);

create table if not exists transactions (
    id varchar(64) primary key,
    amount numeric(19, 2) not null,
    country_code varchar(2) not null,
    ip_address varchar(64) not null,
    creation_date_time timestamp not null,
    user_id varchar(64) not null,
    trusted_device_id varchar(64),
    merchant_id varchar(64) not null,
    card_id varchar(64) not null,
    constraint fk_transactions_user foreign key (user_id) references users (id),
    constraint fk_transactions_trusted_device foreign key (trusted_device_id) references trusted_devices (id),
    constraint fk_transactions_merchant foreign key (merchant_id) references merchants (id),
    constraint fk_transactions_card foreign key (card_id) references cards (id)
);

create index if not exists idx_transactions_user_id on transactions (user_id);
create index if not exists idx_transactions_creation_date_time on transactions (creation_date_time);
create index if not exists idx_transactions_merchant_id on transactions (merchant_id);
create index if not exists idx_transactions_card_id on transactions (card_id);

insert into users (id, email, birth_date, home_country_code, creation_date_time)
values
    ('user-001', 'alice.silva@example.com', date '1992-03-14', 'BR', timestamp '2026-01-10 09:15:00'),
    ('user-002', 'bruno.costa@example.com', date '1988-07-02', 'US', timestamp '2026-01-12 14:45:00'),
    ('user-003', 'carla.souza@example.com', date '1996-11-28', 'AR', timestamp '2026-01-18 11:20:00')
on conflict (id) do nothing;

insert into merchants (id, email, risk_score, category, creation_date_time)
values
    ('merchant-001', 'grocery.hub@example.com', 0.12, 'GROCERY', timestamp '2026-01-05 08:00:00'),
    ('merchant-002', 'travel.now@example.com', 0.73, 'TRAVEL', timestamp '2026-01-06 10:30:00'),
    ('merchant-003', 'movie.plus@example.com', 0.41, 'ENTERTAINMENT', timestamp '2026-01-08 16:10:00')
on conflict (id) do nothing;

insert into cards (id, type, creation_date_time)
values
    ('card-001', 'CREDIT', timestamp '2025-10-01 10:00:00'),
    ('card-002', 'DEBIT', timestamp '2025-12-15 12:30:00'),
    ('card-003', 'CREDIT_AND_DEBIT', timestamp '2026-02-01 18:45:00')
on conflict (id) do nothing;

insert into trusted_devices (id, name, type, creation_date_time)
values
    ('device-001', 'Alice Phone', 'CELLPHONE', timestamp '2026-02-15 07:30:00'),
    ('device-002', 'Bruno Laptop', 'LAPTOP', timestamp '2026-02-20 13:00:00'),
    ('device-003', 'Store POS', 'POS', timestamp '2026-02-22 09:10:00')
on conflict (id) do nothing;

insert into transactions (id, amount, country_code, ip_address, creation_date_time, user_id, trusted_device_id, merchant_id, card_id)
values
    ('tx-001', 120.50, 'BR', '177.54.10.20', timestamp '2026-05-01 09:00:00', 'user-001', 'device-001', 'merchant-001', 'card-001'),
    ('tx-002', 85.90, 'BR', '177.54.10.21', timestamp '2026-05-01 09:03:00', 'user-001', 'device-001', 'merchant-001', 'card-001'),
    ('tx-003', 430.00, 'US', '34.120.10.11', timestamp '2026-05-01 10:10:00', 'user-002', 'device-002', 'merchant-002', 'card-002'),
    ('tx-004', 39.99, 'US', '34.120.10.12', timestamp '2026-05-01 10:40:00', 'user-002', 'device-002', 'merchant-003', 'card-002'),
    ('tx-005', 210.75, 'AR', '181.23.44.10', timestamp '2026-05-01 11:15:00', 'user-003', null, 'merchant-003', 'card-003'),
    ('tx-006', 199.90, 'CL', '190.12.1.33', timestamp '2026-05-01 11:55:00', 'user-003', null, 'merchant-002', 'card-003')
on conflict (id) do nothing;


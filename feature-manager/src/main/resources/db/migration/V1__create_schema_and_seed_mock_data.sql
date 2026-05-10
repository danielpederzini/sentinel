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
select
    format('user-%s', lpad(gs::text, 3, '0')),
    format('user.%s@example.com', lpad(gs::text, 3, '0')),
    date '1980-01-01' + ((gs * 97) % 12000),
    (array['BR', 'US', 'AR', 'CL', 'CA', 'MX', 'GB', 'DE', 'FR', 'IN'])[(gs % 10) + 1],
    timestamp '2026-02-01 08:00:00' + make_interval(hours => gs)
from generate_series(1, 100) as gs
on conflict (id) do nothing;

insert into cards (id, type, creation_date_time)
select
    format('card-%s', lpad(gs::text, 3, '0')),
    case gs % 4
        when 0 then 'CREDIT'
        when 1 then 'DEBIT'
        when 2 then 'CREDIT_AND_DEBIT'
        else 'OTHER'
    end,
    timestamp '2026-01-01 09:00:00' + make_interval(days => gs)
from generate_series(1, 100) as gs
on conflict (id) do nothing;

insert into trusted_devices (id, name, type, creation_date_time)
select
    format('device-%s', lpad(gs::text, 3, '0')),
    format('User %s Device', lpad(gs::text, 3, '0')),
    case gs % 5
        when 0 then 'CELLPHONE'
        when 1 then 'LAPTOP'
        when 2 then 'DESKTOP'
        when 3 then 'POS'
        else 'OTHER'
    end,
    timestamp '2026-02-10 10:00:00' + make_interval(hours => gs)
from generate_series(1, 100) as gs
on conflict (id) do nothing;

insert into transactions (id, amount, country_code, ip_address, creation_date_time, user_id, trusted_device_id, merchant_id, card_id)
select
    format('tx-%s', lpad((gs + 1000)::text, 4, '0')),
    round((25 + ((gs * 19) % 700) + ((gs % 100) / 100.0))::numeric, 2),
    (array['BR', 'US', 'AR', 'CL', 'CA', 'MX', 'GB', 'DE', 'FR', 'IN'])[(gs % 10) + 1],
    format('10.%s.%s.%s', gs % 255, (gs * 3) % 255, (gs * 7) % 255),
    timestamp '2026-05-02 00:00:00' + make_interval(mins => gs * 3),
    format('user-%s', lpad(gs::text, 3, '0')),
    case when gs % 5 = 0 then null else format('device-%s', lpad(gs::text, 3, '0')) end,
    case gs % 3
        when 0 then 'merchant-001'
        when 1 then 'merchant-002'
        else 'merchant-003'
    end,
    format('card-%s', lpad(gs::text, 3, '0'))
from generate_series(1, 100) as gs
on conflict (id) do nothing;


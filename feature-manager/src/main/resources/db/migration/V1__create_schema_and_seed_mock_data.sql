-- ===================================================================
-- V1: Create schema and generate seed data
-- ===================================================================

-- === SCHEMA ===

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
    ip_address varchar(64),
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

create table if not exists transaction_feature_vectors (
    transaction_id varchar(64) primary key,
    amount numeric(19, 2) not null,
    user_average_amount numeric(19, 2) not null,
    user_historical_transaction_count bigint not null default 0,
    user_transaction_count5_min bigint not null,
    user_transaction_count1_hour bigint not null,
    seconds_since_last_transaction bigint not null,
    merchant_risk_score real not null,
    is_device_trusted boolean not null,
    has_country_mismatch boolean not null,
    amount_to_average_ratio real not null,
    hour_of_day integer not null,
    ip_risk_score real not null check (ip_risk_score >= 0 and ip_risk_score <= 1),
    card_age_days bigint not null,
    amount_velocity1_hour numeric(19, 2) not null default 0,
    user_account_age_days bigint not null default 0,
    day_of_week integer not null default 1,
    merchant_category integer not null default 7,
    card_type integer not null default 3,
    distinct_merchant_count1_hour bigint not null default 0,
    log_amount double precision not null default 0,
    log_seconds_since_last_transaction double precision not null default 0,
    log_velocity1_hour double precision not null default 0,
    amount_times_merchant_risk double precision not null default 0,
    risk_score_product double precision not null default 0,
    ip_device_risk double precision not null default 0,
    country_ip_risk double precision not null default 0,
    velocity_amount_interaction double precision not null default 0,
    recency_velocity double precision not null default 0,
    amount_deviation double precision not null default 0,
    is_night boolean not null default false,
    velocity_intensity double precision not null default 0,
    constraint fk_tfv_transaction foreign key (transaction_id) references transactions (id)
);

create table if not exists transaction_predictions (
    transaction_id varchar(64) primary key,
    fraud_probability double precision not null,
    risk_level varchar(16) not null,
    model_version varchar(64) not null,
    constraint fk_tp_transaction foreign key (transaction_id) references transactions (id)
);

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


-- === SEED DATA ===
-- Procedurally generated using PL/pgSQL. Adjust the constants below to
-- control how many rows are inserted.

DO $$
DECLARE
    -- ---------------------------------------------------------------
    -- Configurable seed-data sizes
    -- ---------------------------------------------------------------
    v_num_users        constant integer := 100000;
    v_num_merchants    constant integer := 150;
    v_num_devices      constant integer := 100000;
    v_num_transactions constant integer := 100000;

    v_countries  constant text[] := ARRAY[
        'US','BR','AR','DE','ES','SE','NL','GB','CA','JP',
        'AU','MX','CL','ZA','PT','FR','IT','CH','CN','IN'
    ];
    v_categories constant text[] := ARRAY[
        'GROCERY','RESTAURANT','ENTERTAINMENT','TRAVEL',
        'HEALTHCARE','EDUCATION','UTILITIES','OTHER'
    ];
    v_card_types  constant text[] := ARRAY['CREDIT','DEBIT','CREDIT_AND_DEBIT','OTHER'];
    v_device_types constant text[] := ARRAY['CELLPHONE','LAPTOP','TABLET','POS','OTHER'];
BEGIN
    PERFORM setseed(0.42);

    -- 1. Users
    INSERT INTO users (id, email, birth_date, home_country_code, creation_date_time)
    SELECT
        'user-' || lpad(g::text, 6, '0'),
        'user' || g || '@example.com',
        date '1970-01-01' + floor(random() * 11000)::int,
        v_countries[1 + floor(random() * array_length(v_countries, 1))::int],
        now() - random() * interval '540 days'
    FROM generate_series(1, v_num_users) AS g
    ON CONFLICT (id) DO NOTHING;

    -- 2. Merchants
    INSERT INTO merchants (id, email, risk_score, category, creation_date_time)
    SELECT
        'merchant-' || lpad(g::text, 4, '0'),
        'merchant' || g || '@example.com',
        round((random() * 0.85 + 0.05)::numeric, 2)::real,
        v_categories[1 + floor(random() * array_length(v_categories, 1))::int],
        now() - random() * interval '300 days'
    FROM generate_series(1, v_num_merchants) AS g
    ON CONFLICT (id) DO NOTHING;

    -- 3. Cards (one per user)
    INSERT INTO cards (id, type, creation_date_time)
    SELECT
        'card-' || lpad(g::text, 6, '0'),
        v_card_types[1 + floor(random() * array_length(v_card_types, 1))::int],
        now() - random() * interval '540 days'
    FROM generate_series(1, v_num_users) AS g
    ON CONFLICT (id) DO NOTHING;

    -- 4. Trusted devices
    INSERT INTO trusted_devices (id, name, type, creation_date_time)
    SELECT
        'device-' || lpad(g::text, 6, '0'),
        'Device ' || g,
        v_device_types[1 + floor(random() * array_length(v_device_types, 1))::int],
        now() - random() * interval '540 days'
    FROM generate_series(1, v_num_devices) AS g
    ON CONFLICT (id) DO NOTHING;

    -- 5. Transactions
    INSERT INTO transactions (id, amount, country_code, ip_address, creation_date_time,
                              user_id, trusted_device_id, merchant_id, card_id)
    SELECT
        'tx-' || lpad(g::text, 6, '0'),
        round(exp(2.5 + random() * 5.0)::numeric, 2),
        v_countries[1 + floor(random() * array_length(v_countries, 1))::int],
        (1 + floor(random() * 223))::int || '.' ||
            floor(random() * 256)::int   || '.' ||
            floor(random() * 256)::int   || '.' ||
            (1 + floor(random() * 254))::int,
        now() - random() * interval '365 days',
        'user-'     || lpad((1 + floor(random() * v_num_users))::text, 6, '0'),
        CASE WHEN random() < 0.70
             THEN 'device-' || lpad((1 + floor(random() * v_num_devices))::text, 6, '0')
             ELSE NULL END,
        'merchant-' || lpad((1 + floor(random() * v_num_merchants))::text, 4, '0'),
        'card-'     || lpad((1 + floor(random() * v_num_users))::text, 6, '0')
    FROM generate_series(1, v_num_transactions) AS g
    ON CONFLICT (id) DO NOTHING;

    -- 6. Transaction feature vectors (base + engineered features)
    INSERT INTO transaction_feature_vectors (
        transaction_id, amount, user_average_amount,
        user_historical_transaction_count,
        user_transaction_count5_min, user_transaction_count1_hour,
        seconds_since_last_transaction, merchant_risk_score,
        is_device_trusted, has_country_mismatch, amount_to_average_ratio,
        hour_of_day, ip_risk_score, card_age_days,
        amount_velocity1_hour, user_account_age_days, day_of_week,
        merchant_category, card_type, distinct_merchant_count1_hour,
        log_amount, log_seconds_since_last_transaction, log_velocity1_hour,
        amount_times_merchant_risk, risk_score_product, ip_device_risk,
        country_ip_risk, velocity_amount_interaction, recency_velocity,
        amount_deviation, is_night, velocity_intensity
    )
    SELECT
        b.tx_id,
        b.amount,
        b.avg_amount,
        b.hist_count,
        b.count_5m,
        b.count_1h,
        b.secs_since,
        b.merch_risk,
        b.device_trusted,
        b.country_mm,
        b.avg_ratio,
        b.hr,
        b.ip_risk,
        b.card_age,
        b.velocity_1h,
        b.acct_age,
        b.dow,
        b.merch_cat,
        b.ctype,
        b.dist_merch,
        -- engineered features
        ln(b.amount::double precision + 1.0),
        ln(b.secs_since::double precision + 1.0),
        ln(b.velocity_1h::double precision + 1.0),
        b.amount::double precision * b.merch_risk::double precision,
        b.merch_risk::double precision * b.ip_risk::double precision,
        b.ip_risk::double precision * (CASE WHEN b.device_trusted THEN 0.0 ELSE 1.0 END),
        (CASE WHEN b.country_mm THEN b.ip_risk::double precision ELSE 0.0 END),
        b.velocity_1h::double precision * b.avg_ratio::double precision / 100.0,
        b.count_1h::double precision / (b.secs_since::double precision + 1.0),
        abs(b.avg_ratio::double precision - 1.0),
        (b.hr < 6 OR b.hr >= 22),
        b.velocity_1h::double precision / greatest(b.count_1h, 1)::double precision
    FROM (
        SELECT
            t.id                                                          AS tx_id,
            t.amount,
            round((50.0 + random() * 450.0)::numeric, 2)                 AS avg_amount,
            floor(random() * 50)::bigint                                  AS hist_count,
            floor(random() * 4)::bigint                                   AS count_5m,
            (1 + floor(random() * 9))::bigint                             AS count_1h,
            (60 + floor(random() * 500000))::bigint                       AS secs_since,
            round((random() * 0.85 + 0.05)::numeric, 4)::real            AS merch_risk,
            (t.trusted_device_id IS NOT NULL)                             AS device_trusted,
            (random() < 0.15)                                             AS country_mm,
            round((t.amount / greatest(50.0 + random() * 450.0, 1.0))::numeric, 4)::real AS avg_ratio,
            extract(hour FROM t.creation_date_time)::integer              AS hr,
            round((random() * 0.50)::numeric, 4)::real                    AS ip_risk,
            (30 + floor(random() * 970))::bigint                          AS card_age,
            round((t.amount + random() * 500.0)::numeric, 2)             AS velocity_1h,
            (30 + floor(random() * 1970))::bigint                         AS acct_age,
            extract(isodow FROM t.creation_date_time)::integer            AS dow,
            floor(random() * 8)::integer                                  AS merch_cat,
            floor(random() * 4)::integer                                  AS ctype,
            (1 + floor(random() * 3))::bigint                             AS dist_merch
        FROM transactions t
    ) b
    ON CONFLICT (transaction_id) DO NOTHING;

    -- 7. Transaction predictions
    INSERT INTO transaction_predictions (transaction_id, fraud_probability, risk_level, model_version, is_marked_as_safe)
    SELECT
        sub.tx_id,
        sub.prob,
        CASE
            WHEN sub.prob > 0.70 THEN 'HIGH'
            WHEN sub.prob > 0.30 THEN 'MEDIUM'
            ELSE 'LOW'
        END,
        'lgbm_1.0.0',
        sub.prob < 0.70
    FROM (
        SELECT t.id AS tx_id, round(random()::numeric, 4)::double precision AS prob
        FROM transactions t
    ) sub
    ON CONFLICT (transaction_id) DO NOTHING;

END $$;

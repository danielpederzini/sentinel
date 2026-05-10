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

create table if not exists transaction_feature_vectors (
    transaction_id varchar(64) primary key,
    amount numeric(19, 2) not null,
    user_average_amount numeric(19, 2) not null,
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
    constraint fk_tfv_transaction foreign key (transaction_id) references transactions (id)
);

create table if not exists transaction_predictions (
    transaction_id varchar(64) primary key,
    fraud_probability double precision not null,
    risk_level varchar(16) not null,
    model_version varchar(64) not null,
    constraint fk_tp_transaction foreign key (transaction_id) references transactions (id)
);

insert into users (id, email, birth_date, home_country_code, creation_date_time)
values
    ('user-001', 'hugo.silva1@example.com', date '1993-05-08', 'DE', timestamp '2025-01-22 18:57:00'),
    ('user-002', 'julia.souza2@example.com', date '1988-07-02', 'AR', timestamp '2025-02-08 15:38:00'),
    ('user-003', 'bruno.correia3@example.com', date '1976-12-21', 'SE', timestamp '2026-02-15 16:17:00'),
    ('user-004', 'alice.jones4@example.com', date '1995-03-23', 'JP', timestamp '2026-03-05 10:48:00'),
    ('user-005', 'wendy.lima5@example.com', date '1972-07-04', 'IN', timestamp '2026-05-09 19:02:00'),
    ('user-006', 'ethan.correia6@example.com', date '1973-07-03', 'SE', timestamp '2026-05-28 12:36:00'),
    ('user-007', 'maria.williams7@example.com', date '1972-01-22', 'DE', timestamp '2026-01-28 10:55:00'),
    ('user-008', 'giselle.gomes8@example.com', date '1978-08-21', 'IN', timestamp '2025-03-12 10:42:00'),
    ('user-009', 'sofia.williams9@example.com', date '1999-11-21', 'BR', timestamp '2025-05-24 10:10:00'),
    ('user-010', 'ethan.gomes10@example.com', date '1978-11-23', 'SE', timestamp '2025-03-27 19:49:00'),
    ('user-011', 'david.ferreira11@example.com', date '1996-01-26', 'GB', timestamp '2026-03-03 10:58:00'),
    ('user-012', 'laura.wilson12@example.com', date '1992-06-07', 'NL', timestamp '2026-04-05 11:08:00'),
    ('user-013', 'paula.brown13@example.com', date '1987-09-09', 'US', timestamp '2026-05-13 12:14:00'),
    ('user-014', 'iris.araujo14@example.com', date '1985-02-25', 'AU', timestamp '2025-02-21 09:50:00'),
    ('user-015', 'carlos.barbosa15@example.com', date '1972-07-13', 'ZA', timestamp '2026-05-09 15:55:00'),
    ('user-016', 'alice.johnson16@example.com', date '1993-02-22', 'SE', timestamp '2026-03-04 11:27:00'),
    ('user-017', 'karen.ribeiro17@example.com', date '1970-12-24', 'ES', timestamp '2025-05-04 20:40:00'),
    ('user-018', 'uma.miller18@example.com', date '1990-09-20', 'CN', timestamp '2025-03-25 09:34:00'),
    ('user-019', 'ivan.taylor19@example.com', date '1970-10-11', 'NL', timestamp '2025-01-12 20:51:00'),
    ('user-020', 'uma.ferreira20@example.com', date '1971-04-19', 'BR', timestamp '2025-04-27 08:48:00'),
    ('user-021', 'julia.jones21@example.com', date '1974-03-22', 'NL', timestamp '2025-03-17 20:38:00'),
    ('user-022', 'carlos.almeida22@example.com', date '1999-09-25', 'CN', timestamp '2026-04-22 17:23:00'),
    ('user-023', 'diana.wilson23@example.com', date '1986-08-04', 'DE', timestamp '2025-01-11 07:37:00'),
    ('user-024', 'kevin.ferreira24@example.com', date '1988-04-01', 'BR', timestamp '2025-02-03 07:55:00'),
    ('user-025', 'wendy.souza25@example.com', date '1986-04-09', 'NL', timestamp '2025-05-05 18:59:00'),
    ('user-026', 'laura.melo26@example.com', date '1985-04-26', 'NL', timestamp '2026-02-04 08:42:00'),
    ('user-027', 'carlos.carvalho27@example.com', date '1983-07-15', 'AU', timestamp '2025-01-13 18:21:00'),
    ('user-028', 'giselle.ferreira28@example.com', date '1976-04-18', 'MX', timestamp '2025-04-06 11:29:00'),
    ('user-029', 'paula.davis29@example.com', date '1999-02-15', 'SE', timestamp '2025-01-21 15:53:00'),
    ('user-030', 'alice.souza30@example.com', date '1999-04-06', 'JP', timestamp '2026-04-07 20:25:00'),
    ('user-031', 'david.santos31@example.com', date '1982-01-13', 'ES', timestamp '2026-03-14 18:46:00'),
    ('user-032', 'kevin.johnson32@example.com', date '1992-08-05', 'CN', timestamp '2026-02-02 16:47:00'),
    ('user-033', 'julia.costa33@example.com', date '1993-06-02', 'AU', timestamp '2026-05-28 15:10:00'),
    ('user-034', 'david.araujo34@example.com', date '1972-03-03', 'ZA', timestamp '2025-02-13 08:56:00'),
    ('user-035', 'laura.ferreira35@example.com', date '1988-10-02', 'ZA', timestamp '2025-04-22 16:36:00'),
    ('user-036', 'ivan.nascimento36@example.com', date '1999-05-07', 'GB', timestamp '2025-03-13 09:42:00'),
    ('user-037', 'uma.ribeiro37@example.com', date '1980-02-01', 'MX', timestamp '2025-01-18 10:32:00'),
    ('user-038', 'rafael.pereira38@example.com', date '1999-06-03', 'DE', timestamp '2026-03-06 14:53:00'),
    ('user-039', 'julia.williams39@example.com', date '1979-10-26', 'PT', timestamp '2025-05-10 17:06:00'),
    ('user-040', 'iris.rodrigues40@example.com', date '1973-02-24', 'SE', timestamp '2025-03-10 16:13:00'),
    ('user-041', 'wendy.almeida41@example.com', date '1991-11-28', 'ES', timestamp '2026-03-28 07:05:00'),
    ('user-042', 'carlos.miller42@example.com', date '1978-01-01', 'GB', timestamp '2025-03-06 18:28:00'),
    ('user-043', 'kevin.williams43@example.com', date '1983-09-01', 'CA', timestamp '2025-02-18 07:53:00'),
    ('user-044', 'yasmin.melo44@example.com', date '1987-03-14', 'CH', timestamp '2025-03-12 19:55:00'),
    ('user-045', 'carla.wilson45@example.com', date '1981-04-22', 'DE', timestamp '2025-03-25 15:56:00'),
    ('user-046', 'bella.barbosa46@example.com', date '1993-03-08', 'CL', timestamp '2025-04-01 09:47:00'),
    ('user-047', 'wendy.garcia47@example.com', date '1999-07-26', 'DE', timestamp '2026-02-26 18:06:00'),
    ('user-048', 'zara.davis48@example.com', date '1971-08-08', 'CN', timestamp '2026-03-10 20:50:00'),
    ('user-049', 'oscar.ferreira49@example.com', date '1970-11-07', 'IT', timestamp '2026-03-28 08:49:00'),
    ('user-050', 'sofia.carvalho50@example.com', date '1990-09-13', 'SE', timestamp '2026-01-04 11:11:00'),
    ('user-051', 'miguel.rodrigues51@example.com', date '1971-02-20', 'JP', timestamp '2026-03-14 16:32:00'),
    ('user-052', 'hugo.gomes52@example.com', date '1998-10-07', 'ES', timestamp '2025-04-01 15:59:00'),
    ('user-053', 'julia.johnson53@example.com', date '1993-12-24', 'CN', timestamp '2026-04-03 17:58:00'),
    ('user-054', 'wendy.barbosa54@example.com', date '1980-11-28', 'CA', timestamp '2026-05-10 17:26:00'),
    ('user-055', 'victor.gomes55@example.com', date '1992-05-18', 'CH', timestamp '2025-04-22 13:43:00'),
    ('user-056', 'lucas.barbosa56@example.com', date '1988-05-13', 'SE', timestamp '2025-03-10 10:27:00'),
    ('user-057', 'miguel.barbosa57@example.com', date '1990-06-15', 'MX', timestamp '2026-02-17 14:50:00'),
    ('user-058', 'karen.johnson58@example.com', date '1972-05-17', 'ZA', timestamp '2026-01-27 19:15:00'),
    ('user-059', 'uma.ferreira59@example.com', date '1995-04-05', 'AR', timestamp '2025-02-16 16:54:00'),
    ('user-060', 'elena.ribeiro60@example.com', date '1983-11-19', 'CN', timestamp '2026-04-13 10:09:00'),
    ('user-061', 'alice.wilson61@example.com', date '1994-02-25', 'JP', timestamp '2025-02-26 18:33:00'),
    ('user-062', 'ethan.costa62@example.com', date '1987-04-28', 'CA', timestamp '2026-02-26 14:42:00'),
    ('user-063', 'ivan.correia63@example.com', date '1989-06-25', 'MX', timestamp '2026-05-15 09:47:00'),
    ('user-064', 'fiona.ribeiro64@example.com', date '1978-04-27', 'ES', timestamp '2026-02-09 14:04:00'),
    ('user-065', 'tiago.ferreira65@example.com', date '1978-06-11', 'SE', timestamp '2025-02-05 10:24:00'),
    ('user-066', 'jorge.williams66@example.com', date '1976-02-14', 'JP', timestamp '2026-05-15 13:03:00'),
    ('user-067', 'nadia.miller67@example.com', date '1983-07-25', 'US', timestamp '2025-05-13 14:00:00'),
    ('user-068', 'xavier.oliveira68@example.com', date '1994-07-28', 'JP', timestamp '2025-04-08 11:27:00'),
    ('user-069', 'gabriel.silva69@example.com', date '1982-06-22', 'IT', timestamp '2025-04-05 16:34:00'),
    ('user-070', 'bruno.taylor70@example.com', date '1982-10-19', 'AR', timestamp '2025-04-05 20:29:00'),
    ('user-071', 'lucas.costa71@example.com', date '1978-07-11', 'CN', timestamp '2026-03-11 19:56:00'),
    ('user-072', 'zara.rodrigues72@example.com', date '1994-07-09', 'BR', timestamp '2026-01-24 15:03:00'),
    ('user-073', 'xavier.ferreira73@example.com', date '1990-02-25', 'AU', timestamp '2025-02-07 20:01:00'),
    ('user-074', 'omar.pereira74@example.com', date '1977-03-16', 'CA', timestamp '2025-04-23 11:49:00'),
    ('user-075', 'yasmin.santos75@example.com', date '1989-10-24', 'CA', timestamp '2025-03-04 16:01:00'),
    ('user-076', 'uma.melo76@example.com', date '1991-07-13', 'CN', timestamp '2025-05-23 20:40:00'),
    ('user-077', 'paula.lima77@example.com', date '1992-05-28', 'ZA', timestamp '2025-05-26 07:22:00'),
    ('user-078', 'julia.martins78@example.com', date '1991-06-03', 'PT', timestamp '2026-01-28 13:52:00'),
    ('user-079', 'gabriel.lima79@example.com', date '1983-06-21', 'MX', timestamp '2025-04-06 18:33:00'),
    ('user-080', 'sofia.barbosa80@example.com', date '1995-09-25', 'NL', timestamp '2026-04-27 18:37:00'),
    ('user-081', 'sofia.nascimento81@example.com', date '1997-04-27', 'BR', timestamp '2026-04-08 19:29:00'),
    ('user-082', 'laura.barbosa82@example.com', date '1991-07-11', 'AR', timestamp '2026-03-06 14:13:00'),
    ('user-083', 'xavier.garcia83@example.com', date '1978-06-09', 'ZA', timestamp '2026-05-01 15:12:00'),
    ('user-084', 'felipe.ferreira84@example.com', date '1993-07-16', 'SE', timestamp '2025-04-21 18:31:00'),
    ('user-085', 'diana.garcia85@example.com', date '1970-02-10', 'DE', timestamp '2026-02-10 17:37:00'),
    ('user-086', 'yasmin.rocha86@example.com', date '1987-09-12', 'JP', timestamp '2026-03-23 14:17:00'),
    ('user-087', 'uma.rodrigues87@example.com', date '1977-02-24', 'CN', timestamp '2026-01-24 15:48:00'),
    ('user-088', 'lucas.almeida88@example.com', date '1976-12-16', 'ES', timestamp '2026-01-27 10:18:00'),
    ('user-089', 'oscar.carvalho89@example.com', date '1975-05-01', 'SE', timestamp '2025-03-02 07:35:00'),
    ('user-090', 'tiago.williams90@example.com', date '2000-03-21', 'NL', timestamp '2025-01-19 11:30:00'),
    ('user-091', 'fiona.ribeiro91@example.com', date '1980-03-02', 'ES', timestamp '2026-01-27 08:25:00'),
    ('user-092', 'gabriel.souza92@example.com', date '1988-11-22', 'AU', timestamp '2025-02-26 16:19:00'),
    ('user-093', 'felipe.ferreira93@example.com', date '1973-09-25', 'JP', timestamp '2025-05-13 14:58:00'),
    ('user-094', 'diana.oliveira94@example.com', date '1997-10-14', 'FR', timestamp '2025-05-24 08:48:00'),
    ('user-095', 'nadia.smith95@example.com', date '1976-05-22', 'BR', timestamp '2025-02-06 15:04:00'),
    ('user-096', 'karen.silva96@example.com', date '1983-08-23', 'ZA', timestamp '2026-03-02 10:18:00'),
    ('user-097', 'tiago.williams97@example.com', date '1997-08-03', 'DE', timestamp '2026-05-22 19:59:00'),
    ('user-098', 'maria.martins98@example.com', date '1973-09-08', 'CH', timestamp '2026-02-03 07:10:00'),
    ('user-099', 'uma.barbosa99@example.com', date '1993-10-10', 'MX', timestamp '2025-04-23 11:44:00'),
    ('user-100', 'adrian.rodrigues100@example.com', date '1986-09-16', 'MX', timestamp '2025-05-02 13:47:00')
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

insert into transactions (
    id,
    amount,
    country_code,
    ip_address,
    creation_date_time,
    user_id,
    trusted_device_id,
    merchant_id,
    card_id
)
values
    ('tx-001', 120.50, 'DE', '177.54.10.20', timestamp '2026-05-01 09:00:00', 'user-001', 'device-001', 'merchant-001', 'card-001'),
    ('tx-002', 85.90, 'DE', '177.54.10.21', timestamp '2026-05-01 09:03:00', 'user-001', 'device-001', 'merchant-001', 'card-001'),
    ('tx-003', 430.00, 'AR', '34.120.10.11', timestamp '2026-05-01 10:10:00', 'user-002', 'device-002', 'merchant-002', 'card-002'),
    ('tx-004', 39.99, 'AR', '34.120.10.12', timestamp '2026-05-01 10:40:00', 'user-002', 'device-002', 'merchant-003', 'card-002'),
    ('tx-005', 210.75, 'AR', '181.23.44.10', timestamp '2026-05-01 11:15:00', 'user-003', null, 'merchant-003', 'card-003'),
    ('tx-006', 199.90, 'CL', '190.12.1.33', timestamp '2026-05-01 11:55:00', 'user-003', null, 'merchant-002', 'card-003')
on conflict (id) do nothing;

insert into transaction_feature_vectors (
    transaction_id,
    amount,
    user_average_amount,
    user_transaction_count5_min,
    user_transaction_count1_hour,
    seconds_since_last_transaction,
    merchant_risk_score,
    is_device_trusted,
    has_country_mismatch,
    amount_to_average_ratio,
    hour_of_day,
    ip_risk_score,
    card_age_days
)
values
    ('tx-001', 120.50, 102.30, 2, 2, 180, 0.12, true,  false, 1.17,  9, 0.05, 221),
    ('tx-002', 85.90,  102.30, 2, 2, 120, 0.12, true,  false, 0.84,  9, 0.04, 221),
    ('tx-003', 430.00, 214.50, 1, 3, 300, 0.73, true,  false, 2.00, 10, 0.22, 146),
    ('tx-004', 39.99,  214.50, 1, 3, 90,  0.41, true,  false, 0.19, 10, 0.08, 146),
    ('tx-005', 210.75, 205.32, 1, 2, 240, 0.41, false, true, 1.03, 11, 0.15,  98),
    ('tx-006', 199.90, 205.32, 1, 2, 60,  0.73, false, true, 0.97, 11, 0.19,  98)
on conflict (transaction_id) do nothing;

insert into transaction_predictions (transaction_id, fraud_probability, risk_level, model_version)
values
    ('tx-001', 0.08, 'LOW', 'xgb_0.0.1'),
    ('tx-002', 0.05, 'LOW', 'xgb_0.0.1'),
    ('tx-003', 0.82, 'HIGH', 'xgb_0.0.1'),
    ('tx-004', 0.14, 'LOW', 'xgb_0.0.1'),
    ('tx-005', 0.31, 'MEDIUM', 'xgb_0.0.1'),
    ('tx-006', 0.58, 'MEDIUM', 'xgb_0.0.1')
on conflict (transaction_id) do nothing;


alter table transaction_feature_vectors
    add column if not exists user_historical_transaction_count bigint not null default 0;

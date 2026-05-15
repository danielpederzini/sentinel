alter table transaction_feature_vectors
    add column amount_velocity1_hour numeric(19, 2) not null default 0;

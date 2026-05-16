alter table transaction_feature_vectors
    drop column if exists amount_times_ip_risk,
    drop column if exists card_age_amount_ratio,
    drop column if exists night_amount_ratio;

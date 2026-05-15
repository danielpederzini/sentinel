CREATE USER feature_manager_readonly WITH PASSWORD 'readonly';

GRANT CONNECT ON DATABASE sentinel TO feature_manager_readonly;

GRANT USAGE ON SCHEMA public TO feature_manager_readonly;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO feature_manager_readonly;

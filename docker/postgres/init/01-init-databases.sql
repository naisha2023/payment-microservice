CREATE DATABASE wallet_db;
CREATE DATABASE payment_db;
CREATE DATABASE ledger_db;
CREATE DATABASE notification_db;

GRANT ALL PRIVILEGES ON DATABASE wallet_db TO user_admin;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO user_admin;
GRANT ALL PRIVILEGES ON DATABASE ledger_db TO user_admin;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO user_admin;
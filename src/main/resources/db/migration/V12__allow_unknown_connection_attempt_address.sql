ALTER TABLE connection_attempts
    MODIFY COLUMN ip_hash CHAR(64) NULL;

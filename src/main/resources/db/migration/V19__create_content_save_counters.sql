CREATE TABLE couple_content_save_counters
(
    couple_id                 BIGINT      NOT NULL,
    saver_member_id           BIGINT      NOT NULL,
    save_count                BIGINT      NOT NULL DEFAULT 0,
    last_notified_milestone   BIGINT      NOT NULL DEFAULT 0,
    created_at                DATETIME(6) NOT NULL,
    updated_at                DATETIME(6) NOT NULL,
    PRIMARY KEY (couple_id, saver_member_id),
    CONSTRAINT ck_content_save_counter_count
        CHECK (save_count >= 0 AND last_notified_milestone >= 0),
    CONSTRAINT fk_content_save_counter_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id),
    CONSTRAINT fk_content_save_counter_saver
        FOREIGN KEY (saver_member_id) REFERENCES members (id)
);

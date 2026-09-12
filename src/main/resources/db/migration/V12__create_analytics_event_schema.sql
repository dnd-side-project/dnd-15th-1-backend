-- 둘픽 스키마 V12: 핵심 사용자 행동 이벤트.

CREATE TABLE analytics_events
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    event_key     VARCHAR(255) NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    member_id     BIGINT       NULL,
    couple_id     BIGINT       NULL,
    resource_type VARCHAR(50)  NULL,
    resource_id   BIGINT       NULL,
    occurred_at   DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_analytics_events_event_key
        UNIQUE (event_key),
    CONSTRAINT fk_analytics_events_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_analytics_events_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id)
);

CREATE INDEX idx_analytics_events_type_occurred
    ON analytics_events (event_type, occurred_at);

CREATE INDEX idx_analytics_events_member_occurred
    ON analytics_events (member_id, occurred_at);

CREATE INDEX idx_analytics_events_couple_occurred
    ON analytics_events (couple_id, occurred_at);

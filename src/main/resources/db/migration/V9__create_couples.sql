CREATE TABLE couples
(
    id                          BIGINT      NOT NULL AUTO_INCREMENT,
    status                      VARCHAR(20) NOT NULL,
    connected_at                DATETIME(6) NOT NULL,
    disconnected_at             DATETIME(6) NULL,
    disconnected_by_member_id   BIGINT      NULL,
    created_at                  DATETIME(6) NOT NULL,
    updated_at                  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_couples_disconnected_by_member
        FOREIGN KEY (disconnected_by_member_id) REFERENCES members (id)
);

CREATE TABLE active_couple_members
(
    member_id   BIGINT      NOT NULL,
    couple_id   BIGINT      NOT NULL,
    joined_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    CONSTRAINT uk_active_couple_members_couple_member
        UNIQUE (couple_id, member_id),
    CONSTRAINT fk_active_couple_members_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_active_couple_members_couple
        FOREIGN KEY (couple_id) REFERENCES couples (id)
);

CREATE INDEX idx_active_couple_members_couple_id
    ON active_couple_members (couple_id);

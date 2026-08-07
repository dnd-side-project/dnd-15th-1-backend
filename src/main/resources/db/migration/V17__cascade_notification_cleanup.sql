ALTER TABLE notifications
    DROP FOREIGN KEY fk_notifications_receiver;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_receiver
        FOREIGN KEY (receiver_member_id) REFERENCES members (id) ON DELETE CASCADE;

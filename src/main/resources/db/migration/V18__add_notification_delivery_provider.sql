ALTER TABLE notification_deliveries
    ADD COLUMN provider VARCHAR(10) NOT NULL DEFAULT 'FCM' AFTER push_device_id;

ALTER TABLE notification_deliveries
    ADD CONSTRAINT ck_notification_deliveries_provider
        CHECK (provider IN ('FCM', 'APNS'));

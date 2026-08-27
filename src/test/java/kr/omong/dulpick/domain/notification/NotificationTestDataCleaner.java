package kr.omong.dulpick.domain.notification;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collection;

final class NotificationTestDataCleaner {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    NotificationTestDataCleaner(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void clean(Collection<Long> memberIds, Collection<Long> coupleIds) {
        if (memberIds.isEmpty()) {
            return;
        }
        MapSqlParameterSource members = new MapSqlParameterSource("memberIds", memberIds);
        delete("DELETE FROM notifications WHERE receiver_member_id IN (:memberIds)", members);
        delete("DELETE FROM push_devices WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM marketing_consent_histories WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM member_notification_settings WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM refresh_tokens WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM social_accounts WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM member_profiles WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM connection_codes WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM connection_attempts WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM connection_rate_limit_subjects WHERE member_id IN (:memberIds)", members);
        delete("DELETE FROM active_couple_members WHERE member_id IN (:memberIds)", members);
        deleteCouples(coupleIds);
        delete("DELETE FROM members WHERE id IN (:memberIds)", members);
    }

    private void deleteCouples(Collection<Long> coupleIds) {
        if (coupleIds.isEmpty()) {
            return;
        }
        MapSqlParameterSource couples = new MapSqlParameterSource("coupleIds", coupleIds);
        delete(
                "DELETE FROM couple_content_save_counters WHERE couple_id IN (:coupleIds)",
                couples
        );
        delete("DELETE FROM couples WHERE id IN (:coupleIds)", couples);
    }

    private void delete(String sql, MapSqlParameterSource parameters) {
        jdbcTemplate.update(sql, parameters);
    }
}

package kr.omong.dulpick;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayResetPreparationTest {

    private static final Path RESET_MIGRATION = Path.of(
            "src/main/resources/db/migration-reset/V1__initial_schema.sql"
    );
    private static final Path RESET_PROFILE = Path.of(
            "src/main/resources/application-reset.yaml"
    );

    @Test
    void resetMigrationContainsFinalSchemaWithoutProductionDataOperations() throws IOException {
        String sql = Files.readString(RESET_MIGRATION, StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE members");
        assertThat(sql).contains("CREATE TABLE contents");
        assertThat(sql).contains("CREATE TABLE content_images");
        assertThat(sql).contains("CREATE TABLE place_images");
        assertThat(sql).contains("CREATE TABLE content_image_enrichment_backlogs");
        assertThat(sql).contains("CREATE TABLE place_image_enrichment_backlogs");
        assertThat(sql).contains("CREATE TABLE marketing_notification_campaigns");
        assertThat(sql).contains("CREATE TABLE email_announcements");
        assertThat(sql).contains("dulpick_category_code");
        assertThat(sql).contains("content_hash CHAR(64)");

        assertThat(sql).doesNotContain("DROP TABLE");
        assertThat(sql).doesNotContain("TRUNCATE");
        assertThat(sql).doesNotContain("DELETE FROM");
        assertThat(sql).doesNotContain("INSERT INTO");
        assertThat(sql).doesNotContain("UPDATE places");
        assertThat(sql).doesNotContain("UPDATE member_places");
    }

    @Test
    void resetProfileUsesExplicitCleanDatabaseSettings() throws IOException {
        String profile = Files.readString(RESET_PROFILE, StandardCharsets.UTF_8);

        assertThat(profile).contains("on-profile: reset");
        assertThat(profile).contains("locations: classpath:db/migration-reset");
        assertThat(profile).contains("baseline-on-migrate: false");
        assertThat(profile).contains("clean-disabled: true");
        assertThat(profile).contains("ddl-auto: validate");
    }

    @Test
    void resetMigrationListsAllApplicationTables() throws IOException {
        String sql = Files.readString(RESET_MIGRATION, StandardCharsets.UTF_8);
        List<String> tables = List.of(
                "members", "member_profiles", "social_accounts", "refresh_tokens",
                "login_nonces", "apple_revocation_outbox", "test_auth_credentials", "connection_codes", "couples",
                "active_couple_members", "places", "place_images", "contents", "place_imports",
                "place_candidates", "member_places", "content_places", "content_submissions",
                "member_notification_settings", "marketing_consent_histories", "push_devices",
                "notifications", "notification_deliveries", "member_feedbacks", "date_courses",
                "date_course_places", "recent_searches", "place_classifications", "walking_route_cache",
                "place_image_enrichment_backlogs", "content_images", "content_image_enrichment_backlogs",
                "marketing_notification_campaigns", "email_opt_outs", "email_announcements"
        );

        tables.forEach(table -> assertThat(sql).contains("CREATE TABLE " + table));
    }
}

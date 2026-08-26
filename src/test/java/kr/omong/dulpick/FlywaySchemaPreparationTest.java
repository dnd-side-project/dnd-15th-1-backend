package kr.omong.dulpick;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FlywaySchemaPreparationTest {

    private static final Path MIGRATION_DIRECTORY = Path.of("src/main/resources/db/migration");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("V(\\d+)__.+\\.sql");

    @Test
    void containsOnlyTheRebuiltV1ToV10MigrationChain() throws IOException {
        List<String> versions = migrationFiles()
                .map(path -> VERSIONED_MIGRATION.matcher(path.getFileName().toString()))
                .filter(java.util.regex.Matcher::matches)
                .map(matcher -> matcher.group(1))
                .toList();

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    }

    @Test
    void schemaMigrationsContainAllApplicationTablesAndNoDataOperations() throws IOException {
        String sql = migrationSql();

        List<String> tables = List.of(
                "members", "member_profiles", "social_accounts", "refresh_tokens", "login_nonces",
                "apple_revocation_outbox", "test_auth_credentials", "connection_codes", "couples",
                "active_couple_members", "connection_attempts", "places", "place_images", "contents",
                "place_imports", "place_candidates", "member_places", "content_places", "content_submissions",
                "member_notification_settings", "marketing_consent_histories", "push_devices", "notifications",
                "notification_deliveries", "couple_content_save_counters", "member_feedbacks", "date_courses",
                "date_course_places", "recent_searches", "place_classifications", "walking_route_cache",
                "place_image_enrichment_backlogs", "content_images", "content_image_enrichment_backlogs",
                "marketing_notification_campaigns", "email_opt_outs", "email_announcements"
        );

        tables.forEach(table -> assertThat(sql).contains("CREATE TABLE " + table));
        assertThat(sql).contains("dulpick_category_code");
        assertThat(sql).contains("content_hash CHAR(64)");
        assertThat(sql).doesNotContain("DROP TABLE", "TRUNCATE", "DELETE FROM", "INSERT INTO", "UPDATE ");
    }

    @Test
    void applicationUsesV1WithoutImplicitBaseline() throws IOException {
        String application = Files.readString(Path.of("src/main/resources/application.yaml"), StandardCharsets.UTF_8);

        assertThat(application).contains("baseline-on-migrate: false");
        assertThat(application).contains("clean-disabled: true");
        assertThat(application).doesNotContain("baseline-on-migrate: true");
    }

    private Stream<Path> migrationFiles() throws IOException {
        return Files.list(MIGRATION_DIRECTORY)
                .filter(path -> VERSIONED_MIGRATION.matcher(path.getFileName().toString()).matches())
                .sorted(Comparator.comparingInt(this::version));
    }

    private int version(Path path) {
        var matcher = VERSIONED_MIGRATION.matcher(path.getFileName().toString());
        assertThat(matcher.matches()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    private String migrationSql() throws IOException {
        try (Stream<Path> files = migrationFiles()) {
            return files
                    .map(this::readFile)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Flyway migration을 읽을 수 없습니다: " + path, exception);
        }
    }
}

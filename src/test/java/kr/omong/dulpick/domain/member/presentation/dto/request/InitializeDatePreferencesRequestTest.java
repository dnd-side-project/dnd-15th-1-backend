package kr.omong.dulpick.domain.member.presentation.dto.request;

import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitializeDatePreferencesRequestTest {

    @Test
    void treatsMissingAndBlankPreferencesAsUnset() {
        InitializeDatePreferencesRequest request = new InitializeDatePreferencesRequest(
                null,
                " ",
                "",
                null
        );

        assertThat(request.toDomainOrNull()).isNull();
    }

    @Test
    void convertsAllAllowedValuesToDomainPreferences() {
        InitializeDatePreferencesRequest request = new InitializeDatePreferencesRequest(
                " INDOOR ",
                "ACTIVE",
                "NIGHT",
                "FOOD"
        );

        assertThat(request.toDomainOrNull())
                .extracting(
                        preferences -> preferences.indoorOutdoor().name(),
                        preferences -> preferences.activityLevel().name(),
                        preferences -> preferences.dateTime().name(),
                        preferences -> preferences.dateFocus().name()
                )
                .containsExactly("INDOOR", "ACTIVE", "NIGHT", "FOOD");
    }

    @Test
    void rejectsPartialPreferencesWithNestedFieldPath() {
        InitializeDatePreferencesRequest request = new InitializeDatePreferencesRequest(
                "INDOOR",
                "ACTIVE",
                "NIGHT",
                null
        );

        assertThatThrownBy(request::toDomainOrNull)
                .isInstanceOfSatisfying(
                        InvalidMemberProfileException.class,
                        exception -> assertThat(exception.getFieldErrors().getFirst().field())
                                .isEqualTo("datePreferences.dateFocus")
                );
    }

    @Test
    void rejectsUnknownPreferenceWithNestedFieldPath() {
        InitializeDatePreferencesRequest request = new InitializeDatePreferencesRequest(
                "INDOOR",
                "INVALID",
                "NIGHT",
                "FOOD"
        );

        assertThatThrownBy(request::toDomainOrNull)
                .isInstanceOfSatisfying(
                        InvalidMemberProfileException.class,
                        exception -> assertThat(exception.getFieldErrors().getFirst().field())
                                .isEqualTo("datePreferences.activityLevel")
                );
    }
}

package kr.omong.dulpick.domain.member.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.application.query.MemberQueryService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class MemberControllerStructureTest {

    @Test
    void dependsOnlyOnMemberApplicationFacades() {
        assertThat(MemberController.class.getDeclaredFields())
                .extracting(Field::getType)
                .containsExactlyInAnyOrder(
                        MemberCommandService.class,
                        MemberQueryService.class
                );
    }

    @Test
    void documentsAllDatePreferenceOptions() {
        String description = operationDescription("updateDatePreferences");

        assertThat(description).contains(
                "INDOOR(실내)",
                "OUTDOOR(실외)",
                "ACTIVE(액티비티)",
                "STATIC(정적 활동)",
                "DAY(낮)",
                "NIGHT(밤)",
                "FOOD(식사 중심)",
                "SIGHTSEEING(볼거리 중심)"
        );
    }

    @Test
    void documentsOptionalDatePreferencesDuringOnboarding() {
        String description = operationDescription("initializeProfile");

        assertThat(description)
                .contains("nickname과 profileIcon은 필수")
                .contains("datePreferences는 선택값")
                .contains("생략하거나 null")
                .contains("일부만 입력하면 허용하지 않습니다");
    }

    private String operationDescription(String methodName) {
        for (Method method : MemberController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getAnnotation(Operation.class).description();
            }
        }
        throw new IllegalArgumentException("Controller method not found: " + methodName);
    }
}

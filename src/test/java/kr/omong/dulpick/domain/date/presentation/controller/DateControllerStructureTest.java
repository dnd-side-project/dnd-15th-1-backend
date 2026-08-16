package kr.omong.dulpick.domain.date.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.omong.dulpick.domain.date.application.command.DateCourseCommandService;
import kr.omong.dulpick.domain.date.application.query.DateCourseQueryService;
import kr.omong.dulpick.domain.date.application.query.HomeQueryService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DateControllerStructureTest {

    @Test
    void dateCourseControllerDependsOnlyOnDateFacades() {
        assertThat(Arrays.stream(DateCourseController.class.getDeclaredFields())
                .map(field -> field.getType().getName())
                .toList())
                .containsExactlyInAnyOrder(
                        DateCourseCommandService.class.getName(),
                        DateCourseQueryService.class.getName()
                );
    }

    @Test
    void homeControllerDependsOnlyOnHomeQueryService() {
        assertThat(Arrays.stream(HomeController.class.getDeclaredFields())
                .map(field -> field.getType().getName())
                .toList())
                .containsExactly(HomeQueryService.class.getName());
    }

    @Test
    void saveOperationDocumentsOptimisticLockAndSaveTypes() {
        String description = operationDescription(DateCourseController.class, "save");
        assertThat(description).contains("TEMPORARY", "CONFIRM", "낙관적 락", "version");
    }

    private String operationDescription(Class<?> controllerClass, String methodName) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getAnnotation(Operation.class).description();
            }
        }
        throw new IllegalArgumentException("Controller method not found: " + methodName);
    }
}

package kr.omong.dulpick.domain.member.presentation.controller;

import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.application.query.MemberQueryService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

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
}

package kr.omong.dulpick.domain.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.presentation.dto.request.NonceIssueRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.request.SocialLoginRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.response.NonceResponse;
import kr.omong.dulpick.domain.member.presentation.dto.response.MemberResponse;
import kr.omong.dulpick.domain.member.presentation.dto.response.MemberSocialAccountResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSchemaDocumentationTest {

    @Test
    void documentsAllSupportedSocialProviders() {
        Schema nonceProvider = schemaOf(NonceIssueRequest.class, "provider");
        Schema loginProvider = schemaOf(SocialLoginRequest.class, "provider");
        Schema memberProvider = schemaOf(MemberSocialAccountResponse.class, "provider");

        assertProviderSchema(nonceProvider);
        assertProviderSchema(loginProvider);
        assertProviderSchema(memberProvider);
    }

    @Test
    void documentsAllMemberStatuses() {
        Schema memberStatus = schemaOf(MemberResponse.class, "status");

        assertThat(memberStatus.allowableValues())
                .containsExactly("ACTIVE", "WITHDRAWN");
        assertThat(memberStatus.description())
                .contains("ACTIVE는 활성 회원", "WITHDRAWN은 탈퇴 회원");
    }

    @Test
    void documentsNonceLifetimeAndKoreaTimeZone() {
        Schema expiresAt = schemaOf(NonceResponse.class, "expiresAt");

        assertThat(expiresAt.description())
                .contains("10분", "UTC+9", "Asia/Seoul");
    }

    private void assertProviderSchema(Schema schema) {
        assertThat(schema.allowableValues())
                .containsExactly("KAKAO", "GOOGLE", "APPLE");
        assertThat(schema.description())
                .contains("KAKAO는 카카오", "GOOGLE은 구글", "APPLE은 애플");
    }

    private Schema schemaOf(Class<?> recordType, String componentName) {
        for (RecordComponent component : recordType.getRecordComponents()) {
            if (component.getName().equals(componentName)) {
                return component.getAccessor().getAnnotation(Schema.class);
            }
        }
        throw new IllegalArgumentException("Record component not found: " + componentName);
    }
}

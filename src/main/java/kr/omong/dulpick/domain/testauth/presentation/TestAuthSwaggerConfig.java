package kr.omong.dulpick.domain.testauth.presentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import kr.omong.dulpick.domain.testauth.security.TestAuthAccessKeyFilter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnProperty(
        prefix = "features.test-auth",
        name = "enabled",
        havingValue = "true"
)
public class TestAuthSwaggerConfig {

    public static final String SECURITY_SCHEME = "testAuthKey";

    @Bean
    public OpenApiCustomizer testAuthOpenApiCustomizer() {
        return openApi -> {
            addSecurityScheme(openApi);
            addTag(openApi);
            configureLogoutSecurity(openApi);
        };
    }

    private void addSecurityScheme(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        SecurityScheme accessKey = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(TestAuthAccessKeyFilter.HEADER_NAME)
                .description(
                        "인증2 API 접근 키입니다. 로컬 기본값은 "
                                + "dulpick-local-test-auth-access-key이며, "
                                + "운영 활성화 시 TEST_AUTH_ACCESS_KEY를 사용합니다."
                );
        components.addSecuritySchemes(SECURITY_SCHEME, accessKey);
    }

    private void addTag(OpenAPI openApi) {
        List<Tag> tags = openApi.getTags() == null
                ? new ArrayList<>()
                : new ArrayList<>(openApi.getTags());
        boolean alreadyAdded = tags.stream()
                .anyMatch(tag -> "인증2".equals(tag.getName()));
        if (!alreadyAdded) {
            tags.add(tagInsertionIndex(tags), new Tag()
                    .name("인증2")
                    .description("""
                            Swagger 및 개발 검증에 사용하는 자체 인증 기능입니다.
                            먼저 Authorize에서 testAuthKey를 등록하고 회원가입 또는 로그인을 수행합니다.
                            응답의 Access Token을 bearerAuth에 등록하면 회원 인증이 필요한 다른 API를 호출할 수 있습니다.
                            """));
        }
        openApi.setTags(tags);
    }

    private int tagInsertionIndex(List<Tag> tags) {
        for (int index = 0; index < tags.size(); index++) {
            if ("인증".equals(tags.get(index).getName())) {
                return index + 1;
            }
        }
        return tags.size();
    }

    private void configureLogoutSecurity(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        PathItem logoutPath = openApi.getPaths()
                .get(TestAuthController.BASE_PATH + "/logout");
        if (logoutPath == null || logoutPath.getPost() == null) {
            return;
        }
        SecurityRequirement requirements = new SecurityRequirement()
                .addList(SECURITY_SCHEME)
                .addList("bearerAuth");
        logoutPath.getPost().setSecurity(List.of(requirements));
    }
}

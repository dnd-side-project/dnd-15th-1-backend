package kr.omong.dulpick.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    @Test
    void placesNonceEndpointFirst() {
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/health", new PathItem())
                .addPathItem("/api/v1/members/me", new PathItem())
                .addPathItem("/api/v1/auth/nonce", new PathItem()));

        swaggerConfig.nonceFirstCustomizer().customise(openApi);

        assertThat(openApi.getPaths().keySet())
                .startsWith("/api/v1/auth/nonce");
    }

    @Test
    void providesBearerAuthenticationForProtectedEndpoints() {
        OpenAPI openApi = swaggerConfig.dulpickOpenApi();

        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey("bearerAuth");
        assertThat(openApi.getTags())
                .extracting("name")
                .containsExactly(
                        SwaggerTagNames.AUTH,
                        SwaggerTagNames.MEMBER,
                        SwaggerTagNames.COUPLE_CONNECTION,
                        SwaggerTagNames.FEEDBACK,
                        SwaggerTagNames.NOTIFICATION,
                        SwaggerTagNames.SERVER
                );
    }
}

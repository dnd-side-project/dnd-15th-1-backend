package kr.omong.dulpick.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String NONCE_PATH = "/api/v1/auth/nonce";

    @Bean
    public OpenAPI dulpickOpenApi() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth))
                .tags(List.of(
                        new Tag().name(SwaggerTagNames.AUTH),
                        new Tag().name(SwaggerTagNames.MEMBER),
                        new Tag().name(SwaggerTagNames.COUPLE_CONNECTION),
                        new Tag().name(SwaggerTagNames.SERVER)
                ));
    }

    @Bean
    public OpenApiCustomizer nonceFirstCustomizer() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null || !paths.containsKey(NONCE_PATH)) {
                return;
            }
            Paths orderedPaths = new Paths();
            orderedPaths.addPathItem(NONCE_PATH, paths.get(NONCE_PATH));
            paths.forEach((path, item) -> {
                if (!NONCE_PATH.equals(path)) {
                    orderedPaths.addPathItem(path, item);
                }
            });
            openApi.setPaths(orderedPaths);
        };
    }
}

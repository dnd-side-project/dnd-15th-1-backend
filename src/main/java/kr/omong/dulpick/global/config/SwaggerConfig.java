package kr.omong.dulpick.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;

@Configuration
public class SwaggerConfig {

    private static final String NONCE_PATH = "/api/v1/auth/nonce";
    private static final String ERROR_RESPONSE_SCHEMA = "#/components/schemas/ErrorResponse";
    private static final String WILDCARD_MEDIA_TYPE = "*/*";
    private static final String JSON_MEDIA_TYPE = "application/json";

    @Bean
    public OpenAPI dulpickOpenApi() {
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
        SecurityScheme basicAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic");
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth)
                        .addSecuritySchemes("basicAuth", basicAuth))
                .tags(List.of(
                        new Tag().name(SwaggerTagNames.AUTH),
                        new Tag().name(SwaggerTagNames.MEMBER),
                        new Tag().name(SwaggerTagNames.COUPLE_CONNECTION),
                        new Tag().name(SwaggerTagNames.NOTIFICATION),
                        new Tag().name(SwaggerTagNames.PLACE),
                        new Tag().name(SwaggerTagNames.OPS),
                        new Tag().name(SwaggerTagNames.DATE),
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

    @Bean
    public OpenApiCustomizer errorResponseMediaTypeCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .flatMap(operation -> operation.getResponses().values().stream())
                    .forEach(apiResponse -> {
                        if (apiResponse.getContent() == null) {
                            return;
                        }
                        io.swagger.v3.oas.models.media.MediaType wildcardContent =
                                apiResponse.getContent().get(WILDCARD_MEDIA_TYPE);
                        if (wildcardContent == null
                                || wildcardContent.getSchema() == null
                                || !ERROR_RESPONSE_SCHEMA.equals(wildcardContent.getSchema().get$ref())) {
                            return;
                        }
                        apiResponse.getContent().remove(WILDCARD_MEDIA_TYPE);
                        apiResponse.getContent().addMediaType(JSON_MEDIA_TYPE, wildcardContent);
                    });
        };
    }

    @Bean
    public OpenApiCustomizer schemaExampleCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values()
                        .forEach(schema -> addPropertyExamples(schema, null));
            }
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .flatMap(operation -> operation.getParameters() == null
                            ? java.util.stream.Stream.empty()
                            : operation.getParameters().stream())
                    .forEach(parameter -> {
                        if (parameter.getSchema() == null) {
                            return;
                        }
                        Object example = exampleFor(parameter.getName(), parameter.getSchema());
                        if (example == null) {
                            return;
                        }
                        if (isMissingExample(parameter.getSchema().getExample())) {
                            parameter.getSchema().setExample(example);
                        }
                        if (isMissingExample(parameter.getExample())) {
                            parameter.setExample(example);
                        }
                    });
        };
    }

    private void addPropertyExamples(Schema<?> schema, String propertyName) {
        if (schema == null) {
            return;
        }
        if (isMissingExample(schema.getExample())) {
            Object example = exampleFor(propertyName, schema);
            if (example != null) {
                schema.setExample(example);
            }
        }
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((name, property) ->
                    addPropertyExamples((Schema<?>) property, name));
        }
        if (schema.getItems() != null) {
            addPropertyExamples(schema.getItems(), propertyName);
        }
    }

    private boolean isMissingExample(Object example) {
        return example == null || (example instanceof String value && value.isBlank());
    }

    private Object exampleFor(String propertyName, Schema<?> schema) {
        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return schema.getEnum().getFirst();
        }
        if (propertyName == null) {
            return null;
        }

        String normalizedName = propertyName.toLowerCase(Locale.ROOT);
        String format = schema.getFormat();
        if ("date-time".equals(format)) {
            return "2026-08-16T14:30:00";
        }
        if ("date".equals(format)) {
            return "2026-08-16";
        }
        if ("uuid".equals(format)) {
            return "3fa85f64-5717-4562-b3fc-2c963f66afa6";
        }
        if ("boolean".equals(schema.getType())) {
            return "read".equals(normalizedName) ? false : true;
        }
        if ("integer".equals(schema.getType()) || "number".equals(schema.getType())) {
            return numericExample(normalizedName);
        }
        if ("array".equals(schema.getType()) || schema.getItems() != null) {
            return List.of();
        }
        if (!"string".equals(schema.getType())) {
            return null;
        }
        return stringExample(normalizedName);
    }

    private Object numericExample(String normalizedName) {
        if (normalizedName.equals("page")) {
            return 0;
        }
        if (normalizedName.equals("size")) {
            return 20;
        }
        if (normalizedName.equals("profileicon")) {
            return 1;
        }
        if (normalizedName.equals("daystogether")) {
            return 1;
        }
        if (normalizedName.contains("latitude")) {
            return 37.5446;
        }
        if (normalizedName.contains("longitude")) {
            return 127.0557;
        }
        if (normalizedName.contains("distance")) {
            return 4025;
        }
        if (normalizedName.contains("duration")) {
            return 3914;
        }
        if (normalizedName.contains("count") || normalizedName.contains("elements")) {
            return 10;
        }
        if (normalizedName.contains("pages")) {
            return 1;
        }
        if (normalizedName.contains("expires") || normalizedName.contains("retryafter")) {
            return 900;
        }
        return 101;
    }

    private String stringExample(String normalizedName) {
        if (normalizedName.contains("email")) {
            return "member@example.com";
        }
        if (normalizedName.contains("password")) {
            return "test-password-1234";
        }
        if (normalizedName.contains("idtoken")) {
            return "eyJraWQiOiJexample";
        }
        if (normalizedName.contains("accesstoken")) {
            return "eyJhbGciOiJIUzI1NiJ9.example.access";
        }
        if (normalizedName.contains("refreshtoken")) {
            return "eyJhbGciOiJIUzI1NiJ9.example.refresh";
        }
        if (normalizedName.contains("providerregistration")) {
            return "fcm-registration-token-example";
        }
        if (normalizedName.contains("nonce")) {
            return "l7JcLxgJx7c0nS0wqgWQeQ";
        }
        if (normalizedName.equals("imagekey")
                || normalizedName.equals("imagekeys")
                || normalizedName.equals("storagekey")) {
            return "550e8400-e29b-41d4-a716-446655440000";
        }
        if (normalizedName.contains("url")) {
            return normalizedName.contains("share")
                    ? "https://dulpick.omong.kr/connect?code=ABCDE"
                    : "https://www.instagram.com/reel/example/";
        }
        if (normalizedName.contains("connectioncode") || normalizedName.equals("code")) {
            return normalizedName.contains("connectioncode") ? "ABCDE" : "INVALID_INPUT";
        }
        if (normalizedName.equals("tokentype")) {
            return "Bearer";
        }
        if (normalizedName.equals("kakaoplaceid")) {
            return "18699959";
        }
        if (normalizedName.equals("nickname")) {
            return "둘픽이";
        }
        if (normalizedName.equals("profileicon")) {
            return "1";
        }
        if (normalizedName.contains("address")) {
            return "서울특별시 성동구 연무장길 10";
        }
        if (normalizedName.contains("query")) {
            return "성수동 카페";
        }
        if (normalizedName.contains("category")) {
            return normalizedName.contains("categoryname") ? "맛집" : "음식점 > 한식 > 육류,고기";
        }
        if (normalizedName.equals("name") || normalizedName.contains("extractedname")) {
            return "서울숲 카페";
        }
        if (normalizedName.contains("alias")) {
            return "데이트 카페";
        }
        if (normalizedName.contains("title")) {
            return "서울 데이트 추천 코스";
        }
        if (normalizedName.contains("caption") || normalizedName.equals("content")) {
            return "분위기 좋은 데이트 장소를 소개합니다.";
        }
        if (normalizedName.contains("evidence")) {
            return "분위기 좋은 데이트 장소로 소개되었습니다.";
        }
        if (normalizedName.contains("mentiontype")) {
            return "장소 소개";
        }
        if (normalizedName.equals("body") || normalizedName.equals("message")) {
            return "입력값이 올바르지 않습니다";
        }
        if (normalizedName.contains("displayname")) {
            return "둘픽이";
        }
        if (normalizedName.contains("username")) {
            return "dulpick_user";
        }
        if (normalizedName.contains("referenceid")) {
            return "101";
        }
        if (normalizedName.contains("reason")) {
            return "INVALID_INPUT";
        }
        if (normalizedName.contains("consentversion")) {
            return "2026-08-01";
        }
        if (normalizedName.contains("cursor")) {
            return "next-cursor-token";
        }
        return null;
    }
}

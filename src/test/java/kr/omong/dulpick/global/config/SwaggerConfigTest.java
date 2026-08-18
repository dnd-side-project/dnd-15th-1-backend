package kr.omong.dulpick.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                        SwaggerTagNames.SEARCH,
                        SwaggerTagNames.PLACE,
                        SwaggerTagNames.OPS,
                        SwaggerTagNames.DATE,
                        SwaggerTagNames.SERVER
                );
    }

    @Test
    void fillsMissingSchemaAndParameterExamples() {
        Schema<?> sampleSchema = new Schema<>().type("object")
                .addProperties("feedbackId", new IntegerSchema())
                .addProperties("nickname", new StringSchema());
        Operation operation = new Operation()
                .responses(new ApiResponses())
                .addParametersItem(new Parameter()
                        .name("contentId")
                        .schema(new IntegerSchema()));
        OpenAPI openApi = new OpenAPI()
                .components(new Components().addSchemas("Sample", sampleSchema))
                .paths(new Paths().addPathItem("/sample", new PathItem().get(operation)));

        swaggerConfig.schemaExampleCustomizer().customise(openApi);

        assertThat(((Schema<?>) sampleSchema.getProperties().get("feedbackId")).getExample())
                .isEqualTo(101);
        assertThat(((Schema<?>) sampleSchema.getProperties().get("nickname")).getExample())
                .isEqualTo("둘픽이");
        assertThat(operation.getParameters().getFirst().getExample())
                .isEqualTo(101);
        assertThat(operation.getParameters().getFirst().getSchema().getExample())
                .isEqualTo(101);
    }

    @Test
    void fillsMissingEnumSchemaExamples() {
        Schema<?> enumSchema = new StringSchema()
                ._enum(List.of("UNCLASSIFIED", "CLASSIFIED"));
        OpenAPI openApi = new OpenAPI()
                .components(new Components().addSchemas("PlaceClassificationStatus", enumSchema));

        swaggerConfig.schemaExampleCustomizer().customise(openApi);

        assertThat(enumSchema.getExample()).isEqualTo("UNCLASSIFIED");
    }

    @Test
    void changesErrorResponseWildcardMediaTypeToJson() {
        ApiResponse errorResponse = new ApiResponse()
                .content(new Content().addMediaType("*/*", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
        Operation operation = new Operation()
                .responses(new ApiResponses().addApiResponse("401", errorResponse));
        OpenAPI openApi = new OpenAPI()
                .paths(new Paths().addPathItem("/sample", new PathItem().get(operation)));

        swaggerConfig.errorResponseMediaTypeCustomizer().customise(openApi);

        assertThat(errorResponse.getContent())
                .containsKey("application/json")
                .doesNotContainKey("*/*");
    }
}

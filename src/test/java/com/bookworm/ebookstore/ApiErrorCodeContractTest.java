package com.bookworm.ebookstore;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bookworm.ebookstore.exception.ApiErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorCodeContractTest {

    private static OpenAPI openAPI;

    @BeforeAll
    static void setUp() throws Exception {
        try (InputStream in = ApiErrorCodeContractTest.class.getResourceAsStream("/openapi.yaml")) {
            assertThat(in).as("openapi.yaml must exist on the classpath").isNotNull();
            String yamlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlContent, null, null);
            openAPI = parseResult.getOpenAPI();
        }
    }

    @Test
    @DisplayName("AC-4: Problem.code enum in openapi.yaml exactly equals Java ApiErrorCode enum")
    void apiErrorCodeMatchesOpenApiProblemSchema() {
        Schema<?> problemSchema = openAPI.getComponents().getSchemas().get("Problem");
        assertThat(problemSchema).as("Problem schema must exist in openapi.yaml").isNotNull();

        Schema<?> codeProp = (Schema<?>) problemSchema.getProperties().get("code");
        assertThat(codeProp).as("Problem.code property must exist").isNotNull();

        List<?> enumValues = codeProp.getEnum();
        assertThat(enumValues).as("Problem.code must be an enum").isNotNull();

        Set<String> openApiCodes = enumValues.stream().map(Object::toString).collect(Collectors.toSet());
        Set<String> javaCodes = Arrays.stream(ApiErrorCode.values()).map(Enum::name).collect(Collectors.toSet());

        assertThat(javaCodes)
                .as("Java ApiErrorCode enum constants must match openapi.yaml Problem.code enum values")
                .isEqualTo(openApiCodes);
    }
}

package com.bookworm.ebookstore;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

    private static OpenAPI openAPI;
    private static SwaggerParseResult parseResult;

    @BeforeAll
    static void setUp() throws Exception {
        try (InputStream in = OpenApiContractTest.class.getResourceAsStream("/openapi.yaml")) {
            assertThat(in).as("openapi.yaml must exist on the classpath").isNotNull();
            String yamlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            parseResult = new OpenAPIV3Parser().readContents(yamlContent, null, null);
            openAPI = parseResult.getOpenAPI();
        }
    }

    @Test
    @DisplayName("AC-1: openapi.yaml is valid OpenAPI 3.0.3 with zero errors and zero warnings")
    void openApiSpecIsValid() {
        assertThat(parseResult.getMessages())
                .as("OpenAPI spec parsing messages (errors and warnings)")
                .isEmpty();
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getOpenapi()).isEqualTo("3.0.3");
    }

    @Test
    @DisplayName("AC-2: Exact 25 operations, unique operationIds, exactly one tag per operation, explicit security")
    void operationsValidation() {
        Set<String> allowedTags = Set.of("Auth", "Account", "Catalogue", "Cart", "Orders", "Payments");
        List<Operation> operations = new ArrayList<>();
        Set<String> operationIds = new HashSet<>();

        openAPI.getPaths().forEach((path, pathItem) -> {
            assertThat(path).startsWith("/api/v1");
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
                Operation op = entry.getValue();
                operations.add(op);

                // Unique operationId
                assertThat(op.getOperationId())
                        .as("OperationId must be defined and non-empty")
                        .isNotBlank();
                boolean isUnique = operationIds.add(op.getOperationId());
                assertThat(isUnique)
                        .as("OperationId %s must be unique", op.getOperationId())
                        .isTrue();

                // Exactly one tag from allowed tags
                assertThat(op.getTags())
                        .as("Operation %s must have exactly 1 tag", op.getOperationId())
                        .hasSize(1);
                assertThat(allowedTags)
                        .as("Tag %s on operation %s must be valid", op.getTags().get(0), op.getOperationId())
                        .contains(op.getTags().get(0));

                // Explicit security: either bearerAuth or empty security: []
                List<SecurityRequirement> security = op.getSecurity();
                assertThat(security)
                        .as("Operation %s must have explicit security setting", op.getOperationId())
                        .isNotNull();
                if (security.isEmpty()) {
                    // Public operation
                } else {
                    assertThat(security)
                            .as("Bearer auth operation %s must specify bearerAuth", op.getOperationId())
                            .anyMatch(sec -> sec.containsKey("bearerAuth"));
                }
            }
        });

        assertThat(operations).hasSize(25);
    }

    @Test
    @DisplayName("AC-6: Paged operations (#8, #11, #13, #21) have page/size parameters and *Page schema")
    void pagedOperationsAndSchemas() {
        Map<String, String> pagedEndpoints = Map.of(
                "/api/v1/books", "BookPage",
                "/api/v1/authors", "AuthorPage",
                "/api/v1/publishers", "PublisherPage",
                "/api/v1/orders", "OrderPage"
        );

        pagedEndpoints.forEach((path, pageSchemaName) -> {
            PathItem pathItem = openAPI.getPaths().get(path);
            assertThat(pathItem).as("PathItem for %s", path).isNotNull();
            Operation getOp = pathItem.getGet();
            assertThat(getOp).as("GET operation for %s", path).isNotNull();

            // Check page & size parameters (resolving $ref if needed)
            List<Parameter> params = getOp.getParameters();
            assertThat(params).isNotNull();

            boolean hasPage = params.stream().anyMatch(p -> {
                String name = p.getName();
                String in = p.getIn();
                if (p.get$ref() != null && p.get$ref().contains("PageQuery")) {
                    return true;
                }
                return "page".equals(name) && "query".equals(in);
            });
            boolean hasSize = params.stream().anyMatch(p -> {
                String name = p.getName();
                String in = p.getIn();
                if (p.get$ref() != null && p.get$ref().contains("SizeQuery")) {
                    return true;
                }
                return "size".equals(name) && "query".equals(in);
            });
            assertThat(hasPage).as("%s must have 'page' query parameter", path).isTrue();
            assertThat(hasSize).as("%s must have 'size' query parameter", path).isTrue();

            // Check response schema
            var response200 = getOp.getResponses().get("200");
            assertThat(response200).isNotNull();
            var content = response200.getContent().get("application/json");
            assertThat(content).isNotNull();
            assertThat(content.getSchema().get$ref()).contains(pageSchemaName);
        });

        // Check page schema properties
        List.of("BookPage", "AuthorPage", "PublisherPage", "OrderPage").forEach(schemaName -> {
            Schema<?> schema = openAPI.getComponents().getSchemas().get(schemaName);
            assertThat(schema).as("Schema %s", schemaName).isNotNull();
            Map<String, Schema> props = schema.getProperties();
            assertThat(props).containsKeys("content", "page", "size", "totalElements", "totalPages");
        });
    }

    @Test
    @DisplayName("AC-8: No response schema contains sensitive fields and request payment fields are writeOnly")
    void sensitiveFieldsValidation() {
        Set<String> sensitiveFields = Set.of("cardNumber", "cvv", "expiry", "walletMobileNumber");
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();

        // 1. Check all schemas used in responses (or generally non-request schemas)
        Set<String> requestSchemas = Set.of(
                "RegisterRequest", "LoginRequest", "AddressRequest",
                "AddCartItemRequest", "UpdateCartItemRequest", "CreateOrderRequest", "PaymentRequest"
        );

        schemas.forEach((name, schema) -> {
            if (!requestSchemas.contains(name)) {
                checkNoSensitiveProperties(name, schema, sensitiveFields);
            }
        });

        // 2. Check PaymentRequest and RegisterRequest writeOnly properties
        Schema<?> paymentRequest = schemas.get("PaymentRequest");
        assertThat(paymentRequest).isNotNull();
        for (String field : sensitiveFields) {
            Schema prop = (Schema) paymentRequest.getProperties().get(field);
            assertThat(prop).as("PaymentRequest field %s", field).isNotNull();
            assertThat(prop.getWriteOnly()).as("PaymentRequest field %s must be writeOnly", field).isTrue();
        }

        Schema<?> registerRequest = schemas.get("RegisterRequest");
        assertThat(registerRequest).isNotNull();
        Schema passwordProp = (Schema) registerRequest.getProperties().get("password");
        assertThat(passwordProp.getWriteOnly()).as("RegisterRequest password must be writeOnly").isTrue();

        Schema<?> loginRequest = schemas.get("LoginRequest");
        assertThat(loginRequest).isNotNull();
        Schema loginPasswordProp = (Schema) loginRequest.getProperties().get("password");
        assertThat(loginPasswordProp.getWriteOnly()).as("LoginRequest password must be writeOnly").isTrue();
    }

    private void checkNoSensitiveProperties(String schemaName, Schema<?> schema, Set<String> sensitiveFields) {
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((propName, propSchema) -> {
                assertThat(sensitiveFields)
                        .as("Schema %s property %s must not be sensitive", schemaName, propName)
                        .doesNotContain(propName);
                if (propSchema instanceof Schema<?> nestedSchema) {
                    checkNoSensitiveProperties(schemaName + "." + propName, nestedSchema, sensitiveFields);
                }
            });
        }
        if (schema instanceof ArraySchema arraySchema && arraySchema.getItems() != null) {
            checkNoSensitiveProperties(schemaName + "[]", arraySchema.getItems(), sensitiveFields);
        }
    }
}

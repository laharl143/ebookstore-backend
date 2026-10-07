package com.bookworm.ebookstore;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
class ContractDriftTest {

    private static OpenAPI openAPI;
    private static Set<String> specEndpoints;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @BeforeAll
    static void setUp() throws Exception {
        try (InputStream in = ContractDriftTest.class.getResourceAsStream("/openapi.yaml")) {
            assertThat(in).as("openapi.yaml must exist on the classpath").isNotNull();
            String yamlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlContent, null, null);
            openAPI = parseResult.getOpenAPI();
            assertThat(openAPI).isNotNull();

            specEndpoints = new HashSet<>();
            openAPI.getPaths().forEach((path, pathItem) -> {
                String normalizedPath = normalizePath(path);
                for (PathItem.HttpMethod httpMethod : pathItem.readOperationsMap().keySet()) {
                    specEndpoints.add(httpMethod.name().toUpperCase() + " " + normalizedPath);
                }
            });
        }
    }

    @Test
    @DisplayName("AC-5: Every Spring MVC handler under /api/v1 has a matching operation in openapi.yaml")
    void springMvcHandlersMatchOpenApiSpec() {
        Map<RequestMappingInfo, ?> handlerMethods = handlerMapping.getHandlerMethods();

        for (RequestMappingInfo info : handlerMethods.keySet()) {
            Set<String> patterns = info.getDirectPaths();
            if (patterns.isEmpty() && info.getPathPatternsCondition() != null) {
                patterns = info.getPathPatternsCondition().getPatternValues();
            }

            for (String pattern : patterns) {
                if (pattern.startsWith("/api/v1")) {
                    Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                    if (methods.isEmpty()) {
                        fail("Handler pattern '%s' does not specify an HTTP method constraint", pattern);
                    }

                    String normalizedPattern = normalizePath(pattern);
                    for (RequestMethod method : methods) {
                        String key = method.name() + " " + normalizedPattern;
                        assertThat(specEndpoints)
                                .as("Spring MVC handler endpoint '%s' must be documented in openapi.yaml", key)
                                .contains(key);
                    }
                }
            }
        }
    }

    /**
     * Normalizes path variable names like /api/v1/books/{bookId} to /api/v1/books/{}.
     */
    private static String normalizePath(String path) {
        return path.replaceAll("\\{[^}]+}", "{}");
    }
}

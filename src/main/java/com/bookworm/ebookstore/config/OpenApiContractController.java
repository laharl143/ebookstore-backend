package com.bookworm.ebookstore.config;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the hand-written openapi.yaml contract at /openapi.yaml.
 */
@RestController
public class OpenApiContractController {

    private final byte[] openApiYamlBytes;

    public OpenApiContractController() throws IOException {
        ClassPathResource resource = new ClassPathResource("openapi.yaml");
        this.openApiYamlBytes = resource.getInputStream().readAllBytes();
    }

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml;charset=UTF-8")
    public ResponseEntity<byte[]> getOpenApiYaml() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml;charset=UTF-8"))
                .body(openApiYamlBytes);
    }
}

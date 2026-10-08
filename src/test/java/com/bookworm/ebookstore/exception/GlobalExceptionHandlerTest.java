package com.bookworm.ebookstore.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandlerTest.StubTestController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    public static class StubPayload {
        @NotBlank(message = "Name cannot be blank")
        private String name;

        public StubPayload() {}

        public StubPayload(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @RestController
    @RequestMapping("/test")
    @Validated
    public static class StubTestController {

        @PostMapping("/body")
        public String postBody(@Valid @RequestBody StubPayload body) {
            return "ok";
        }

        @GetMapping("/paged")
        public String getPaged(@RequestParam @Min(1) @Max(50) Integer size) {
            return "ok";
        }

        @GetMapping("/not-found")
        public String notFound() {
            throw new ResourceNotFoundException(ApiErrorCode.BOOK_NOT_FOUND, "Book not found with ID 999");
        }

        @GetMapping("/server-error")
        public String serverError() {
            throw new RuntimeException("Unexpected database failure");
        }
    }

    @Test
    @DisplayName("AC-4: Body validation error produces 400 VALIDATION_FAILED with field errors and application/problem+json")
    void bodyValidationError() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.title", is("Validation failed")))
                .andExpect(jsonPath("$.instance", is("/test/body")))
                .andExpect(jsonPath("$.errors.name", notNullValue()));
    }

    @Test
    @DisplayName("AC-4: Query parameter range validation error produces 400 VALIDATION_FAILED keyed by parameter")
    void parameterRangeValidationError() throws Exception {
        mockMvc.perform(get("/test/paged").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.errors.size", notNullValue()));
    }

    @Test
    @DisplayName("AC-4: Query parameter type mismatch (size=abc) produces 400 MALFORMED_REQUEST")
    void parameterTypeMismatchError() throws Exception {
        mockMvc.perform(get("/test/paged").param("size", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")))
                .andExpect(jsonPath("$.title", is("Malformed request")));
    }

    @Test
    @DisplayName("AC-4: ResourceNotFoundException produces mapped 404 code (BOOK_NOT_FOUND)")
    void resourceNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("BOOK_NOT_FOUND")))
                .andExpect(jsonPath("$.detail", is("Book not found with ID 999")));
    }

    @Test
    @DisplayName("AC-4: Client with Accept: application/json receives Problem body with application/problem+json without 406")
    void acceptHeaderJsonResponse() throws Exception {
        mockMvc.perform(get("/test/paged").param("size", "0").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("AC-4: Unhandled exception returns 500 INTERNAL_ERROR with request URI instance")
    void unhandledExceptionReturns500() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
                .andExpect(jsonPath("$.title", is("Internal server error")))
                .andExpect(jsonPath("$.instance", is("/test/server-error")));
    }
}

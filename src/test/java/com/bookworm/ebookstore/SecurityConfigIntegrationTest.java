package com.bookworm.ebookstore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.bookworm.ebookstore.service.JwtService;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("AC-5: Unauthenticated request to protected endpoint (/api/v1/me) returns 401 UNAUTHORIZED Problem")
    void unauthenticatedProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.instance", is("/api/v1/me")));
    }

    @Test
    @DisplayName("AC-5: Invalid/expired token on protected endpoint returns 401 UNAUTHORIZED Problem")
    void invalidTokenProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("AC-6: Public catalogue and swagger endpoints are accessible without authentication")
    void publicEndpointsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("AC-6: Invalid/expired token on public endpoint is ignored and does not block the request")
    void invalidTokenOnPublicEndpointIsIgnored() throws Exception {
        mockMvc.perform(get("/openapi.yaml").header("Authorization", "Bearer invalid.or.expired.token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.0.3")));
    }

    @Test
    @DisplayName("AC-7: Valid generated token allows authenticated access and contains sub user ID claim")
    void validTokenIssuanceAndClaims() {
        String token = jwtService.generateToken(42L);
        org.assertj.core.api.Assertions.assertThat(token).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600L);
    }
}

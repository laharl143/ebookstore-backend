package com.bookworm.ebookstore.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SecurityBeansConfig {

    @Value("${app.jwt.secret:bookworm-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}

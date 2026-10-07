package com.bookworm.ebookstore.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ProblemAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(ProblemAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<RequestMatcher> publicMatchers = List.of(
                new AntPathRequestMatcher("/api/v1/auth/**", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/api/v1/categories/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/v1/books/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/v1/authors/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/v1/publishers/**", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/openapi.yaml", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/swagger-ui.html"),
                new AntPathRequestMatcher("/v3/api-docs/**")
        );

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicMatchers.toArray(new RequestMatcher[0])).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new PublicIgnoringBearerTokenResolver(publicMatchers))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> {})
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                );

        return http.build();
    }
}

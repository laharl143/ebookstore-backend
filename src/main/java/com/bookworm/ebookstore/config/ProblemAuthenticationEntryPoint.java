package com.bookworm.ebookstore.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.bookworm.ebookstore.exception.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ProblemAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json;charset=UTF-8");

        var problem = new ProblemResponse(
                "about:blank",
                ApiErrorCode.UNAUTHORIZED.getDefaultTitle(),
                HttpStatus.UNAUTHORIZED.value(),
                "Full authentication is required to access this resource",
                request.getRequestURI(),
                ApiErrorCode.UNAUTHORIZED.name()
        );

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }

    public record ProblemResponse(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code
    ) {}
}

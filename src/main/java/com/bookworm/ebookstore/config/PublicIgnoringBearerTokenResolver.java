package com.bookworm.ebookstore.config;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Custom BearerTokenResolver that skips resolving bearer tokens on public paths.
 * This prevents expired or malformed tokens from blocking public browsing operations (AC-6).
 */
public class PublicIgnoringBearerTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
    private final List<RequestMatcher> publicMatchers;

    public PublicIgnoringBearerTokenResolver(List<RequestMatcher> publicMatchers) {
        this.publicMatchers = publicMatchers;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        for (RequestMatcher matcher : publicMatchers) {
            if (matcher.matches(request)) {
                return null;
            }
        }
        return defaultResolver.resolve(request);
    }
}

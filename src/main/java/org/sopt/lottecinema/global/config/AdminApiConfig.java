package org.sopt.lottecinema.global.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.sopt.lottecinema.global.common.ErrorResponse;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@SecurityScheme(name = "adminBearer", type = SecuritySchemeType.HTTP, scheme = "bearer")
@Profile("admin-api")
public class AdminApiConfig {

    @Bean
    FilterRegistrationBean<AdminTokenFilter> adminTokenFilter(
            @Value("${ADMIN_API_TOKEN:}") String token, JsonMapper jsonMapper) {
        if (!token.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("ADMIN_API_TOKEN은 소문자 16진수 64자리여야 합니다.");
        }
        var registration = new FilterRegistrationBean<>(new AdminTokenFilter(token, jsonMapper));
        registration.addUrlPatterns("/api/v1/admin/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
        return registration;
    }

    static class AdminTokenFilter extends HttpFilter {
        private static final String BEARER_PREFIX = "Bearer ";
        private final byte[] expectedToken;
        private final JsonMapper jsonMapper;

        AdminTokenFilter(String token, JsonMapper jsonMapper) {
            this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
            this.jsonMapper = jsonMapper;
        }

        @Override
        protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (matchesToken(authorization)) {
                chain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.fail(ErrorCode.ADMIN_AUTHENTICATION_REQUIRED));
        }

        private boolean matchesToken(String authorization) {
            if (authorization == null || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                return false;
            }
            byte[] suppliedToken = authorization.substring(BEARER_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedToken, suppliedToken);
        }
    }
}

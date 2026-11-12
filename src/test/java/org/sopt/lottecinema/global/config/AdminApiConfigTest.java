package org.sopt.lottecinema.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class AdminApiConfigTest {
    private static final String TOKEN = "a".repeat(64);

    @Test
    void rejectsMissingOrWrongToken() throws Exception {
        var filter = new AdminApiConfig.AdminTokenFilter(TOKEN, JsonMapper.builder().build());
        for (String value : new String[] {null, "Bearer " + "b".repeat(64), "Basic " + TOKEN}) {
            var request = new MockHttpServletRequest();
            if (value != null) request.addHeader("Authorization", value);
            var response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(response.getContentAsString()).contains("ADMIN_AUTHENTICATION_REQUIRED");
            verifyNoInteractions(chain);
        }
    }

    @Test
    void acceptsCorrectBearerToken() throws Exception {
        var filter = new AdminApiConfig.AdminTokenFilter(TOKEN, JsonMapper.builder().build());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void refusesToStartWithInvalidToken() {
        var config = new AdminApiConfig();
        assertThatThrownBy(() -> config.adminTokenFilter("", JsonMapper.builder().build()))
                .isInstanceOf(IllegalStateException.class);
    }
}

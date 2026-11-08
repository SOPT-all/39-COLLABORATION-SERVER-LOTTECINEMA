package org.sopt.lottecinema.global.exception;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerFallbackTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void preservesSpringExceptionStatus() throws Exception {
        mvc.perform(get("/status"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void returnsInternalErrorForUnexpectedException() throws Exception {
        mvc.perform(get("/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void returnsNotAcceptableWhenJsonIsRejected() throws Exception {
        mvc.perform(get("/json").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void returnsInternalErrorForInvalidReturnValue() throws Exception {
        mvc.perform(get("/invalid-return"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @RestController
    static class TestController {
        @GetMapping("/invalid-return")
        @NotBlank
        String invalidReturn() {
            return "";
        }

        @GetMapping("/status")
        String status() {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException("internal detail");
        }

        @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
        String json() {
            return "{}";
        }
    }
}

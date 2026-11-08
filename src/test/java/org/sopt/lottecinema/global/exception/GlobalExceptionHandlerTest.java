package org.sopt.lottecinema.global.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.sopt.lottecinema.global.common.SuccessResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

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
    void returnsEveryValidationError() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void mapsMalformedBody() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"))
                .andExpect(jsonPath("$.data").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.errors").doesNotHaveJsonPath());
    }

    @Test
    void preservesAllowedMethods() throws Exception {
        mvc.perform(put("/test"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("POST")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void mapsInvalidParameterType() throws Exception {
        mvc.perform(get("/test").param("number", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
    }

    @Test
    void mapsMissingParameter() throws Exception {
        mvc.perform(get("/test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    void mapsUnsupportedContentType() throws Exception {
        mvc.perform(post("/test").contentType(MediaType.TEXT_PLAIN).content("bad"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void returnsMethodValidationDetails() throws Exception {
        mvc.perform(get("/test").param("number", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("number"));
    }

    @Test
    void omitsErrorCodeOnSuccess() throws Exception {
        mvc.perform(get("/test").param("number", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void returnsCreatedResponse() throws Exception {
        mvc.perform(post("/created"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Created"))
                .andExpect(jsonPath("$.data").value("created"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void includesNullDataOnSuccessWithoutData() throws Exception {
        mvc.perform(get("/empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").hasJsonPath())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.code").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.errors").doesNotHaveJsonPath());
    }

    @RestController
    static class TestController {
        @GetMapping("/empty")
        SuccessResponse<Void> getEmpty() {
            return SuccessResponse.success();
        }

        @PostMapping("/created")
        ResponseEntity<SuccessResponse<String>> createResource() {
            return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created("created"));
        }

        @PostMapping("/test")
        String create(@Valid @RequestBody TestRequest request) {
            return "ok";
        }

        @GetMapping("/test")
        SuccessResponse<String> get(@RequestParam("number") @Min(1) int number) {
            return SuccessResponse.success("ok");
        }
    }

    record TestRequest(@NotBlank String name, @NotBlank String email) {
    }
}


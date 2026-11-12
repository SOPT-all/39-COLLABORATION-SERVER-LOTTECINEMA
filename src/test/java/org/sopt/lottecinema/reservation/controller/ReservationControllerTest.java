package org.sopt.lottecinema.reservation.controller;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.lottecinema.global.common.ValidationError;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.global.exception.GlobalExceptionHandler;
import org.sopt.lottecinema.reservation.dto.ReservationResponse;
import org.sopt.lottecinema.reservation.dto.ReservationResult;
import org.sopt.lottecinema.reservation.dto.TicketCounts;
import org.sopt.lottecinema.reservation.service.ReservationService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerTest {

    private static final String BODY = """
            {"requestId":"f7527fb5-60db-4814-b1f2-67f7630869e9",
             "ticketCounts":{"adult":2,"teen":0,"senior":0,"disabled":0},
             "seatCodes":["A1","A2"],"expectedTotalAmount":30000}
            """;

    private final ReservationService service = mock(ReservationService.class);
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;

    /** 실제 컨트롤러와 공통 예외 처리기를 연결해 경로·본문 검증과 응답 JSON을 확인한다. */
    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ReservationController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    /** 각 테스트에서 사용한 검증기 자원을 정리한다. */
    @AfterEach
    void tearDown() {
        validator.close();
    }

    /** 신규 예매와 재시도 응답이 서로 다른 HTTP 상태와 동일한 데이터 구조를 사용하는지 확인한다. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsCreatedOrReplayedReservation(boolean created) throws Exception {
        TicketCounts counts = new TicketCounts(BigInteger.TWO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
        ReservationResponse response = new ReservationResponse(1L, 1203L, List.of("A1", "A2"), counts,
                30000, "2026-11-12T10:00:00+09:00");
        when(service.createReservation(eq(1203L), any())).thenReturn(new ReservationResult(response, created));
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(created ? 201 : 200))
                .andExpect(jsonPath("$.status").value(created ? 201 : 200))
                .andExpect(jsonPath("$.message").value(created ? "Created" : "OK"))
                .andExpect(jsonPath("$.data.reservationId").value(1))
                .andExpect(jsonPath("$.data.ticketCounts.adult").value(2))
                .andExpect(jsonPath("$.data.createdAt").value("2026-11-12T10:00:00+09:00"));
    }

    /** 정수 필드에 문자열·소수·불리언·객체를 넣으면 자동 변환 없이 400을 반환한다. */
    @ParameterizedTest
    @ValueSource(strings = {"\"2\"", "2.0", "true", "{}", "[]"})
    void rejectsNonIntegerCounts(String value) throws Exception {
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"adult\":2", "\"adult\":" + value)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
        verifyNoInteractions(service);
    }

    /** JSON 타입은 맞지만 인원이 범위를 벗어나거나 누락되면 중첩 필드 경로를 반환한다. */
    @ParameterizedTest
    @ValueSource(strings = {"-1", "9", "9999999999999999999999999", "null"})
    void preservesNestedValidationField(String value) throws Exception {
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("\"adult\":2", "\"adult\":" + value)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("ticketCounts.adult"));
        verifyNoInteractions(service);
    }

    /** UUID나 좌석·금액·객체의 JSON 형식이 잘못된 요청을 거부한다. */
    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "null", "{\"requestId\":123}",
            "{\"requestId\":\"invalid\"}", "{\"seatCodes\":\"A1\"}", "{\"seatCodes\":[1]}",
            "{\"ticketCounts\":[]}", "{\"expectedTotalAmount\":\"30000\"}"})
    void rejectsMalformedBody(String body) throws Exception {
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
        verifyNoInteractions(service);
    }

    /** 필수 필드가 빠지거나 좌석 배열 요소가 null이면 검증 상세를 반환한다. */
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"requestId\":null}", "{\"seatCodes\":[null]}"})
    void rejectsMissingFields(String body) throws Exception {
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
        verifyNoInteractions(service);
    }

    /** 경로 ID가 잘못된 숫자 또는 범위이면 서비스 호출 전에 거부한다. */
    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "0", "-1"})
    void rejectsInvalidPath(String id) throws Exception {
        mvc.perform(post("/api/v1/showtimes/{id}/reservations", id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    /** 서비스에서 확인한 좌석 관련 검증 오류도 errors 배열로 전달한다. */
    @Test
    void preservesServiceValidationErrors() throws Exception {
        when(service.createReservation(eq(1203L), any())).thenThrow(new BusinessException(ErrorCode.VALIDATION_FAILED,
                "입력값을 확인해 주세요.", List.of(new ValidationError("seatCodes", "좌석을 확인해 주세요."))));
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("seatCodes"));
    }

    /** 좌석·금액·요청 ID의 충돌 코드가 각각 HTTP 409로 매핑되는지 확인한다. */
    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"SEAT_ALREADY_RESERVED", "RESERVATION_AMOUNT_CHANGED",
            "RESERVATION_REQUEST_CONFLICT"})
    void returnsConflictCode(ErrorCode errorCode) throws Exception {
        when(service.createReservation(eq(1203L), any())).thenThrow(new BusinessException(errorCode));
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(errorCode.getCode()));
    }

    /** 지원하지 않는 입력·응답 미디어 타입에 공통 415와 본문 없는 406을 반환한다. */
    @Test
    void rejectsUnsupportedMediaTypes() throws Exception {
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.TEXT_PLAIN).content(BODY))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(post("/api/v1/showtimes/1203/reservations").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML).content(BODY))
                .andExpect(status().isNotAcceptable()).andExpect(content().string(""));
        verifyNoInteractions(service);
    }
}

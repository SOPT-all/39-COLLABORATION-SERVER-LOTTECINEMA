package org.sopt.lottecinema.showtime.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.global.exception.GlobalExceptionHandler;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.CinemaInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.MovieInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.ScreenInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.SeatInfo;
import org.sopt.lottecinema.showtime.dto.TicketPrices;
import org.sopt.lottecinema.showtime.service.ShowtimeSeatService;
import org.sopt.lottecinema.showtime.service.ShowtimeService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;


class ShowtimeControllerTest {

    private final ShowtimeSeatService showtimeSeatService = mock(ShowtimeSeatService.class);
    private final ShowtimeService showtimeService = mock(ShowtimeService.class);
    private LocalValidatorFactoryBean validator;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ShowtimeController(showtimeSeatService, showtimeService))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void returnsSeatContractWithExplicitNullPoster() throws Exception {
        when(showtimeSeatService.getShowtimeSeats(1203L)).thenReturn(new ShowtimeSeatResponse(
                "2026-11-12T14:25:00+09:00", "2026-11-12T16:14:00+09:00", "2D",
                new MovieInfo("테스트 영화", null, "ALL"), new CinemaInfo("용산"), new ScreenInfo("4관", "RECLINER"),
                2, 3, List.of(new SeatInfo("E1", 0, 0, false), new SeatInfo("E3", 0, 2, true),
                new SeatInfo("E4", 0, 3, false)), new TicketPrices(15000, 12000, 10000, 10000)));

        mvc.perform(get("/api/v1/showtimes/1203/seats").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.startAt").value("2026-11-12T14:25:00+09:00"))
                .andExpect(jsonPath("$.data.movie.posterUrl").hasJsonPath())
                .andExpect(jsonPath("$.data.movie.posterUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.availableSeatCount").value(2))
                .andExpect(jsonPath("$.data.totalSeatCount").value(3))
                .andExpect(jsonPath("$.data.seats[0].isReserved").value(false))
                .andExpect(jsonPath("$.data.seats[0].isAccessible").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.seats[0].reserved").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.data.ticketPrices.adult").value(15000));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void rejectsInvalidLongPath(String id) throws Exception {
        mvc.perform(get("/api/v1/showtimes/{id}/seats", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
        verifyNoInteractions(showtimeSeatService);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositivePathWithFieldDetails(long id) throws Exception {
        mvc.perform(get("/api/v1/showtimes/{id}/seats", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("showtimeId"));
        verifyNoInteractions(showtimeSeatService);
    }

    @Test
    void returnsNotFoundForUnknownShowtime() throws Exception {
        when(showtimeSeatService.getShowtimeSeats(1203L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));
        mvc.perform(get("/api/v1/showtimes/1203/seats")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void returnsServerErrorForMissingData() throws Exception {
        when(showtimeSeatService.getShowtimeSeats(1203L)).thenThrow(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        mvc.perform(get("/api/v1/showtimes/1203/seats")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void returnsEmptyNotAcceptableForXml() throws Exception {
        mvc.perform(get("/api/v1/showtimes/1203/seats").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable()).andExpect(content().string(""));
        verifyNoInteractions(showtimeSeatService);
    }


    // ===================== 회차 목록 조회 API (GET /api/v1/showtimes) =====================

    @Test
    @DisplayName("정상 응답 계약: 필드 구성과 잔여석 값")
    void returnsShowtimeListContract() throws Exception {
        ShowtimeListResponse.ShowtimeResponse item = new ShowtimeListResponse.ShowtimeResponse(
                10L,
                OffsetDateTime.of(2026, 11, 27, 10, 0, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 11, 27, 12, 0, 0, 0, ZoneOffset.ofHours(9)),
                "1관", "2D", "SUBTITLED", 100, 20, 80);
        when(showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27)))
                .thenReturn(new ShowtimeListResponse(List.of(item)));

        mvc.perform(get("/api/v1/showtimes").param("movieId", "1").param("cinemaId", "2")
                        .param("date", "2026-11-27").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.showtimes[0].showtimeId").value(10))
                .andExpect(jsonPath("$.data.showtimes[0].startAt").exists())
                .andExpect(jsonPath("$.data.showtimes[0].endAt").exists())
                .andExpect(jsonPath("$.data.showtimes[0].screenName").value("1관"))
                .andExpect(jsonPath("$.data.showtimes[0].formatType").value("2D"))
                .andExpect(jsonPath("$.data.showtimes[0].languageType").value("SUBTITLED"))
                .andExpect(jsonPath("$.data.showtimes[0].totalSeatCount").value(100))
                .andExpect(jsonPath("$.data.showtimes[0].reservedSeatCount").value(20))
                .andExpect(jsonPath("$.data.showtimes[0].availableSeatCount").value(80));
    }

    @Test
    @DisplayName("date 생략 → 200, 서비스에는 null 전달")
    void passesNullDateToServiceWhenOmitted() throws Exception {
        when(showtimeService.getShowtimeLists(1L, 2L, null))
                .thenReturn(new ShowtimeListResponse(List.of()));

        mvc.perform(get("/api/v1/showtimes").param("movieId", "1").param("cinemaId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showtimes").isEmpty());

        verify(showtimeService).getShowtimeLists(1L, 2L, null);
    }

    @ParameterizedTest
    @DisplayName("date 형식 오류 → 400 INVALID_PARAMETER_TYPE")
    @ValueSource(strings = {"abc", "2026-13-45", "2026-02-30", "20261127", "2026/11/27"})
    void rejectsInvalidDateFormat(String date) throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", "1").param("cinemaId", "2").param("date", date))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
        verifyNoInteractions(showtimeService);
    }

    @Test
    @DisplayName("필수 쿼리 파라미터 누락 → 400 MISSING_PARAMETER")
    void rejectsMissingMovieId() throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("cinemaId", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
        verifyNoInteractions(showtimeService);
    }

    @Test
    void rejectsMissingCinemaId() throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
        verifyNoInteractions(showtimeService);
    }

    @ParameterizedTest
    @DisplayName("숫자가 아닌 ID → 400 INVALID_PARAMETER_TYPE")
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void rejectsInvalidLongQuery(String id) throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", id).param("cinemaId", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
        verifyNoInteractions(showtimeService);
    }

    @ParameterizedTest
    @DisplayName("@Positive 위반 → 400 VALIDATION_FAILED + 필드명(movieId / cinemaId)")
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveMovieIdWithFieldDetails(long id) throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", String.valueOf(id)).param("cinemaId", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("movieId"));
        verifyNoInteractions(showtimeService);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveCinemaIdWithFieldDetails(long id) throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", "1").param("cinemaId", String.valueOf(id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("cinemaId"));
        verifyNoInteractions(showtimeService);
    }

    @Test
    @DisplayName("영화/영화관 없음 → 404 + 서비스가 준 상황별 메시지 유지")
    void returnsNotFoundWithMessageForUnknownMovie() throws Exception {
        when(showtimeService.getShowtimeLists(eq(999L), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "해당 영화가 존재하지 않습니다."));

        mvc.perform(get("/api/v1/showtimes").param("movieId", "999").param("cinemaId", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 영화가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("목록 API도 XML 요청은 본문 없는 406")
    void returnsEmptyNotAcceptableForXmlOnList() throws Exception {
        mvc.perform(get("/api/v1/showtimes").param("movieId", "1").param("cinemaId", "2")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable()).andExpect(content().string(""));
        verifyNoInteractions(showtimeService);
    }
}

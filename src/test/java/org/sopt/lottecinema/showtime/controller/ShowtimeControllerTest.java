package org.sopt.lottecinema.showtime.controller;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.global.exception.GlobalExceptionHandler;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.CinemaInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.MovieInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.ScreenInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.SeatInfo;
import org.sopt.lottecinema.showtime.dto.TicketPrices;
import org.sopt.lottecinema.showtime.service.ShowtimeSeatService;
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

class ShowtimeControllerTest {

    private final ShowtimeSeatService service = mock(ShowtimeSeatService.class);
    private LocalValidatorFactoryBean validator;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ShowtimeController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void closeValidator() {
        validator.close();
    }

    @Test
    void returnsSeatContractWithExplicitNullPoster() throws Exception {
        when(service.getShowtimeSeats(1203L)).thenReturn(new ShowtimeSeatResponse(
                "2026-11-12T14:25:00+09:00", "2026-11-12T16:14:00+09:00", "2D",
                new MovieInfo("테스트 영화", null, "ALL"), new CinemaInfo("용산"), new ScreenInfo("4관", "RECLINER"),
                2, 3, List.of(new SeatInfo("E1", 0, 0, false), new SeatInfo("E3", 0, 2, true),
                new SeatInfo("E4", 0, 3, false)), new TicketPrices(15000, 12000, 10000, 10000)));

        mvc.perform(get("/api/showtimes/1203/seats").accept(MediaType.APPLICATION_JSON))
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
        mvc.perform(get("/api/showtimes/{id}/seats", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositivePathWithFieldDetails(long id) throws Exception {
        mvc.perform(get("/api/showtimes/{id}/seats", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("showtimeId"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForUnknownShowtime() throws Exception {
        when(service.getShowtimeSeats(1203L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));
        mvc.perform(get("/api/showtimes/1203/seats")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void returnsServerErrorForMissingData() throws Exception {
        when(service.getShowtimeSeats(1203L)).thenThrow(new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        mvc.perform(get("/api/showtimes/1203/seats")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void returnsEmptyNotAcceptableForXml() throws Exception {
        mvc.perform(get("/api/showtimes/1203/seats").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable()).andExpect(content().string(""));
        verifyNoInteractions(service);
    }
}

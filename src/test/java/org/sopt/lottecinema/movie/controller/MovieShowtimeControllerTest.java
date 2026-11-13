package org.sopt.lottecinema.movie.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.global.exception.GlobalExceptionHandler;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse.CinemaResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse.RegionResponse;
import org.sopt.lottecinema.movie.service.MovieShowtimeService;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MovieShowtimeControllerTest {

    private static final String PATH = "/api/v1/movies/{movieId}/cinemas";
    private final MovieShowtimeService service = mock(MovieShowtimeService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MovieShowtimeController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void returnsCinemaAvailabilityAndEmptyCinemaArray() throws Exception {
        when(service.getCinemaByMovieId(7L)).thenReturn(new MovieCinemaResponse(List.of(
                new RegionResponse(1L, "서울", List.of(new CinemaResponse(11L, "용산", true),
                        new CinemaResponse(12L, "건대입구", false))),
                new RegionResponse(2L, "경기", List.of()))));

        mvc.perform(get(PATH, 7)).andExpect(status().isOk()).andExpect(content().json("""
                {"status":200,"message":"OK","data":{"regions":[
                  {"regionId":1,"regionName":"서울","cinemas":[
                    {"cinemaId":11,"cinemaName":"용산","hasShowtimes":true},
                    {"cinemaId":12,"cinemaName":"건대입구","hasShowtimes":false}]},
                  {"regionId":2,"regionName":"경기","cinemas":[]}]}}
                """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveMovieId(long id) throws Exception {
        mvc.perform(get(PATH, id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("movieId"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedMovieId() throws Exception {
        mvc.perform(get(PATH, "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
        verifyNoInteractions(service);
    }

    @Test
    void returnsNotFoundForUnknownMovie() throws Exception {
        when(service.getCinemaByMovieId(7L)).thenThrow(
                new BusinessException(ErrorCode.NOT_FOUND, "영화가 존재하지 않습니다."));

        mvc.perform(get(PATH, 7)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("영화가 존재하지 않습니다."));
    }
}

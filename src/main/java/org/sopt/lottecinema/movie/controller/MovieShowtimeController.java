package org.sopt.lottecinema.movie.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse;
import org.sopt.lottecinema.movie.service.MovieShowtimeService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MovieShowtimeController implements MovieShowtimeApi {
    private final MovieShowtimeService showtimeService;

    @Override
    public SuccessResponse<MovieCinemaResponse> getCinemaByMovieId(Long movieId){
        MovieCinemaResponse response = showtimeService.getCinemaByMovieId(movieId);
        return SuccessResponse.success(response);
    }
}
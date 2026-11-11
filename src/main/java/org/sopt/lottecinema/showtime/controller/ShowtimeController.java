package org.sopt.lottecinema.showtime.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.MovieCinemaResponse;
import org.sopt.lottecinema.showtime.service.ShowtimeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShowtimeController implements ShowtimeAPI{
    private final ShowtimeService showtimeService;

    @Override
    public SuccessResponse<MovieCinemaResponse> getCinemaByMovieId(
            @PathVariable Long movieId
    ){
        MovieCinemaResponse response = showtimeService.getCinemaByMovieId(movieId);
        return SuccessResponse.success(response);
    }
}
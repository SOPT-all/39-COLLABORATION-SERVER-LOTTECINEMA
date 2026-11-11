package org.sopt.lottecinema.showtime.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.MovieCinemaResponse;
import org.sopt.lottecinema.showtime.service.ShowtimeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Transactional(readOnly = true)
@RestController
@RequiredArgsConstructor
public class ShowtimeController implements ShowtimeAPI{
    private final ShowtimeService showtimeService;

    @Override
    public SuccessResponse<MovieCinemaResponse> getCinemaByMovieId(
            @PathVariable
            @Positive(message = "영화 ID는 1 이상이어야 합니다.")
            Long movieId
    ){
        MovieCinemaResponse response = showtimeService.getCinemaByMovieId(movieId);

        return SuccessResponse.success(response);
    }
}

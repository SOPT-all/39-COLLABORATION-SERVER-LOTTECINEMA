package org.sopt.lottecinema.movie.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.movie.dto.MovieListResponse;
import org.sopt.lottecinema.movie.service.MovieService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MovieController implements MovieListApi {

    private final MovieService movieService;

    @Override
    public SuccessResponse<List<MovieListResponse>> findMovies() {
        List<MovieListResponse> response = movieService.findMovies();
        return SuccessResponse.success(response);
    }
}
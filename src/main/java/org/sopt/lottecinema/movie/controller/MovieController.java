package org.sopt.lottecinema.movie.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.movie.dto.MovieListsResponse;
import org.sopt.lottecinema.movie.service.MovieService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MovieController implements MovieListApi {

    private final MovieService movieService;

    @Override
    public SuccessResponse<MovieListsResponse> findMovies() {
        MovieListsResponse response = movieService.findMovies();
        return SuccessResponse.success(response);
    }
}

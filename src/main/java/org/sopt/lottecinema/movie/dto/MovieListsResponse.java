package org.sopt.lottecinema.movie.dto;

import java.util.List;

public record MovieListsResponse(
        List<MovieListResponse> popularMovies,
        List<MovieListResponse> lotsiPickMovies,
        List<MovieListResponse> lotsiPlayMovies
) {
}

package org.sopt.lottecinema.movie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(requiredProperties = {"popularMovies", "lotsiPickMovies", "lotsiPlayMovies"})
public record MovieListsResponse(
        List<MovieListResponse> popularMovies,
        List<MovieListResponse> lotsiPickMovies,
        List<MovieListResponse> lotsiPlayMovies
) {
}

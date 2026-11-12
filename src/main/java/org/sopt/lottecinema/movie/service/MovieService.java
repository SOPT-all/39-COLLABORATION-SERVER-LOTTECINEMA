package org.sopt.lottecinema.movie.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.movie.dto.MovieListResponse;
import org.sopt.lottecinema.movie.dto.MovieListsResponse;
import org.sopt.lottecinema.movie.entity.CurationType;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieListsResponse findMovies() {
        List<MovieListResponse> popularMovies = movieRepository
                .findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc().stream()
                .map(MovieListResponse::from)
                .toList();

        List<Movie> curatedMovies = movieRepository
                .findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc();

        return new MovieListsResponse(
                popularMovies,
                findCuratedMovies(curatedMovies, CurationType.LOTSI_PICK),
                findCuratedMovies(curatedMovies, CurationType.LOTSI_PLAY)
        );
    }

    private List<MovieListResponse> findCuratedMovies(List<Movie> movies, CurationType type) {
        return movies.stream()
                .filter(movie -> movie.getCurationType() == type)
                .map(MovieListResponse::from)
                .toList();
    }
}

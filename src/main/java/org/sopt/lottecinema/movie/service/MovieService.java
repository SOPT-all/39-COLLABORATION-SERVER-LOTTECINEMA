package org.sopt.lottecinema.movie.service;

import java.time.LocalDate;
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
    private final MovieListDateProvider movieListDateProvider;

    public MovieListsResponse findMovies() {
        LocalDate referenceDate = movieListDateProvider.getReferenceDate();
        List<MovieListResponse> popularMovies = movieRepository
                .findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc().stream()
                .map(movie -> MovieListResponse.from(movie, referenceDate))
                .toList();

        List<Movie> curatedMovies = movieRepository
                .findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc();

        return new MovieListsResponse(
                popularMovies,
                findCuratedMovies(curatedMovies, CurationType.LOTSI_PICK, referenceDate),
                findCuratedMovies(curatedMovies, CurationType.LOTSI_PLAY, referenceDate)
        );
    }

    private List<MovieListResponse> findCuratedMovies(List<Movie> movies, CurationType type, LocalDate referenceDate) {
        return movies.stream()
                .filter(movie -> movie.getCurationType() == type)
                .map(movie -> MovieListResponse.from(movie, referenceDate))
                .toList();
    }
}

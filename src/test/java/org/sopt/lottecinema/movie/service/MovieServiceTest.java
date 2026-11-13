package org.sopt.lottecinema.movie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.lottecinema.movie.dto.MovieListResponse;
import org.sopt.lottecinema.movie.dto.MovieListsResponse;
import org.sopt.lottecinema.movie.entity.CurationType;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieListDateProvider movieListDateProvider;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository, movieListDateProvider);
        when(movieListDateProvider.getReferenceDate()).thenReturn(LocalDate.of(2026, 10, 27));
    }

    @Test
    void returnsSortedMoviesSeparatedByCategoryAndAllowsOverlap() {
        Movie pick = movie(1L, CurationType.LOTSI_PICK, (short) 1, (short) 2);
        Movie play = movie(2L, CurationType.LOTSI_PLAY, (short) 1, (short) 1);
        Movie secondPick = movie(3L, CurationType.LOTSI_PICK, (short) 2, null);
        when(movieRepository.findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc())
                .thenReturn(List.of(play, pick));
        when(movieRepository.findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc())
                .thenReturn(List.of(pick, play, secondPick));

        MovieListsResponse result = movieService.findMovies();

        assertThat(result.popularMovies()).extracting(MovieListResponse::movieId).containsExactly(2L, 1L);
        assertThat(result.lotsiPickMovies()).extracting(MovieListResponse::movieId).containsExactly(1L, 3L);
        assertThat(result.lotsiPlayMovies()).extracting(MovieListResponse::movieId).containsExactly(2L);
        MovieListResponse response = result.lotsiPickMovies().get(0);
        assertThat(response.title()).isEqualTo("테스트 영화 1");
        assertThat(response.posterUrl()).isEqualTo("https://example.com/poster.jpg");
        assertThat(response.genre()).isEqualTo("애니메이션");
        assertThat(response.runtimeMinutes()).isEqualTo((short) 100);
        assertThat(response.ageRating()).isEqualTo("전체");
        assertThat(response.releaseDate()).isEqualTo(LocalDate.of(2026, 10, 27));
        assertThat(response.curationType()).isEqualTo(CurationType.LOTSI_PICK);
        assertThat(response.curationOrder()).isEqualTo((short) 1);
        assertThat(response.popularityRank()).isEqualTo((short) 2);
        assertThat(response.bookingRate()).isEqualByComparingTo("25.00");
        assertThat(response.movieRating()).isEqualByComparingTo("8.5");
        assertThat(response.dDay()).isNull();
    }

    @Test
    void usesOneReferenceDateForAllListsAndPreservesStoredRatings() {
        Movie pick = movie(1L, CurationType.LOTSI_PICK, (short) 1, (short) 1);
        Movie play = movie(2L, CurationType.LOTSI_PLAY, (short) 1, (short) 2);
        ReflectionTestUtils.setField(pick, "releaseDate", LocalDate.of(2026, 10, 30));
        ReflectionTestUtils.setField(play, "releaseDate", LocalDate.of(2026, 10, 28));
        when(movieListDateProvider.getReferenceDate()).thenReturn(
                LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28));
        when(movieRepository.findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc())
                .thenReturn(List.of(pick, play));
        when(movieRepository.findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc())
                .thenReturn(List.of(pick, play));

        MovieListsResponse result = movieService.findMovies();

        assertThat(result.popularMovies()).extracting(MovieListResponse::dDay).containsExactly(3L, 1L);
        assertThat(result.popularMovies()).allSatisfy(response -> assertThat(response.movieRating()).isNull());
        assertThat(result.lotsiPickMovies()).containsExactly(result.popularMovies().get(0));
        assertThat(result.lotsiPlayMovies()).containsExactly(result.popularMovies().get(1));
        assertThat(pick.getMovieRating()).isEqualByComparingTo("8.5");
        assertThat(play.getMovieRating()).isEqualByComparingTo("8.5");
        verify(movieListDateProvider).getReferenceDate();
    }

    @Test
    void returnsEmptyArraysWhenNoMoviesExist() {
        when(movieRepository.findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc()).thenReturn(List.of());
        when(movieRepository.findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc())
                .thenReturn(List.of());

        MovieListsResponse result = movieService.findMovies();

        assertThat(result.popularMovies()).isEmpty();
        assertThat(result.lotsiPickMovies()).isEmpty();
        assertThat(result.lotsiPlayMovies()).isEmpty();
    }

    private Movie movie(Long id, CurationType type, Short order, Short rank) {
        Movie movie = new Movie("테스트 영화 " + id, "https://example.com/poster.jpg", "애니메이션", (short) 100,
                "전체", LocalDate.of(2026, 10, 27), type, order, rank,
                new BigDecimal("25.00"), new BigDecimal("8.5"));
        ReflectionTestUtils.setField(movie, "id", id);
        return movie;
    }
}

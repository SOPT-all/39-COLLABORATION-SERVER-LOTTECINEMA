package org.sopt.lottecinema.movie.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.sopt.lottecinema.movie.entity.CurationType;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository);
    }

    @Test
    void returnsMovieListWithMovieInformation() {
        Movie movie = new Movie(
                "테스트 영화", "https://example.com/poster.jpg", "애니메이션", (short) 100,
                "전체", LocalDate.of(2026, 11, 1), CurationType.LOTSI_PICK,
                (short) 1, (short) 2, new BigDecimal("25.00"), new BigDecimal("8.5")
        );
        ReflectionTestUtils.setField(movie, "id", 1L);
        when(movieRepository.findAll()).thenReturn(List.of(movie));

        List<MovieListResponse> result = movieService.findMovies();

        assertThat(result).hasSize(1);
        MovieListResponse response = result.get(0);
        assertThat(response.movieId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("테스트 영화");
        assertThat(response.posterUrl()).isEqualTo("https://example.com/poster.jpg");
        assertThat(response.genre()).isEqualTo("애니메이션");
        assertThat(response.runtimeMinutes()).isEqualTo((short) 100);
        assertThat(response.ageRating()).isEqualTo("전체");
        assertThat(response.releaseDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.curationType()).isEqualTo(CurationType.LOTSI_PICK);
        assertThat(response.curationOrder()).isEqualTo((short) 1);
        assertThat(response.popularityRank()).isEqualTo((short) 2);
        assertThat(response.bookingRate()).isEqualByComparingTo("25.00");
        assertThat(response.movieRating()).isEqualByComparingTo("8.5");
    }

    @Test
    void returnsEmptyListWhenNoMoviesExist() {
        when(movieRepository.findAll()).thenReturn(List.of());

        List<MovieListResponse> result = movieService.findMovies();

        assertThat(result).isEmpty();
    }

    @Test
    void createsMovieWithExistingConstructorWithoutCurationOrder() {
        Movie movie = new Movie("예매 테스트", null, "애니메이션", (short) 100, "ALL",
                LocalDate.of(2026, 11, 1), null, null, BigDecimal.ONE, BigDecimal.ONE);

        assertThat(movie.getCurationOrder()).isNull();
        assertThat(movie.getPopularityRank()).isNull();
    }
}

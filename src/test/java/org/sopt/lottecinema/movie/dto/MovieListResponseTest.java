package org.sopt.lottecinema.movie.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sopt.lottecinema.movie.entity.Movie;

class MovieListResponseTest {

    @ParameterizedTest
    @CsvSource(value = {
            "2026-10-30, 8.5, null, 3",
            "2026-10-28, 8.5, null, 1",
            "2026-10-27, 8.5, 8.5, null",
            "2026-10-26, 8.5, 8.5, null",
            "2026-10-30, null, null, 3",
            "2026-10-27, null, null, null",
            "2026-10-26, null, null, null",
            "2026-11-01, 8.5, null, 5"
    }, nullValues = "null")
    void returnsRatingOrDaysUntilRelease(
            LocalDate releaseDate, BigDecimal storedRating, BigDecimal expectedRating, Long expectedDDay
    ) {
        Movie movie = new Movie("테스트 영화", "https://example.com/poster.jpg", "애니메이션", (short) 100,
                "전체", releaseDate, null, null, null, new BigDecimal("25.00"), storedRating);

        MovieListResponse response = MovieListResponse.from(movie, LocalDate.of(2026, 10, 27));

        assertThat(response.movieRating()).isEqualTo(expectedRating);
        assertThat(response.dDay()).isEqualTo(expectedDDay);
        assertThat(movie.getMovieRating()).isEqualTo(storedRating);
    }
}

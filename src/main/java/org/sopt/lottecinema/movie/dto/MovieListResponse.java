package org.sopt.lottecinema.movie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.sopt.lottecinema.movie.entity.CurationType;
import org.sopt.lottecinema.movie.entity.Movie;

@Schema(requiredProperties = {"movieId", "title", "posterUrl", "genre", "runtimeMinutes", "ageRating",
        "releaseDate", "curationType", "curationOrder", "popularityRank", "bookingRate", "movieRating", "dDay"})
public record MovieListResponse(
        Long movieId,
        String title,
        @Schema(nullable = true) String posterUrl,
        String genre,
        short runtimeMinutes,
        String ageRating,
        LocalDate releaseDate,
        @Schema(nullable = true) CurationType curationType,
        @Schema(nullable = true) Short curationOrder,
        @Schema(nullable = true) Short popularityRank,
        @Schema(nullable = true) BigDecimal bookingRate,
        @Schema(nullable = true) BigDecimal movieRating,
        @Schema(nullable = true, description = "개봉 전에는 남은 일수, 개봉 당일 및 이후에는 null입니다.") Long dDay
) {
    public static MovieListResponse from(Movie movie, LocalDate referenceDate) {
        boolean isUpcoming = movie.getReleaseDate().isAfter(referenceDate);
        return new MovieListResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getPosterUrl(),
                movie.getGenre(),
                movie.getRuntimeMinutes(),
                movie.getAgeRating(),
                movie.getReleaseDate(),
                movie.getCurationType(),
                movie.getCurationOrder(),
                movie.getPopularityRank(),
                movie.getBookingRate(),
                isUpcoming ? null : movie.getMovieRating(),
                isUpcoming ? ChronoUnit.DAYS.between(referenceDate, movie.getReleaseDate()) : null
        );
    }
}

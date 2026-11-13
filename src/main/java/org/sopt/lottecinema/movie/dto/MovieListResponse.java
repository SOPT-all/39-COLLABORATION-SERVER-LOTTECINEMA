package org.sopt.lottecinema.movie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.sopt.lottecinema.movie.entity.CurationType;
import org.sopt.lottecinema.movie.entity.Movie;

public record MovieListResponse(
        Long movieId,
        String title,
        String posterUrl,
        String genre,
        short runtimeMinutes,
        String ageRating,
        LocalDate releaseDate,
        CurationType curationType,
        Short curationOrder,
        Short popularityRank,
        BigDecimal bookingRate,
        BigDecimal movieRating,
        Long dDay
) {
    public static MovieListResponse from(Movie movie, LocalDate referenceDate) {
        boolean upcoming = movie.getReleaseDate().isAfter(referenceDate);
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
                upcoming ? null : movie.getMovieRating(),
                upcoming ? ChronoUnit.DAYS.between(referenceDate, movie.getReleaseDate()) : null
        );
    }
}

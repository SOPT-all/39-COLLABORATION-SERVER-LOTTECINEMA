package org.sopt.lottecinema.movie.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        BigDecimal movieRating
) {
    public static MovieListResponse from(Movie movie) {
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
                movie.getMovieRating()
        );
    }
}
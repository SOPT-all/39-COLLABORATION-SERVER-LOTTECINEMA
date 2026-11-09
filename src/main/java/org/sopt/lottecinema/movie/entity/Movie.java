package org.sopt.lottecinema.movie.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "movie",
        check = {
                @CheckConstraint(name = "ck_movie_runtime", constraint = "runtime_minutes > 0"),
                @CheckConstraint(name = "ck_movie_rate", constraint = "booking_rate between 0 and 100"),
                @CheckConstraint(name = "ck_movie_rating", constraint = "movie_rating between 0 and 10")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    @Column(nullable = false, length = 100)
    private String genre;

    @Column(name = "runtime_minutes", nullable = false)
    private short runtimeMinutes;

    @Column(name = "age_rating", nullable = false, length = 20)
    private String ageRating;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "curation_type", length = 20)
    private CurationType curationType;

    @Column(name = "popularity_rank")
    private Short popularityRank;

    @Column(name = "booking_rate", precision = 5, scale = 2)
    private BigDecimal bookingRate;

    @Column(name = "movie_rating", precision = 3, scale = 1)
    private BigDecimal movieRating;

    public Movie(
            String title,
            String posterUrl,
            String genre,
            short runtimeMinutes,
            String ageRating,
            LocalDate releaseDate,
            CurationType curationType,
            Short popularityRank,
            BigDecimal bookingRate,
            BigDecimal movieRating
    ) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.genre = genre;
        this.runtimeMinutes = runtimeMinutes;
        this.ageRating = ageRating;
        this.releaseDate = releaseDate;
        this.curationType = curationType;
        this.popularityRank = popularityRank;
        this.bookingRate = bookingRate;
        this.movieRating = movieRating;
    }
}

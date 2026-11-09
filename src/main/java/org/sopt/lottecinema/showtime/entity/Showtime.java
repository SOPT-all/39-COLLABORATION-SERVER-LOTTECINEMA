package org.sopt.lottecinema.showtime.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.screen.entity.Screen;

@Entity
@Table(
        name = "showtime",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_showtime_screen_start", columnNames = {"screen_id", "start_at"}
        ),
        indexes = @Index(name = "ix_showtime_movie_start", columnList = "movie_id, start_at"),
        check = @CheckConstraint(name = "ck_showtime_times", constraint = "end_at > start_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_showtime_1"))
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "screen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_showtime_2"))
    private Screen screen;

    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "start_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime startAt;

    @JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "end_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "format_type", nullable = false, length = 20)
    private FormatType formatType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "language_type", nullable = false, length = 20)
    private LanguageType languageType;

    public Showtime(
            Movie movie,
            Screen screen,
            LocalDateTime startAt,
            LocalDateTime endAt,
            FormatType formatType,
            LanguageType languageType
    ) {
        this.movie = movie;
        this.screen = screen;
        this.startAt = startAt;
        this.endAt = endAt;
        this.formatType = formatType;
        this.languageType = languageType;
    }
}

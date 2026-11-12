package org.sopt.lottecinema.showtime.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    boolean existsByScreenIdAndStartAtLessThanAndEndAtGreaterThan(
            Long screenId, LocalDateTime endAt, LocalDateTime startAt);

    // 응답에 필요한 관계를 한 번에 로딩해 DTO 변환 중 추가 쿼리와 지연 로딩 오류를 방지한다.
    @Query("""
            select s from Showtime s
            join fetch s.movie
            join fetch s.screen screen
            join fetch screen.cinema
            where s.id = :showtimeId
            """)
    Optional<Showtime> findWithDetailsById(@Param("showtimeId") Long showtimeId);

    @Query(value = """
        SELECT
            st.id AS showtimeId,
            st.start_at AS startAt,
            st.end_at AS endAt,
            sc.name AS screenName,
            sc.seat_type AS seatType,
            st.format_type AS formatType,
            st.language_type AS languageType,
            (
                SELECT COUNT(*)
                FROM seat seat
                WHERE seat.screen_id = sc.id
            ) AS totalSeatCount,
            (
                SELECT COUNT(*)
                FROM reservation_seat rs
                WHERE rs.showtime_id = st.id
            ) AS reservedSeatCount
        FROM showtime st
        JOIN screen sc ON sc.id = st.screen_id
        WHERE st.movie_id = :movieId
          AND sc.cinema_id = :cinemaId
          AND st.start_at >= :startAt
          AND st.start_at < :endAt
        ORDER BY st.start_at ASC
        """, nativeQuery = true)
    List<ShowtimeRow> findShowtimesByMovieAndCinemaAndDate(
            @Param("movieId") Long movieId,
            @Param("cinemaId") Long cinemaId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    interface ShowtimeRow {
        Long getShowtimeId();
        LocalDateTime getStartAt();
        LocalDateTime getEndAt();
        String getScreenName();
        SeatType getSeatType();
        String getFormatType();
        String getLanguageType();
        Long getTotalSeatCount();
        Long getReservedSeatCount();
    }
}

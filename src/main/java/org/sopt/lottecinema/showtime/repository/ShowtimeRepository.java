package org.sopt.lottecinema.showtime.repository;

import java.util.Optional;
import java.time.LocalDateTime;
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
}

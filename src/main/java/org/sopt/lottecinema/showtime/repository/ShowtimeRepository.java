package org.sopt.lottecinema.showtime.repository;

import java.util.Optional;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // 응답에 필요한 관계를 한 번에 로딩해 DTO 변환 중 추가 쿼리와 지연 로딩 오류를 방지한다.
    @Query("""
            select s from Showtime s
            join fetch s.movie
            join fetch s.screen screen
            join fetch screen.cinema
            where s.id = :showtimeId
            """)
    Optional<Showtime> findWithDetailsById(@Param("showtimeId") Long showtimeId);

    @Query("""
    select distinct
        r.id as regionId,
        r.name as regionName,
        r.displayOrder as regionDisplayOrder,
        c.id as cinemaId,
        c.name as cinemaName,
        c.displayOrder as cinemaDisplayOrder
    from Showtime st
    join st.screen s
    join s.cinema c
    join c.region r
    where st.movie.id = :movieId
    order by r.displayOrder, c.displayOrder
    """)
    List<MovieCinemaRow> findCinemasByMovieId(@Param("movieId") Long movieId);

    public interface MovieCinemaRow {
        Long getRegionId();
        String getRegionName();
        Long getCinemaId();
        String getCinemaName();
    }
}

package org.sopt.lottecinema.movie.repository;

import org.sopt.lottecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;

@Repository
public interface MovieShowtimeRepository extends JpaRepository<Showtime, Long> {

    // 지역 및 영화관 목록
    @Query("""
        select 
            r.id as regionId,
            r.name as regionName,
            c.id as cinemaId,
            c.name as cinemaName
        from Region r
        left join Cinema c on c.region = r
        order by r.displayOrder, r.id, c.displayOrder, c.id
        """)
    List<RegionCinemaRow> findAllRegionsAndCinemas();

    // 해당 영화를 상영하는 영화관 ID 집합
    @Query("""
        select distinct s.cinema.id
        from Showtime st
        join st.screen s
        where st.movie.id = :movieId
    """)
    Set<Long> findCinemaIdsWithShowtimes(@Param("movieId") Long movieId);

    interface RegionCinemaRow {
        Long getRegionId();
        String getRegionName();
        Long getCinemaId();
        String getCinemaName();
    }
}

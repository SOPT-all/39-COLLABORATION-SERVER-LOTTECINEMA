package org.sopt.lottecinema.showtime.repository;

import org.sopt.lottecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

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

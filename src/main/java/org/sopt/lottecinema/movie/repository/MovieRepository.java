package org.sopt.lottecinema.movie.repository;

import java.util.List;
import org.sopt.lottecinema.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc();

    List<Movie> findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc();
}

package org.sopt.lottecinema.movie.repository;

import org.sopt.lottecinema.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}

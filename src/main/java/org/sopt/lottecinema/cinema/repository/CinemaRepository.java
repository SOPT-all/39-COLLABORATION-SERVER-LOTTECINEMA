package org.sopt.lottecinema.cinema.repository;

import org.sopt.lottecinema.cinema.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {
}

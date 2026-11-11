package org.sopt.lottecinema.showtime.repository;

import java.util.Optional;
import org.sopt.lottecinema.showtime.entity.ShowtimeTicketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeTicketPriceRepository extends JpaRepository<ShowtimeTicketPrice, Long> {

    /** 회차당 한 행으로 등록된 유형별 단가를 조회한다. */
    Optional<ShowtimeTicketPrice> findByShowtimeId(Long showtimeId);
}

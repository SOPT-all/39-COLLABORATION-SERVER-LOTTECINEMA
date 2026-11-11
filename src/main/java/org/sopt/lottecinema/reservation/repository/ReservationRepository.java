package org.sopt.lottecinema.reservation.repository;

import java.util.Optional;
import java.util.UUID;
import org.sopt.lottecinema.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** 회차 조건 없이 요청 UUID를 조회해 이미 성공한 예매인지 확인한다. */
    Optional<Reservation> findByRequestId(UUID requestId);
}

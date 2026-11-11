package org.sopt.lottecinema.reservation.repository;

import java.util.List;
import org.sopt.lottecinema.reservation.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {

    /** 예매에 확정된 좌석 코드를 읽는다. 최초 응답과 재응답에서 같은 정렬 규칙을 적용한다. */
    @Query("select rs.seat.seatCode from ReservationSeat rs where rs.reservation.id = :reservationId")
    List<String> findSeatCodesByReservationId(@Param("reservationId") Long reservationId);
}

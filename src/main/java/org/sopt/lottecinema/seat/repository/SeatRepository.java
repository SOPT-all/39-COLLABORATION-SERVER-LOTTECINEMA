package org.sopt.lottecinema.seat.repository;

import java.util.List;
import org.sopt.lottecinema.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** 해당 상영관에 속한 기존 좌석 목록을 조회한다. */
    List<Seat> findByScreenId(Long screenId);

    // 회차 조건을 ON 절에 두어 미예매 좌석도 남긴다. 다른 회차의 같은 좌석 예매는 점유로 세지 않는다.
    @Query("""
            select new org.sopt.lottecinema.seat.repository.SeatStatus(
                seat.seatCode, seat.rowIndex, seat.columnIndex,
                case when reserved.id is not null then true else false end)
            from Seat seat
            left join ReservationSeat reserved on reserved.seat = seat and reserved.showtime.id = :showtimeId
            where seat.screen.id = :screenId
            order by seat.rowIndex, seat.columnIndex
            """)
    List<SeatStatus> findStatuses(@Param("screenId") Long screenId, @Param("showtimeId") Long showtimeId);
}

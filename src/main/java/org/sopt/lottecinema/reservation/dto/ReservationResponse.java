package org.sopt.lottecinema.reservation.dto;

import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.sopt.lottecinema.reservation.entity.Reservation;

/** 확정된 예매 정보. 같은 요청의 재응답도 저장된 값으로 구성한다. */
public record ReservationResponse(
        Long reservationId,
        Long showtimeId,
        List<String> seatCodes,
        TicketCounts ticketCounts,
        int totalAmount,
        String createdAt
) {

    /** 저장된 인원·총액·시각을 응답으로 옮기고 좌석 코드를 문자열 순서로 정렬한다. */
    public static ReservationResponse from(Reservation reservation, List<String> seatCodes) {
        TicketCounts counts = new TicketCounts(
                BigInteger.valueOf(reservation.getAdultCount()),
                BigInteger.valueOf(reservation.getTeenCount()),
                BigInteger.valueOf(reservation.getSeniorCount()),
                BigInteger.valueOf(reservation.getDisabledCount())
        );
        String createdAt = reservation.getCreatedAt().atOffset(ZoneOffset.ofHours(9))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new ReservationResponse(
                reservation.getId(),
                reservation.getShowtime().getId(),
                seatCodes.stream().sorted().toList(),
                counts,
                reservation.getTotalAmount(),
                createdAt
        );
    }
}

package org.sopt.lottecinema.seat.repository;

/** 좌석 배치와 요청 회차의 점유 여부를 한 쿼리로 읽는 조회 결과다. */
public record SeatStatus(String seatCode, Integer rowIndex, Integer columnIndex, boolean reserved) {
}

package org.sopt.lottecinema.reservation.dto;

/** created가 true이면 신규 생성 201, false이면 기존 예매 재응답 200으로 반환한다. */
public record ReservationResult(ReservationResponse response, boolean created) {
}

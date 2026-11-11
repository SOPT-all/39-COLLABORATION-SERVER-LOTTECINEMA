package org.sopt.lottecinema.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.reservation.dto.CreateReservationRequest;
import org.sopt.lottecinema.reservation.dto.ReservationResponse;
import org.sopt.lottecinema.reservation.dto.ReservationResult;
import org.sopt.lottecinema.reservation.service.ReservationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/showtimes")
public class ReservationController implements ReservationApi {

    private final ReservationService reservationService;

    /** 서비스의 커밋된 결과를 신규 생성 201 또는 기존 예매 재응답 200으로 반환한다. */
    @Override
    @PostMapping(value = "/{showtimeId}/reservations", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuccessResponse<ReservationResponse>> createReservation(
            @PathVariable("showtimeId") Long showtimeId,
            @RequestBody CreateReservationRequest request
    ) {
        ReservationResult result = reservationService.createReservation(showtimeId, request);
        if (result.created()) {
            return ResponseEntity.status(201).body(SuccessResponse.created(result.response()));
        }
        return ResponseEntity.ok(SuccessResponse.success(result.response()));
    }
}

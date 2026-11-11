package org.sopt.lottecinema.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.sopt.lottecinema.global.common.ErrorResponse;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.reservation.dto.CreateReservationRequest;
import org.sopt.lottecinema.reservation.dto.ReservationResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Reservation", description = "예매 API")
public interface ReservationApi {

    /** 예매 생성의 입력 검증, 성공 상태와 오류 계약을 Swagger에 선언한다. */
    @Operation(summary = "상영 회차 예매 생성", description = "선택 좌석을 확정합니다. 동일 UUID와 동일 내용의 재시도는 기존 결과를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신규 예매 확정"),
            @ApiResponse(responseCode = "200", description = "동일 요청의 기존 예매 재응답"),
            @ApiResponse(responseCode = "400", description = "요청 형식 또는 유효성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회차 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "406", description = "지원하지 않는 응답 형식", content = @Content),
            @ApiResponse(responseCode = "409", description = "좌석 충돌, 금액 변경, 요청 ID 재사용 또는 마감",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "지원하지 않는 Content-Type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<SuccessResponse<ReservationResponse>> createReservation(
            @Parameter(description = "상영 회차 ID", required = true) @Positive Long showtimeId,
            @Valid CreateReservationRequest request);
}

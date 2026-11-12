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

    /** 시연용 회차의 예매 확정과 동일 요청 재시도에 대한 입력·응답 계약을 선언한다. */
    @Operation(summary = "상영 회차 예매 생성", description = """
            선택 좌석을 DB에 확정 저장합니다. 고정 더미데이터 시연을 위해 상영 시작 전·진행 중·종료 후 모두 예매할 수 있습니다.
            외부 결제 승인은 수행하지 않으며, 단순 화면 이동만 필요한 경우에는 이 API를 호출하지 않습니다.
            같은 제출을 재시도할 때는 동일 requestId와 내용을 보내면 최초 결과를 200으로 반환합니다.
            인원·좌석·회차·금액을 바꾼 새 제출에는 새로운 requestId를 사용합니다.
            expectedTotalAmount에는 좌석 조회의 ticketPrices로 계산한 할인 전 상품금액을 전달합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "신규 예매 확정"),
            @ApiResponse(responseCode = "200", description = "동일 요청의 기존 예매 재응답"),
            @ApiResponse(responseCode = "400", description = "요청 형식 또는 유효성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회차 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "406", description = "지원하지 않는 응답 형식", content = @Content),
            @ApiResponse(responseCode = "409", description = "좌석 충돌, 금액 변경 또는 요청 ID의 다른 내용 재사용",
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

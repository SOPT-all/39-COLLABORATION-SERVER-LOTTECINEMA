package org.sopt.lottecinema.showtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.sopt.lottecinema.global.common.ErrorResponse;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;


import java.time.LocalDate;

/** Swagger 계약과 입력 제약을 선언하며 컨트롤러가 이를 구현한다. */
@Tag(name = "Showtime", description = "상영 회차 API")
public interface ShowtimeApi {

    @Operation(summary = "상영 회차 좌석 조회", description = "영화·극장 정보, 좌석 배치와 회차별 예매 상태, 잔여석 및 유형별 단가를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 현황 조회 성공. 매진과 마감 회차도 조회할 수 있습니다."),
            @ApiResponse(responseCode = "400", description = "INVALID_PARAMETER_TYPE 또는 VALIDATION_FAILED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND. 해당 회차가 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "406", description = "지원하지 않는 응답 형식. 본문이 없습니다.", content = @Content),
            @ApiResponse(responseCode = "500", description = "INTERNAL_SERVER_ERROR. 좌표 누락 또는 서버 오류입니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    SuccessResponse<ShowtimeSeatResponse> getShowtimeSeats(
            @Parameter(name = "showtimeId", in = ParameterIn.PATH, description = "선택한 상영 회차 ID", required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1"))
            @Positive(message = "상영 회차 ID는 1 이상이어야 합니다.") Long showtimeId);

    @Operation(summary = "상영 회차 조회", description = "영화·영화관 별 상영 회차의 세부 정보(상영 시작 시간, 좌석 현황, 등)를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공. 해당 날짜에 회차가 없으면 빈 배열을 반환합니다."),
            @ApiResponse(responseCode = "400", description = "MISSING_PARAMETER, INVALID_PARAMETER_TYPE 또는 VALIDATION_FAILED",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND. 영화 또는 영화관이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    SuccessResponse<ShowtimeListResponse> getShowtimeLists(
            @Parameter(name = "movieId", in = ParameterIn.QUERY, required = true, description = "조회할 영화 ID",
                schema = @Schema(type = "integer", format = "int64", minimum = "1"))
            @Positive(message = "영화 ID는 1 이상이어야 합니다.") Long movieId,
            @Parameter(name= "cinemaId", in = ParameterIn.QUERY, required = true, description = "조회할 영화관 ID",
                    schema = @Schema(type = "integer", format = "int64", minimum = "1"))
            @Positive(message = "영화관 ID는 1 이상이어야 합니다.") Long cinemaId,
            @Parameter(name= "date", in = ParameterIn.QUERY, description = "조회할 날짜, 생략 시 KST 기준 오늘 날짜를 사용합니다.",
                    schema = @Schema(type = "string", format = "date", example = "2026-11-27")) LocalDate date);
}

package org.sopt.lottecinema.showtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.sopt.lottecinema.global.common.ErrorResponse;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.springframework.http.MediaType;


import java.time.LocalDate;

/** Swagger 계약과 입력 제약을 선언하며 컨트롤러가 이를 구현한다. */
@Tag(name = "Showtime", description = "상영 회차 API")
public interface ShowtimeApi {

    @Operation(summary = "상영 회차 좌석 조회", description = """
            영화·극장 정보, 좌석 배치와 회차별 예매 상태, 잔여석 및 유형별 단가를 조회합니다.
            고정 더미데이터 시연을 위해 과거·상영 중·종료 회차도 조회할 수 있으며, 조회만으로 좌석을 선점하지 않습니다.
            상영 정보 확인창(V2-03)과 좌석 선택 화면(V3-01)은 같은 showtimeId의 응답을 사용합니다.
            isReserved=false는 선택 가능(T), true는 예매 확정되어 선택 불가(F)인 좌석입니다.
            통로에는 좌석 객체가 없으며, 시연 화면은 빈 공간을 포함한 12행×15열 배치를 유지합니다.
            ticketPrices는 유형별 1인 단가이며, 프론트는 이 값으로 할인 전 상품금액을 계산합니다.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 현황 조회 성공. 과거·상영 중·종료·매진 회차도 조회할 수 있습니다."),
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
            @ApiResponse(responseCode = "200", description = "상영 회차 조회 성공<br>"+"해당 날짜에 회차가 없으면 빈 배열을 반환합니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                            @ExampleObject(name = "상영 회차 존재 (잔여석 / 매진 포함)", value = """
                                    {
                                      "status": 200,
                                      "message": "OK",
                                      "data": {
                                        "showtimes": [
                                          {
                                            "showtimeId": 101,
                                            "startAt": "2026-11-27T09:00:00+09:00",
                                            "endAt": "2026-11-27T11:00:00+09:00",
                                            "screenName": "1관",
                                            "formatType": "2D",
                                            "languageType": "SUBTITLED",
                                            "totalSeatCount": 77,
                                            "reservedSeatCount": 3,
                                            "availableSeatCount": 74
                                          },
                                          {
                                            "showtimeId": 102,
                                            "startAt": "2026-11-27T13:00:00+09:00",
                                            "endAt": "2026-11-27T15:00:00+09:00",
                                            "screenName": "1관",
                                            "formatType": "3D",
                                            "languageType": "DUBBED",
                                            "totalSeatCount": 77,
                                            "reservedSeatCount": 77,
                                            "availableSeatCount": 0
                                          }
                                        ]
                                      }
                                    }
                                    """),
                            @ExampleObject(name = "해당 날짜에 상영 회차 없음", value = """
                                    {
                                      "status": 200,
                                      "message": "OK",
                                      "data": {
                                        "showtimes": []
                                      }
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "400", description = "MISSING_PARAMETER, INVALID_PARAMETER_TYPE 또는 VALIDATION_FAILED",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "필수 파라미터 누락(movieId, cinemaId)", value = """
                                    {
                                      "status": 400,
                                      "code": "MISSING_PARAMETER",
                                      "message": "필수 요청 파라미터가 누락되었습니다."
                                    }
                                    """),
                            @ExampleObject(name = "형식 오류 (date=abc)", value = """
                                    {
                                      "status": 400,
                                      "code": "INVALID_PARAMETER_TYPE",
                                      "message": "요청 파라미터의 형식이 올바르지 않습니다."
                                    }
                                    """),
                            @ExampleObject(name = "검증 실패", value = """
                                    {
                                      "status": 400,
                                      "code": "VALIDATION_FAILED",
                                      "message": "입력값을 확인해 주세요.",
                                      "errors": [
                                        { "field": "movieId", "message": "영화 ID는 1 이상이어야 합니다." }
                                      ]
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND. 영화 또는 영화관이 없습니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class), examples = {
                            @ExampleObject(name = "영화 없음", value = """
                                    {
                                      "status": 404,
                                      "code": "NOT_FOUND",
                                      "message": "해당 영화가 존재하지 않습니다."
                                    }
                                    """),
                            @ExampleObject(name = "영화관 없음", value = """
                                    {
                                      "status": 404,
                                      "code": "NOT_FOUND",
                                      "message": "해당 영화관이 존재하지 않습니다."
                                    }
                                    """)
                    }))
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

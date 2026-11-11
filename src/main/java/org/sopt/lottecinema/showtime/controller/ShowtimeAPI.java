package org.sopt.lottecinema.showtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.MovieCinemaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/movies")
@Tag(name = "Movie", description = "영화별 지역 및 영화관 조회 API")
public interface ShowtimeAPI {

    @Operation(
            summary = "영화별 지역 및 영화관 조회",
            description = "선택한 영화의 상영 회차가 존재하는 지역과 영화관 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "영화별 지역 및 영화관 목록 조회 성공<br>" +
                            "영화는 존재하지만 상영 회차가 없으면 해당 영화를 상영하는 곳이 없다는 의미이며, `regions`는 빈 배열을 반환합니다.",
                    content = @Content(
                            schema = @Schema(implementation = SuccessResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "상영 회차가 있는 경우",
                                            value = """
                                                    {
                                                      "status": 200,
                                                      "message": "OK",
                                                      "data": {
                                                        "regions": [
                                                          {
                                                            "regionId": 1,
                                                            "regionName": "서울",
                                                            "cinemas": [
                                                              {
                                                                "cinemaId": 20,
                                                                "cinemaName": "홍대입구"
                                                              },
                                                              {
                                                                "cinemaId": 21,
                                                                "cinemaName": "건대입구"
                                                              }
                                                            ]
                                                          },
                                                          {
                                                            "regionId": 2,
                                                            "regionName": "경기",
                                                            "cinemas": [
                                                              {
                                                                "cinemaId": 35,
                                                                "cinemaName": "수원"
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "상영 회차가 없는 경우",
                                            value = """
                                                    {
                                                      "status": 200,
                                                      "message": "OK",
                                                      "data": {
                                                        "regions": []
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "movieId 형식이 잘못되었거나 1 미만인 경우",
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "숫자가 아닌 값",
                                            value = """
                                                    {
                                                      "code": "INVALID_PARAMETER_TYPE",
                                                      "message": "요청 파라미터의 형식이 올바르지 않습니다.",
                                                      "status": 400
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "1 미만의 값",
                                            value = """
                                                    {
                                                      "code": "VALIDATION_FAILED",
                                                      "errors": [
                                                        {
                                                          "field": "movieId",
                                                          "message": "영화 ID는 1 이상이어야 합니다."
                                                        }
                                                      ],
                                                      "message": "입력값을 확인해 주세요.",
                                                      "status": 400
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 영화 ID가 존재하지 않을 경우",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "NOT_FOUND",
                                              "message": "대상을 찾을 수 없습니다.",
                                              "status": 404
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/{movieId}/cinemas")
    SuccessResponse<MovieCinemaResponse> getCinemaByMovieId(
            @Parameter(
                    description = "조회할 영화 ID (1 이상)",
                    required = true,
                    example = "1"
            )
            @PathVariable Long movieId
    );
}
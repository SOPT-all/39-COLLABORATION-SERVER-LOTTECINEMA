package org.sopt.lottecinema.movie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/movies")
@Tag(name = "Movie", description = "영화별 지역 및 영화관 조회 API")
interface MovieShowtimeApi {

    @Operation(
            summary = "영화별 지역 및 영화관 조회",
            description = "선택한 영화의 상영 회차 유무와 관계없이 모든 지역과 영화관을 조회합니다. " + "영화관별 hasShowtimes는 날짜와 관계없이 해당 영화의 회차가 하나 이상 등록되어 있으면 true입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "영화별 지역 및 영화관 목록 조회 성공<br>" + "등록된 영화관이 없는 지역은 `cinemas`가 빈 배열로 반환됩니다.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                                    @ExampleObject(
                                            name = "등록된 지역과 영화관이 있는 경우",
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
                                                                            "cinemaId": 1,
                                                                            "cinemaName": "가산디지털",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 2,
                                                                            "cinemaName": "가양",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 3,
                                                                            "cinemaName": "강동",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 4,
                                                                            "cinemaName": "건대입구",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 5,
                                                                            "cinemaName": "김포공항",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 6,
                                                                            "cinemaName": "노원",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 7,
                                                                            "cinemaName": "도곡",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 8,
                                                                            "cinemaName": "독산",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 9,
                                                                            "cinemaName": "서울대입구",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 10,
                                                                            "cinemaName": "수락산",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 11,
                                                                            "cinemaName": "수유",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 12,
                                                                            "cinemaName": "신대방(구로디지털역)",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 13,
                                                                            "cinemaName": "신도림",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 14,
                                                                            "cinemaName": "신림",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 15,
                                                                            "cinemaName": "영등포",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 16,
                                                                            "cinemaName": "용산",
                                                                            "hasShowtimes": true
                                                                        },
                                                                        {
                                                                            "cinemaId": 17,
                                                                            "cinemaName": "월드타워",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 18,
                                                                            "cinemaName": "은평(롯데몰)",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 19,
                                                                            "cinemaName": "청량리",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 20,
                                                                            "cinemaName": "합정",
                                                                            "hasShowtimes": false
                                                                        },
                                                                        {
                                                                            "cinemaId": 21,
                                                                            "cinemaName": "홍대입구",
                                                                            "hasShowtimes": false
                                                                        }
                                                                    ]
                                                                },
                                                                {
                                                                    "regionId": 2,
                                                                    "regionName": "경기/인천",
                                                                    "cinemas": []
                                                                },
                                                                {
                                                                    "regionId": 3,
                                                                    "regionName": "충청/대전",
                                                                    "cinemas": []
                                                                },
                                                                {
                                                                    "regionId": 4,
                                                                    "regionName": "전라/광주",
                                                                    "cinemas": []
                                                                },
                                                                {
                                                                    "regionId": 5,
                                                                    "regionName": "경북/대구",
                                                                    "cinemas": []
                                                                },
                                                                {
                                                                    "regionId": 6,
                                                                    "regionName": "경남/부산/울산",
                                                                    "cinemas": []
                                                                },
                                                                {
                                                                    "regionId": 7,
                                                                    "regionName": "강원",
                                                                    "cinemas": []
                                                                },
                                                                {
                                                                    "regionId": 8,
                                                                    "regionName": "제주",
                                                                    "cinemas": []
                                                                }
                                                            ]
                                                        }
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "등록된 지역이 없는 경우",
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
                    })
            ),
            @ApiResponse(responseCode = "400", description = "movieId 형식이 잘못되었거나 1 미만인 경우",
                    content =  @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = {
                                    @ExampleObject(
                                            name = "숫자가 아닌 값",
                                            value = """
                                                    {
                                                      "code": "INVALID_PARAMETER_TYPE",
                                                      "message": "요청 파라미터의 형식이 올바르지 않습니다.",
                                                      "status": 400
                                                    }
                                                    """),
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
                                                    """)
                            })
            ),
            @ApiResponse(responseCode = "404", description = "해당 영화 ID가 존재하지 않을 경우",
                    content =  @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "NOT_FOUND",
                                              "message": "대상을 찾을 수 없습니다.",
                                              "status": 404
                                            }
                                            """)
                    )
            )
    })
    @GetMapping("/{movieId}/cinemas")
    SuccessResponse<MovieCinemaResponse> getCinemaByMovieId(
            @Parameter(
                    description = "조회할 영화 ID (1 이상)",
                    required = true,
                    example = "1")
            @PathVariable
            @Positive(message = "영화 ID는 1 이상이어야 합니다.")
            Long movieId
    );
}
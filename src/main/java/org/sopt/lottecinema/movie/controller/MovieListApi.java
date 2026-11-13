package org.sopt.lottecinema.movie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.movie.dto.MovieListsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/movies")
@Tag(name = "Movie", description = "영화 API")
public interface MovieListApi {

    @Operation(
            summary = "영화 목록 조회",
            description = """
                    인기 영화, 롯시픽, 롯시플 영화 목록을 각각 정렬하여 조회합니다.
                    각 목록에 결과가 없으면 빈 배열을 반환합니다.

                    세 목록은 요청당 동일한 기준일로 개봉 여부를 판단합니다.
                    - 개봉 전: movieRating은 null, dDay는 개봉일까지 남은 일수인 양의 정수입니다.
                      예를 들어 3일 뒤 개봉하면 dDay는 3이며, 화면에서는 '개봉 D-3'로 표시할 수 있습니다.
                    - 개봉 당일 및 이후: movieRating은 저장된 평점, dDay는 null입니다.
                      평점이 등록되지 않았다면 movieRating도 null입니다.
                    - DB의 개봉일과 평점 원본은 변경하지 않습니다.

                    기준일은 기본적으로 시연 날짜인 2026-10-27입니다.
                    MOVIE_LIST_REFERENCE_DATE 환경변수로 다른 기준일을 지정할 수 있으며,
                    빈 값으로 설정하면 Asia/Seoul의 현재 날짜를 사용합니다.
                    이 설정은 영화 목록에만 적용하며 상영 회차 조회와 예매 마감 판단에는 적용하지 않습니다.
                    아래 응답 예시는 기준일이 2026-10-27인 경우입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "영화 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "개봉 당일 평점과 개봉 3일 전 D-day",
                                    summary = "2026-10-27 기준: 당일 개봉 영화는 평점, 10월 30일 개봉 영화는 D-3",
                                    value = """
                                            {
                                              "status": 200,
                                              "message": "OK",
                                              "data": {
                                                "popularMovies": [
                                                  {
                                                    "movieId": 1,
                                                    "title": "개봉 당일 영화",
                                                    "posterUrl": "https://example.com/posters/movie.jpg",
                                                    "genre": "애니메이션",
                                                    "runtimeMinutes": 100,
                                                    "ageRating": "전체",
                                                    "releaseDate": "2026-10-27",
                                                    "curationType": "LOTSI_PICK",
                                                    "curationOrder": 1,
                                                    "popularityRank": 1,
                                                    "bookingRate": 25.00,
                                                    "movieRating": 8.5,
                                                    "dDay": null
                                                  },
                                                  {
                                                    "movieId": 2,
                                                    "title": "개봉 예정 영화",
                                                    "posterUrl": "https://example.com/posters/upcoming.jpg",
                                                    "genre": "드라마",
                                                    "runtimeMinutes": 110,
                                                    "ageRating": "12세이상관람가",
                                                    "releaseDate": "2026-10-30",
                                                    "curationType": null,
                                                    "curationOrder": null,
                                                    "popularityRank": 2,
                                                    "bookingRate": 20.00,
                                                    "movieRating": null,
                                                    "dDay": 3
                                                  }
                                                ],
                                                "lotsiPickMovies": [
                                                  {
                                                    "movieId": 1,
                                                    "title": "개봉 당일 영화",
                                                    "posterUrl": "https://example.com/posters/movie.jpg",
                                                    "genre": "애니메이션",
                                                    "runtimeMinutes": 100,
                                                    "ageRating": "전체",
                                                    "releaseDate": "2026-10-27",
                                                    "curationType": "LOTSI_PICK",
                                                    "curationOrder": 1,
                                                    "popularityRank": 1,
                                                    "bookingRate": 25.00,
                                                    "movieRating": 8.5,
                                                    "dDay": null
                                                  }
                                                ],
                                                "lotsiPlayMovies": []
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content
            )
    })
    @GetMapping
    SuccessResponse<MovieListsResponse> findMovies();
}

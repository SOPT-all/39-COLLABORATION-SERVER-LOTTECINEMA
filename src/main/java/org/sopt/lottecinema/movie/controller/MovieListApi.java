package org.sopt.lottecinema.movie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.movie.dto.MovieListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/movies")
@Tag(name = "Movie", description = "영화 API")
public interface MovieListApi {

    @Operation(
            summary = "영화 목록 조회",
            description = "등록된 영화 전체와 영화별 정보를 조회합니다. "
                    + "조회 결과가 없으면 빈 배열을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "영화 목록 조회 성공",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 200,
                                              "message": "OK",
                                              "data": [
                                                {
                                                  "movieId": 1,
                                                  "title": "테스트 영화",
                                                  "posterUrl": "https://example.com/posters/movie.jpg",
                                                  "genre": "애니메이션",
                                                  "runtimeMinutes": 100,
                                                  "ageRating": "전체",
                                                  "releaseDate": "2026-11-01",
                                                  "curationType": "LOTSI_PICK",
                                                  "curationOrder": 1,
                                                  "popularityRank": 2,
                                                  "bookingRate": 25.00,
                                                  "movieRating": 8.5
                                                }
                                              ]
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
    SuccessResponse<List<MovieListResponse>> findMovies();
}
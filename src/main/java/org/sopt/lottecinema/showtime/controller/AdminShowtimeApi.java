package org.sopt.lottecinema.showtime.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeRequest;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Showtime", description = "관리자 상영 회차 API")
public interface AdminShowtimeApi {
    @Operation(summary = "상영 회차 등록", description = "Authorization 헤더에 Bearer 관리자 토큰을 전달합니다.",
            security = @SecurityRequirement(name = "adminBearer"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 또는 시간 범위 오류"),
            @ApiResponse(responseCode = "401", description = "관리자 인증 실패"),
            @ApiResponse(responseCode = "404", description = "영화 또는 상영관 없음"),
            @ApiResponse(responseCode = "409", description = "상영 시간 충돌")
    })
    ResponseEntity<SuccessResponse<CreateShowtimeResponse>> createShowtime(CreateShowtimeRequest request);
}

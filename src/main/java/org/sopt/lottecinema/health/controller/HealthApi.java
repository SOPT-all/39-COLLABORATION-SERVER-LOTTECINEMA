package org.sopt.lottecinema.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.sopt.lottecinema.global.common.SuccessResponse;

@Tag(name = "Health", description = "애플리케이션 상태 확인 API")
public interface HealthApi {

    @Operation(summary = "애플리케이션 상태 확인", description = "애플리케이션의 HTTP 응답 상태를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "애플리케이션이 요청에 응답할 수 있습니다.")
    SuccessResponse<String> getHealth();
}

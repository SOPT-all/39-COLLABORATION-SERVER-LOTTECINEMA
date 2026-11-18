package org.sopt.lottecinema.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"message"})
public record ValidationError(
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "객체 전체의 오류이면 생략됩니다.", requiredMode = Schema.RequiredMode.NOT_REQUIRED) String field,
        String message
) {
}

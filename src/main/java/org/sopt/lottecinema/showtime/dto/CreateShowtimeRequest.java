package org.sopt.lottecinema.showtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import org.sopt.lottecinema.showtime.entity.LanguageType;

public record CreateShowtimeRequest(
        @NotNull @Positive Long movieId,
        @NotNull @Positive Long screenId,
        @NotNull @Schema(description = "한국 시간, 시간대 표시 없는 ISO 8601", example = "2026-11-20T14:00:00") LocalDateTime startAt,
        @NotNull @Schema(description = "한국 시간, 시간대 표시 없는 ISO 8601", example = "2026-11-20T16:00:00") LocalDateTime endAt,
        @NotNull @Pattern(regexp = "2D|3D|4DX") @Schema(allowableValues = {"2D", "3D", "4DX"}, example = "2D") String formatType,
        @NotNull LanguageType languageType
) { }

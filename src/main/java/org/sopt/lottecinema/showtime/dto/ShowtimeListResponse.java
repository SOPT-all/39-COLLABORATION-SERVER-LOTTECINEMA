package org.sopt.lottecinema.showtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.lottecinema.screen.entity.SeatType;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(requiredProperties = {"showtimes"})
public record ShowtimeListResponse(
        List<ShowtimeResponse> showtimes
) {
    public ShowtimeListResponse {
        showtimes = List.copyOf(showtimes);
    }
    @Schema(requiredProperties = {"showtimeId", "startAt", "endAt", "screenName", "seatType", "formatType",
            "languageType", "totalSeatCount", "reservedSeatCount", "availableSeatCount"})
    public record ShowtimeResponse(
            Long showtimeId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String screenName,
            SeatType seatType,
            @Schema(allowableValues = {"2D", "3D", "4DX"}) String formatType,
            @Schema(allowableValues = {"DUBBED", "SUBTITLED", "NONE"}) String languageType,
            Integer totalSeatCount,
            Integer reservedSeatCount,
            Integer availableSeatCount
    ) {
    }
}

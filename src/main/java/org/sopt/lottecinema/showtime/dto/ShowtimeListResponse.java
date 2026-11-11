package org.sopt.lottecinema.showtime.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ShowtimeListResponse(
        List<ShowtimeResponse> showtimes
) {
    public record ShowtimeResponse(
            Long showtimeId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String screenName,
            String formatType,
            String languageType,
            Integer totalSeatCount,
            Integer reservedSeatCount,
            Integer availableSeatCount
    ) {
    }
    public ShowtimeListResponse{
        showtimes = List.copyOf(showtimes);
    }
}

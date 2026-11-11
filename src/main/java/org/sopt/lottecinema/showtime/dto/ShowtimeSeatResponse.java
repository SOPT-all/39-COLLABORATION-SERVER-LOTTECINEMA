package org.sopt.lottecinema.showtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 회차 정보와 전체 좌석 현황을 함께 제공해 두 좌석 선택 화면이 같은 데이터를 사용하게 한다. */
public record ShowtimeSeatResponse(
        String startAt,
        String endAt,
        String formatType,
        MovieInfo movie,
        CinemaInfo cinema,
        ScreenInfo screen,
        int availableSeatCount,
        int totalSeatCount,
        List<SeatInfo> seats,
        TicketPrices ticketPrices
) {
    public record MovieInfo(String title, @JsonInclude(JsonInclude.Include.ALWAYS) String posterUrl, String ageRating) {
    }

    public record CinemaInfo(String name) {
    }

    public record ScreenInfo(String name, String seatType) {
    }

    // Boolean null은 접근성 자료 미등록이므로 생략한다. 예약 여부는 항상 명시한다.
    public record SeatInfo(
            String seatCode,
            int rowIndex,
            int columnIndex,
            @JsonProperty("isReserved") boolean reserved,
            @JsonProperty("isAccessible") @JsonInclude(JsonInclude.Include.NON_NULL) Boolean accessible
    ) {
    }
}

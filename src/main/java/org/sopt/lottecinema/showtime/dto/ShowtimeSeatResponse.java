package org.sopt.lottecinema.showtime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 회차 정보와 전체 좌석 현황을 함께 제공해 두 좌석 선택 화면이 같은 데이터를 사용하게 한다. */
@Schema(requiredProperties = {"startAt", "endAt", "formatType", "movie", "cinema", "screen",
        "availableSeatCount", "totalSeatCount", "seats", "ticketPrices"})
public record ShowtimeSeatResponse(
        @Schema(format = "date-time") String startAt,
        @Schema(format = "date-time") String endAt,
        @Schema(allowableValues = {"2D", "3D", "4DX"}) String formatType,
        MovieInfo movie,
        CinemaInfo cinema,
        ScreenInfo screen,
        @Schema(description = "확정 예매를 제외한 선택 가능 좌석 수", example = "77") int availableSeatCount,
        @Schema(description = "통로를 제외한 실제 좌석 수", example = "77") int totalSeatCount,
        List<SeatInfo> seats,
        @Schema(description = "할인 전 상품금액 계산에 사용하는 유형별 1인 단가(원)") TicketPrices ticketPrices
) {
    @Schema(requiredProperties = {"title", "posterUrl", "ageRating"})
    public record MovieInfo(String title,
                            @JsonInclude(JsonInclude.Include.ALWAYS) @Schema(nullable = true) String posterUrl,
                            String ageRating) {
    }

    @Schema(requiredProperties = {"name"})
    public record CinemaInfo(String name) {
    }

    @Schema(requiredProperties = {"name", "seatType"})
    public record ScreenInfo(String name, @Schema(allowableValues = {"STANDARD", "RECLINER"}) String seatType) {
    }

    // 좌표는 0부터 시작한다. 화면에서 선택 중인 상태는 프론트가 관리하고, 예매 확정 여부만 서버에서 읽는다.
    @Schema(requiredProperties = {"seatCode", "rowIndex", "columnIndex", "isReserved"})
    public record SeatInfo(
            String seatCode,
            int rowIndex,
            int columnIndex,
            @JsonProperty("isReserved")
            @Schema(description = "false: 선택 가능(T), true: 예매 확정되어 선택 불가(F)", example = "false") boolean reserved
    ) {
    }
}

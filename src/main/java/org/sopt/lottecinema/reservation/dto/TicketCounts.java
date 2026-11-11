package org.sopt.lottecinema.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;

/**
 * 관람 유형별 인원. null은 누락된 값이므로 @NotNull에서 거부하고, 0명은 정상 입력으로 받는다.
 * BigInteger로 큰 정수도 먼저 읽어 @Min·@Max에서 인원 범위 오류로 안내한다.
 */
public record TicketCounts(
        @NotNull @Min(0) @Max(8) BigInteger adult,
        @NotNull @Min(0) @Max(8) BigInteger teen,
        @NotNull @Min(0) @Max(8) BigInteger senior,
        @NotNull @Min(0) @Max(8) BigInteger disabled
) {
    /** @Valid에서 각 유형이 0~8명인지 확인한 뒤 네 유형의 인원수를 합산한다. */
    public int totalCount() {
        return adult.intValueExact() + teen.intValueExact() + senior.intValueExact() + disabled.intValueExact();
    }
}

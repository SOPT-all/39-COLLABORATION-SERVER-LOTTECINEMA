package org.sopt.lottecinema.reservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;

/** 관람 유형별 인원. 누락과 0명을 구분하기 위해 null을 표현할 수 있는 타입을 사용한다. */
public record TicketCounts(
        @NotNull @Min(0) @Max(8) BigInteger adult,
        @NotNull @Min(0) @Max(8) BigInteger teen,
        @NotNull @Min(0) @Max(8) BigInteger senior,
        @NotNull @Min(0) @Max(8) BigInteger disabled
) {
    /** 유형별 범위 검증을 통과한 인원수를 합산한다. */
    public int totalCount() {
        return adult.intValueExact() + teen.intValueExact() + senior.intValueExact() + disabled.intValueExact();
    }
}

package org.sopt.lottecinema.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 모든 회차에 적용할 원 단위 단가. 누락 또는 잘못된 설정은 애플리케이션 시작 시 검증한다. */
@Validated
@ConfigurationProperties(prefix = "ticket.prices")
public record TicketPriceProperties(
        // 최대 8인 총액이 예매 테이블의 INT 범위 안에 들어오도록 유형별 상한을 둔다.
        @NotNull @Min(0) @Max(268435455) Integer adult,
        @NotNull @Min(0) @Max(268435455) Integer teen,
        @NotNull @Min(0) @Max(268435455) Integer senior,
        @NotNull @Min(0) @Max(268435455) Integer disabled
) {
}

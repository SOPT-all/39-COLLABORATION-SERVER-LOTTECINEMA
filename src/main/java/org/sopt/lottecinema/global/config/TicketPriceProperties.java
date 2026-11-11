package org.sopt.lottecinema.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * application.yml의 ticket.prices 하위 값을 같은 이름의 필드로 읽는다. 단위는 원이다.
 * 애플리케이션의 @EnableConfigurationProperties가 이 설정을 빈으로 등록해 서비스에 주입한다.
 * @Validated는 누락 또는 잘못된 금액을 애플리케이션 시작 시 검증한다.
 */
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

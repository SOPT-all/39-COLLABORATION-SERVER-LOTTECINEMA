package org.sopt.lottecinema.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TicketPricePropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PriceConfiguration.class);

    @Test
    void bindsConfiguredPricesIncludingZero() {
        runner.withPropertyValues("ticket.prices.adult=15000", "ticket.prices.teen=12000",
                "ticket.prices.senior=10000", "ticket.prices.disabled=0").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(TicketPriceProperties.class))
                            .isEqualTo(new TicketPriceProperties(15000, 12000, 10000, 0));
                });
    }

    @Test
    void rejectsMissingPricesAtStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @CsvSource({"adult,-1", "teen,-1", "senior,-1", "disabled,-1", "adult,268435456", "teen,invalid"})
    void rejectsInvalidPriceAtStartup(String type, String amount) {
        runner.withPropertyValues("ticket.prices.adult=15000", "ticket.prices.teen=12000",
                "ticket.prices.senior=10000", "ticket.prices.disabled=10000")
                .withPropertyValues("ticket.prices." + type + "=" + amount)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TicketPriceProperties.class)
    static class PriceConfiguration {
    }
}

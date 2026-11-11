package org.sopt.lottecinema;

import org.sopt.lottecinema.global.config.TicketPriceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TicketPriceProperties.class)
public class LottecinemaApplication {

    public static void main(String[] args) {
        SpringApplication.run(LottecinemaApplication.class, args);
    }

}

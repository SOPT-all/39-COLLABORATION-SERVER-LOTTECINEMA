package org.sopt.lottecinema.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /** 예매 마감과 생성 시각에 사용할 KST 시계를 제공한다. 테스트에서는 고정 시계로 교체한다. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}

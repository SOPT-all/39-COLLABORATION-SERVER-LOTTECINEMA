package org.sopt.lottecinema.seat.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("seat-profile-test")
class SeatDataSeederProfileTest {

    @Autowired private ApplicationContext applicationContext;

    @Test
    void enablesSeederForAnotherProfile() {
        assertThat(applicationContext.getBeansOfType(SeatDataSeeder.class)).hasSize(1);
    }
}

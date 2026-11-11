package org.sopt.lottecinema.seat.config;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.sopt.lottecinema.cinema.entity.Cinema;
import org.sopt.lottecinema.region.entity.Region;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 트랜잭션에서 상영관 좌석 시딩의 전체 롤백을 확인한다. */
@SpringBootTest
class SeatDataSeederTransactionTest {

    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private SeatDataSeeder seeder;
    @Autowired private SeatRepository seatRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void rollsBackAllScreensWhenLaterScreenConflicts() {
        FixtureIds fixture = transactionTemplate.execute(status -> {
            Region region = new Region("충돌-" + UUID.randomUUID().toString().substring(0, 8), (short) 0);
            entityManager.persist(region);
            Cinema cinema = new Cinema(region, "롤백 테스트", (short) 0);
            entityManager.persist(cinema);
            Screen first = new Screen(cinema, "1관", SeatType.STANDARD);
            Screen second = new Screen(cinema, "2관", SeatType.STANDARD);
            entityManager.persist(first);
            entityManager.persist(second);
            entityManager.persist(new Seat(second, "A10", 5, 10));
            entityManager.flush();
            return new FixtureIds(region.getId(), cinema.getId(), first.getId(), second.getId());
        });
        try {
            assertThatThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("A10");
            assertThat(seatRepository.findByScreenId(fixture.firstScreenId())).isEmpty();
            assertThat(seatRepository.findByScreenId(fixture.secondScreenId())).hasSize(1);
        } finally {
            deleteFixture(fixture);
        }
    }

    private void deleteFixture(FixtureIds fixture) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("delete from seat where screen_id in (?, ?)", fixture.firstScreenId(), fixture.secondScreenId());
            jdbcTemplate.update("delete from screen where id in (?, ?)", fixture.firstScreenId(), fixture.secondScreenId());
            jdbcTemplate.update("delete from cinema where id = ?", fixture.cinemaId());
            jdbcTemplate.update("delete from region where id = ?", fixture.regionId());
        });
    }

    private record FixtureIds(Long regionId, Long cinemaId, Long firstScreenId, Long secondScreenId) {
    }
}

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
        Long[] ids = transactionTemplate.execute(status -> {
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
            return new Long[]{region.getId(), cinema.getId(), first.getId(), second.getId()};
        });
        try {
            assertThatThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("A10");
            assertThat(seatRepository.findByScreenId(ids[2])).isEmpty();
            assertThat(seatRepository.findByScreenId(ids[3])).hasSize(1);
        } finally {
            deleteFixture(ids);
        }
    }

    private void deleteFixture(Long[] ids) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("delete from seat where screen_id in (?, ?)", ids[2], ids[3]);
            jdbcTemplate.update("delete from screen where id in (?, ?)", ids[2], ids[3]);
            jdbcTemplate.update("delete from cinema where id = ?", ids[1]);
            jdbcTemplate.update("delete from region where id = ?", ids[0]);
        });
    }

}

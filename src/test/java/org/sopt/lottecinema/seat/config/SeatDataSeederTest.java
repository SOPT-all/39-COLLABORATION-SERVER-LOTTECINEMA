package org.sopt.lottecinema.seat.config;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.lottecinema.cinema.entity.Cinema;
import org.sopt.lottecinema.region.entity.Region;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.screen.repository.ScreenRepository;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 같은 MySQL DB에서 초기화와 재시작, 기존 좌석 백필을 검증한다. 테스트 데이터는 롤백한다. */
@SpringBootTest
@Transactional
class SeatDataSeederTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ScreenRepository screenRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ApplicationContext applicationContext;

    private Screen screen;
    private SeatDataSeeder seeder;

    @BeforeEach
    void setUp() {
        Region region = new Region("시딩-" + UUID.randomUUID().toString().substring(0, 8), (short) 0);
        entityManager.persist(region);
        Cinema cinema = new Cinema(region, "시딩 극장", (short) 0);
        entityManager.persist(cinema);
        screen = new Screen(cinema, "1관", SeatType.STANDARD);
        entityManager.persist(screen);
        entityManager.flush();
        seeder = new SeatDataSeeder(screenRepository, seatRepository, screen.getId());
    }

    @Test
    void seedsExactLayoutAndKeepsSeatIdsOnRepeatedStartup() throws Exception {
        seeder.run(new DefaultApplicationArguments());
        List<Seat> first = seatRepository.findByScreenId(screen.getId());
        List<Long> firstIds = first.stream().map(Seat::getId).toList();
        assertThat(first).hasSize(77);
        assertThat(first).filteredOn(seat -> seat.getRowIndex() == 0).extracting(Seat::getSeatCode)
                .containsExactlyInAnyOrder("A10", "A11");
        assertThat(first).filteredOn(seat -> seat.getSeatCode().equals("E12"))
                .singleElement().satisfies(seat -> {
                    assertThat(seat.getRowIndex()).isEqualTo(4);
                    assertThat(seat.getColumnIndex()).isEqualTo(13);
                });
        assertThat(first).noneMatch(seat -> seat.getRowIndex() >= 7 || seat.getColumnIndex() == 2
                || seat.getColumnIndex() == 12 || seat.getSeatCode().equals("E13"));

        entityManager.clear();
        seeder.run(new DefaultApplicationArguments());
        assertThat(seatRepository.findByScreenId(screen.getId())).extracting(Seat::getId)
                .containsExactlyInAnyOrderElementsOf(firstIds);
    }

    @Test
    void fillsLegacyCoordinatesWithoutReplacingSeat() throws Exception {
        entityManager.createNativeQuery("""
                insert into seat (screen_id, seat_code) values (:screenId, 'B3')
                """).setParameter("screenId", screen.getId()).executeUpdate();
        Long originalId = seatRepository.findByScreenId(screen.getId()).getFirst().getId();
        entityManager.clear();

        seeder.run(new DefaultApplicationArguments());
        entityManager.clear();
        List<Seat> seats = seatRepository.findByScreenId(screen.getId());
        assertThat(seats).hasSize(77);
        assertThat(seats).filteredOn(seat -> seat.getSeatCode().equals("B3")).singleElement().satisfies(seat -> {
            assertThat(seat.getId()).isEqualTo(originalId);
            assertThat(seat.getRowIndex()).isEqualTo(1);
            assertThat(seat.getColumnIndex()).isEqualTo(3);
        });
    }

    @Test
    void rejectsConflictingLayoutBeforeAddingSeats() {
        seatRepository.saveAndFlush(new Seat(screen, "A10", 5, 10));
        assertThatThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("A10");
        assertThat(seatRepository.findByScreenId(screen.getId())).hasSize(1);
    }

    @Test
    void rejectsMissingTargetScreen() {
        SeatDataSeeder missingScreenSeeder = new SeatDataSeeder(screenRepository, seatRepository, Long.MAX_VALUE);
        assertThatThrownBy(() -> missingScreenSeeder.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("상영관이 없습니다");
    }

    @Test
    void enablesStartupSeederOnlyForSeedProfile() {
        assertThat(applicationContext.getBeansOfType(SeatDataSeeder.class)).isEmpty();
    }
}

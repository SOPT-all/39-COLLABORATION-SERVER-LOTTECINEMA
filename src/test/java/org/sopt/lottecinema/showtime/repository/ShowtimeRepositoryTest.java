package org.sopt.lottecinema.showtime.repository;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.sopt.lottecinema.cinema.entity.Cinema;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.region.entity.Region;
import org.sopt.lottecinema.reservation.entity.Reservation;
import org.sopt.lottecinema.reservation.entity.ReservationSeat;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.sopt.lottecinema.seat.repository.SeatStatus;
import org.sopt.lottecinema.showtime.entity.FormatType;
import org.sopt.lottecinema.showtime.entity.LanguageType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.entity.ShowtimeTicketPrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 MySQL에서 회차 조인과 조회 횟수를 확인하고, 테스트 데이터는 트랜잭션 종료 시 롤백한다. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class ShowtimeRepositoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private ShowtimeTicketPriceRepository priceRepository;
    @Autowired private SeatRepository seatRepository;

    @Test
    void findsOrderedSeatsWithReservationsOnlyForRequestedShowtimeInThreeQueries() {
        Region region = new Region("좌석조회-" + UUID.randomUUID().toString().substring(0, 8), (short) 0);
        entityManager.persist(region);
        Cinema cinema = new Cinema(region, "테스트 극장", (short) 0);
        entityManager.persist(cinema);
        Screen screen = new Screen(cinema, "1관", SeatType.STANDARD);
        Screen otherScreen = new Screen(cinema, "2관", SeatType.RECLINER);
        entityManager.persist(screen);
        entityManager.persist(otherScreen);
        Movie movie = new Movie("테스트 영화", null, "애니메이션", (short) 100, "ALL",
                LocalDate.of(2026, 11, 1), null, null, BigDecimal.ONE, BigDecimal.ONE);
        entityManager.persist(movie);
        Showtime first = new Showtime(movie, screen, LocalDateTime.of(2026, 11, 12, 10, 0),
                LocalDateTime.of(2026, 11, 12, 12, 0), FormatType.TWO_D, LanguageType.NONE);
        Showtime second = new Showtime(movie, screen, first.getStartAt().plusHours(3), first.getEndAt().plusHours(3),
                FormatType.THREE_D, LanguageType.NONE);
        entityManager.persist(first);
        entityManager.persist(second);
        // 삽입 순서를 뒤집고 통로를 비워도 결과는 저장된 좌표 순서여야 한다.
        Seat a3 = new Seat(screen, "A3", 0, 2);
        Seat a1 = new Seat(screen, "A1", 0, 0);
        entityManager.persist(a3);
        entityManager.persist(a1);
        entityManager.persist(new Seat(otherScreen, "B1", 0, 0));
        reserve(first, a1);
        reserve(second, a3);
        entityManager.persist(new ShowtimeTicketPrice(first, 15000, 12000, 10000, 10000));
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        Showtime loaded = showtimeRepository.findWithDetailsById(first.getId()).orElseThrow();
        List<SeatStatus> seats = seatRepository.findStatuses(screen.getId(), first.getId());
        ShowtimeTicketPrice price = priceRepository.findByShowtimeId(first.getId()).orElseThrow();

        assertThat(loaded.getMovie().getTitle()).isEqualTo("테스트 영화");
        assertThat(loaded.getScreen().getCinema().getName()).isEqualTo("테스트 극장");
        assertThat(seats).extracting("seatCode").containsExactly("A1", "A3");
        assertThat(seats).extracting("reserved").containsExactly(true, false);
        assertThat(seats).extracting("columnIndex").containsExactly(0, 2);
        assertThat(price.getAdultPrice()).isEqualTo(15000);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    private void reserve(Showtime showtime, Seat seat) {
        Reservation reservation = new Reservation(UUID.randomUUID(), showtime, (byte) 1, (byte) 0, (byte) 0,
                (byte) 0, 15000, LocalDateTime.of(2026, 11, 1, 12, 0));
        entityManager.persist(reservation);
        entityManager.persist(new ReservationSeat(reservation, seat));
    }
}

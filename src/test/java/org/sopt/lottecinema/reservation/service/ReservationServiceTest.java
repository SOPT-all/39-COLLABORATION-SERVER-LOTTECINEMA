package org.sopt.lottecinema.reservation.service;

import jakarta.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sopt.lottecinema.cinema.entity.Cinema;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.region.entity.Region;
import org.sopt.lottecinema.reservation.dto.CreateReservationRequest;
import org.sopt.lottecinema.reservation.dto.ReservationResult;
import org.sopt.lottecinema.reservation.dto.TicketCounts;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.showtime.entity.FormatType;
import org.sopt.lottecinema.showtime.entity.LanguageType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.SeatInfo;
import org.sopt.lottecinema.showtime.service.ShowtimeSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** 실제 MySQL의 커밋·롤백과 동시 요청을 검증한다. 서비스의 독립 트랜잭션이 읽을 수 있게 준비 데이터를 커밋한다. */
@SpringBootTest
@Import(ReservationServiceTest.FixedClockConfiguration.class)
class ReservationServiceTest {

    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transaction;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ReservationService service;
    @Autowired private ShowtimeSeatService showtimeSeatService;
    private Fixture fixture;

    /** 회차 세 개와 좌석을 준비한다. 세 번째 회차의 시작은 고정 시계의 현재 시각과 같다. */
    @BeforeEach
    void setUp() {
        fixture = transaction.execute(status -> {
            Region region = new Region("예매-" + UUID.randomUUID().toString().substring(0, 8), (short) 0);
            entityManager.persist(region);
            Cinema cinema = new Cinema(region, "예매 테스트", (short) 0);
            entityManager.persist(cinema);
            Screen screen = new Screen(cinema, "1관", SeatType.STANDARD);
            Screen other = new Screen(cinema, "2관", SeatType.STANDARD);
            entityManager.persist(screen);
            entityManager.persist(other);
            Movie movie = new Movie("예매 테스트", null, "애니메이션", (short) 100, "ALL",
                    LocalDate.of(2026, 11, 1), null, null, BigDecimal.ONE, BigDecimal.ONE);
            entityManager.persist(movie);
            Showtime first = new Showtime(movie, screen, LocalDateTime.of(2026, 11, 12, 14, 25),
                    LocalDateTime.of(2026, 11, 12, 16, 5), FormatType.TWO_D, LanguageType.NONE);
            Showtime second = new Showtime(movie, screen, first.getStartAt().plusHours(3), first.getEndAt().plusHours(3),
                    FormatType.TWO_D, LanguageType.NONE);
            Showtime startingNow = new Showtime(movie, screen, LocalDateTime.of(2026, 11, 12, 10, 0),
                    LocalDateTime.of(2026, 11, 12, 11, 40), FormatType.TWO_D, LanguageType.NONE);
            entityManager.persist(first);
            entityManager.persist(second);
            entityManager.persist(startingNow);
            for (int column = 0; column < 8; column++) {
                entityManager.persist(new Seat(screen, "A" + (column + 1), 0, column));
            }
            entityManager.persist(new Seat(other, "B1", 0, 0));
            entityManager.flush();
            return new Fixture(region.getId(), cinema.getId(), movie.getId(), screen.getId(), other.getId(),
                    first.getId(), second.getId(), startingNow.getId());
        });
    }

    /** 커밋된 예매와 준비 데이터를 자식 테이블부터 정리해 다른 테스트의 시작 시딩에 영향을 주지 않는다. */
    @AfterEach
    void tearDown() {
        transaction.executeWithoutResult(status -> {
            jdbc.update("delete from reservation_seat where showtime_id in (?, ?, ?)",
                    fixture.showtimeId(), fixture.secondShowtimeId(), fixture.startingNowShowtimeId());
            jdbc.update("delete from reservation where showtime_id in (?, ?, ?)",
                    fixture.showtimeId(), fixture.secondShowtimeId(), fixture.startingNowShowtimeId());
            jdbc.update("delete from showtime where id in (?, ?, ?)",
                    fixture.showtimeId(), fixture.secondShowtimeId(), fixture.startingNowShowtimeId());
            jdbc.update("delete from seat where screen_id in (?, ?)", fixture.screenId(), fixture.otherScreenId());
            jdbc.update("delete from screen where id in (?, ?)", fixture.screenId(), fixture.otherScreenId());
            jdbc.update("delete from cinema where id = ?", fixture.cinemaId());
            jdbc.update("delete from region where id = ?", fixture.regionId());
            jdbc.update("delete from movie where id = ?", fixture.movieId());
        });
    }

    /** 좌석 순서나 회차의 상영 시각이 바뀌어도 동일 요청의 최초 저장 결과를 반환하는지 확인한다. */
    @Test
    void createsAndReplaysStoredReservationAfterShowtimeStarts() {
        UUID requestId = UUID.randomUUID();
        ReservationResult created = service.createReservation(fixture.showtimeId(), request(requestId, List.of("A2", "A1")));
        assertThat(created.created()).isTrue();
        assertThat(created.response().seatCodes()).containsExactly("A1", "A2");
        assertThat(created.response().createdAt()).isEqualTo("2026-11-12T10:00:00+09:00");
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.showtimeId()).availableSeatCount()).isEqualTo(6);
        jdbc.update("update showtime set start_at = '2020-01-01 10:00:00' where id = ?", fixture.showtimeId());
        ReservationResult replayed = service.createReservation(fixture.showtimeId(), request(requestId, List.of("A1", "A2")));
        assertThat(replayed.created()).isFalse();
        assertThat(replayed.response()).isEqualTo(created.response());
        assertThat(countReservations()).isEqualTo(1);
    }

    /** 과거 시연 회차의 77석 조회 → 두 좌석 확정 → 재시도에서 실제 좌석 상태와 잔여석이 일치하는지 검증한다. */
    @Test
    void reservesDemoSeatsAndReplaysWithoutReducingAvailabilityAgain() throws IOException {
        prepareDemoLayout();
        ShowtimeSeatResponse preview = showtimeSeatService.getShowtimeSeats(fixture.showtimeId());
        assertThat(preview.startAt()).isEqualTo("2026-10-27T14:25:00+09:00");
        assertThat(preview.endAt()).isEqualTo("2026-10-27T16:14:00+09:00");
        assertThat(preview.totalSeatCount()).isEqualTo(77);
        assertThat(preview.availableSeatCount()).isEqualTo(77);
        assertThat(preview.seats()).allSatisfy(seat -> assertThat(seat.reserved()).isFalse());
        assertThat(preview.seats()).filteredOn(seat -> List.of("E4", "E5").contains(seat.seatCode()))
                .extracting(SeatInfo::seatCode, SeatInfo::rowIndex, SeatInfo::columnIndex)
                .containsExactly(tuple("E4", 4, 4), tuple("E5", 4, 5));
        // 같은 데이터를 미리보기와 좌석표에서 다시 읽어도 조회만으로 예매가 생기지 않는다.
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.showtimeId())).isEqualTo(preview);
        assertThat(countReservations()).isZero();

        UUID requestId = UUID.randomUUID();
        CreateReservationRequest request = request(requestId, List.of("E4", "E5"));
        ReservationResult created = service.createReservation(fixture.showtimeId(), request);
        assertThat(created.created()).isTrue();
        assertThat(created.response().seatCodes()).containsExactly("E4", "E5");
        assertThat(created.response().totalAmount()).isEqualTo(preview.ticketPrices().adult() * 2);

        ShowtimeSeatResponse confirmed = showtimeSeatService.getShowtimeSeats(fixture.showtimeId());
        assertThat(confirmed.totalSeatCount()).isEqualTo(77);
        assertThat(confirmed.availableSeatCount()).isEqualTo(75);
        assertThat(confirmed.seats()).filteredOn(SeatInfo::reserved).extracting(SeatInfo::seatCode)
                .containsExactly("E4", "E5");

        ReservationResult replayed = service.createReservation(
                fixture.showtimeId(), request(requestId, List.of("E5", "E4")));
        assertThat(replayed.created()).isFalse();
        assertThat(replayed.response()).isEqualTo(created.response());
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.showtimeId())).isEqualTo(confirmed);

        assertError(ErrorCode.RESERVATION_REQUEST_CONFLICT, fixture.showtimeId(), request(requestId, List.of("E6", "E7")));
        assertError(ErrorCode.SEAT_ALREADY_RESERVED, fixture.showtimeId(), request(UUID.randomUUID(), List.of("E4", "E5")));
        assertThat(countReservations()).isEqualTo(1);
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.showtimeId())).isEqualTo(confirmed);
        // 같은 물리 좌석이어도 다른 회차의 확정 상태에는 영향을 주지 않는다.
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.secondShowtimeId()).availableSeatCount()).isEqualTo(77);
    }

    /** 기본 테스트 좌석을 시더와 같은 77석 배치로 바꾸고 원본 시연 회차의 날짜·시각을 적용한다. */
    private void prepareDemoLayout() throws IOException {
        ClassPathResource layout = new ClassPathResource("seed/seats-12x15.csv");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(layout.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> positions = reader.lines().skip(1).toList();
            transaction.executeWithoutResult(status -> {
                jdbc.update("delete from seat where screen_id = ?", fixture.screenId());
                for (String position : positions) {
                    String[] fields = position.split(",");
                    jdbc.update("insert into seat (screen_id, seat_code, row_index, column_index) values (?, ?, ?, ?)",
                            fixture.screenId(), fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
                }
                jdbc.update("update showtime set start_at = ?, end_at = ? where id = ?",
                        LocalDateTime.of(2026, 10, 27, 14, 25), LocalDateTime.of(2026, 10, 27, 16, 14), fixture.showtimeId());
            });
        }
    }

    /** 뒤쪽 좌석에서 충돌하면 앞서 삽입된 좌석과 예매 행도 남지 않는지 확인한다. */
    @Test
    void rollsBackEntireReservationWhenOneSeatConflicts() {
        service.createReservation(fixture.showtimeId(), request(UUID.randomUUID(), List.of("A2")));
        assertError(ErrorCode.SEAT_ALREADY_RESERVED, fixture.showtimeId(), request(UUID.randomUUID(), List.of("A1", "A2")));
        assertThat(countReservations()).isEqualTo(1);
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.showtimeId()).availableSeatCount()).isEqualTo(7);
        assertThat(service.createReservation(fixture.showtimeId(), request(UUID.randomUUID(), List.of("A1"))).created()).isTrue();
    }

    /** 같은 UUID로 회차·좌석·인원·금액을 바꾸는 재사용을 거부한다. */
    @Test
    void rejectsChangedRequestWithSameUuid() {
        UUID id = UUID.randomUUID();
        CreateReservationRequest first = request(id, List.of("A1"));
        service.createReservation(fixture.showtimeId(), first);
        assertError(ErrorCode.RESERVATION_REQUEST_CONFLICT, fixture.secondShowtimeId(), first);
        assertError(ErrorCode.RESERVATION_REQUEST_CONFLICT, fixture.showtimeId(), request(id, List.of("A2")));
        assertError(ErrorCode.RESERVATION_REQUEST_CONFLICT, fixture.showtimeId(),
                new CreateReservationRequest(id, new TicketCounts(BigInteger.ZERO, BigInteger.ONE, BigInteger.ZERO,
                        BigInteger.ZERO), List.of("A1"), BigInteger.valueOf(15000)));
        assertError(ErrorCode.RESERVATION_REQUEST_CONFLICT, fixture.showtimeId(),
                new CreateReservationRequest(id, first.ticketCounts(), first.seatCodes(), BigInteger.ZERO));
    }

    /** 다른 회차에서는 같은 물리 좌석을 각각 예매할 수 있다. */
    @Test
    void reservesSamePhysicalSeatInDifferentShowtimes() {
        service.createReservation(fixture.showtimeId(), request(UUID.randomUUID(), List.of("A1")));
        service.createReservation(fixture.secondShowtimeId(), request(UUID.randomUUID(), List.of("A1")));
        assertThat(countReservations()).isEqualTo(2);
    }

    /** 총 8명과 유형별 공통 단가를 함께 적용해 서버 총액을 계산한다. */
    @Test
    void calculatesMixedTicketPricesForEightPeople() {
        TicketCounts counts = new TicketCounts(BigInteger.TWO, BigInteger.TWO, BigInteger.TWO, BigInteger.TWO);
        CreateReservationRequest request = new CreateReservationRequest(UUID.randomUUID(), counts,
                List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8"), BigInteger.valueOf(94000));
        assertThat(service.createReservation(fixture.showtimeId(), request).response().totalAmount()).isEqualTo(94000);
    }

    /** 없는 회차, 소속이 다른 좌석, 대소문자 불일치와 금액 불일치를 검사한다. */
    @Test
    void rejectsInvalidShowtimeSeatsAndAmount() {
        assertError(ErrorCode.NOT_FOUND, Long.MAX_VALUE, request(UUID.randomUUID(), List.of("A1")));
        assertError(ErrorCode.VALIDATION_FAILED, fixture.showtimeId(), request(UUID.randomUUID(), List.of("B1")));
        assertError(ErrorCode.VALIDATION_FAILED, fixture.showtimeId(), request(UUID.randomUUID(), List.of("a1")));
        CreateReservationRequest valid = request(UUID.randomUUID(), List.of("A1"));
        assertError(ErrorCode.RESERVATION_AMOUNT_CHANGED, fixture.showtimeId(),
                new CreateReservationRequest(valid.requestId(), valid.ticketCounts(), valid.seatCodes(), BigInteger.ZERO));
        assertThat(countReservations()).isZero();
    }

    /** 상영 전·시작 정각·진행 중·종료 후 모두 확정하고, 생성 시각에는 실제 시계를 사용하는지 확인한다. */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "상영 전, 2026-11-12T10:00:01, 2026-11-12T11:40:01",
            "시작 정각, 2026-11-12T10:00:00, 2026-11-12T11:40:00",
            "상영 중, 2026-11-12T09:59:59, 2026-11-12T11:39:59",
            "종료 후, 2026-10-27T14:25:00, 2026-10-27T16:14:00"
    })
    void reservesRegardlessOfShowtimeTime(String caseName, LocalDateTime startAt, LocalDateTime endAt) {
        jdbc.update("update showtime set start_at = ?, end_at = ? where id = ?",
                startAt, endAt, fixture.startingNowShowtimeId());

        ReservationResult result = service.createReservation(
                fixture.startingNowShowtimeId(), request(UUID.randomUUID(), List.of("A1")));

        assertThat(result.created()).isTrue();
        assertThat(result.response().createdAt()).isEqualTo("2026-11-12T10:00:00+09:00");
        assertThat(countReservations()).isEqualTo(1);
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.startingNowShowtimeId()).availableSeatCount()).isEqualTo(7);
    }

    /** 좌석 중복, 0명·9명, 인원과 좌석 수 불일치는 DB 저장 전에 검증한다. */
    @Test
    void rejectsInvalidSelectionsWithFieldDetails() {
        assertError(ErrorCode.VALIDATION_FAILED, fixture.showtimeId(), request(UUID.randomUUID(), List.of("A1", "A1")));
        assertError(ErrorCode.VALIDATION_FAILED, fixture.showtimeId(),
                new CreateReservationRequest(UUID.randomUUID(), new TicketCounts(BigInteger.ZERO, BigInteger.ZERO,
                        BigInteger.ZERO, BigInteger.ZERO), List.of("A1"), BigInteger.ZERO));
        assertError(ErrorCode.VALIDATION_FAILED, fixture.showtimeId(),
                new CreateReservationRequest(UUID.randomUUID(), new TicketCounts(BigInteger.valueOf(8), BigInteger.ONE,
                        BigInteger.ZERO, BigInteger.ZERO), List.of("A1"), BigInteger.ZERO));
        assertError(ErrorCode.VALIDATION_FAILED, fixture.showtimeId(),
                new CreateReservationRequest(UUID.randomUUID(), new TicketCounts(BigInteger.TWO, BigInteger.ZERO,
                        BigInteger.ZERO, BigInteger.ZERO), List.of("A1"), BigInteger.valueOf(30000)));
    }

    /** 동일 UUID의 동시 제출은 예매 한 건과 신규·재응답 결과 한 건씩으로 수렴한다. */
    @Test
    void handlesConcurrentIdenticalUuid() throws Exception {
        CreateReservationRequest request = request(UUID.randomUUID(), List.of("A1", "A2"));
        List<Attempt> results = runConcurrently(request, request);
        assertThat(results).allSatisfy(attempt -> assertThat(attempt.error()).isNull());
        assertThat(results).extracting(attempt -> attempt.result().created()).containsExactlyInAnyOrder(true, false);
        assertThat(results.get(0).result().response()).isEqualTo(results.get(1).result().response());
        assertThat(countReservations()).isEqualTo(1);
    }

    /** 서로 다른 UUID의 동시 좌석 점유는 한 건만 성공하고 실패 요청 전체를 롤백한다. */
    @Test
    void handlesConcurrentSeatConflict() throws Exception {
        List<Attempt> results = runConcurrently(request(UUID.randomUUID(), List.of("A1", "A2")),
                request(UUID.randomUUID(), List.of("A1", "A3")));
        assertThat(results.stream().filter(attempt -> attempt.result() != null).count()).isEqualTo(1);
        assertThat(results.stream().filter(attempt -> attempt.error() == ErrorCode.SEAT_ALREADY_RESERVED).count()).isEqualTo(1);
        assertThat(countReservations()).isEqualTo(1);
        assertThat(showtimeSeatService.getShowtimeSeats(fixture.showtimeId()).availableSeatCount()).isEqualTo(6);
    }

    /** 같은 UUID의 다른 내용이 동시에 제출되면 요청 ID 충돌로 분류한다. */
    @Test
    void handlesConcurrentChangedUuid() throws Exception {
        UUID id = UUID.randomUUID();
        List<Attempt> results = runConcurrently(request(id, List.of("A1")), request(id, List.of("A2")));
        assertThat(results.stream().filter(attempt -> attempt.result() != null).count()).isEqualTo(1);
        assertThat(results.stream().filter(attempt -> attempt.error() == ErrorCode.RESERVATION_REQUEST_CONFLICT).count()).isEqualTo(1);
        assertThat(countReservations()).isEqualTo(1);
    }

    /** 두 작업이 준비된 뒤 함께 시작하며 각 작업은 서비스의 독립 DB 트랜잭션을 사용한다. */
    private List<Attempt> runConcurrently(CreateReservationRequest first, CreateReservationRequest second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Attempt> firstResult = executor.submit(() -> attempt(first, ready, start));
            Future<Attempt> secondResult = executor.submit(() -> attempt(second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    /** 동시 테스트의 정상 결과와 예상 비즈니스 오류를 기록한다. 다른 예외는 테스트를 실패시킨다. */
    private Attempt attempt(CreateReservationRequest request, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 요청 시작 신호가 없습니다.");
        }
        try {
            return new Attempt(service.createReservation(fixture.showtimeId(), request), null);
        } catch (BusinessException exception) {
            return new Attempt(null, exception.getErrorCode());
        }
    }

    /** 선택 좌석 수만큼 성인 인원을 설정한 유효 요청을 만든다. */
    private CreateReservationRequest request(UUID requestId, List<String> seatCodes) {
        return new CreateReservationRequest(requestId, new TicketCounts(BigInteger.valueOf(seatCodes.size()),
                BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO), seatCodes, BigInteger.valueOf(15000L * seatCodes.size()));
    }

    /** 서비스 호출이 기대한 공통 도메인 오류 코드로 실패하는지 확인한다. */
    private void assertError(ErrorCode code, Long showtimeId, CreateReservationRequest request) {
        assertThatThrownBy(() -> service.createReservation(showtimeId, request)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }

    /** 테스트 회차에 커밋된 예매 행 수를 읽어 실패·재시도에서 중복 행이 없는지 확인한다. */
    private int countReservations() {
        return jdbc.queryForObject("select count(*) from reservation where showtime_id in (?, ?, ?)", Integer.class,
                fixture.showtimeId(), fixture.secondShowtimeId(), fixture.startingNowShowtimeId());
    }

    private record Fixture(Long regionId, Long cinemaId, Long movieId, Long screenId, Long otherScreenId,
                           Long showtimeId, Long secondShowtimeId, Long startingNowShowtimeId) {
    }

    private record Attempt(ReservationResult result, ErrorCode error) {
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        /** 상영 시각과 독립된 생성 시각을 반복 가능하게 검증할 고정 KST 시계를 제공한다. */
        @Bean
        @Primary
        Clock reservationClock() {
            return Clock.fixed(Instant.parse("2026-11-12T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}

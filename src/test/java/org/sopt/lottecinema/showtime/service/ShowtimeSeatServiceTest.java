package org.sopt.lottecinema.showtime.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.lottecinema.cinema.entity.Cinema;
import org.sopt.lottecinema.global.config.TicketPriceProperties;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.region.entity.Region;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.sopt.lottecinema.seat.repository.SeatStatus;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.dto.TicketPrices;
import org.sopt.lottecinema.showtime.entity.FormatType;
import org.sopt.lottecinema.showtime.entity.LanguageType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeSeatServiceTest {

    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private SeatRepository seatRepository;
    private ShowtimeSeatService service;

    private Screen screen;
    private Movie movie;

    @BeforeEach
    void setUp() {
        service = new ShowtimeSeatService(showtimeRepository, seatRepository,
                new TicketPriceProperties(15000, 12000, 10000, 10000));
        Region region = new Region("서울", (short) 0);
        Cinema cinema = new Cinema(region, "용산", (short) 0);
        screen = new Screen(cinema, "4관", SeatType.RECLINER);
        ReflectionTestUtils.setField(screen, "id", 3L);
        movie = new Movie("테스트 영화", null, "애니메이션", (short) 109, "ALL",
                LocalDate.of(2026, 11, 1), null, null, null, BigDecimal.ONE, BigDecimal.ONE);
    }

    @ParameterizedTest
    @EnumSource(FormatType.class)
    void returnsSeatCountsPricesAndKoreanOffsetEvenAfterShowtimeStarted(FormatType format) {
        prepareShowtime(format);
        when(seatRepository.findStatuses(3L, 1203L)).thenReturn(List.of(
                new SeatStatus("E1", 0, 0, false),
                new SeatStatus("E3", 0, 2, true),
                new SeatStatus("E4", 0, 3, false)));
        TicketPrices prices = new TicketPrices(15000, 12000, 10000, 10000);

        ShowtimeSeatResponse response = service.getShowtimeSeats(1203L);

        assertThat(response.startAt()).isEqualTo("2020-11-12T14:25:00+09:00");
        assertThat(response.endAt()).isEqualTo("2020-11-12T16:14:00+09:00");
        assertThat(response.formatType()).isEqualTo(format.getCode());
        assertThat(response.movie().posterUrl()).isNull();
        assertThat(response.movie().title()).isEqualTo("테스트 영화");
        assertThat(response.cinema().name()).isEqualTo("용산");
        assertThat(response.screen().seatType()).isEqualTo("RECLINER");
        assertThat(response.totalSeatCount()).isEqualTo(3);
        assertThat(response.availableSeatCount()).isEqualTo(2);
        assertThat(response.seats()).extracting("columnIndex").containsExactly(0, 2, 3);
        assertThat(response.ticketPrices()).isEqualTo(prices);
    }

    @Test
    void returnsSoldOutSeats() {
        prepareShowtime(FormatType.TWO_D);
        when(seatRepository.findStatuses(3L, 1203L)).thenReturn(List.of(new SeatStatus("A1", 0, 0, true)));
        assertThat(service.getShowtimeSeats(1203L).availableSeatCount()).isZero();
    }

    @Test
    void returnsZeroCountsForScreenWithoutSeats() {
        prepareShowtime(FormatType.TWO_D);
        when(seatRepository.findStatuses(3L, 1203L)).thenReturn(List.of());
        ShowtimeSeatResponse response = service.getShowtimeSeats(1203L);
        assertThat(response.availableSeatCount()).isZero();
        assertThat(response.totalSeatCount()).isZero();
        assertThat(response.seats()).isEmpty();
    }

    @Test
    void rejectsMissingShowtimeBeforeReadingSeatsOrPrices() {
        when(showtimeRepository.findWithDetailsById(1203L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getShowtimeSeats(1203L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verifyNoInteractions(seatRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidSeats")
    void rejectsIncompleteOrInvalidLegacySeatData(SeatStatus seat) {
        prepareShowtime(FormatType.TWO_D);
        when(seatRepository.findStatuses(3L, 1203L)).thenReturn(List.of(seat));
        assertThatThrownBy(() -> service.getShowtimeSeats(1203L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    static Stream<SeatStatus> invalidSeats() {
        return Stream.of(new SeatStatus("A1", null, 0, false), new SeatStatus("A1", 0, null, false),
                new SeatStatus("A1", -1, 0, false), new SeatStatus("A1", 0, -1, false),
                new SeatStatus(" ", 0, 0, false));
    }

    private void prepareShowtime(FormatType format) {
        Showtime showtime = new Showtime(movie, screen, LocalDateTime.of(2020, 11, 12, 14, 25),
                LocalDateTime.of(2020, 11, 12, 16, 14), format, LanguageType.NONE);
        when(showtimeRepository.findWithDetailsById(1203L)).thenReturn(Optional.of(showtime));
    }
}

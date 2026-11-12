package org.sopt.lottecinema.showtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.lottecinema.cinema.repository.CinemaRepository;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse.ShowtimeResponse;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository.ShowtimeRow;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceTest {

    @Mock
    private CinemaRepository cinemaRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private MovieRepository movieRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T16:00:00Z"), ZoneId.of("Asia/Seoul"));

    private ShowtimeService showtimeService;

    @BeforeEach
    void setUp() {
        showtimeService = new ShowtimeService(cinemaRepository, showtimeRepository, movieRepository, clock);
    }

    @Test
    @DisplayName("date가 없으면 Clock zone과 무관하게 KST 기준 오늘 하루 범위로 조회한다")
    void defaultsToTodayInKst() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of(row(1L, "2026-11-13T10:00", "2026-11-13T12:00", 100L, 0L)));

        showtimeService.getShowtimeLists(1L, 2L, null);

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(showtimeRepository).findShowtimesByMovieAndCinemaAndDate(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(2L), start.capture(), end.capture());
        assertThat(start.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 13, 0, 0));
        assertThat(end.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 14, 0, 0));
    }

    @Test
    @DisplayName("date가 있으면 해당 날짜 00:00 이상 ~ 다음 날 00:00 미만 범위로 조회한다")
    void usesGivenDateRange() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 27, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 28, 0, 0);
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(1L, 2L, start, end))
                .willReturn(List.of(row(1L, "2026-11-27T10:00", "2026-11-27T12:00", 100L, 0L)));

        ShowtimeListResponse response = showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27));

        assertThat(response.showtimes()).hasSize(1);
        verify(showtimeRepository).findShowtimesByMovieAndCinemaAndDate(1L, 2L, start, end);
    }

    @Test
    @DisplayName("잔여석은 전체 - 예매 좌석이고, 시각에는 KST(+09:00) 오프셋이 붙는다")
    void mapsRowToResponse() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of(row(10L, "2026-11-27T10:00", "2026-11-27T12:30", 100L, 37L)));

        ShowtimeResponse r = showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27))
                .showtimes().get(0);

        assertThat(r.showtimeId()).isEqualTo(10L);
        assertThat(r.screenName()).isEqualTo("1관");
        assertThat(r.formatType()).isEqualTo("2D");
        assertThat(r.languageType()).isEqualTo("SUBTITLED");
        assertThat(r.totalSeatCount()).isEqualTo(100);
        assertThat(r.reservedSeatCount()).isEqualTo(37);
        assertThat(r.availableSeatCount()).isEqualTo(63);
        assertThat(r.startAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(r.startAt().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 27, 10, 0));
        assertThat(r.endAt().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 27, 12, 30));
    }

    @Test
    @DisplayName("전 좌석이 예매되면 잔여석은 0이다(매진)")
    void soldOut() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of(row(1L, "2026-11-27T10:00", "2026-11-27T12:00", 100L, 100L)));

        ShowtimeResponse r = showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27))
                .showtimes().get(0);

        assertThat(r.availableSeatCount()).isZero();
    }

    @Test
    @DisplayName("회차가 있으면 영화/영화관 존재 여부를 조회하지 않는다")
    void doesNotCheckExistenceWhenRowsPresent() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of(row(1L, "2026-11-27T10:00", "2026-11-27T12:00", 100L, 0L)));

        showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27));

        verifyNoInteractions(movieRepository, cinemaRepository);
    }

    @Test
    @DisplayName("회차가 없어도 영화와 영화관이 존재하면 빈 목록을 반환한다")
    void emptyListWhenMovieAndCinemaExist() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of());
        given(movieRepository.existsById(1L)).willReturn(true);
        given(cinemaRepository.existsById(2L)).willReturn(true);

        ShowtimeListResponse response = showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27));

        assertThat(response.showtimes()).isEmpty();
    }

    @Test
    @DisplayName("회차가 없고 영화가 없으면 NOT_FOUND(영화) — 영화관은 확인하지 않는다")
    void notFoundWhenMovieMissing() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of());
        given(movieRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    assertThat(e.getMessage()).isEqualTo("해당 영화가 존재하지 않습니다.");
                });
        verifyNoInteractions(cinemaRepository);
    }

    @Test
    @DisplayName("회차가 없고 영화관이 없으면 NOT_FOUND(영화관)")
    void notFoundWhenCinemaMissing() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of());
        given(movieRepository.existsById(1L)).willReturn(true);
        given(cinemaRepository.existsById(2L)).willReturn(false);

        assertThatThrownBy(() -> showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    assertThat(e.getMessage()).isEqualTo("해당 영화관이 존재하지 않습니다.");
                });
    }

    @Test
    @DisplayName("조회된 순서(start_at 오름차순)를 그대로 유지한다")
    void keepsRepositoryOrder() {
        given(showtimeRepository.findShowtimesByMovieAndCinemaAndDate(any(), any(), any(), any()))
                .willReturn(List.of(
                        row(1L, "2026-11-27T09:00", "2026-11-27T11:00", 100L, 0L),
                        row(2L, "2026-11-27T13:00", "2026-11-27T15:00", 100L, 0L),
                        row(3L, "2026-11-27T19:00", "2026-11-27T21:00", 100L, 0L)));

        ShowtimeListResponse response = showtimeService.getShowtimeLists(1L, 2L, LocalDate.of(2026, 11, 27));

        assertThat(response.showtimes()).extracting(ShowtimeResponse::showtimeId).containsExactly(1L, 2L, 3L);
    }

    // ---- helper ----
    private static ShowtimeRow row(Long id, String startAt, String endAt, Long total, Long reserved) {
        return new StubRow(id, LocalDateTime.parse(startAt), LocalDateTime.parse(endAt), total, reserved);
    }

    private record StubRow(Long id, LocalDateTime start, LocalDateTime end, Long total, Long reserved)
            implements ShowtimeRow {
        @Override public Long getShowtimeId() { return id; }
        @Override public LocalDateTime getStartAt() { return start; }
        @Override public LocalDateTime getEndAt() { return end; }
        @Override public String getScreenName() { return "1관"; }
        @Override public String getFormatType() { return "2D"; }
        @Override public String getLanguageType() { return "SUBTITLED"; }
        @Override public Long getTotalSeatCount() { return total; }
        @Override public Long getReservedSeatCount() { return reserved; }
    }
}


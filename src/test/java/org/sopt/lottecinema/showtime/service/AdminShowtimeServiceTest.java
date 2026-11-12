package org.sopt.lottecinema.showtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.repository.ScreenRepository;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeRequest;
import org.sopt.lottecinema.showtime.entity.FormatType;
import org.sopt.lottecinema.showtime.entity.LanguageType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminShowtimeServiceTest {
    @Mock MovieRepository movies;
    @Mock ScreenRepository screens;
    @Mock ShowtimeRepository showtimes;
    @Mock Movie movie;
    @Mock Screen screen;
    AdminShowtimeService service;
    LocalDateTime start = LocalDateTime.of(2026, 11, 20, 14, 0);

    @BeforeEach
    void setUp() { service = new AdminShowtimeService(movies, screens, showtimes); }

    private CreateShowtimeRequest request(LocalDateTime end) {
        return new CreateShowtimeRequest(1L, 2L, start, end, "2D", LanguageType.SUBTITLED);
    }

    @Test
    void rejectsEndAtOrBeforeStart() {
        for (LocalDateTime end : new LocalDateTime[] {start, start.minusMinutes(1)}) {
            assertThatThrownBy(() -> service.createShowtime(request(end)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SHOWTIME_RANGE));
        }
        verifyNoInteractions(movies, screens, showtimes);
    }

    @Test
    void rejectsMissingMovieAndScreen() {
        when(movies.findById(1L)).thenReturn(Optional.empty(), Optional.of(movie));
        when(screens.findById(2L)).thenReturn(Optional.empty());
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.createShowtime(request(start.plusHours(2))))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
        verifyNoInteractions(showtimes);
    }

    @Test
    void rejectsOverlappingShowtime() {
        when(movies.findById(1L)).thenReturn(Optional.of(movie));
        when(screens.findById(2L)).thenReturn(Optional.of(screen));
        when(showtimes.existsByScreenIdAndStartAtLessThanAndEndAtGreaterThan(2L, start.plusHours(2), start)).thenReturn(true);
        assertThatThrownBy(() -> service.createShowtime(request(start.plusHours(2))))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHOWTIME_CONFLICT));
        verify(showtimes, never()).save(any());
    }

    @Test
    void savesAndReturnsId() {
        when(movies.findById(1L)).thenReturn(Optional.of(movie));
        when(screens.findById(2L)).thenReturn(Optional.of(screen));
        when(showtimes.save(any())).thenAnswer(invocation -> {
            Showtime saved = invocation.getArgument(0);
            assertThat(saved.getFormatType()).isEqualTo(FormatType.TWO_D);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });
        assertThat(service.createShowtime(request(start.plusHours(2))).showtimeId()).isEqualTo(10L);
    }
}

package org.sopt.lottecinema.showtime.service;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.cinema.repository.CinemaRepository;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse.ShowtimeResponse;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository.ShowtimeRow;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowtimeService {
    private final CinemaRepository cinemaRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final Clock clock;

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    public ShowtimeListResponse getShowtimeLists(Long movieId, Long cinemaId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(clock);

        // 해당 날짜의 시작 시각부터 다음 날 시작 직전까지 조회함
        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        List<ShowtimeRepository.ShowtimeRow> rows = showtimeRepository.findShowtimesByMovieAndCinemaAndDate(movieId, cinemaId, startAt, endAt);

        // 상영 회차가 없을 때 영화/영화관 존재 여부를 확인함
        if (rows.isEmpty()) {
            if (!movieRepository.existsById(movieId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "해당 영화가 존재하지 않습니다.");
            } else if (!cinemaRepository.existsById(cinemaId)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "해당 영화관이 존재하지 않습니다.");}}
        List<ShowtimeListResponse.ShowtimeResponse> showtimes = rows.stream()
                .map(this::toResponse)
                .toList();

        return new ShowtimeListResponse(showtimes);
    }

    // dto 응답용
    private ShowtimeResponse toResponse(ShowtimeRow row) {
        Integer totalSeatCount = Math.toIntExact(row.getTotalSeatCount());
        Integer reservedSeatCount = Math.toIntExact(row.getReservedSeatCount());
        Integer availableSeatCount = totalSeatCount - reservedSeatCount;

        return new ShowtimeResponse(
                row.getShowtimeId(),
                row.getStartAt().atOffset(KST_OFFSET),
                row.getEndAt().atOffset(KST_OFFSET),
                row.getScreenName(),
                row.getSeatType(),
                row.getFormatType(),
                row.getLanguageType(),
                totalSeatCount,
                reservedSeatCount,
                availableSeatCount
        );
    }
}

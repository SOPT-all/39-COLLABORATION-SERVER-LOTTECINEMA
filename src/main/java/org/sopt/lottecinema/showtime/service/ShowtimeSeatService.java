package org.sopt.lottecinema.showtime.service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.lottecinema.global.config.TicketPriceProperties;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.sopt.lottecinema.seat.repository.SeatStatus;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.CinemaInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.MovieInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.ScreenInfo;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse.SeatInfo;
import org.sopt.lottecinema.showtime.dto.TicketPrices;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeSeatService {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketPriceProperties ticketPriceProperties;

    /** 회차와 좌석을 동일 스냅샷에서 읽고, 공통 단가 설정을 함께 응답한다. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ShowtimeSeatResponse getShowtimeSeats(Long showtimeId) {
        Showtime showtime = showtimeRepository.findWithDetailsById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 상영 회차를 찾을 수 없습니다."));
        Screen screen = showtime.getScreen();
        Movie movie = showtime.getMovie();
        List<SeatStatus> statuses = seatRepository.findStatuses(screen.getId(), showtimeId);
        validateLayout(screen.getId(), statuses);
        List<SeatInfo> seats = statuses.stream()
                .map(seat -> new SeatInfo(seat.seatCode(), seat.rowIndex(), seat.columnIndex(),
                        seat.reserved()))
                .toList();

        // 같은 좌석 목록에서 집계하므로 동시 예매가 발생해도 목록과 잔여석 수가 서로 어긋나지 않는다.
        int availableSeatCount = (int) statuses.stream().filter(seat -> !seat.reserved()).count();
        // 단가는 모든 회차가 공유한다. 예매에서도 같은 설정으로 총액을 계산한다.
        TicketPrices prices = new TicketPrices(ticketPriceProperties.adult(), ticketPriceProperties.teen(),
                ticketPriceProperties.senior(), ticketPriceProperties.disabled());
        return new ShowtimeSeatResponse(
                showtime.getStartAt().atOffset(KST_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                showtime.getEndAt().atOffset(KST_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                showtime.getFormatType().getCode(),
                new MovieInfo(movie.getTitle(), movie.getPosterUrl(), movie.getAgeRating()),
                new CinemaInfo(screen.getCinema().getName()),
                new ScreenInfo(screen.getName(), screen.getSeatType().name()),
                availableSeatCount, seats.size(), seats, prices);
    }

    private void validateLayout(Long screenId, List<SeatStatus> seats) {
        // 기존 DB에서 좌표 백필이 끝나지 않았다면 임의의 위치로 노출하지 않고 운영자가 식별하게 한다.
        for (SeatStatus seat : seats) {
            if (seat.rowIndex() == null || seat.columnIndex() == null || seat.rowIndex() < 0 || seat.columnIndex() < 0
                    || seat.seatCode() == null || seat.seatCode().isBlank() || seat.seatCode().length() > 10) {
                log.error("상영관 좌석 배치가 올바르지 않습니다. screenId={}, seatCode={}", screenId, seat.seatCode());
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }
    }
}

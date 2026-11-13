package org.sopt.lottecinema.showtime.service;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.repository.ScreenRepository;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeRequest;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeResponse;
import org.sopt.lottecinema.showtime.entity.FormatType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminShowtimeService {
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ShowtimeRepository showtimeRepository;

    @Transactional
    public CreateShowtimeResponse createShowtime(CreateShowtimeRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_SHOWTIME_RANGE);
        }
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "영화를 찾을 수 없습니다."));
        Screen screen = screenRepository.findById(request.screenId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상영관을 찾을 수 없습니다."));
        if (showtimeRepository.existsByScreenIdAndStartAtLessThanAndEndAtGreaterThan(
                request.screenId(), request.endAt(), request.startAt())) {
            throw new BusinessException(ErrorCode.SHOWTIME_CONFLICT);
        }
        FormatType format = switch (request.formatType()) {
            case "2D" -> FormatType.TWO_D;
            case "3D" -> FormatType.THREE_D;
            case "4DX" -> FormatType.FOUR_DX;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST);
        };
        Showtime showtime = showtimeRepository.save(new Showtime(
                movie, screen, request.startAt(), request.endAt(), format, request.languageType()));
        return new CreateShowtimeResponse(showtime.getId());
    }
}

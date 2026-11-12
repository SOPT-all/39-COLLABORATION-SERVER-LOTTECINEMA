package org.sopt.lottecinema.showtime.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeListResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.service.ShowtimeSeatService;
import org.sopt.lottecinema.showtime.service.ShowtimeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController implements ShowtimeApi {

    private final ShowtimeSeatService showtimeSeatService;
    private final ShowtimeService showtimeService;

    /** 인터페이스의 양수 제약으로 경로를 검증한 뒤 서비스 결과를 공통 200 응답으로 감싼다. */
    @Override
    @GetMapping(value = "/{showtimeId}/seats", produces = MediaType.APPLICATION_JSON_VALUE)
    public SuccessResponse<ShowtimeSeatResponse> getShowtimeSeats(
            @PathVariable("showtimeId") Long showtimeId
    ) {
        ShowtimeSeatResponse response = showtimeSeatService.getShowtimeSeats(showtimeId);
        return SuccessResponse.success(response);
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public SuccessResponse<ShowtimeListResponse> getShowtimeLists(
            @RequestParam("movieId") Long movieId,
            @RequestParam("cinemaId") Long cinemaId,
            @RequestParam(value ="date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ){
        ShowtimeListResponse response = showtimeService.getShowtimeLists(movieId, cinemaId, date);
        return SuccessResponse.success(response);
    }
}

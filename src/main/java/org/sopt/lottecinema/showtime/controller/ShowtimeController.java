package org.sopt.lottecinema.showtime.controller;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.ShowtimeSeatResponse;
import org.sopt.lottecinema.showtime.service.ShowtimeSeatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController implements ShowtimeApi {

    private final ShowtimeSeatService showtimeSeatService;

    /** 인터페이스의 양수 제약으로 경로를 검증한 뒤 서비스 결과를 공통 200 응답으로 감싼다. */
    @Override
    @GetMapping(value = "/{showtimeId}/seats", produces = MediaType.APPLICATION_JSON_VALUE)
    public SuccessResponse<ShowtimeSeatResponse> getShowtimeSeats(
            @PathVariable("showtimeId") Long showtimeId
    ) {
        ShowtimeSeatResponse response = showtimeSeatService.getShowtimeSeats(showtimeId);
        return SuccessResponse.success(response);
    }
}

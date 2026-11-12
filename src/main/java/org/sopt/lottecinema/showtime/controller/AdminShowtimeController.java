package org.sopt.lottecinema.showtime.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.common.SuccessResponse;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeRequest;
import org.sopt.lottecinema.showtime.dto.CreateShowtimeResponse;
import org.sopt.lottecinema.showtime.service.AdminShowtimeService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("admin-api")
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/showtimes")
public class AdminShowtimeController implements AdminShowtimeApi {
    private final AdminShowtimeService adminShowtimeService;

    @Override
    @PostMapping
    public ResponseEntity<SuccessResponse<CreateShowtimeResponse>> createShowtime(@Valid @RequestBody CreateShowtimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.created(adminShowtimeService.createShowtime(request)));
    }
}

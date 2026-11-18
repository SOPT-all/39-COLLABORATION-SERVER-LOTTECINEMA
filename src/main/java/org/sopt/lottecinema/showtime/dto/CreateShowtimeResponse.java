package org.sopt.lottecinema.showtime.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"showtimeId"})
public record CreateShowtimeResponse(Long showtimeId) { }

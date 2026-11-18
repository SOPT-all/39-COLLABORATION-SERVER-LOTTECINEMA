package org.sopt.lottecinema.movie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// 영화별 지역 및 영화관 목록의 최상위 응답
@Schema(requiredProperties = {"myCinemas", "regions"})
public record MovieCinemaResponse(
        List<MyCinemaResponse> myCinemas,
        List<RegionResponse> regions
) {
    @Schema(requiredProperties = {"cinemaId", "cinemaName"})
    public record MyCinemaResponse(
            Long cinemaId,
            String cinemaName
    ) {}

    @Schema(requiredProperties = {"regionId", "regionName", "cinemas"})
    public record RegionResponse(
            Long regionId,
            String regionName,
            List <CinemaResponse> cinemas
    ){}

    @Schema(requiredProperties = {"cinemaId", "cinemaName", "hasShowtimes"})
    public record CinemaResponse(
            Long cinemaId,
            String cinemaName,
            boolean hasShowtimes
    ){}
}

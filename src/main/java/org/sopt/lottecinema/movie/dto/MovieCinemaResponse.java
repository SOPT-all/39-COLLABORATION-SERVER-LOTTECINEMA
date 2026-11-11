package org.sopt.lottecinema.movie.dto;

import java.util.List;

// 영화별 지역 및 영화관 목록의 최상위 응답
public record MovieCinemaResponse(
        List<RegionResponse> regions
) {
    public record RegionResponse(
            Long regionId,
            String regionName,
            List <CinemaResponse> cinemas
    ){}

    public record CinemaResponse(
            Long cinemaId,
            String cinemaName
    ){}
}

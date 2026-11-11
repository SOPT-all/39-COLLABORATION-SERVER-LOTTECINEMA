package org.sopt.lottecinema.showtime.dto;

import java.util.List;

// 영화별 지역 및 영화관 목록의 최상위 응답
public record MovieCinemaResponse(
        List<RegionReponse> regions
) {
    public record RegionReponse(
            Long regionId,
            String regionName,
            List <CinemaReponse> cinemas
    ){}

    public record CinemaReponse(
            Long cinemaId,
            String cinemaName
    ){}
}

package org.sopt.lottecinema.movie.service;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository.MovieCinemaRow;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieShowtimeService {
    private final MovieShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;

    private static class RegionBuilder{
        private final Long regionId;
        private final String regionName;
        private final Map<Long, MovieCinemaResponse.CinemaResponse> cinemas =
                new LinkedHashMap<>();
        private RegionBuilder(Long regionId, String regionName){
            this.regionId = regionId;
            this.regionName = regionName;
        }
        private  MovieCinemaResponse.RegionResponse toResponse(){
            return new MovieCinemaResponse.RegionResponse(
                    regionId,
                    regionName,
                    List.copyOf(cinemas.values())
            );
        }
    }

    public MovieCinemaResponse getCinemaByMovieId(Long movieId) {
        List<MovieCinemaRow> rows = showtimeRepository.findCinemasByMovieId(movieId);

        if (rows.isEmpty() && !movieRepository.existsById(movieId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        Map<Long, RegionBuilder> regions = new LinkedHashMap<>();
        for(MovieCinemaRow row : rows){
            RegionBuilder region = regions.computeIfAbsent(
                    row.getRegionId(),
                    id -> new RegionBuilder(row.getRegionId(), row.getRegionName())
            );
            region.cinemas.putIfAbsent(
                    row.getCinemaId(),
                    new MovieCinemaResponse.CinemaResponse(
                            row.getCinemaId(),
                            row.getCinemaName()
                    )
            );
        }

        List<MovieCinemaResponse.RegionResponse> regionReponses =
                regions.values().stream()
                        .map(RegionBuilder::toResponse)
                        .toList();
        return new MovieCinemaResponse(regionReponses);
    }

}

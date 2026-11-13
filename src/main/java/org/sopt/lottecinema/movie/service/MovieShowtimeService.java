package org.sopt.lottecinema.movie.service;

import lombok.RequiredArgsConstructor;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse.RegionResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse.CinemaResponse;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository.RegionCinemaRow;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieShowtimeService {
    private final MovieShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;

    public MovieCinemaResponse getCinemaByMovieId(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "영화가 존재하지 않습니다.");
        }
        Set<Long> screeningCinemaIds = showtimeRepository.findCinemaIdsWithShowtimes(movieId);

        Map<Long, String> regionNames = new LinkedHashMap<>();  // 순서 지키기 위해 LinkedHashMap 사용
        Map<Long, List<CinemaResponse>> cinemasByRegion = new HashMap<>();

        for(RegionCinemaRow row: showtimeRepository.findAllRegionsAndCinemas()){
            regionNames.putIfAbsent(row.getRegionId(), row.getRegionName());
            List<CinemaResponse> cinemas = cinemasByRegion.computeIfAbsent(row.getRegionId(), k -> new ArrayList<>());

            if(row.getCinemaId() != null) {
                cinemas.add(new CinemaResponse(
                        row.getCinemaId(),
                        row.getCinemaName(),
                        screeningCinemaIds.contains(row.getCinemaId())
                ));
            }
        }
        List<RegionResponse> regions = regionNames.entrySet().stream()
                .map(e -> new RegionResponse(
                        e.getKey(), e.getValue(), List.copyOf(cinemasByRegion.get(e.getKey()))))
                .toList();
        return new MovieCinemaResponse(regions);
    }
}

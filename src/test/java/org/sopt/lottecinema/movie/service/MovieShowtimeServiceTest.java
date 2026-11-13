package org.sopt.lottecinema.movie.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse.CinemaResponse;
import org.sopt.lottecinema.movie.dto.MovieCinemaResponse.RegionResponse;
import org.sopt.lottecinema.movie.repository.MovieRepository;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository.RegionCinemaRow;

class MovieShowtimeServiceTest {

    private final MovieShowtimeRepository showtimes = mock(MovieShowtimeRepository.class);
    private final MovieRepository movies = mock(MovieRepository.class);
    private final MovieShowtimeService service = new MovieShowtimeService(showtimes, movies);

    // 영화별 지역 및 영화관 조회 테스트
    // 지역별 그룹핑, 영화관 없는 지역 포함, 상영 영화관 ID 집합 기준으로 hasShowtimes 가 계산되는지 테스트
    @Test
    void groupsCinemasByRegionAndMarksShowtimesFromCinemaIdSet() {
        when(movies.existsById(7L)).thenReturn(true);
        // 용산(12)만 해당 영화를 상영한다.
        when(showtimes.findCinemaIdsWithShowtimes(7L)).thenReturn(Set.of(12L));
        when(showtimes.findAllRegionsAndCinemas()).thenReturn(List.of(
                new Row(2L, "서울", 12L, "용산"),
                new Row(2L, "서울", 11L, "건대입구"),
                new Row(1L, "경기", null, null)));

        assertThat(service.getCinemaByMovieId(7L).regions()).containsExactly(
                new RegionResponse(2L, "서울", List.of(new CinemaResponse(12L, "용산", true),
                        new CinemaResponse(11L, "건대입구", false))),
                new RegionResponse(1L, "경기", List.of()));
    }

    // 해당 영화의 상영 영화관이 하나도 없으면 모든 영화관이 hasShowtimes = false
    @Test
    void marksAllCinemasFalseWhenNoCinemaScreensMovie() {
        when(movies.existsById(7L)).thenReturn(true);
        when(showtimes.findCinemaIdsWithShowtimes(7L)).thenReturn(Set.of());
        when(showtimes.findAllRegionsAndCinemas()).thenReturn(List.of(
                new Row(1L, "서울", 11L, "건대입구"),
                new Row(1L, "서울", 12L, "용산")));

        assertThat(service.getCinemaByMovieId(7L).regions()).containsExactly(
                new RegionResponse(1L, "서울", List.of(new CinemaResponse(11L, "건대입구", false),
                        new CinemaResponse(12L, "용산", false))));
    }

    // 영화는 존재하지만 조회한 지역이 0인 경우 테스트
    @Test
    void returnsEmptyListWhenNoRegionsExist() {
        when(movies.existsById(7L)).thenReturn(true);
        when(showtimes.findCinemaIdsWithShowtimes(7L)).thenReturn(Set.of());
        when(showtimes.findAllRegionsAndCinemas()).thenReturn(List.of());

        assertThat(service.getCinemaByMovieId(7L).regions()).isEmpty();
    }

    // 특정 영화가 존재하지 않을 경우 테스트 -> 예외 처리가 잘 진행된다.
    // 영화 존재 확인에서 먼저 실패하므로 상영 정보 저장소는 전혀 호출되지 않아야 한다.
    @Test
    void rejectsUnknownMovieBeforeLoadingCinemas() {
        when(movies.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.getCinemaByMovieId(7L))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verifyNoInteractions(showtimes);
    }

    // 프로젝션 인터페이스(RegionCinemaRow)를 record 로 구현한 테스트용 row
    private record Row(Long getRegionId, String getRegionName, Long getCinemaId,
                       String getCinemaName) implements RegionCinemaRow {}
}
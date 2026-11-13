package org.sopt.lottecinema.movie.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sopt.lottecinema.cinema.entity.Cinema;
import org.sopt.lottecinema.movie.entity.Movie;
import org.sopt.lottecinema.movie.repository.MovieShowtimeRepository.RegionCinemaRow;
import org.sopt.lottecinema.region.entity.Region;
import org.sopt.lottecinema.screen.entity.Screen;
import org.sopt.lottecinema.screen.entity.SeatType;
import org.sopt.lottecinema.showtime.entity.FormatType;
import org.sopt.lottecinema.showtime.entity.LanguageType;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// Spring이 설정된 DB에 연결한 뒤 실제 저장소 쿼리를 실행한다. 각 테스트에서 저장한 데이터는 종료 시 롤백된다.
@SpringBootTest
@Transactional
class MovieShowtimeRepositoryTest {

    private static final String PREFIX = "영화관 조회 테스트 ";
    // 지난 회차도 상영 이력에 포함되어야 함
    private static final LocalDateTime START = LocalDateTime.of(2000, 1, 1, 10, 0);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MovieShowtimeRepository repository;

    // 요청한 영화를 상영하는 영화관 ID 집합만 반환하는지 테스트
    @Test
    void returnsOnlyCinemaIdsThatScreenRequestedMovieWithoutDuplicates() {
        Movie target = movie("조회 대상");
        Movie other = movie("다른 영화");
        Region region = region(PREFIX + "회차", 1);
        Cinema scheduled = cinema(region, "다회차", 1);
        Cinema otherOnly = cinema(region, "다른 영화만 상영", 2);
        Cinema unscheduled = cinema(region, "회차 없음", 3);

        // 특정 영화가 같은 영화관의 1관에 두 회차, 2관에 한 회차 존재 -> distinct 로 영화관 ID 는 한 번만 나와야 한다.
        Screen first = screen(scheduled, "1관");
        showtime(target, first, START);
        showtime(target, first, START.plusHours(3));
        showtime(target, screen(scheduled, "2관"), START);
        showtime(other, screen(otherOnly, "1관"), START);
        screen(unscheduled, "1관");

        // 대상 영화는 새로 만든 영화라 시드 데이터와 섞이지 않으므로 정확히 한 개만 나와야 한다.
        assertThat(repository.findCinemaIdsWithShowtimes(target.getId()))
                .containsExactly(scheduled.getId());
    }

    // 회차가 하나도 없는 영화는 빈 집합을 반환해야 한다. (모든 영화관이 hasShowtimes = false 가 되는 기반)
    @Test
    void returnsEmptySetWhenMovieHasNoShowtimes() {
        Movie movie = movie("회차 없는 영화");
        Region region = region(PREFIX + "빈 집합", 1);
        screen(cinema(region, "영화관", 1), "1관");

        assertThat(repository.findCinemaIdsWithShowtimes(movie.getId())).isEmpty();
    }

    // 영화와 무관하게 전체 지역/영화관을 정렬해 반환하는지 테스트
    @Test
    void includesEmptyRegionsAndOrdersByDisplayOrderThenId() {
        // 저장 순서와 상관없이 displayOrder대로 조회된다.
        Region last = region(PREFIX + "후순위", 2);
        Region first = region(PREFIX + "우선", 1);
        Region tied = region(PREFIX + "동순위", 1);
        Cinema later = cinema(first, "후순위", 2);
        Cinema earlier = cinema(first, "우선", 1);
        Cinema sameOrder = cinema(first, "동순위", 1);
        Set<Long> ids = Set.of(first.getId(), tied.getId(), last.getId());

        // 영화관이 없는 지역(tied, last)은 cinemaId 가 null 인 행으로 내려와야 한다.
        assertThat(repository.findAllRegionsAndCinemas())
                .filteredOn(row -> ids.contains(row.getRegionId()))
                .extracting(RegionCinemaRow::getRegionId, RegionCinemaRow::getCinemaId)
                .containsExactly(
                        tuple(first.getId(), earlier.getId()),
                        tuple(first.getId(), sameOrder.getId()),
                        tuple(first.getId(), later.getId()),
                        tuple(tied.getId(), null),
                        tuple(last.getId(), null));
    }

    private Movie movie(String title) {
        Movie movie = new Movie(title, null, "드라마", (short) 100, "전체",
                LocalDate.of(2026, 11, 1), null, null, null, null, null);
        entityManager.persist(movie);
        return movie;
    }

    private Region region(String name, int order) {
        Region region = new Region(name, (short) order);
        entityManager.persist(region);
        return region;
    }

    private Cinema cinema(Region region, String name, int order) {
        Cinema cinema = new Cinema(region, name, (short) order);
        entityManager.persist(cinema);
        return cinema;
    }

    private Screen screen(Cinema cinema, String name) {
        Screen screen = new Screen(cinema, name, SeatType.STANDARD);
        entityManager.persist(screen);
        return screen;
    }

    private void showtime(Movie movie, Screen screen, LocalDateTime start) {
        entityManager.persist(new Showtime(movie, screen, start, start.plusHours(2),
                FormatType.TWO_D, LanguageType.NONE));
    }
}
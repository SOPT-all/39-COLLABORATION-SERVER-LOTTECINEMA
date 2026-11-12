package org.sopt.lottecinema.movie.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sopt.lottecinema.movie.entity.CurationType;
import org.sopt.lottecinema.movie.entity.Movie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void findsMoviesInRankAndCurationOrderWithIdAsTieBreaker() {
        Movie later = save(CurationType.LOTSI_PICK, (short) 2, (short) 2);
        Movie first = save(CurationType.LOTSI_PICK, (short) 1, (short) 1);
        Movie tied = save(CurationType.LOTSI_PLAY, (short) 1, (short) 1);
        Movie missingOrder = save(CurationType.LOTSI_PICK, null, null);
        Movie noCuration = save(null, null, null);
        movieRepository.flush();

        Set<Long> testIds = Set.of(later.getId(), first.getId(), tied.getId(), missingOrder.getId(), noCuration.getId());
        List<Long> popularIds = movieRepository.findByPopularityRankIsNotNullOrderByPopularityRankAscIdAsc().stream()
                .map(Movie::getId).filter(testIds::contains).toList();
        List<Long> curatedIds = movieRepository
                .findByCurationTypeIsNotNullAndCurationOrderIsNotNullOrderByCurationOrderAscIdAsc().stream()
                .map(Movie::getId).filter(testIds::contains).toList();

        assertThat(popularIds).containsExactly(first.getId(), tied.getId(), later.getId());
        assertThat(curatedIds).containsExactly(first.getId(), tied.getId(), later.getId());
    }

    private Movie save(CurationType type, Short order, Short rank) {
        return movieRepository.save(new Movie("조회 테스트", null, "드라마", (short) 100, "전체",
                LocalDate.of(2026, 11, 1), type, order, rank, null, null));
    }
}

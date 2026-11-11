package org.sopt.lottecinema.screen.repository;

import java.util.List;
import org.sopt.lottecinema.screen.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    /** 모든 상영관을 ID 순으로 조회한다. */
    List<Screen> findAllByOrderByIdAsc();
}

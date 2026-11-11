package org.sopt.lottecinema.screen.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.sopt.lottecinema.screen.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    // 여러 서버가 동시에 시딩해도 같은 상영관의 좌석 초기화는 순서대로 진행한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select screen from Screen screen where screen.id = :screenId")
    Optional<Screen> findByIdForUpdate(@Param("screenId") Long screenId);
}

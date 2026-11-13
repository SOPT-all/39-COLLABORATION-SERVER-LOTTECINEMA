package org.sopt.lottecinema.movie.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MovieListDateProvider {

    private final Clock clock;
    private final LocalDate referenceDate;

    public MovieListDateProvider(Clock clock, @Value("${movie.list.reference-date:}") String referenceDate) {
        this.clock = clock.withZone(ZoneId.of("Asia/Seoul"));
        this.referenceDate = referenceDate.isBlank() ? null : LocalDate.parse(referenceDate);
    }

    /** 영화 목록의 개봉 여부 판단에 사용할 날짜를 반환한다. 공통 시계는 변경하지 않는다. */
    public LocalDate getReferenceDate() {
        return referenceDate != null ? referenceDate : LocalDate.now(clock);
    }
}

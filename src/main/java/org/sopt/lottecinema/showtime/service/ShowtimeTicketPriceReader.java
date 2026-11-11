package org.sopt.lottecinema.showtime.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.showtime.dto.TicketPrices;
import org.sopt.lottecinema.showtime.repository.ShowtimeTicketPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 좌석 조회와 예매 생성에서 재사용하는 회차별 단가 조회다. 호출자의 트랜잭션에 참여한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowtimeTicketPriceReader {

    private final ShowtimeTicketPriceRepository priceRepository;

    public TicketPrices getTicketPrices(Long showtimeId) {
        var price = priceRepository.findByShowtimeId(showtimeId).orElseThrow(() -> {
            log.error("회차 단가가 등록되지 않았습니다. showtimeId={}", showtimeId);
            return new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        });
        return new TicketPrices(price.getAdultPrice(), price.getTeenPrice(), price.getSeniorPrice(), price.getDisabledPrice());
    }
}

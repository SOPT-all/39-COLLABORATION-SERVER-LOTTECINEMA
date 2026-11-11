package org.sopt.lottecinema.reservation.service;

import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.sopt.lottecinema.global.common.ValidationError;
import org.sopt.lottecinema.global.config.TicketPriceProperties;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import org.sopt.lottecinema.reservation.dto.CreateReservationRequest;
import org.sopt.lottecinema.reservation.dto.ReservationResponse;
import org.sopt.lottecinema.reservation.dto.ReservationResult;
import org.sopt.lottecinema.reservation.dto.TicketCounts;
import org.sopt.lottecinema.reservation.entity.Reservation;
import org.sopt.lottecinema.reservation.entity.ReservationSeat;
import org.sopt.lottecinema.reservation.repository.ReservationRepository;
import org.sopt.lottecinema.reservation.repository.ReservationSeatRepository;
import org.sopt.lottecinema.seat.entity.Seat;
import org.sopt.lottecinema.seat.repository.SeatRepository;
import org.sopt.lottecinema.showtime.entity.Showtime;
import org.sopt.lottecinema.showtime.repository.ShowtimeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketPriceProperties prices;
    private final Clock clock;
    private final TransactionTemplate transaction;

    /** 저장 실패 후 끝난 트랜잭션을 재사용하지 않도록 독립 트랜잭션 실행기를 준비한다. */
    public ReservationService(ReservationRepository reservationRepository, ReservationSeatRepository reservationSeatRepository,
                              ShowtimeRepository showtimeRepository, SeatRepository seatRepository,
                              TicketPriceProperties prices, Clock clock, PlatformTransactionManager transactionManager) {
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.prices = prices;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 예매를 생성하고, 유일 제약 충돌은 전체 롤백이 끝난 뒤 재응답 또는 도메인 오류로 변환한다. */
    public ReservationResult createReservation(Long showtimeId, CreateReservationRequest request) {
        request.validateSelection();
        try {
            // execute는 커밋까지 끝낸 뒤 반환한다. 컨트롤러는 그 이후에만 201을 보낸다.
            return transaction.execute(status -> createInTransaction(showtimeId, request));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateConstraint(exception, "uk_reservation_request")) {
                // 다른 요청이 같은 UUID로 먼저 성공했다면 새 트랜잭션에서 커밋된 결과를 읽는다.
                return transaction.execute(status -> {
                    Reservation existing = reservationRepository.findByRequestId(request.requestId())
                            .orElseThrow(() -> exception);
                    return replayReservation(existing, showtimeId, request);
                });
            }
            if (isDuplicateConstraint(exception, "uk_reservation_seat_showtime")) {
                throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
            }
            // 외래 키 오류나 알 수 없는 DB 장애를 좌석 충돌로 오인하지 않도록 그대로 전달한다.
            throw exception;
        }
    }

    /** 기존 UUID를 먼저 확인하고, 신규 요청이면 회차·좌석·마감·금액을 검사한 뒤 함께 저장한다. */
    private ReservationResult createInTransaction(Long showtimeId, CreateReservationRequest request) {
        Reservation existing = reservationRepository.findByRequestId(request.requestId()).orElse(null);
        if (existing != null) {
            return replayReservation(existing, showtimeId, request);
        }
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 상영 회차를 찾을 수 없습니다."));
        List<Seat> seats = seatRepository.findByScreenIdAndSeatCodeInOrderByIdAsc(
                showtime.getScreen().getId(), request.seatCodes());
        validateSelectedSeats(seats, request.seatCodes());
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"));
        if (!now.isBefore(showtime.getStartAt())) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_BOOKABLE);
        }
        int totalAmount = calculateTotalAmount(request.ticketCounts());
        if (!BigInteger.valueOf(totalAmount).equals(request.expectedTotalAmount())) {
            throw new BusinessException(ErrorCode.RESERVATION_AMOUNT_CHANGED);
        }
        TicketCounts counts = request.ticketCounts();
        Reservation reservation = new Reservation(request.requestId(), showtime, counts.adult().byteValueExact(),
                counts.teen().byteValueExact(), counts.senior().byteValueExact(), counts.disabled().byteValueExact(),
                totalAmount, now.truncatedTo(ChronoUnit.SECONDS));

        // UUID 중복을 좌석 INSERT보다 먼저 확인한다. 어느 좌석이라도 실패하면 예매 행도 롤백된다.
        reservationRepository.saveAndFlush(reservation);
        List<ReservationSeat> reservationSeats = seats.stream().map(seat -> new ReservationSeat(reservation, seat)).toList();
        reservationSeatRepository.saveAllAndFlush(reservationSeats);
        ReservationResponse response = ReservationResponse.from(reservation, request.seatCodes());
        return new ReservationResult(response, true);
    }

    /** 저장된 요청과 회차·인원·좌석 집합·총액을 대조해 같은 요청에만 최초 결과를 반환한다. */
    private ReservationResult replayReservation(Reservation reservation, Long showtimeId, CreateReservationRequest request) {
        List<String> codes = reservationSeatRepository.findSeatCodesByReservationId(reservation.getId());
        ReservationResponse response = ReservationResponse.from(reservation, codes);
        if (!response.showtimeId().equals(showtimeId) || !response.ticketCounts().equals(request.ticketCounts())
                || !response.seatCodes().equals(request.seatCodes().stream().sorted().toList())
                || !BigInteger.valueOf(response.totalAmount()).equals(request.expectedTotalAmount())) {
            throw new BusinessException(ErrorCode.RESERVATION_REQUEST_CONFLICT);
        }
        return new ReservationResult(response, false);
    }

    /** DB의 대소문자 비교 설정과 무관하게 조회된 좌석 코드가 요청값과 정확히 같은지 검사한다. */
    private void validateSelectedSeats(List<Seat> seats, List<String> requestedCodes) {
        List<String> foundCodes = seats.stream().map(Seat::getSeatCode).toList();
        if (seats.size() != requestedCodes.size() || !foundCodes.containsAll(requestedCodes)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "입력값을 확인해 주세요.",
                    List.of(new ValidationError("seatCodes", "해당 상영관의 좌석 코드를 선택해 주세요.")));
        }
    }

    /** 공통 설정의 단가와 검증된 인원으로 총액을 계산한다. 곱셈부터 long으로 계산해 오버플로를 막는다. */
    private int calculateTotalAmount(TicketCounts counts) {
        long total = counts.adult().longValueExact() * prices.adult()
                + counts.teen().longValueExact() * prices.teen()
                + counts.senior().longValueExact() * prices.senior()
                + counts.disabled().longValueExact() * prices.disabled();
        return Math.toIntExact(total);
    }

    /** MySQL의 중복 키 오류(1062)와 알려진 제약명을 함께 확인해 충돌 원인을 구분한다. */
    private boolean isDuplicateConstraint(Throwable exception, String constraintName) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String actualName = violation.getConstraintName();
                return violation.getSQLException().getErrorCode() == 1062
                        && actualName != null && actualName.endsWith(constraintName);
            }
        }
        return false;
    }
}

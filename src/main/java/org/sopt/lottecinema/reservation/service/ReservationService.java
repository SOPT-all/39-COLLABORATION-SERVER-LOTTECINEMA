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

    private static final ZoneId BOOKING_ZONE = ZoneId.of("Asia/Seoul");
    private static final String REQUEST_ID_CONSTRAINT = "uk_reservation_request";
    private static final String SHOWTIME_SEAT_CONSTRAINT = "uk_reservation_seat_showtime";
    private static final int MYSQL_DUPLICATE_KEY_ERROR = 1062;

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketPriceProperties ticketPrices;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    /**
     * 예매와 좌석을 한 트랜잭션으로 저장할 실행기를 준비한다.
     * REQUIRES_NEW는 호출할 때마다 별도 트랜잭션을 만들어, 저장 실패 후 재조회에도 사용할 수 있게 한다.
     */
    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            TicketPriceProperties ticketPrices,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.ticketPrices = ticketPrices;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 예매를 생성하고, 유일 제약 충돌은 전체 롤백이 끝난 뒤 재응답 또는 도메인 오류로 변환한다. */
    public ReservationResult createReservation(Long showtimeId, CreateReservationRequest request) {
        // @Valid 이후, 총인원과 좌석 수처럼 여러 필드를 함께 확인해야 하는 규칙을 검사한다.
        request.validateSelection();
        try {
            // execute는 커밋까지 끝낸 뒤 반환한다. 컨트롤러는 그 이후에만 201을 보낸다.
            return transactionTemplate.execute(status -> createInTransaction(showtimeId, request));
        } catch (DataIntegrityViolationException exception) {
            // 이 시점에는 실패한 트랜잭션의 롤백이 끝났으므로 새 트랜잭션으로 재조회할 수 있다.
            if (isDuplicateConstraint(exception, REQUEST_ID_CONSTRAINT)) {
                return replayAfterRequestConflict(showtimeId, request, exception);
            }
            if (isDuplicateConstraint(exception, SHOWTIME_SEAT_CONSTRAINT)) {
                throw new BusinessException(ErrorCode.SEAT_ALREADY_RESERVED);
            }
            // 외래 키 오류나 알 수 없는 DB 장애를 좌석 충돌로 오인하지 않도록 그대로 전달한다.
            throw exception;
        }
    }

    /** 같은 요청 ID로 동시에 저장을 시도한 경우, 먼저 완료된 예매를 새 트랜잭션에서 읽어 비교한다. */
    private ReservationResult replayAfterRequestConflict(
            Long showtimeId, CreateReservationRequest request, DataIntegrityViolationException originalException
    ) {
        return transactionTemplate.execute(status -> {
            Reservation existing = reservationRepository.findByRequestId(request.requestId())
                    .orElseThrow(() -> originalException);
            return replayReservation(existing, showtimeId, request);
        });
    }

    /** 기존 UUID를 먼저 확인하고, 신규 요청이면 회차·좌석·마감·금액을 검사한 뒤 함께 저장한다. */
    private ReservationResult createInTransaction(Long showtimeId, CreateReservationRequest request) {
        // 1. requestId로 기존 예매가 있는지 확인한다.
        Reservation existing = reservationRepository.findByRequestId(request.requestId()).orElse(null);
        if (existing != null) {
            // 이미 성공한 요청은 현재 가격이나 상영 시작 시각과 무관하게 최초 저장 결과로 응답한다.
            return replayReservation(existing, showtimeId, request);
        }

        // 2. 기존 예매가 없으면 showtimeId로 회차를 조회한다.
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "해당 상영 회차를 찾을 수 없습니다."));

        // 3. 상영관 ID와 요청 좌석 코드로 좌석들을 조회한다.
        List<Seat> seats = seatRepository.findByScreenIdAndSeatCodeInOrderByIdAsc(
                showtime.getScreen().getId(), request.seatCodes());

        // 4. 요청한 모든 좌석이 해당 상영관에 존재하고 코드의 대소문자까지 일치하는지 검사한다.
        validateSelectedSeats(seats, request.seatCodes());

        // 5. KST 기준 현재 시각이 상영 시작 시각 이상이면 예매를 거부한다.
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), BOOKING_ZONE);
        if (!now.isBefore(showtime.getStartAt())) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_BOOKABLE);
        }

        // 6. 공통 유형별 단가로 총액을 계산하고 사용자가 확인한 금액과 비교한다.
        int totalAmount = calculateTotalAmount(request.ticketCounts());
        if (!BigInteger.valueOf(totalAmount).equals(request.expectedTotalAmount())) {
            throw new BusinessException(ErrorCode.RESERVATION_AMOUNT_CHANGED);
        }

        // 7. 예매와 선택 좌석을 같은 트랜잭션에서 저장한다.
        TicketCounts counts = request.ticketCounts();
        // 검증된 인원(0~8명)을 엔티티의 byte 필드로 옮긴다.
        // DB의 초 단위 저장 정밀도에 맞춰 최초 응답과 재조회 응답의 생성 시각을 동일하게 유지한다.
        Reservation reservation = new Reservation(
                request.requestId(),
                showtime,
                counts.adult().byteValueExact(),
                counts.teen().byteValueExact(),
                counts.senior().byteValueExact(),
                counts.disabled().byteValueExact(),
                totalAmount,
                now.truncatedTo(ChronoUnit.SECONDS)
        );

        // UUID 중복을 좌석 INSERT보다 먼저 확인한다. 어느 좌석이라도 실패하면 예매 행도 롤백된다.
        // flush는 SQL을 즉시 실행한다. 커밋은 바깥의 transactionTemplate.execute가 정상 종료할 때 수행한다.
        reservationRepository.saveAndFlush(reservation);
        List<ReservationSeat> reservationSeats = seats.stream()
                .map(seat -> new ReservationSeat(reservation, seat))
                .toList();
        reservationSeatRepository.saveAllAndFlush(reservationSeats);

        // 8. 저장한 값으로 응답을 만들고 신규 생성 여부를 컨트롤러에 전달한다.
        ReservationResponse response = ReservationResponse.from(reservation, request.seatCodes());
        return new ReservationResult(response, true);
    }

    /** 저장된 요청과 회차·인원·좌석 집합·총액을 대조해 같은 요청에만 최초 결과를 반환한다. */
    private ReservationResult replayReservation(Reservation reservation, Long showtimeId, CreateReservationRequest request) {
        List<String> savedSeatCodes = reservationSeatRepository.findSeatCodesByReservationId(reservation.getId());
        ReservationResponse response = ReservationResponse.from(reservation, savedSeatCodes);

        // 좌석 선택 순서는 예매 내용에 영향을 주지 않으므로 양쪽을 정렬한 상태로 비교한다.
        List<String> sortedRequestedSeatCodes = request.seatCodes().stream().sorted().toList();
        boolean sameShowtime = response.showtimeId().equals(showtimeId);
        boolean sameTicketCounts = response.ticketCounts().equals(request.ticketCounts());
        boolean sameSeats = response.seatCodes().equals(sortedRequestedSeatCodes);
        boolean sameAmount = BigInteger.valueOf(response.totalAmount()).equals(request.expectedTotalAmount());
        boolean sameRequest = sameShowtime && sameTicketCounts && sameSeats && sameAmount;

        // 같은 ID라도 회차·인원·좌석·금액 중 하나가 달라지면 다른 요청으로 판단한다.
        if (!sameRequest) {
            throw new BusinessException(ErrorCode.RESERVATION_REQUEST_CONFLICT);
        }
        return new ReservationResult(response, false);
    }

    /** DB의 대소문자 비교 설정과 무관하게 조회된 좌석 코드가 요청값과 정확히 같은지 검사한다. */
    private void validateSelectedSeats(List<Seat> seats, List<String> requestedCodes) {
        List<String> foundCodes = seats.stream().map(Seat::getSeatCode).toList();
        boolean sameSeatCount = seats.size() == requestedCodes.size();
        // MySQL이 b1로 B1을 조회하더라도, Java 문자열 비교로 잘못된 대소문자를 거부한다.
        boolean allCodesMatch = foundCodes.containsAll(requestedCodes);
        if (!sameSeatCount || !allCodesMatch) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "입력값을 확인해 주세요.",
                    List.of(new ValidationError("seatCodes", "해당 상영관의 좌석 코드를 선택해 주세요.")));
        }
    }

    /** 공통 설정의 단가와 검증된 인원으로 총액을 계산한다. 곱셈부터 long으로 계산해 오버플로를 막는다. */
    private int calculateTotalAmount(TicketCounts counts) {
        long adultAmount = counts.adult().longValueExact() * ticketPrices.adult();
        long teenAmount = counts.teen().longValueExact() * ticketPrices.teen();
        long seniorAmount = counts.senior().longValueExact() * ticketPrices.senior();
        long disabledAmount = counts.disabled().longValueExact() * ticketPrices.disabled();
        long total = adultAmount + teenAmount + seniorAmount + disabledAmount;
        return Math.toIntExact(total);
    }

    /** MySQL의 중복 키 오류(1062)와 알려진 제약명을 함께 확인해 충돌 원인을 구분한다. */
    private boolean isDuplicateConstraint(Throwable exception, String constraintName) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String actualName = violation.getConstraintName();
                // Hibernate가 제약명에 테이블명을 붙일 수 있어 알려진 제약명으로 끝나는지 확인한다.
                return violation.getSQLException().getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR
                        && actualName != null
                        && actualName.endsWith(constraintName);
            }
        }
        return false;
    }
}

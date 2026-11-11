package org.sopt.lottecinema.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.sopt.lottecinema.global.common.ValidationError;
import org.sopt.lottecinema.global.exception.BusinessException;
import org.sopt.lottecinema.global.exception.ErrorCode;
import tools.jackson.databind.annotation.JsonDeserialize;

/** 새 제출마다 requestId를 만들며, 네트워크 재시도에는 같은 ID와 요청값을 보낸다. */
@JsonDeserialize(using = CreateReservationRequestDeserializer.class)
public record CreateReservationRequest(
        @NotNull UUID requestId,
        @NotNull @Valid TicketCounts ticketCounts,
        @NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 10) String> seatCodes,
        @NotNull @PositiveOrZero BigInteger expectedTotalAmount
) {
    /**
     * @Valid가 필수값과 유형별 인원 범위를 확인한 뒤, 인원과 좌석 사이의 관계를 검사한다.
     * 확인한 오류를 모아서 반환하므로 여러 입력 문제를 한 응답에서 확인할 수 있다.
     */
    public void validateSelection() {
        List<ValidationError> errors = new ArrayList<>();
        int totalCount = ticketCounts.totalCount();
        int selectedSeatCount = seatCodes.size();
        // Set은 같은 좌석 코드를 한 번만 보관하므로 원본과 개수를 비교해 중복을 찾는다.
        Set<String> uniqueSeatCodes = new HashSet<>(seatCodes);

        if (totalCount < 1 || totalCount > 8) {
            errors.add(new ValidationError("ticketCounts", "총인원은 1~8명이어야 합니다."));
        }
        if (uniqueSeatCodes.size() != selectedSeatCount) {
            errors.add(new ValidationError("seatCodes", "중복된 좌석을 선택할 수 없습니다."));
        }
        if (totalCount != selectedSeatCount) {
            errors.add(new ValidationError("seatCodes", "선택한 좌석 수와 총인원이 같아야 합니다."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "입력값을 확인해 주세요.", errors);
        }
    }
}

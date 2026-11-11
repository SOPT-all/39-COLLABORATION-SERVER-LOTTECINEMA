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
    /** 개별 필드 검증 후 총인원, 좌석 중복, 인원과 좌석 수의 일치를 검사한다. */
    public void validateSelection() {
        List<ValidationError> errors = new ArrayList<>();
        int totalCount = ticketCounts.totalCount();
        if (totalCount < 1 || totalCount > 8) {
            errors.add(new ValidationError("ticketCounts", "총인원은 1~8명이어야 합니다."));
        }
        if (new HashSet<>(seatCodes).size() != seatCodes.size()) {
            errors.add(new ValidationError("seatCodes", "중복된 좌석을 선택할 수 없습니다."));
        }
        if (totalCount != seatCodes.size()) {
            errors.add(new ValidationError("seatCodes", "선택한 좌석 수와 총인원이 같아야 합니다."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "입력값을 확인해 주세요.", errors);
        }
    }
}

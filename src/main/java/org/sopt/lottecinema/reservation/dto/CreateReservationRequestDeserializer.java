package org.sopt.lottecinema.reservation.dto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/**
 * 예매 JSON을 필드별로 읽고 DTO로 변환한다. 숫자 문자열과 소수는 정수로 자동 변환하지 않는다.
 * reportInputMismatch에서 발생한 매핑 예외는 공통 예외 처리기를 통해 400 응답으로 전달된다.
 */
public class CreateReservationRequestDeserializer extends ValueDeserializer<CreateReservationRequest> {

    /** 최상위 객체를 확인하고 인원 → 좌석 → UUID → 금액 순서로 읽어 요청 DTO를 만든다. */
    @Override
    public CreateReservationRequest deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode root = context.readTree(parser);
        if (!root.isObject()) {
            return context.reportInputMismatch(CreateReservationRequest.class, "요청 본문은 객체여야 합니다.");
        }
        TicketCounts ticketCounts = readTicketCounts(root.get("ticketCounts"), context);
        List<String> seatCodes = readSeatCodes(root.get("seatCodes"), context);
        UUID requestId = readRequestId(root.get("requestId"), context);
        BigInteger expectedTotalAmount = readInteger(root.get("expectedTotalAmount"), context);

        return new CreateReservationRequest(requestId, ticketCounts, seatCodes, expectedTotalAmount);
    }

    /** 인원 객체의 네 값을 읽는다. 누락·null은 이후 @Valid의 필수값 검증에 전달한다. */
    private TicketCounts readTicketCounts(JsonNode counts, DeserializationContext context) {
        if (counts == null || counts.isNull()) {
            return null;
        }
        if (!counts.isObject()) {
            return context.reportInputMismatch(CreateReservationRequest.class, "ticketCounts는 객체여야 합니다.");
        }
        return new TicketCounts(
                readInteger(counts.get("adult"), context),
                readInteger(counts.get("teen"), context),
                readInteger(counts.get("senior"), context),
                readInteger(counts.get("disabled"), context)
        );
    }

    /** 좌석 배열의 각 요소를 문자열로 읽고, 좌석 수·길이·공백 검사는 Bean Validation에 맡긴다. */
    private List<String> readSeatCodes(JsonNode seats, DeserializationContext context) {
        if (seats == null || seats.isNull()) {
            return null;
        }
        if (!seats.isArray()) {
            return context.reportInputMismatch(CreateReservationRequest.class, "seatCodes는 배열이어야 합니다.");
        }
        List<String> seatCodes = new ArrayList<>();
        for (JsonNode seat : seats) {
            seatCodes.add(readString(seat, context));
        }
        return seatCodes;
    }

    /** 요청 ID를 UUID로 읽고 표준 문자열 표기와 대조한다. 대소문자는 구분하지 않는다. */
    private UUID readRequestId(JsonNode value, DeserializationContext context) {
        String requestIdText = readString(value, context);
        if (requestIdText == null) {
            return null;
        }

        UUID requestId;
        try {
            requestId = UUID.fromString(requestIdText);
        } catch (IllegalArgumentException exception) {
            return context.reportInputMismatch(CreateReservationRequest.class, "requestId의 UUID 형식을 확인해 주세요.");
        }
        // UUID.fromString이 허용하는 축약 표기도 거부하고, 8-4-4-4-12자리 형식만 받는다.
        if (!requestId.toString().equalsIgnoreCase(requestIdText)) {
            return context.reportInputMismatch(CreateReservationRequest.class, "requestId의 UUID 형식을 확인해 주세요.");
        }
        return requestId;
    }

    /** 정수 토큰만 읽으며, 누락된 값은 필수값 검증을 위해 null로 전달한다. */
    private BigInteger readInteger(JsonNode value, DeserializationContext context) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber()) {
            return context.reportInputMismatch(CreateReservationRequest.class, "인원과 금액은 정수여야 합니다.");
        }
        return value.bigIntegerValue();
    }

    /** 문자열 토큰만 읽어 숫자 좌석 코드 등의 자동 변환을 막는다. */
    private String readString(JsonNode value, DeserializationContext context) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isString()) {
            return context.reportInputMismatch(CreateReservationRequest.class, "요청 ID와 좌석 코드는 문자열이어야 합니다.");
        }
        return value.asString();
    }
}

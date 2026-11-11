package org.sopt.lottecinema.reservation.dto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

/** 이 요청에서만 숫자 문자열과 소수를 정수로 자동 변환하지 않도록 JSON 타입을 확인한다. */
public class CreateReservationRequestDeserializer extends ValueDeserializer<CreateReservationRequest> {

    /** JSON 객체를 요청 DTO로 읽는다. null과 누락은 이후 Bean Validation에서 검사한다. */
    @Override
    public CreateReservationRequest deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode root = context.readTree(parser);
        if (!root.isObject()) {
            return context.reportInputMismatch(CreateReservationRequest.class, "요청 본문은 객체여야 합니다.");
        }
        JsonNode counts = root.get("ticketCounts");
        TicketCounts ticketCounts = null;
        if (counts != null && !counts.isNull()) {
            if (!counts.isObject()) {
                return context.reportInputMismatch(CreateReservationRequest.class, "ticketCounts는 객체여야 합니다.");
            }
            ticketCounts = new TicketCounts(readInteger(counts.get("adult"), context),
                    readInteger(counts.get("teen"), context), readInteger(counts.get("senior"), context),
                    readInteger(counts.get("disabled"), context));
        }
        List<String> seatCodes = null;
        JsonNode seats = root.get("seatCodes");
        if (seats != null && !seats.isNull()) {
            if (!seats.isArray()) {
                return context.reportInputMismatch(CreateReservationRequest.class, "seatCodes는 배열이어야 합니다.");
            }
            seatCodes = new ArrayList<>();
            for (JsonNode seat : seats) {
                seatCodes.add(readString(seat, context));
            }
        }
        String requestId = readString(root.get("requestId"), context);
        UUID uuid = null;
        if (requestId != null) {
            try {
                uuid = UUID.fromString(requestId);
                if (!uuid.toString().equalsIgnoreCase(requestId)) {
                    return context.reportInputMismatch(CreateReservationRequest.class, "requestId의 UUID 형식을 확인해 주세요.");
                }
            } catch (IllegalArgumentException exception) {
                return context.reportInputMismatch(CreateReservationRequest.class, "requestId의 UUID 형식을 확인해 주세요.");
            }
        }
        return new CreateReservationRequest(uuid, ticketCounts, seatCodes,
                readInteger(root.get("expectedTotalAmount"), context));
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

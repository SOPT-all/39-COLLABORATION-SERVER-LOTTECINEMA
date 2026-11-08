package org.sopt.lottecinema.global.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값을 확인해 주세요."),
    MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY", "요청 본문을 읽을 수 없습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "필수 요청 파라미터가 누락되었습니다."),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE", "요청 파라미터의 형식이 올바르지 않습니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "대상을 찾을 수 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE", "지원하지 않는 응답 형식입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 요청 형식입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

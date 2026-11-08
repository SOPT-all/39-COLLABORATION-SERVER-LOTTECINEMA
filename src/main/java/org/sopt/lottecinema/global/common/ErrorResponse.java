package org.sopt.lottecinema.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.sopt.lottecinema.global.exception.ErrorCode;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private final int status;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<ValidationError> errors;

    /** ErrorCode에 정의된 상태와 기본 메시지로 오류 응답 본문을 만듭니다. */
    public static ErrorResponse fail(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 같은 오류 코드에 상황별 메시지가 필요한 비즈니스 예외에 사용합니다. */
    public static ErrorResponse fail(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getCode(), message, null);
    }

    /** 요청 검증 실패 시 필드별 또는 객체 전체의 오류 목록을 함께 전달합니다. */
    public static ErrorResponse fail(ErrorCode errorCode, List<ValidationError> errors) {
        return new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getCode(), errorCode.getMessage(), errors);
    }

    /** Spring이 결정한 HTTP 상태를 본문에 반영하고 오류 코드의 기본 메시지를 사용합니다. */
    public static ErrorResponse fail(int status, ErrorCode errorCode) {
        return new ErrorResponse(status, errorCode.getCode(), errorCode.getMessage(), null);
    }
}

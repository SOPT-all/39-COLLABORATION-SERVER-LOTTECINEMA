package org.sopt.lottecinema.global.exception;

import java.util.List;
import lombok.Getter;
import org.sopt.lottecinema.global.common.ValidationError;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ValidationError> errors;

    /** 오류 코드의 기본 메시지로 비즈니스 예외를 만든다. */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    /** 상황에 맞는 메시지를 담아 비즈니스 예외를 만든다. */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /** 서비스에서 확인한 필드별 검증 오류를 공통 오류 응답에 전달한다. */
    public BusinessException(ErrorCode errorCode, String message, List<ValidationError> errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = errors == null ? null : List.copyOf(errors);
    }
}


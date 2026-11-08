package org.sopt.lottecinema.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.sopt.lottecinema.global.exception.ErrorCode;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final int status;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<ValidationError> errors;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "OK", null, null, data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "OK", null, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode(), null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getHttpStatus().value(), message, errorCode.getCode(), null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, List<ValidationError> errors) {
        return new ApiResponse<>(errorCode.getHttpStatus().value(), errorCode.getMessage(), errorCode.getCode(), errors, null);
    }

    public static ApiResponse<Void> fail(int status, ErrorCode errorCode) {
        return new ApiResponse<>(status, errorCode.getMessage(), errorCode.getCode(), null, null);
    }
}

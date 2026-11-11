package org.sopt.lottecinema.global.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"status", "message", "data"})
public class SuccessResponse<T> {

    private final int status;
    private final String message;

    private final T data;

    public static <T> SuccessResponse<T> success(T data) {
        return new SuccessResponse<>(200, "OK", data);
    }

    public static SuccessResponse<Void> success() {
        return new SuccessResponse<>(200, "OK", null);
    }

    public static <T> SuccessResponse<T> created(T data) {
        return new SuccessResponse<>(201, "Created", data);
    }
}

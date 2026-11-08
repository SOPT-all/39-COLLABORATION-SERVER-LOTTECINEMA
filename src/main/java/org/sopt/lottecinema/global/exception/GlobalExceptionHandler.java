package org.sopt.lottecinema.global.exception;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.sopt.lottecinema.global.common.ErrorResponse;
import org.sopt.lottecinema.global.common.ValidationError;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;

import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스 등에서 비즈니스 규칙 위반을 BusinessException으로 직접 발생시킨 경우
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.fail(errorCode, ex.getMessage()));
    }

    // @Valid로 검사한 요청 객체의 필드나 객체 전체가 검증 조건을 만족하지 못한 경우
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ValidationError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new ValidationError(error instanceof FieldError field ? field.getField() : null,
                        error.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ErrorResponse.fail(ErrorCode.VALIDATION_FAILED, errors));
    }

    // 컨트롤러 메서드의 파라미터 또는 반환값이 검증 조건을 만족하지 못한 경우
    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        // 반환값 검증 실패는 서버가 잘못된 응답을 생성한 것이므로 500으로 처리합니다.
        if (ex.isForReturnValue()) {
            log.error("응답 검증 중 서버 오류가 발생했습니다.", ex);
            return respond(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        List<ValidationError> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error -> errors.add(
                        new ValidationError(getParameterName(result.getMethodParameter()), error.getDefaultMessage()))));
        ex.getCrossParameterValidationResults().forEach(error ->
                errors.add(new ValidationError(null, error.getDefaultMessage())));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ErrorResponse.fail(ErrorCode.VALIDATION_FAILED, errors));
    }

    // JSON 문법이 잘못되었거나 필수 요청 본문을 읽을 수 없는 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return respond(ErrorCode.MALFORMED_REQUEST_BODY);
    }

    // 필수 쿼리 파라미터가 요청에 없는 경우
    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return respond(ErrorCode.MISSING_PARAMETER);
    }

    // 숫자가 필요한 파라미터에 문자열을 전달하는 등 메서드 인자의 타입이 맞지 않는 경우
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(ErrorCode.INVALID_PARAMETER_TYPE);
    }

    // 메서드 인자 외의 값을 필요한 타입으로 변환하지 못한 경우
    @ExceptionHandler(TypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleOtherTypeMismatch(TypeMismatchException ex) {
        return respond(ErrorCode.BAD_REQUEST);
    }

    // 요청을 처리할 경로나 정적 리소스를 Spring이 찾지 못한 경우
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    protected ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return respond(ErrorCode.NOT_FOUND);
    }

    // 해당 경로에서 지원하지 않는 HTTP 메서드를 사용한 경우 (예: GET만 가능한 경로에 POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .headers(ex.getHeaders())
                .body(ErrorResponse.fail(errorCode));
    }

    // 요청 본문의 Content-Type을 해당 API에서 지원하지 않는 경우
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        ErrorCode errorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .headers(ex.getHeaders())
                .body(ErrorResponse.fail(errorCode));
    }

    // 요청의 Accept 헤더에 맞는 응답 형식을 제공할 수 없는 경우 (406, 응답 본문 없음)
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    protected ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return ResponseEntity.status(ex.getStatusCode()).headers(ex.getHeaders()).build();
    }

    // 위에서 따로 처리하지 않은 예외가 발생한 경우
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        // Spring에서 상태를 지정한 예외는 해당 상태와 헤더를 유지합니다.
        if (ex instanceof org.springframework.web.ErrorResponse errorResponse) {
            var status = errorResponse.getStatusCode();
            ErrorCode errorCode = status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_SERVER_ERROR;
            if (status.is5xxServerError()) {
                log.error("요청 처리 중 서버 오류가 발생했습니다.", ex);
            }
            return ResponseEntity.status(status)
                    .headers(errorResponse.getHeaders())
                    .body(ErrorResponse.fail(status.value(), errorCode));
        }
        log.error("요청 처리 중 서버 오류가 발생했습니다.", ex);
        return respond(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.fail(errorCode));
    }

    private String getParameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null) {
            if (!requestParam.name().isEmpty()) return requestParam.name();
            if (!requestParam.value().isEmpty()) return requestParam.value();
        }
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null) {
            if (!pathVariable.name().isEmpty()) return pathVariable.name();
            if (!pathVariable.value().isEmpty()) return pathVariable.value();
        }
        return parameter.getParameterName();
    }
}

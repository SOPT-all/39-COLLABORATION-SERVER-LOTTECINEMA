package org.sopt.lottecinema.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.lottecinema.global.common.ApiResponse;
import org.sopt.lottecinema.global.common.ValidationError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.fail(errorCode, e.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (ex instanceof HttpMediaTypeNotAcceptableException) {
            return super.handleExceptionInternal(ex, null, headers, status, request);
        }
        if (ex instanceof MethodArgumentNotValidException validationException) {
            var errors = validationException.getBindingResult().getAllErrors().stream()
                    .map(error -> new ValidationError(error instanceof FieldError fieldError ? fieldError.getField() : null,
                            error.getDefaultMessage()))
                    .toList();
            return super.handleExceptionInternal(ex, ApiResponse.fail(ErrorCode.VALIDATION_FAILED, errors), headers, status, request);
        }
        if (ex instanceof HandlerMethodValidationException validationException && status.is4xxClientError()) {
            var errors = new java.util.ArrayList<ValidationError>();
            validationException.getParameterValidationResults().forEach(result ->
                    result.getResolvableErrors().forEach(error -> errors.add(new ValidationError(
                            getParameterName(result.getMethodParameter()), error.getDefaultMessage()))));
            validationException.getCrossParameterValidationResults().forEach(error ->
                    errors.add(new ValidationError(null, error.getDefaultMessage())));
            return super.handleExceptionInternal(ex, ApiResponse.fail(ErrorCode.VALIDATION_FAILED, errors), headers, status, request);
        }
        ErrorCode code = getErrorCode(ex, status);
        if (status.is5xxServerError()) {
            log.error("요청 처리 중 서버 오류가 발생했습니다.", ex);
        }
        return super.handleExceptionInternal(ex, ApiResponse.fail(status.value(), code), headers, status, request);
    }

    private String getParameterName(org.springframework.core.MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isEmpty()) return requestParam.name();
        if (requestParam != null && !requestParam.value().isEmpty()) return requestParam.value();
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.name().isEmpty()) return pathVariable.name();
        if (pathVariable != null && !pathVariable.value().isEmpty()) return pathVariable.value();
        return parameter.getParameterName();
    }

    private ErrorCode getErrorCode(Exception ex, HttpStatusCode status) {
        if (ex instanceof HandlerMethodValidationException && status.is4xxClientError()) return ErrorCode.VALIDATION_FAILED;
        if (ex instanceof HttpMessageNotReadableException) return ErrorCode.MALFORMED_REQUEST_BODY;
        if (ex instanceof MissingServletRequestParameterException) return ErrorCode.MISSING_PARAMETER;
        if (ex instanceof MethodArgumentTypeMismatchException) return ErrorCode.INVALID_PARAMETER_TYPE;
        if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) return ErrorCode.NOT_FOUND;
        if (ex instanceof HttpRequestMethodNotSupportedException) return ErrorCode.METHOD_NOT_ALLOWED;
        if (ex instanceof HttpMediaTypeNotSupportedException) return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_SERVER_ERROR;
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("요청 처리 중 서버 오류가 발생했습니다.", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}

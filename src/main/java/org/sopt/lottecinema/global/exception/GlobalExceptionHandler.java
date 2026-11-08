package org.sopt.lottecinema.global.exception;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sopt.lottecinema.global.common.ApiResponse;
import org.sopt.lottecinema.global.common.ValidationError;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.fail(code, ex.getMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new ValidationError(error instanceof FieldError field ? field.getField() : null,
                        error.getDefaultMessage()))
                .toList();
        return super.handleExceptionInternal(ex, ApiResponse.fail(ErrorCode.VALIDATION_FAILED, errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("응답 검증 중 서버 오류가 발생했습니다.", ex);
            return respond(ex, headers, status, request, ErrorCode.INTERNAL_SERVER_ERROR);
        }
        List<ValidationError> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result ->
                result.getResolvableErrors().forEach(error -> errors.add(new ValidationError(
                        getParameterName(result.getMethodParameter()), error.getDefaultMessage()))));
        ex.getCrossParameterValidationResults().forEach(error ->
                errors.add(new ValidationError(null, error.getDefaultMessage())));
        return super.handleExceptionInternal(ex, ApiResponse.fail(ErrorCode.VALIDATION_FAILED, errors),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, ErrorCode.MALFORMED_REQUEST_BODY);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, ErrorCode.MISSING_PARAMETER);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode code = ex instanceof MethodArgumentTypeMismatchException
                ? ErrorCode.INVALID_PARAMETER_TYPE : ErrorCode.BAD_REQUEST;
        return respond(ex, headers, status, request, code);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, ErrorCode.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, ErrorCode.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, ErrorCode.METHOD_NOT_ALLOWED);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ex, headers, status, request, ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return super.handleExceptionInternal(ex, null, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ErrorCode code = status.is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL_SERVER_ERROR;
        if (status.is5xxServerError()) {
            log.error("요청 처리 중 서버 오류가 발생했습니다.", ex);
        }
        return respond(ex, headers, status, request, code);
    }

    private ResponseEntity<Object> respond(Exception ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request, ErrorCode code) {
        return super.handleExceptionInternal(ex, ApiResponse.fail(status.value(), code), headers, status, request);
    }

    private String getParameterName(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        if (requestParam != null && !requestParam.name().isEmpty()) return requestParam.name();
        if (requestParam != null && !requestParam.value().isEmpty()) return requestParam.value();
        PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
        if (pathVariable != null && !pathVariable.name().isEmpty()) return pathVariable.name();
        if (pathVariable != null && !pathVariable.value().isEmpty()) return pathVariable.value();
        return parameter.getParameterName();
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("요청 처리 중 서버 오류가 발생했습니다.", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}

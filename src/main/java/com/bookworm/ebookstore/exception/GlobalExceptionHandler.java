package com.bookworm.ebookstore.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns exceptions into RFC 9457 problem details (application/problem+json) with stable error codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiErrorCode errorCode = ex.getErrorCode();
        ProblemDetail problem = buildProblemDetail(errorCode.getHttpStatus(), errorCode.getDefaultTitle(), ex.getMessage(), errorCode, request.getRequestURI());
        return ResponseEntity.status(errorCode.getHttpStatus()).contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            // e.g. getPaged.size -> size
            String field = propertyPath.contains(".") ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
            errors.putIfAbsent(field, violation.getMessage());
        }

        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED.getDefaultTitle(),
                "Parameter validation failed", ApiErrorCode.VALIDATION_FAILED, request.getRequestURI());
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("uq_users_email")) {
            ProblemDetail problem = buildProblemDetail(ApiErrorCode.EMAIL_TAKEN.getHttpStatus(), ApiErrorCode.EMAIL_TAKEN.getDefaultTitle(),
                    "An account with this email address already exists", ApiErrorCode.EMAIL_TAKEN, request.getRequestURI());
            return ResponseEntity.status(ApiErrorCode.EMAIL_TAKEN.getHttpStatus()).contentType(PROBLEM_JSON).body(problem);
        }
        log.error("Unhandled DataIntegrityViolationException at URI: {}", request.getRequestURI(), ex);
        ProblemDetail problem = buildProblemDetail(ApiErrorCode.INTERNAL_ERROR.getHttpStatus(), ApiErrorCode.INTERNAL_ERROR.getDefaultTitle(),
                "A database constraint violation occurred", ApiErrorCode.INTERNAL_ERROR, request.getRequestURI());
        return ResponseEntity.status(ApiErrorCode.INTERNAL_ERROR.getHttpStatus()).contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String detail = "Failed to convert parameter '" + ex.getName() + "' of value '" + ex.getValue() + "'";
        ProblemDetail problem = buildProblemDetail(ApiErrorCode.MALFORMED_REQUEST.getHttpStatus(), ApiErrorCode.MALFORMED_REQUEST.getDefaultTitle(),
                detail, ApiErrorCode.MALFORMED_REQUEST, request.getRequestURI());
        return ResponseEntity.status(ApiErrorCode.MALFORMED_REQUEST.getHttpStatus()).contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllUncaughtExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request to URI: {}", request.getRequestURI(), ex);
        ProblemDetail problem = buildProblemDetail(ApiErrorCode.INTERNAL_ERROR.getHttpStatus(), ApiErrorCode.INTERNAL_ERROR.getDefaultTitle(),
                "An unexpected internal error occurred", ApiErrorCode.INTERNAL_ERROR, request.getRequestURI());
        return ResponseEntity.status(ApiErrorCode.INTERNAL_ERROR.getHttpStatus()).contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value");
        }
        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid object");
        });

        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED.getDefaultTitle(),
                "Request validation failed", ApiErrorCode.VALIDATION_FAILED, path);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getAllValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            if (paramName == null) {
                paramName = "parameter";
            }
            final String name = paramName;
            result.getResolvableErrors().forEach(err -> {
                errors.putIfAbsent(name, err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid parameter");
            });
        });

        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED.getDefaultTitle(),
                "Parameter validation failed", ApiErrorCode.VALIDATION_FAILED, path);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_REQUEST.getDefaultTitle(),
                "Required request body is missing or unreadable", ApiErrorCode.MALFORMED_REQUEST, path);
        return ResponseEntity.badRequest().contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_REQUEST.getDefaultTitle(),
                "Required request parameter '" + ex.getParameterName() + "' is missing", ApiErrorCode.MALFORMED_REQUEST, path);
        return ResponseEntity.badRequest().contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.NOT_FOUND, ApiErrorCode.NOT_FOUND.getDefaultTitle(),
                ex.getMessage(), ApiErrorCode.NOT_FOUND, path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, ApiErrorCode.METHOD_NOT_ALLOWED.getDefaultTitle(),
                ex.getMessage(), ApiErrorCode.METHOD_NOT_ALLOWED, path);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).contentType(PROBLEM_JSON).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String path = getRequestUri(request);
        ProblemDetail problem = buildProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ApiErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultTitle(),
                ex.getMessage(), ApiErrorCode.UNSUPPORTED_MEDIA_TYPE, path);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).contentType(PROBLEM_JSON).body(problem);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail, ApiErrorCode code, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", code.name());
        if (path != null) {
            problem.setInstance(URI.create(path));
        }
        return problem;
    }

    private String getRequestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }
}

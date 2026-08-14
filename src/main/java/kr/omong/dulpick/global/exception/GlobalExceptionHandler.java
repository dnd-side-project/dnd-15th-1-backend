package kr.omong.dulpick.global.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorMonitoringService errorMonitoringService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(
                ErrorLevel.INFO,
                ErrorCode.INVALID_INPUT,
                exception,
                request
        );
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        error.getCode(),
                        error.getDefaultMessage()
                ))
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();
        return response(ErrorCode.INVALID_INPUT, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(
                ErrorLevel.INFO,
                ErrorCode.INVALID_INPUT,
                exception,
                request
        );
        FieldErrorResponse fieldError = new FieldErrorResponse(
                exception.getName(),
                "TYPE_MISMATCH",
                "요청한 형식과 일치하지 않습니다"
        );
        return response(ErrorCode.INVALID_INPUT, List.of(fieldError));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(
                ErrorLevel.INFO,
                ErrorCode.INVALID_INPUT,
                exception,
                request
        );
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getConstraintDescriptor()
                                .getAnnotation()
                                .annotationType()
                                .getSimpleName(),
                        violation.getMessage()
                ))
                .sorted(Comparator.comparing(FieldErrorResponse::field))
                .toList();
        return response(ErrorCode.INVALID_INPUT, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(
                ErrorLevel.INFO,
                ErrorCode.INVALID_INPUT,
                exception,
                request
        );
        return response(ErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(
                ErrorLevel.INFO,
                ErrorCode.METHOD_NOT_ALLOWED,
                exception,
                request
        );
        return response(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(
                ErrorLevel.INFO,
                ErrorCode.NOT_FOUND,
                exception,
                request
        );
        return response(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException exception,
            HttpServletRequest request
    ) {
        ErrorLevel level = resolveBusinessLevel(exception.getErrorCode());
        errorMonitoringService.record(level, exception.getErrorCode(), exception, request);
        if (exception instanceof FieldValidationException validationException) {
            return response(exception.getErrorCode(), validationException.getFieldErrors());
        }
        return response(exception.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        errorMonitoringService.record(ErrorLevel.CRITICAL, ErrorCode.INTERNAL_ERROR, exception, request);
        return response(ErrorCode.INTERNAL_ERROR);
    }

    private ErrorLevel resolveBusinessLevel(ErrorCode errorCode) {
        if (errorCode.getHttpStatus().is5xxServerError()) {
            return ErrorLevel.CRITICAL;
        }
        if (errorCode == ErrorCode.INVALID_INPUT
                || errorCode == ErrorCode.NOT_FOUND
                || errorCode == ErrorCode.METHOD_NOT_ALLOWED) {
            return ErrorLevel.INFO;
        }
        return ErrorLevel.WARNING;
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode));
    }

    private ResponseEntity<ErrorResponse> response(
            ErrorCode errorCode,
            List<FieldErrorResponse> fieldErrors
    ) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.from(errorCode, fieldErrors));
    }
}

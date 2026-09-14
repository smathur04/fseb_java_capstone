package assembly.general.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Email already exists");
    }

    // 400 — malformed or unreadable request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed request body");
    }

    // 400 — Bean Validation failure on a @RequestBody DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
    }

    // 400 — required query/path param missing
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing required parameter");
    }

    // 400 — param present but wrong type (e.g. "abc" where a UUID/int was expected)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid parameter type");
    }

    // 401 — bad login credentials
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    @ExceptionHandler(BadLoginCredException.class)
    public ResponseEntity<ErrorResponse> handleBadLoginCredentials(BadLoginCredException ex) {
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid email or password");
    }

    // 401 — request required authentication and had none/invalid
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuth(InsufficientAuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }

    // 403 — authenticated but not permitted
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to access this resource");
    }

    // 404 — no route matches the requested URL at all
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    // 404 — a lookup (e.g. Optional.orElseThrow with no custom exception) came up empty
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    // 405 — wrong HTTP verb for this route (e.g. DELETE on a GET-only endpoint)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not supported");
    }

    // 409 — violates a DB constraint (unique, foreign key, not-null, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", "Data integrity violation");
    }

    // 409 — request conflicts with the current state of the resource
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with current state");
    }

    // 400 — invalid argument passed somewhere in application logic
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid argument");
    }

    // 500 — anything unanticipated
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        ErrorResponse response = new ErrorResponse(error, message, Instant.now());
        return ResponseEntity.status(status).body(response);
    }
}
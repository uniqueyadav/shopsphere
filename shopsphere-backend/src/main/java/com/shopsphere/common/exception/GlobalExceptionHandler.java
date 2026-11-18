package com.shopsphere.common.exception;

import java.util.Objects;

import com.shopsphere.common.response.ApiResponse;

import jakarta.validation.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException exception) {
		String message = Objects.requireNonNullElse(exception.getReason(), "The request could not be completed.");
		return ResponseEntity.status(exception.getStatusCode()).body(ApiResponse.failure(message));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid request value."))
				.orElse("Please check your request and try again.");
		return ResponseEntity.badRequest().body(ApiResponse.failure(message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.failure("Please check your request and try again."));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(ApiResponse.failure("Request body is invalid."));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
			MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.badRequest()
				.body(ApiResponse.failure("The value provided for " + exception.getName() + " is invalid."));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResponse.failure("A record with the provided unique value already exists."));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(ApiResponse.failure("Uploaded images exceed the maximum request size."));
	}
}
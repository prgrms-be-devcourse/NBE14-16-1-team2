package com.cafe.beanorder.global.error;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * 공통 예외 처리.
 *
 * <ul>
 *   <li>Spring MVC 표준 예외(400·404·405·406·415 등)는 상위 클래스 ResponseEntityExceptionHandler 가 처리하고,
 *       상태 코드는 그대로 둔 채 본문만 code/message 형식으로 바꾼다.</li>
 *   <li>400 계열 입력 오류의 code 는 INVALID_INPUT 으로 통일한다.</li>
 *   <li>BusinessException 은 ErrorCode 의 상태와 메시지로 응답한다.</li>
 *   <li>그 밖의 예외는 서버 로그에 원인을 남기고 500 과 일반 메시지만 반환한다.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handle_business_exception(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
			.body(ErrorResponse.of(errorCode, exception.getMessage()));
	}

	/** 서비스 계층 등에서 @Validated 로 검증할 때 발생하는 예외 (Spring MVC 표준 처리 대상이 아님) */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handle_constraint_violation(ConstraintViolationException exception) {
		List<String> violationMessages = exception.getConstraintViolations().stream()
			.map(GlobalExceptionHandler::format_violation)
			.sorted()
			.toList();
		return invalid_input(join_messages(violationMessages));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handle_unexpected_exception(Exception exception) {
		log.error("처리되지 않은 예외", exception);
		return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	// ===== Spring MVC 표준 예외: 입력 오류 메시지를 구체화 =====

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> fieldMessages = exception.getBindingResult().getAllErrors().stream()
			.map(error -> error instanceof FieldError fieldError
				? fieldError.getField() + ": " + fieldError.getDefaultMessage()
				: error.getDefaultMessage())
			.sorted()
			.toList();
		return handleExceptionInternal(exception, invalid_input_body(join_messages(fieldMessages)), headers, status,
				request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> parameterMessages = exception.getParameterValidationResults().stream()
			.flatMap(result -> result.getResolvableErrors().stream()
				.map(error -> result.getMethodParameter().getParameterName() + ": " + default_message(error)))
			.sorted()
			.toList();
		return handleExceptionInternal(exception, invalid_input_body(join_messages(parameterMessages)), headers, status,
				request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return handleExceptionInternal(exception,
				invalid_input_body("요청 본문이 없거나 JSON 형식이 올바르지 않습니다."), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException exception, HttpHeaders headers, HttpStatusCode status,
			WebRequest request) {
		return handleExceptionInternal(exception,
				invalid_input_body("필수 파라미터 '" + exception.getParameterName() + "'이(가) 없습니다."), headers, status,
				request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String parameterName = exception.getPropertyName() != null ? exception.getPropertyName() : "요청 값";
		return handleExceptionInternal(exception,
				invalid_input_body("'" + parameterName + "' 값의 형식이 올바르지 않습니다."), headers, status, request);
	}

	/**
	 * 상위 클래스가 처리하는 모든 표준 예외가 거치는 지점.
	 * 상태 코드와 헤더(예: 405 의 Allow)는 유지하고, 본문만 code/message 로 맞춘다.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		Object errorBody = body instanceof ErrorResponse ? body : default_body(statusCode);
		if (statusCode.is5xxServerError()) {
			log.error("서버 오류 응답 ({})", statusCode.value(), exception);
		}
		return super.handleExceptionInternal(exception, errorBody, headers, statusCode, request);
	}

	// ===== 내부 도우미 =====

	private static ErrorResponse default_body(HttpStatusCode statusCode) {
		ErrorCode errorCode = switch (statusCode.value()) {
			case 400 -> ErrorCode.INVALID_INPUT;
			case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 406 -> ErrorCode.NOT_ACCEPTABLE;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> null;
		};
		if (errorCode != null) {
			return ErrorResponse.of(errorCode);
		}
		if (statusCode.is5xxServerError()) {
			return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
		}
		// 그 밖의 표준 상태(예: 413)는 HTTP 상태 이름을 code 로 사용
		HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
		String code = httpStatus != null ? httpStatus.name() : "HTTP_" + statusCode.value();
		return new ErrorResponse(code, "요청을 처리할 수 없습니다.");
	}

	private static ResponseEntity<ErrorResponse> invalid_input(String message) {
		return ResponseEntity.badRequest().body(invalid_input_body(message));
	}

	private static ErrorResponse invalid_input_body(String message) {
		return ErrorResponse.of(ErrorCode.INVALID_INPUT, message);
	}

	private static String join_messages(List<String> messages) {
		return messages.isEmpty() ? ErrorCode.INVALID_INPUT.getMessage() : String.join(", ", messages);
	}

	private static String format_violation(ConstraintViolation<?> violation) {
		String propertyPath = violation.getPropertyPath().toString();
		String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
		return fieldName + ": " + violation.getMessage();
	}

	private static String default_message(MessageSourceResolvable error) {
		return error.getDefaultMessage() != null ? error.getDefaultMessage() : ErrorCode.INVALID_INPUT.getMessage();
	}

}

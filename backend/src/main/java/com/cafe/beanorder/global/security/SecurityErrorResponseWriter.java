package com.cafe.beanorder.global.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.cafe.beanorder.global.error.ErrorCode;
import com.cafe.beanorder.global.error.ErrorResponse;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 보안 필터 단계 오류 응답 작성기.
 * 필터에서 끝나는 요청은 @RestControllerAdvice 를 거치지 않으므로, 여기서 같은 code/message JSON 을 직접 쓴다.
 * 응답을 reset 하지 않아 앞 단계(CorsFilter)가 넣은 CORS 헤더가 유지된다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode));
	}

}

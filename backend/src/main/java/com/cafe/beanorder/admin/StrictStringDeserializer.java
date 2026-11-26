package com.cafe.beanorder.admin;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * JSON 문자열 토큰만 String 으로 읽는다. Jackson 기본 동작은 123·true 같은 값을 "123"·"true" 로 바꿔 주는데,
 * 로그인 비밀번호에서는 이를 형식 오류(400 INVALID_INPUT)로 처리하기 위해 사용한다.
 * null 은 이 역직렬화기를 거치지 않고 null 로 남아 @NotBlank 검증에서 걸린다.
 */
class StrictStringDeserializer extends ValueDeserializer<String> {

	@Override
	public String deserialize(JsonParser parser, DeserializationContext context) {
		if (parser.currentToken() == JsonToken.VALUE_STRING) {
			return parser.getString();
		}
		return (String) context.handleUnexpectedToken(String.class, parser);
	}

}

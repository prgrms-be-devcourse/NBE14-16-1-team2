package com.cafe.beanorder.global.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.cafe.beanorder.global.support.CommonModuleTestController;

@SpringBootTest
@ActiveProfiles("test")
@Import(CommonModuleTestController.class)
class GlobalExceptionHandlerTest {

	private static final String ITEMS_URL = "/api/test-support/items";

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void set_up() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	// ===== 성공 응답: 래퍼 없이 실제 데이터 =====

	@Test
	void returns_dto_without_wrapper() throws Exception {
		mockMvc.perform(get(ITEMS_URL + "/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.itemId").value(1))
			.andExpect(jsonPath("$.itemName").value("콜롬비아 원두"))
			.andExpect(jsonPath("$.success").doesNotExist())
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void returns_list_as_json_array() throws Exception {
		mockMvc.perform(get(ITEMS_URL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].itemId").value(1));
	}

	@Test
	void returns_201_with_created_dto() throws Exception {
		mockMvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemName\":\"에티오피아 원두\",\"quantity\":3}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.itemId").value(10))
			.andExpect(jsonPath("$.quantity").value(3))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	void returns_204_with_empty_body() throws Exception {
		mockMvc.perform(delete(ITEMS_URL + "/1"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	// ===== 400 입력 오류 =====

	@Test
	void returns_400_when_request_dto_is_invalid() throws Exception {
		expect_only_code_and_message(mockMvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemName\":\"\",\"quantity\":0}")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value(containsString("itemName")))
			.andExpect(jsonPath("$.message").value(containsString("quantity")));
	}

	@Test
	void returns_400_when_json_is_malformed() throws Exception {
		expect_only_code_and_message(mockMvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemName\": ")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value("요청 본문이 없거나 JSON 형식이 올바르지 않습니다."));
	}

	@Test
	void returns_400_when_request_body_is_missing() throws Exception {
		mockMvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void returns_400_when_path_variable_type_is_wrong() throws Exception {
		mockMvc.perform(get(ITEMS_URL + "/abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value("'itemId' 값의 형식이 올바르지 않습니다."));
	}

	@Test
	void returns_400_when_request_param_type_is_wrong() throws Exception {
		mockMvc.perform(get(ITEMS_URL + "/search").param("quantity", "many"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value("'quantity' 값의 형식이 올바르지 않습니다."));
	}

	@Test
	void returns_400_when_required_param_is_missing() throws Exception {
		mockMvc.perform(get(ITEMS_URL + "/search"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value("필수 파라미터 'quantity'이(가) 없습니다."));
	}

	@Test
	void returns_400_when_method_parameter_validation_fails() throws Exception {
		mockMvc.perform(get(ITEMS_URL + "/search").param("quantity", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value(containsString("quantity")));
	}

	@Test
	void returns_400_when_constraint_violation_is_thrown() throws Exception {
		mockMvc.perform(post(ITEMS_URL + "/service-validation").contentType(MediaType.APPLICATION_JSON)
				.content("{\"itemName\":\"원두\",\"quantity\":0}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.message").value(containsString("quantity")));
	}

	// ===== 업무 예외 =====

	@Test
	void returns_404_for_business_not_found() throws Exception {
		expect_only_code_and_message(mockMvc.perform(get(ITEMS_URL + "/404")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
	}

	@Test
	void returns_409_for_business_conflict() throws Exception {
		mockMvc.perform(get(ITEMS_URL + "/409"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ORDER_STATUS_CONFLICT"));
	}

	// ===== 예상하지 못한 오류 =====

	@Test
	void returns_500_with_generic_message_and_hides_details() throws Exception {
		expect_only_code_and_message(mockMvc.perform(get(ITEMS_URL + "/500")))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()))
			.andExpect(content().string(not(containsString("SELECT"))))
			.andExpect(content().string(not(containsString("password"))))
			.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	// ===== 프레임워크 상태 코드 보존 =====

	@Test
	void keeps_405_for_unsupported_method() throws Exception {
		mockMvc.perform(patch(ITEMS_URL + "/1"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(header().exists("Allow"))
			.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	void keeps_415_for_unsupported_content_type() throws Exception {
		mockMvc.perform(post(ITEMS_URL).contentType(MediaType.TEXT_PLAIN).content("itemName=원두"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void keeps_404_for_unknown_api_path() throws Exception {
		mockMvc.perform(get("/api/no-such-path"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	private static ResultActions expect_only_code_and_message(ResultActions resultActions) throws Exception {
		return resultActions
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$.code").exists())
			.andExpect(jsonPath("$.message").exists());
	}

}

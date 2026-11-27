package com.cafe.beanorder.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductApiTest {

	private static final String PRODUCTS_URL = "/api/products";
	private static final String ADMIN_PRODUCTS_URL = "/api/admin/products";
	private static final String LOGIN_URL = "/api/admin/login";

	/** application-test.properties 의 테스트 전용 관리자 비밀번호 */
	private static final String TEST_PASSWORD = "test-only-admin-password";

	private static final String VALID_BODY = """
		{"productName":"에티오피아 예가체프","price":18000,"description":"꽃향기","origin":"ETHIOPIA","imagePath":"/images/yirgacheffe.png"}
		""";

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private JsonMapper jsonMapper;

	/** 토큰 없는 요청 (고객 API, 401 확인용) */
	private MockMvc mockMvc;

	/** 관리자 토큰이 붙은 요청 (관리자 API) */
	private MockMvc adminMockMvc;

	private Product activeProduct;
	private Product deletedProduct;

	@BeforeEach
	void set_up() throws Exception {
		// 실제 Spring Security 필터(관리자 토큰 검사)를 거치도록 springSecurity() 를 적용한다.
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
		// 관리자 API 용: 로그인해서 받은 토큰을 모든 요청에 붙인다.
		String adminToken = login_and_get_token();
		adminMockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity())
			.defaultRequest(get("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
			.build();

		activeProduct = productRepository.save(
			Product.create("콜롬비아 수프리모", 15000, "산미가 있는 원두", Origin.COLOMBIA, "/images/colombia.png"));
		deletedProduct = Product.create("브라질 산토스", 12000, null, Origin.BRAZIL, null);
		deletedProduct.mark_deleted();
		productRepository.saveAndFlush(deletedProduct);
	}

	// ===== 고객 =====

	@Test
	void customer_list_excludes_deleted_products() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].productId").value(activeProduct.getId()))
			.andExpect(jsonPath("$[0].productName").value("콜롬비아 수프리모"))
			.andExpect(jsonPath("$[0].price").value(15000))
			.andExpect(jsonPath("$[0].origin").value("COLOMBIA"))
			.andExpect(jsonPath("$[0].isDeleted").doesNotExist());
	}

	@Test
	void customer_detail_returns_product() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL + "/" + activeProduct.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.productId").value(activeProduct.getId()))
			.andExpect(jsonPath("$.description").value("산미가 있는 원두"))
			.andExpect(jsonPath("$.imagePath").value("/images/colombia.png"));
	}

	@Test
	void customer_detail_of_deleted_product_returns_404() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL + "/" + deletedProduct.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void customer_detail_of_missing_product_returns_404() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL + "/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	// ===== 등록 =====

	@Test
	void create_returns_201_and_saves_product() throws Exception {
		adminMockMvc.perform(post(ADMIN_PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.productId").isNumber())
			.andExpect(jsonPath("$.productName").value("에티오피아 예가체프"))
			.andExpect(jsonPath("$.price").value(18000))
			.andExpect(jsonPath("$.origin").value("ETHIOPIA"))
			.andExpect(jsonPath("$.isDeleted").value(false))
			.andExpect(jsonPath("$.createdAt").exists())
			.andExpect(jsonPath("$.updatedAt").exists());

		assertThat(productRepository.count()).isEqualTo(3);
	}

	@Test
	void create_with_invalid_values_returns_400() throws Exception {
		String body = """
			{"productName":" ","price":0,"origin":null}
			""";
		adminMockMvc.perform(post(ADMIN_PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));

		assertThat(productRepository.count()).isEqualTo(2);
	}

	@Test
	void create_with_unknown_origin_returns_400() throws Exception {
		String body = """
			{"productName":"케냐 AA","price":17000,"origin":"KENYA"}
			""";
		adminMockMvc.perform(post(ADMIN_PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	// ===== 수정 =====

	@Test
	void update_replaces_product_info() throws Exception {
		String body = """
			{"productName":"콜롬비아 나리뇨","price":16000,"origin":"COLOMBIA"}
			""";
		adminMockMvc.perform(put(ADMIN_PRODUCTS_URL + "/" + activeProduct.getId())
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.productId").value(activeProduct.getId()))
			.andExpect(jsonPath("$.productName").value("콜롬비아 나리뇨"))
			.andExpect(jsonPath("$.price").value(16000))
			.andExpect(jsonPath("$.description").doesNotExist())
			.andExpect(jsonPath("$.imagePath").doesNotExist());
	}

	@Test
	void update_of_deleted_product_returns_404() throws Exception {
		adminMockMvc.perform(put(ADMIN_PRODUCTS_URL + "/" + deletedProduct.getId())
				.contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void update_with_invalid_values_returns_400() throws Exception {
		String body = """
			{"productName":"콜롬비아","price":-1,"origin":"COLOMBIA"}
			""";
		adminMockMvc.perform(put(ADMIN_PRODUCTS_URL + "/" + activeProduct.getId())
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	// ===== 삭제 =====

	@Test
	void delete_marks_flag_and_hides_from_customer() throws Exception {
		adminMockMvc.perform(delete(ADMIN_PRODUCTS_URL + "/" + activeProduct.getId()))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		// 행은 남아 있고 플래그만 바뀐다
		Product found = productRepository.findById(activeProduct.getId()).orElseThrow();
		assertThat(found.isDeleted()).isTrue();
		assertThat(productRepository.count()).isEqualTo(2);

		// 고객에게는 보이지 않는다
		mockMvc.perform(get(PRODUCTS_URL + "/" + activeProduct.getId()))
			.andExpect(status().isNotFound());
		mockMvc.perform(get(PRODUCTS_URL))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void delete_of_already_deleted_product_returns_404() throws Exception {
		adminMockMvc.perform(delete(ADMIN_PRODUCTS_URL + "/" + deletedProduct.getId()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void delete_of_missing_product_returns_404() throws Exception {
		adminMockMvc.perform(delete(ADMIN_PRODUCTS_URL + "/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	// ===== 관리자 인증 =====

	@Test
	void admin_apis_without_token_return_401_and_change_nothing() throws Exception {
		String productUrl = ADMIN_PRODUCTS_URL + "/" + activeProduct.getId();

		expect_unauthorized(mockMvc.perform(
			post(ADMIN_PRODUCTS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)));
		expect_unauthorized(mockMvc.perform(
			put(productUrl).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY)));
		expect_unauthorized(mockMvc.perform(delete(productUrl)));

		Product unchanged = productRepository.findById(activeProduct.getId()).orElseThrow();
		assertThat(unchanged.getProductName()).isEqualTo("콜롬비아 수프리모");
		assertThat(unchanged.isDeleted()).isFalse();
		assertThat(productRepository.count()).isEqualTo(2);
	}

	@Test
	void admin_api_with_invalid_token_returns_401() throws Exception {
		expect_unauthorized(mockMvc.perform(delete(ADMIN_PRODUCTS_URL + "/" + activeProduct.getId())
			.header(HttpHeaders.AUTHORIZATION, "Bearer not-issued-token")));

		assertThat(productRepository.findById(activeProduct.getId()).orElseThrow().isDeleted()).isFalse();
	}

	@Test
	void customer_apis_do_not_require_token() throws Exception {
		mockMvc.perform(get(PRODUCTS_URL))
			.andExpect(status().isOk());
		mockMvc.perform(get(PRODUCTS_URL + "/" + activeProduct.getId()))
			.andExpect(status().isOk());
	}

	// ===== 도우미 =====

	private String login_and_get_token() throws Exception {
		String body = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"" + TEST_PASSWORD + "\"}"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		return jsonMapper.readTree(body).get("token").asString();
	}

	private static void expect_unauthorized(ResultActions resultActions) throws Exception {
		resultActions
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

}

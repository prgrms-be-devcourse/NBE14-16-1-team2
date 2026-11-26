package com.cafe.beanorder.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;
import com.cafe.beanorder.support.MutableTestClock;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductEntityTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 11, 27, 9, 0, 0);

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		MutableTestClock mutableTestClock() {
			return new MutableTestClock(START_TIME.atZone(SEOUL).toInstant(), SEOUL);
		}

	}

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private MutableTestClock mutableTestClock;

	@BeforeEach
	void reset_clock() {
		mutableTestClock.set_instant(START_TIME.atZone(SEOUL).toInstant());
	}

	@Test
	void saves_and_finds_product_with_generated_id() {
		Product product = Product.create("콜롬비아 수프리모", 15000, "산미가 있는 원두", Origin.COLOMBIA, "/images/colombia.png");

		entityManager.persist(product);
		flush_and_clear();

		Product foundProduct = entityManager.find(Product.class, product.getId());
		assertThat(product.getId()).isNotNull();
		assertThat(foundProduct.getProductName()).isEqualTo("콜롬비아 수프리모");
		assertThat(foundProduct.getPrice()).isEqualTo(15000);
		assertThat(foundProduct.getDescription()).isEqualTo("산미가 있는 원두");
		assertThat(foundProduct.getOrigin()).isEqualTo(Origin.COLOMBIA);
		assertThat(foundProduct.getImagePath()).isEqualTo("/images/colombia.png");
	}

	@Test
	void stores_origin_as_string() {
		Product product = Product.create("예가체프", 18000, null, Origin.ETHIOPIA, null);
		entityManager.persist(product);
		entityManager.flush();

		Object storedOrigin = entityManager
			.createNativeQuery("SELECT origin FROM product WHERE product_id = ?1")
			.setParameter(1, product.getId())
			.getSingleResult();
		assertThat(storedOrigin).hasToString("ETHIOPIA");
	}

	@Test
	void new_product_is_not_deleted() {
		Product product = Product.create("브라질 산토스", 12000, null, Origin.BRAZIL, null);
		entityManager.persist(product);
		flush_and_clear();

		assertThat(entityManager.find(Product.class, product.getId()).isDeleted()).isFalse();
	}

	@Test
	void records_created_at_and_refreshes_updated_at_on_change() {
		Product product = Product.create("브라질 산토스", 12000, null, Origin.BRAZIL, null);
		entityManager.persist(product);
		flush_and_clear();

		Product savedProduct = entityManager.find(Product.class, product.getId());
		assertThat(savedProduct.getCreatedAt()).isEqualTo(START_TIME);
		assertThat(savedProduct.getUpdatedAt()).isEqualTo(START_TIME);

		mutableTestClock.advance(Duration.ofHours(1));
		savedProduct.update_info("브라질 세하도", 13000, "고소한 맛", Origin.BRAZIL, null);
		flush_and_clear();

		Product updatedProduct = entityManager.find(Product.class, product.getId());
		assertThat(updatedProduct.getCreatedAt()).isEqualTo(START_TIME);
		assertThat(updatedProduct.getUpdatedAt()).isEqualTo(START_TIME.plusHours(1));
		assertThat(updatedProduct.getProductName()).isEqualTo("브라질 세하도");
		assertThat(updatedProduct.getPrice()).isEqualTo(13000);
	}

	@Test
	void mark_deleted_keeps_row() {
		Product product = Product.create("기타 블렌드", 10000, null, Origin.OTHER, null);
		entityManager.persist(product);
		flush_and_clear();

		entityManager.find(Product.class, product.getId()).mark_deleted();
		flush_and_clear();

		Product deletedProduct = entityManager.find(Product.class, product.getId());
		assertThat(deletedProduct).isNotNull();
		assertThat(deletedProduct.isDeleted()).isTrue();
	}

	@Test
	void rejects_non_positive_price_and_missing_required_values() {
		assert_invalid_input(() -> Product.create("원두", 0, null, Origin.OTHER, null));
		assert_invalid_input(() -> Product.create("원두", -1000, null, Origin.OTHER, null));
		assert_invalid_input(() -> Product.create(" ", 1000, null, Origin.OTHER, null));
		assert_invalid_input(() -> Product.create("원두", 1000, null, null, null));

		Product product = Product.create("원두", 1000, null, Origin.OTHER, null);
		assert_invalid_input(() -> product.update_info("원두", 0, null, Origin.OTHER, null));
		assertThat(product.getPrice()).isEqualTo(1000);
	}

	@Test
	void uses_controllable_clock_in_asia_seoul() {
		assertThat(Instant.now(mutableTestClock)).isEqualTo(START_TIME.atZone(SEOUL).toInstant());
		assertThat(mutableTestClock.getZone()).isEqualTo(SEOUL);
	}

	private void assert_invalid_input(Runnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	private void flush_and_clear() {
		entityManager.flush();
		entityManager.clear();
	}

}

package com.cafe.beanorder.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductDataInitializerTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ApplicationContext applicationContext;

	private ProductDataInitializer productDataInitializer;

	@BeforeEach
	void set_up() {
		// test 프로필에서는 빈으로 등록되지 않으므로 직접 만든다.
		productDataInitializer = new ProductDataInitializer(productRepository);
	}

	@Test
	void is_not_registered_in_test_profile() {
		assertThat(applicationContext.getBeansOfType(ProductDataInitializer.class)).isEmpty();
		assertThat(productRepository.count()).isZero();
	}

	@Test
	void inserts_default_products_when_empty() {
		int insertedCount = productDataInitializer.insert_default_products();

		assertThat(insertedCount).isEqualTo(ProductDataInitializer.default_products().size());
		assertThat(productRepository.findAllByIsDeletedFalseOrderByIdAsc())
			.extracting(Product::getProductName)
				.containsExactly("Colombia Narino", "Brazil Serra Do Caparao", "Colombia Quindio", "Ethiopia Sidamo");
	}

	@Test
	void does_not_insert_twice() {
		productDataInitializer.insert_default_products();
		long countAfterFirstRun = productRepository.count();

		int insertedCount = productDataInitializer.insert_default_products();

		assertThat(insertedCount).isZero();
		assertThat(productRepository.count()).isEqualTo(countAfterFirstRun);
	}

	@Test
	void does_not_insert_when_products_already_exist() {
		productRepository.save(Product.create("관리자 등록 상품", 10000, null, Origin.OTHER, null));

		int insertedCount = productDataInitializer.insert_default_products();

		assertThat(insertedCount).isZero();
		assertThat(productRepository.count()).isEqualTo(1);
	}

}

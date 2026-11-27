package com.cafe.beanorder.product;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * local 프로필로 서버를 켤 때 기본 상품을 넣는다.
 *
 * <ul>
 *   <li>상품 테이블이 비어 있을 때만 넣는다. 재시작해도 중복되지 않고, 삭제된 상품이 하나라도 있으면 넣지 않는다.</li>
 *   <li>test 프로필에서는 동작하지 않으므로 테스트 데이터에 섞이지 않는다.</li>
 *   <li>기본 상품으로 다시 시작하려면 서버를 끄고 backend/data 폴더를 지운 뒤 다시 켠다.</li>
 * </ul>
 *
 * 상품 이름·가격·설명은 임시 값이다. (팀 확정 필요)
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class ProductDataInitializer implements ApplicationRunner {

	private final ProductRepository productRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		insert_default_products();
	}

	/** 상품이 하나도 없을 때만 기본 상품을 저장한다. 저장한 개수를 돌려준다. */
	int insert_default_products() {
		if (productRepository.count() > 0) {
			return 0;
		}
		List<Product> defaultProducts = default_products();
		productRepository.saveAll(defaultProducts);
		return defaultProducts.size();
	}

	static List<Product> default_products() {
		return List.of(
			Product.create("Colombia Narino", 5000, "균형 잡힌 산미와 단맛이 있는 원두", Origin.COLOMBIA, null),
			Product.create("Brazil Serra Do Caparao", 6000, "산미가 적고 바디감이 묵직한 맛의 원두", Origin.BRAZIL, null),
			Product.create("Colombia Quindio", 8000, "청포도와 꽃향기가 느껴지는 원두", Origin.COLOMBIA, null),
			Product.create("Ethiopia Sidamo", 7000, "홍차처럼 가볍고 맑은 원두", Origin.ETHIOPIA, null));
	}

}

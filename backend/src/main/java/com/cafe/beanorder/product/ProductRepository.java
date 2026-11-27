package com.cafe.beanorder.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상품 Repository.
 *
 * <p>Product 에는 전역 삭제 필터가 없다. 고객 조회는 IsDeletedFalse 메서드를 쓰고,
 * 관리자 조회는 삭제 상품도 포함하는 메서드(findAllByOrderByIdAsc, findById)를 쓴다.</p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	/** 고객 상품 목록: 삭제되지 않은 상품만, 상품 ID 오름차순. */
	List<Product> findAllByIsDeletedFalseOrderByIdAsc();

	/** 고객 상품 상세: 삭제된 상품이면 빈 값. */
	Optional<Product> findByIdAndIsDeletedFalse(Long id);

	/** 관리자 상품 목록: 삭제 상품 포함, 상품 ID 오름차순. */
	List<Product> findAllByOrderByIdAsc();

}

package com.cafe.beanorder.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상품 Repository.
 *
 * <p>Product 에는 전역 삭제 필터가 없다. 고객 조회는 IsDeletedFalse 메서드를 쓰고,
 * 관리자 수정·삭제는 삭제 상품도 찾는 기본 메서드(findById)로 찾은 뒤 삭제 여부를 확인한다.</p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

	/** 고객 상품 목록: 삭제되지 않은 상품만, 상품 ID 오름차순. */
	List<Product> findAllByIsDeletedFalseOrderByIdAsc();

	/** 고객 상품 상세: 삭제된 상품이면 빈 값. */
	Optional<Product> findByIdAndIsDeletedFalse(Long id);

}

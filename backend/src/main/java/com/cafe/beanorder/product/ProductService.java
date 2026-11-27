package com.cafe.beanorder.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 상품 조회·등록·수정·삭제.
 *
 * <ul>
 *   <li>고객 조회: 삭제된 상품은 목록에서 빠지고, 상세는 404(PRODUCT_NOT_FOUND).</li>
 *   <li>관리자 조회: 삭제된 상품도 포함.</li>
 *   <li>삭제: 행을 지우지 않고 is_deleted 만 true 로 바꾼다.</li>
 *   <li>이미 삭제된 상품의 수정·삭제는 404(PRODUCT_NOT_FOUND). 복구 기능이 없으므로 삭제 상품은 더 바꾸지 않는다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	// ===== 고객 =====

	public List<ProductResponse> get_products() {
		return productRepository.findAllByIsDeletedFalseOrderByIdAsc().stream()
			.map(ProductResponse::from)
			.toList();
	}

	public ProductResponse get_product(Long productId) {
		Product product = productRepository.findByIdAndIsDeletedFalse(productId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		return ProductResponse.from(product);
	}

	// ===== 관리자 =====

	public List<AdminProductResponse> get_admin_products() {
		return productRepository.findAllByOrderByIdAsc().stream()
			.map(AdminProductResponse::from)
			.toList();
	}

	public AdminProductResponse get_admin_product(Long productId) {
		return AdminProductResponse.from(find_product(productId));
	}

	@Transactional
	public AdminProductResponse create_product(ProductRequest request) {
		Product product = Product.create(
			request.productName(),
			request.price(),
			request.description(),
			request.origin(),
			request.imagePath());
		// saveAndFlush: 응답에 Auditing 으로 채워진 createdAt·updatedAt 을 담기 위해 바로 반영한다.
		return AdminProductResponse.from(productRepository.saveAndFlush(product));
	}

	@Transactional
	public AdminProductResponse update_product(Long productId, ProductRequest request) {
		Product product = find_active_product(productId);
		product.update_info(
			request.productName(),
			request.price(),
			request.description(),
			request.origin(),
			request.imagePath());
		// flush: 응답의 updatedAt 이 수정 시각으로 갱신되도록 바로 반영한다.
		productRepository.flush();
		return AdminProductResponse.from(product);
	}

	@Transactional
	public void delete_product(Long productId) {
		find_active_product(productId).mark_deleted();
	}

	private Product find_product(Long productId) {
		return productRepository.findById(productId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	private Product find_active_product(Long productId) {
		Product product = find_product(productId);
		if (product.isDeleted()) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		return product;
	}

}

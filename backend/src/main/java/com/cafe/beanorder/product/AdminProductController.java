package com.cafe.beanorder.product;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 상품 API (등록·수정·논리 삭제).
 * /api/admin/** 이라 global.security 설정이 관리자 Bearer 토큰을 검사한다.
 * 관리자용 상품 목록·상세 조회는 다음 작업에서 추가한다.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final ProductService productService;

	/** 상품 등록 → 201 */
	@PostMapping
	public ResponseEntity<AdminProductResponse> create_product(@Valid @RequestBody ProductRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create_product(request));
	}

	/** 상품 수정 → 200, 없거나 삭제된 상품이면 404 */
	@PutMapping("/{productId}")
	public AdminProductResponse update_product(@PathVariable Long productId,
			@Valid @RequestBody ProductRequest request) {
		return productService.update_product(productId, request);
	}

	/** 상품 삭제(is_deleted = true) → 204, 없거나 이미 삭제된 상품이면 404 */
	@DeleteMapping("/{productId}")
	public ResponseEntity<Void> delete_product(@PathVariable Long productId) {
		productService.delete_product(productId);
		return ResponseEntity.noContent().build();
	}

}

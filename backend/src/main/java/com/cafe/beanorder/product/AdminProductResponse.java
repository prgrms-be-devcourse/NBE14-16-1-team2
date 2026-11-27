package com.cafe.beanorder.product;

import java.time.LocalDateTime;

/**
 * 관리자용 상품 응답 (목록·상세·등록·수정 공통).
 * 고객 응답 필드 + isDeleted, createdAt, updatedAt.
 */
public record AdminProductResponse(
		Long productId,
		String productName,
		int price,
		String description,
		Origin origin,
		String imagePath,
		boolean isDeleted,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static AdminProductResponse from(Product product) {
		return new AdminProductResponse(
			product.getId(),
			product.getProductName(),
			product.getPrice(),
			product.getDescription(),
			product.getOrigin(),
			product.getImagePath(),
			product.isDeleted(),
			product.getCreatedAt(),
			product.getUpdatedAt());
	}

}

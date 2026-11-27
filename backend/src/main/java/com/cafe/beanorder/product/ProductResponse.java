package com.cafe.beanorder.product;

/** 고객용 상품 응답 (목록·상세 공통). 삭제 여부와 관리용 일시는 포함하지 않는다. */
public record ProductResponse(
		Long productId,
		String productName,
		int price,
		String description,
		Origin origin,
		String imagePath) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
			product.getId(),
			product.getProductName(),
			product.getPrice(),
			product.getDescription(),
			product.getOrigin(),
			product.getImagePath());
	}

}

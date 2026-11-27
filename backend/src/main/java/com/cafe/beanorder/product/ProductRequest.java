package com.cafe.beanorder.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 관리자 상품 등록(POST)·수정(PUT) 요청.
 * PUT 은 전체 교체이므로 등록과 같은 필드를 모두 보낸다. (description, imagePath 는 생략하면 null)
 */
public record ProductRequest(
		@NotBlank @Size(max = 100) String productName,
		@NotNull @Min(1) Integer price,
		String description,
		@NotNull Origin origin,
		@Size(max = 255) String imagePath) {
}

package com.cafe.beanorder.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** 고객 상품 API. 삭제된 상품은 보이지 않는다. */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	/** 상품 목록 조회 → 200 */
	@GetMapping
	public List<ProductResponse> get_products() {
		return productService.get_products();
	}

	/** 상품 상세 조회 → 200, 없거나 삭제된 상품이면 404 */
	@GetMapping("/{productId}")
	public ProductResponse get_product(@PathVariable Long productId) {
		return productService.get_product(productId);
	}

}

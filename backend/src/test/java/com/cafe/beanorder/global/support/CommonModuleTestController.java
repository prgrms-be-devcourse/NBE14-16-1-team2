package com.cafe.beanorder.global.support;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 공통 모듈 검증 전용 컨트롤러.
 * src/test 에만 있고 @TestComponent 라서 컴포넌트 스캔 대상이 아니며, 테스트에서 @Import 로만 등록된다.
 */
@TestComponent
@RestController
@RequestMapping("/api/test-support/items")
@RequiredArgsConstructor
public class CommonModuleTestController {

	public static final String SECRET_DETAIL = "SELECT * FROM product WHERE password='secret'";

	private final Validator validator;

	public record ItemRequest(@NotBlank String itemName, @NotNull @Min(1) Integer quantity) {
	}

	public record ItemResponse(Long itemId, String itemName, Integer quantity) {
	}

	@GetMapping
	public List<ItemResponse> get_items() {
		return List.of(new ItemResponse(1L, "콜롬비아 원두", 2), new ItemResponse(2L, "브라질 원두", 1));
	}

	@GetMapping("/{itemId}")
	public ItemResponse get_item(@PathVariable Long itemId) {
		if (itemId == 404L) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		if (itemId == 409L) {
			throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
		}
		if (itemId == 500L) {
			throw new IllegalStateException(SECRET_DETAIL);
		}
		return new ItemResponse(itemId, "콜롬비아 원두", 2);
	}

	@PostMapping
	public ResponseEntity<ItemResponse> create_item(@Valid @RequestBody ItemRequest itemRequest) {
		ItemResponse itemResponse = new ItemResponse(10L, itemRequest.itemName(), itemRequest.quantity());
		return ResponseEntity.status(HttpStatus.CREATED).body(itemResponse);
	}

	@DeleteMapping("/{itemId}")
	public ResponseEntity<Void> delete_item(@PathVariable Long itemId) {
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public List<ItemResponse> search_items(@RequestParam @Min(1) int quantity) {
		return List.of(new ItemResponse(1L, "콜롬비아 원두", quantity));
	}

	@PostMapping("/service-validation")
	public ItemResponse validate_in_service(@RequestBody ItemRequest itemRequest) {
		Set<ConstraintViolation<ItemRequest>> violations = validator.validate(itemRequest);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		return new ItemResponse(11L, itemRequest.itemName(), itemRequest.quantity());
	}

}

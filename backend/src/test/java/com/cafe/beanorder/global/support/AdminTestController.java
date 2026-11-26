package com.cafe.beanorder.global.support;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;
import com.cafe.beanorder.global.support.CommonModuleTestController.ItemRequest;
import com.cafe.beanorder.global.support.CommonModuleTestController.ItemResponse;

/**
 * 관리자 API 보호 검증 전용 컨트롤러. 업무 API 가 아직 없어서 src/test 에만 둔다. (@TestComponent, @Import 로만 등록)
 * 핸들러가 실제로 실행된 횟수를 세어, 인증 실패·preflight 에서 관리자 로직이 실행되지 않았는지 확인한다.
 */
@TestComponent
@RestController
@RequestMapping("/api/admin/test-support/items")
public class AdminTestController {

	public static final AtomicInteger HANDLER_CALLS = new AtomicInteger();

	@GetMapping
	public List<ItemResponse> get_items() {
		HANDLER_CALLS.incrementAndGet();
		return List.of(new ItemResponse(1L, "관리자 전용 원두", 5));
	}

	@GetMapping("/{itemId}")
	public ItemResponse get_item(@PathVariable Long itemId) {
		HANDLER_CALLS.incrementAndGet();
		if (itemId == 404L) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		if (itemId == 409L) {
			throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
		}
		if (itemId == 500L) {
			throw new IllegalStateException("관리자 테스트 내부 오류");
		}
		return new ItemResponse(itemId, "관리자 전용 원두", 5);
	}

	@PostMapping
	public ItemResponse create_item(@Valid @RequestBody ItemRequest itemRequest) {
		HANDLER_CALLS.incrementAndGet();
		return new ItemResponse(10L, itemRequest.itemName(), itemRequest.quantity());
	}

	/** 컨테이너 오류 처리(ERROR dispatch)를 거치는 응답. 인증 통과 후 오류 단계에서 401 로 바뀌지 않는지 확인용. */
	@GetMapping("/send-error")
	public void send_error(HttpServletResponse response) throws IOException {
		HANDLER_CALLS.incrementAndGet();
		response.sendError(HttpServletResponse.SC_CONFLICT);
	}

}

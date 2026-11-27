package com.cafe.beanorder.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 관리자 주문 수정 요청. PUT 이므로 모든 값을 보내고, items 는 기존 목록 전체를 교체한다. */
public record OrderUpdateRequest(
		@NotBlank @Size(max = 255) String address,
		@NotBlank @Size(max = 10) String zipcode,
		@NotEmpty List<@Valid OrderItemRequest> items) {
}

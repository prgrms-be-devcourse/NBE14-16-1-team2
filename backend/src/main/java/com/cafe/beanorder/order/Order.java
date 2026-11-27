package com.cafe.beanorder.order;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;
import com.cafe.beanorder.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.cafe.beanorder.global.error.DomainPreconditions.require_text;
import static com.cafe.beanorder.global.error.DomainPreconditions.require_value;

/**
 * 주문 (테이블 orders, ORDER 는 SQL 예약어).
 *
 * <ul>
 *   <li>주문 시각과 배송 예정일은 생성 시 외부에서 받는다. (14시 기준 배송일 계산은 아직 없음)</li>
 *   <li>신규 주문의 상태는 ORDERED 로 고정된다. 상태 전환 메서드는 업무 정책이 확정된 뒤 추가한다.</li>
 *   <li>주문 상세는 add_order_item() 으로만 추가하며, 주문을 persist 하면 함께 저장된다(CascadeType.PERSIST).</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long id;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "address", nullable = false, length = 255)
	private String address;

	@Column(name = "zipcode", nullable = false, length = 10)
	private String zipcode;

	@Column(name = "ordered_at", nullable = false)
	private LocalDateTime orderedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "delivery_date", nullable = false)
	private LocalDate deliveryDate;

	@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, orphanRemoval = true)
	private final List<OrderItem> orderItems = new ArrayList<>();

	private Order(String email, String address, String zipcode, LocalDateTime orderedAt, LocalDate deliveryDate) {
		this.email = require_text(email, "email");
		this.address = require_text(address, "address");
		this.zipcode = require_text(zipcode, "zipcode");
		this.orderedAt = require_value(orderedAt, "orderedAt");
		this.deliveryDate = require_value(deliveryDate, "deliveryDate");
		this.status = OrderStatus.ORDERED;
	}

	public static Order create(String email, String address, String zipcode, LocalDateTime orderedAt,
			LocalDate deliveryDate) {
		return new Order(email, address, zipcode, orderedAt, deliveryDate);
	}

	/**
	 * 주문 상세를 추가한다. 상품의 현재 이름과 가격을 스냅샷으로 복사하고,
	 * 컬렉션과 OrderItem.order(연관관계 주인)를 함께 설정한다.
	 */
	public OrderItem add_order_item(Product product, int quantity) {
		OrderItem orderItem = OrderItem.create(this, product, quantity);
		orderItems.add(orderItem);
		return orderItem;
	}

	/** 주문 총액 = 각 상세의 (주문 당시 단가 × 수량) 합계. 저장 컬럼 없이 계산만 한다. */
	public long calculate_total_amount() {
		return orderItems.stream()
			.mapToLong(OrderItem::calculate_amount)
			.sum();
	}

	/** 읽기 전용 목록. 상세 추가는 add_order_item() 을 사용한다. */
	public List<OrderItem> getOrderItems() {
		return Collections.unmodifiableList(orderItems);
	}

    	/** 관리자 주문 취소. ORDERED 상태에서만 가능하고, 그 외 상태면 409. */
	public void cancel() {
		if (status != OrderStatus.ORDERED) {
			throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
		}
		this.status = OrderStatus.CANCELLED;
	}

    	/** 관리자 주문 수정: 주소·우편번호 변경. ORDERED 상태에서만 가능하고, 그 외 상태면 409. */
	public void update(String address, String zipcode) {
		require_ordered();
		this.address = require_text(address, "address");
		this.zipcode = require_text(zipcode, "zipcode");
	}

	/**
	 * 관리자 주문 수정: 주문 상품 목록을 요청 목록으로 교체한다.
	 * 이미 있던 상품은 수량만 바꿔 주문 당시 상품명·단가를 유지하고,
	 * 새 상품은 현재 상품 정보로 추가하며, 요청에 없는 상품은 삭제된다(orphanRemoval).
	 */
	public void replace_order_items(List<Product> products, List<Integer> quantities) {
		require_ordered();
		Map<Long, OrderItem> existingItems = new HashMap<>();
		for (OrderItem item : orderItems) {
			existingItems.put(item.getProduct().getId(), item);
		}

		List<OrderItem> newItems = new ArrayList<>();
		for (int i = 0; i < products.size(); i++) {
			Product product = products.get(i);
			int quantity = quantities.get(i);
			OrderItem item = existingItems.remove(product.getId());
			if (item != null) {
				item.change_quantity(quantity);
			} else {
				item = OrderItem.create(this, product, quantity);
			}
			newItems.add(item);
		}

		orderItems.clear();
		orderItems.addAll(newItems);
	}

	/**
	 * 주문 병합: other 주문의 상세를 이 주문으로 합친다.
	 * 같은 상품은 수량을 더하고 이 주문의 주문 당시 단가를 유지한다. other 는 호출한 쪽에서 삭제한다.
	 */
	public void merge_from(Order other) {
		require_ordered();
		for (OrderItem source : other.orderItems) {
			OrderItem sameItem = find_item_by_product(source.getProduct().getId());
			if (sameItem != null) {
				sameItem.change_quantity(sameItem.getQuantity() + source.getQuantity());
			} else {
				orderItems.add(OrderItem.copy_of(this, source));
			}
		}
	}

	private OrderItem find_item_by_product(Long productId) {
		for (OrderItem item : orderItems) {
			if (item.getProduct().getId().equals(productId)) {
				return item;
			}
		}
		return null;
	}

	private void require_ordered() {
		if (status != OrderStatus.ORDERED) {
			throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT);
		}
	}

}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

	@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
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

}

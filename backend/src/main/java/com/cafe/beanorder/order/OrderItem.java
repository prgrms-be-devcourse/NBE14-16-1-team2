package com.cafe.beanorder.order;

import com.cafe.beanorder.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import static com.cafe.beanorder.global.error.DomainPreconditions.require_positive;
import static com.cafe.beanorder.global.error.DomainPreconditions.require_value;

/**
 * 주문 상세 (테이블 order_item). 주문-상세 양방향 관계의 주인(FK 보유)이다.
 *
 * <ul>
 *   <li>productName·orderPrice 는 주문 당시 상품명·1개당 단가 스냅샷이다. 상품이 바뀌어도 변하지 않는다.</li>
 *   <li>DB FK 삭제 규칙: order_id 는 ON DELETE CASCADE, product_id 는 ON DELETE RESTRICT.
 *       Product 쪽으로는 JPA cascade 를 전파하지 않는다.</li>
 * </ul>
 */
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	@OnDelete(action = OnDeleteAction.RESTRICT)
	private Product product;

	@Column(name = "product_name", nullable = false, length = 100)
	private String productName;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "order_price", nullable = false)
	private int orderPrice;

	private OrderItem(Order order, Product product, int quantity) {
		this.order = require_value(order, "order");
		this.product = require_value(product, "product");
		this.quantity = require_positive(quantity, "quantity");
		this.productName = product.getProductName();
		this.orderPrice = product.getPrice();
	}

	/** Order.add_order_item() 에서만 사용한다. */
	static OrderItem create(Order order, Product product, int quantity) {
		return new OrderItem(order, product, quantity);
	}

	/** 항목 금액 = 주문 당시 단가 × 수량. int 오버플로를 피하려고 곱하기 전에 long 으로 바꾼다. */
	public long calculate_amount() {
		return (long) orderPrice * quantity;
	}

    	/** 관리자 주문 수정에서 수량만 바꾼다. 주문 당시 상품명·단가 스냅샷은 그대로 둔다. */
	void change_quantity(int quantity) {
		this.quantity = require_positive(quantity, "quantity");
	}

	/** 주문 병합: 다른 주문의 상세를 옮길 때 주문 당시 상품명·단가를 그대로 복사한다. */
	static OrderItem copy_of(Order order, OrderItem source) {
		OrderItem copy = new OrderItem(order, source.product, source.quantity);
		copy.productName = source.productName;
		copy.orderPrice = source.orderPrice;
		return copy;
	}

}

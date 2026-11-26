package com.cafe.beanorder.product;

import static com.cafe.beanorder.global.error.DomainPreconditions.require_positive;
import static com.cafe.beanorder.global.error.DomainPreconditions.require_text;
import static com.cafe.beanorder.global.error.DomainPreconditions.require_value;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 원두 상품 (테이블 product).
 *
 * <ul>
 *   <li>price 는 현재 판매 단가다. 주문 당시 단가는 OrderItem.orderPrice 에 따로 저장한다.</li>
 *   <li>삭제는 물리 삭제 대신 mark_deleted() 로 is_deleted 를 true 로 바꾼다.
 *       전역 필터가 없으므로, 고객 조회·주문 생성에서는 삭제 상품을 직접 제외해야 한다.</li>
 * </ul>
 */
@Entity
@Table(name = "product")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long id;

	@Column(name = "product_name", nullable = false, length = 100)
	private String productName;

	@Column(name = "price", nullable = false)
	private int price;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "origin", nullable = false, length = 20)
	private Origin origin;

	@Column(name = "image_path", length = 255)
	private String imagePath;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted;

	private Product(String productName, int price, String description, Origin origin, String imagePath) {
		apply_info(productName, price, description, origin, imagePath);
		this.isDeleted = false;
	}

	public static Product create(String productName, int price, String description, Origin origin,
			String imagePath) {
		return new Product(productName, price, description, origin, imagePath);
	}

	/** 관리자 상품 수정. 기존 주문 상세의 상품명·단가 스냅샷에는 영향을 주지 않는다. */
	public void update_info(String productName, int price, String description, Origin origin, String imagePath) {
		apply_info(productName, price, description, origin, imagePath);
	}

	/** 논리 삭제. 행과 기존 주문 상세의 참조는 그대로 남는다. (복구 기능 없음) */
	public void mark_deleted() {
		this.isDeleted = true;
	}

	private void apply_info(String productName, int price, String description, Origin origin, String imagePath) {
		this.productName = require_text(productName, "productName");
		this.price = require_positive(price, "price");
		this.origin = require_value(origin, "origin");
		this.description = description;
		this.imagePath = imagePath;
	}

}

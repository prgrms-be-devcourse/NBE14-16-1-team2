package com.cafe.beanorder.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.cafe.beanorder.global.error.BusinessException;
import com.cafe.beanorder.global.error.ErrorCode;
import com.cafe.beanorder.product.Origin;
import com.cafe.beanorder.product.Product;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderEntityTest {

	private static final LocalDateTime ORDERED_AT = LocalDateTime.of(2026, 11, 27, 10, 30);
	private static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 11, 27);

	@Autowired
	private EntityManager entityManager;

	@Test
	void new_order_starts_as_ordered_and_stores_status_as_string() {
		Order order = new_order();
		entityManager.persist(order);
		entityManager.flush();

		Object storedStatus = entityManager
			.createNativeQuery("SELECT status FROM orders WHERE order_id = ?1")
			.setParameter(1, order.getId())
			.getSingleResult();
		assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
		assertThat(storedStatus).hasToString("ORDERED");
	}

	@Test
	void saves_order_with_items_by_cascade_and_reloads_after_clear() {
		Product colombia = persist_product("콜롬비아", 15000);
		Product brazil = persist_product("브라질", 12000);

		Order order = new_order();
		order.add_order_item(colombia, 2);
		order.add_order_item(brazil, 3);
		entityManager.persist(order);
		flush_and_clear();

		Order foundOrder = entityManager.find(Order.class, order.getId());
		assertThat(foundOrder.getEmail()).isEqualTo("customer@example.com");
		assertThat(foundOrder.getAddress()).isEqualTo("서울시 강남구 테헤란로 1");
		assertThat(foundOrder.getZipcode()).isEqualTo("06236");
		assertThat(foundOrder.getOrderedAt()).isEqualTo(ORDERED_AT);
		assertThat(foundOrder.getDeliveryDate()).isEqualTo(DELIVERY_DATE);
		assertThat(foundOrder.getOrderItems()).hasSize(2)
			.allSatisfy(orderItem -> {
				assertThat(orderItem.getId()).isNotNull();
				assertThat(orderItem.getOrder()).isSameAs(foundOrder);
			});
		assertThat(foundOrder.getOrderItems())
			.extracting(orderItem -> orderItem.getProduct().getId())
			.containsExactlyInAnyOrder(colombia.getId(), brazil.getId());

		List<?> foreignKeys = entityManager
			.createNativeQuery("SELECT order_id FROM order_item WHERE order_id = ?1")
			.setParameter(1, order.getId())
			.getResultList();
		assertThat(foreignKeys).hasSize(2);
	}

	@Test
	void calculates_item_and_total_amount_from_snapshot_price() {
		Order order = new_order();
		OrderItem colombiaItem = order.add_order_item(persist_product("콜롬비아", 15000), 2);
		order.add_order_item(persist_product("브라질", 12000), 3);
		entityManager.persist(order);
		flush_and_clear();

		Order foundOrder = entityManager.find(Order.class, order.getId());
		assertThat(colombiaItem.calculate_amount()).isEqualTo(30000L);
		assertThat(foundOrder.calculate_total_amount()).isEqualTo(15000L * 2 + 12000L * 3);
	}

	@Test
	void calculates_amount_without_int_overflow() {
		Order order = new_order();
		Product expensiveProduct = Product.create("고가 원두", Integer.MAX_VALUE, null, Origin.OTHER, null);
		OrderItem orderItem = order.add_order_item(expensiveProduct, 2);

		assertThat(orderItem.calculate_amount()).isEqualTo(2L * Integer.MAX_VALUE);
		assertThat(order.calculate_total_amount()).isEqualTo(2L * Integer.MAX_VALUE);
	}

	@Test
	void keeps_snapshot_when_product_name_and_price_change() {
		Product product = persist_product("콜롬비아", 15000);
		Order order = new_order();
		order.add_order_item(product, 1);
		entityManager.persist(order);
		flush_and_clear();

		entityManager.find(Product.class, product.getId())
			.update_info("콜롬비아 수프리모", 20000, null, Origin.COLOMBIA, null);
		flush_and_clear();

		OrderItem orderItem = entityManager.find(Order.class, order.getId()).getOrderItems().get(0);
		assertThat(orderItem.getProductName()).isEqualTo("콜롬비아");
		assertThat(orderItem.getOrderPrice()).isEqualTo(15000);
		assertThat(orderItem.getProduct().getProductName()).isEqualTo("콜롬비아 수프리모");
		assertThat(orderItem.getProduct().getPrice()).isEqualTo(20000);
		assertThat(orderItem.calculate_amount()).isEqualTo(15000L);
	}

	@Test
	void keeps_product_reference_after_logical_delete() {
		Product product = persist_product("콜롬비아", 15000);
		Order order = new_order();
		order.add_order_item(product, 1);
		entityManager.persist(order);
		flush_and_clear();

		entityManager.find(Product.class, product.getId()).mark_deleted();
		flush_and_clear();

		OrderItem orderItem = entityManager.find(Order.class, order.getId()).getOrderItems().get(0);
		assertThat(orderItem.getProduct().getId()).isEqualTo(product.getId());
		assertThat(orderItem.getProduct().isDeleted()).isTrue();
	}

	@Test
	void finds_orders_with_jpql_entity_name_order() {
		Order order = new_order();
		order.add_order_item(persist_product("콜롬비아", 15000), 1);
		entityManager.persist(order);
		flush_and_clear();

		List<Order> foundOrders = entityManager
			.createQuery("select distinct o from Order o join fetch o.orderItems where o.email = :email", Order.class)
			.setParameter("email", "customer@example.com")
			.getResultList();
		assertThat(foundOrders).hasSize(1);
		assertThat(foundOrders.get(0).getOrderItems()).hasSize(1);
	}

	@Test
	void order_items_cannot_be_modified_from_outside() {
		Order order = new_order();
		order.add_order_item(Product.create("원두", 1000, null, Origin.OTHER, null), 1);

		assertThatThrownBy(() -> order.getOrderItems().clear())
			.isInstanceOf(UnsupportedOperationException.class);
		assertThat(order.getOrderItems()).hasSize(1);
	}

	@Test
	void rejects_non_positive_quantity_and_missing_required_values() {
		Product product = Product.create("원두", 1000, null, Origin.OTHER, null);
		Order order = new_order();

		assert_invalid_input(() -> order.add_order_item(product, 0));
		assert_invalid_input(() -> order.add_order_item(product, -1));
		assert_invalid_input(() -> order.add_order_item(null, 1));
		assertThat(order.getOrderItems()).isEmpty();

		assert_invalid_input(() -> Order.create(" ", "주소", "06236", ORDERED_AT, DELIVERY_DATE));
		assert_invalid_input(() -> Order.create("a@b.com", "", "06236", ORDERED_AT, DELIVERY_DATE));
		assert_invalid_input(() -> Order.create("a@b.com", "주소", null, ORDERED_AT, DELIVERY_DATE));
		assert_invalid_input(() -> Order.create("a@b.com", "주소", "06236", null, DELIVERY_DATE));
		assert_invalid_input(() -> Order.create("a@b.com", "주소", "06236", ORDERED_AT, null));
	}

	// ===== ERD 물리 FK 삭제 규칙 (테스트 H2 인메모리 DB에서만 실행) =====

	@Test
	void creates_foreign_keys_with_erd_delete_rules() {
		List<?> rules = entityManager.createNativeQuery("""
				SELECT kcu.COLUMN_NAME, rc.DELETE_RULE
				FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
				JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
				  ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
				WHERE kcu.TABLE_NAME = 'ORDER_ITEM'
				ORDER BY kcu.COLUMN_NAME
				""").getResultList();

		assertThat(rules).hasSize(2);
		assertThat(rules).extracting(row -> ((Object[]) row)[0] + "=" + ((Object[]) row)[1])
			.containsExactly("ORDER_ID=CASCADE", "PRODUCT_ID=RESTRICT");
	}

	@Test
	void physical_delete_of_order_removes_its_items_by_db_cascade() {
		Product product = persist_product("콜롬비아", 15000);
		Order order = new_order();
		order.add_order_item(product, 1);
		order.add_order_item(product, 2);
		entityManager.persist(order);
		flush_and_clear();

		entityManager.createNativeQuery("DELETE FROM orders WHERE order_id = ?1")
			.setParameter(1, order.getId())
			.executeUpdate();

		Number remainingItems = (Number) entityManager
			.createNativeQuery("SELECT COUNT(*) FROM order_item WHERE order_id = ?1")
			.setParameter(1, order.getId())
			.getSingleResult();
		assertThat(remainingItems.longValue()).isZero();
		assertThat(entityManager.find(Product.class, product.getId())).isNotNull();
	}

	@Test
	void physical_delete_of_product_used_by_order_item_is_blocked() {
		Product product = persist_product("콜롬비아", 15000);
		Order order = new_order();
		order.add_order_item(product, 1);
		entityManager.persist(order);
		flush_and_clear();

		assertThatThrownBy(() -> entityManager.createNativeQuery("DELETE FROM product WHERE product_id = ?1")
				.setParameter(1, product.getId())
				.executeUpdate())
			.isInstanceOf(PersistenceException.class);
	}

	private Order new_order() {
		return Order.create("customer@example.com", "서울시 강남구 테헤란로 1", "06236", ORDERED_AT, DELIVERY_DATE);
	}

	private Product persist_product(String productName, int price) {
		Product product = Product.create(productName, price, null, Origin.COLOMBIA, null);
		entityManager.persist(product);
		return product;
	}

	private void assert_invalid_input(Runnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOf(BusinessException.class)
			.extracting(exception -> ((BusinessException) exception).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_INPUT);
	}

	private void flush_and_clear() {
		entityManager.flush();
		entityManager.clear();
	}

}

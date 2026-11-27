package com.cafe.beanorder.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            select distinct o from Order o
            left join fetch o.orderItems
            where (:status is null or o.status = :status)
              and (:deliveryDate is null or o.deliveryDate = :deliveryDate)
              and (:email is null or o.email = :email)
            order by o.orderedAt desc
            """)
    List<Order> search_orders(@Param("status") OrderStatus status,
                              @Param("deliveryDate") LocalDate deliveryDate,
                              @Param("email") String email);

    	/** 주문 병합 대상: 자기 자신을 뺀, 같은 이메일·배송일·주소의 다른 주문 (오래된 순) */
	@Query("""
		select o from Order o
		where o.id <> :orderId
		  and o.email = :email
		  and o.deliveryDate = :deliveryDate
		  and trim(o.address) = :address
		  and o.status = :status
		order by o.orderedAt asc
		""")
	List<Order> find_merge_targets(@Param("orderId") Long orderId,
			@Param("email") String email,
			@Param("deliveryDate") LocalDate deliveryDate,
			@Param("address") String address,
			@Param("status") OrderStatus status);

    	/**
	 * 스케줄러용 일괄 상태 변경: from 상태이면서 배송 예정일이 lastDeliveryDate 이하인 주문을 to 상태로 바꾼다.
	 * "이하"로 잡아서 서버 중단으로 놓친 날의 주문도 다음 실행 때 함께 처리한다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
		update Order o
		set o.status = :to
		where o.status = :from
		  and o.deliveryDate <= :lastDeliveryDate
		""")
	int update_status_until(@Param("from") OrderStatus from,
			@Param("to") OrderStatus to,
			@Param("lastDeliveryDate") LocalDate lastDeliveryDate);
}

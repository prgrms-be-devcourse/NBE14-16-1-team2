package com.cafe.beanorder.order;

import org.springframework.data.jpa.repository.JpaRepository;
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

}

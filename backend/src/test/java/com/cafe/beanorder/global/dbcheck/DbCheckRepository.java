package com.cafe.beanorder.global.dbcheck;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DB 연결 검증 전용 Repository (테스트 소스에만 존재).
 */
public interface DbCheckRepository extends JpaRepository<DbCheckEntity, Long> {

	List<DbCheckEntity> findByLabel(String label);

}

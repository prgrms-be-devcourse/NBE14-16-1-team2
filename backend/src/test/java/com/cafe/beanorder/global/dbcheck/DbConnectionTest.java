package com.cafe.beanorder.global.dbcheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DbConnectionTest {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private DbCheckRepository dbCheckRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void uses_h2_in_memory_db_for_test_profile() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
			assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		}
	}

	@Test
	void saves_and_finds_entity_with_jpa() {
		LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0);
		DbCheckEntity savedEntity = dbCheckRepository.save(new DbCheckEntity("connection-check", createdAt));

		// 영속성 컨텍스트를 비워 실제 DB에서 다시 조회되도록 한다.
		entityManager.flush();
		entityManager.clear();

		DbCheckEntity foundEntity = dbCheckRepository.findById(savedEntity.getId()).orElseThrow();
		assertThat(foundEntity.getId()).isNotNull();
		assertThat(foundEntity.getLabel()).isEqualTo("connection-check");
		assertThat(foundEntity.getCreatedAt()).isEqualTo(createdAt);
		assertThat(dbCheckRepository.findByLabel("connection-check")).hasSize(1);
	}

	@Test
	void uses_asia_seoul_time_zone() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
	}

}

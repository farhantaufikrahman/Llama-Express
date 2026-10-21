package com.llamaexpressbackend;

import com.llamaexpressbackend.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class BackendApplicationTests extends AbstractIntegrationTest {

	@Autowired
	private Flyway flyway;

	@Test
	void contextLoads_andAllMigrationsApplied() {
		assertThat(flyway.info().current()).as("versi schema saat ini").isNotNull();
		assertThat(flyway.info().pending()).as("migrasi yang belum diterapkan").isEmpty();
	}
}
package org.zalando.catwatch.backend.repo;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.catwatch.backend.CatWatchBackendApplication;

import static org.junit.Assume.assumeTrue;
import static org.zalando.catwatch.backend.repo.util.DatabasePing.isDatabaseAvailable;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CatWatchBackendApplication.class)
public abstract class AbstractRepositoryIT {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Before
	public void skipIfDatabaseNotAvailable() {
		assumeTrue(isDatabaseAvailable(jdbcTemplate));
	}

}

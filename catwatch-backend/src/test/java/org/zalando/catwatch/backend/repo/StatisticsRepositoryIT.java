package org.zalando.catwatch.backend.repo;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.zalando.catwatch.backend.model.Statistics;
import org.zalando.catwatch.backend.util.TestUtils;

public class StatisticsRepositoryIT extends AbstractRepositoryIT {

	@Autowired
	private StatisticsRepository repository;

	private Statistics s1, s2, s3, s4, s5, s6;

	private final String ORGANIZATION1 = "organization1", ORGANIZATION2 = "organization2";

	/**
	 * Tests if the statistics of a specified organization are retrieved
	 * correctly. Tests the {@link StatisticsRepository.findByOrganizationName}
	 * method
	 */
	@Test
	public void findByOrganizationNameTest() {

		Assert.assertThat(this.repository, Matchers.notNullValue());

		this.repository.deleteAll();

		Assert.assertThat(this.repository.findAll(), Matchers.iterableWithSize(0));

		createStatistics();
		Assert.assertThat(this.s1, Matchers.notNullValue());
		Assert.assertThat(this.s2, Matchers.notNullValue());
		Assert.assertThat(this.s3, Matchers.notNullValue());

		List<Statistics> statistics;

		s1.setOrganizationName(ORGANIZATION1);

		// empty result
		statistics = this.repository.findByOrganizationName(ORGANIZATION1);

		Assert.assertThat(statistics, Matchers.empty());

		this.repository.save(s1);

		statistics = this.repository.findByOrganizationName(ORGANIZATION1);

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to one", statistics.size(),
				Matchers.equalTo(1));

		TestUtils.checkEquals(s1, statistics.get(0));

		// store a second statistic for the same organizaiton
		this.s2.setOrganizationName(ORGANIZATION1);
		this.repository.save(s2);

		statistics = this.repository.findByOrganizationName(ORGANIZATION1);

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to two", statistics.size(),
				Matchers.equalTo(2));

		// add a statistic for another organization
		this.s3.setOrganizationName(ORGANIZATION2);
		this.repository.save(s3);

		statistics = this.repository.findByOrganizationName(ORGANIZATION1);

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to two", statistics.size(),
				Matchers.equalTo(2));

		statistics = this.repository.findByOrganizationName(ORGANIZATION2);

		Assert.assertThat("Number of statistics for " + ORGANIZATION2 + " should be equal to one", statistics.size(),
				Matchers.equalTo(1));

		// store the same statistic again
		this.repository.save(s3);

		statistics = this.repository.findByOrganizationName(s3.getOrganizationName());

		Assert.assertThat(
				"Number of statistics for " + ORGANIZATION2
						+ " should be equal to one, since the added statistic should ovveride the existing one",
				statistics.size(), Matchers.equalTo(1));

		TestUtils.checkEquals(statistics.get(0), s3);

	}

	@Test
	public void findByOrganizationNameOrderBySnapshotDateAscTest() {

		Assert.assertThat(this.repository, Matchers.notNullValue());

		this.repository.deleteAll();

		Assert.assertThat(this.repository.findAll(), Matchers.iterableWithSize(0));

		createStatistics();
		Assert.assertThat(this.s1, Matchers.notNullValue());
		Assert.assertThat(this.s2, Matchers.notNullValue());

		List<Statistics> statistics;

		statistics = findLatestStatistics(ORGANIZATION1);

		Assert.assertThat(statistics, Matchers.empty());

		this.s1.setOrganizationName(ORGANIZATION1);
		this.s1.setSnapshotDate(new Date(System.currentTimeMillis() - 60000));

		this.repository.save(s1);

		statistics = findLatestStatistics(s1.getOrganizationName());

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to one", statistics.size(),
				Matchers.equalTo(1));

		TestUtils.checkEquals(s1, statistics.get(0));

		// add a newer statistics entry
		this.s2.setOrganizationName(ORGANIZATION1);
		this.s2.setSnapshotDate(new Date(System.currentTimeMillis()));

		this.repository.save(s2);

		statistics = findLatestStatistics(s1.getOrganizationName());

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to one", statistics.size(),
				Matchers.equalTo(1));

		TestUtils.checkEquals(s2, statistics.get(0));

		// add an older statistics record
		this.s3.setOrganizationName(ORGANIZATION1);
		this.s3.setSnapshotDate(new Date(System.currentTimeMillis() - 30000));

		this.repository.save(s3);

		statistics = findLatestStatistics(s1.getOrganizationName());

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to one", statistics.size(),
				Matchers.equalTo(1));

		TestUtils.checkEquals(s2, statistics.get(0));

		// add statistics of another organization, but with the newest time
		// stamp
		this.s4.setOrganizationName(ORGANIZATION2);
		this.s4.setSnapshotDate(new Date(System.currentTimeMillis() + 30000));

		this.repository.save(s4);

		statistics = findLatestStatistics(s2.getOrganizationName());

		Assert.assertThat("Number of statistics for " + ORGANIZATION1 + " should be equal to one", statistics.size(),
				Matchers.equalTo(1));

		TestUtils.checkEquals(s2, statistics.get(0));
	}

	@Test
	public void findByOrganizationNameAndSnapshotDateAfterAndSnapshotDateBeforeOrderBySnapshotDateDescTest()
			throws Exception {

		// given
		Date now = Date.from(now());
		Date oneDayAgo = Date.from(now().minus(1, DAYS));
		Date twoDaysAgo = Date.from(now().minus(2, DAYS));
		Date threeDaysAgo = Date.from(now().minus(3, DAYS));
		Date foursDaysAgo = Date.from(now().minus(4, DAYS));

		Date startDate = threeDaysAgo;
		Date endDate = twoDaysAgo;

		repository.deleteAll();

		// // snapshot date is before requested period of time
		s1 = createAndSaveStatistics(ORGANIZATION1, foursDaysAgo);
		// snapshot date is at the exact start of the requested period of time
		s2 = createAndSaveStatistics(ORGANIZATION1, threeDaysAgo);
		// snapshot date is within requested period of time
		s3 = createAndSaveStatistics(ORGANIZATION1, twoDaysAgo);
		// snapshot date is within requested period of time
		s4 = createAndSaveStatistics(ORGANIZATION2, twoDaysAgo);
		// snapshot date is after requested period of time
		s5 = createAndSaveStatistics(ORGANIZATION1, oneDayAgo);

		assertThat(repository.findAll(), iterableWithSize(5));

		// when
		List<Statistics> stats = findInPeriod(startDate, endDate);

		// then
		assertThat(stats, hasSize(2));
		assertThat(stats.get(0).getSnapshotDate().getTime(), equalTo(s3.getSnapshotDate().getTime()));
		assertThat(stats.get(1).getSnapshotDate().getTime(), equalTo(s2.getSnapshotDate().getTime()));

		// when
		// stats = findInPeriod(null, endDate);
		//
		// // then
		// assertThat(stats, hasSize(4));
		// assertThat(stats.get(0).getSnapshotDate(),
		// equalTo(s4.getSnapshotDate()));
		// assertThat(stats.get(1).getSnapshotDate(),
		// equalTo(s3.getSnapshotDate()));
		// assertThat(stats.get(2).getSnapshotDate(),
		// equalTo(s2.getSnapshotDate()));
		// assertThat(stats.get(3).getSnapshotDate(),
		// equalTo(s1.getSnapshotDate()));

		// when
		// stats = findInPeriod(startDate, null);
		//
		// // then
		// assertThat(stats, hasSize(3));
		// assertThat(stats.get(0).getSnapshotDate(),
		// equalTo(s6.getSnapshotDate()));
		// assertThat(stats.get(1).getSnapshotDate(),
		// equalTo(s4.getSnapshotDate()));
		// assertThat(stats.get(2).getSnapshotDate(),
		// equalTo(s3.getSnapshotDate()));
	}

	private List<Statistics> findLatestStatistics(String organization) {
		return this.repository.findByOrganizationNameOrderByKeySnapshotDateDesc(organization, new PageRequest(0, 1));
	}

	private List<Statistics> findInPeriod(Date startDate, Date endDate) {
		// return
		// repository.findByOrganizationNameAndSnapshotDateAfterAndSnapshotDateBeforeOrderBySnapshotDateDesc(
		// ORGANIZATION1, startDate, endDate);
		return repository.findStatisticsByOrganizationAndDate(ORGANIZATION1, startDate, endDate);
	}

	private Statistics createAndSaveStatistics(String organizationName, Date snapshotDate) {
		Statistics s = new Statistics(new Double(Math.random() * 1000).intValue(), snapshotDate);
		s.setOrganizationName(organizationName);
		s.setSnapshotDate(snapshotDate);
		return repository.save(s);
	}

	private void createStatistics() {
		s1 = new Statistics(new Double(Math.random() * 1000).intValue(), new Date(System.currentTimeMillis() - 100));
		s1.setAllContributorsCount(10);
		s1.setAllForksCount(12);
		s1.setAllSizeCount(100);
		s1.setAllStarsCount(23);
		s1.setMembersCount(7);
		s1.setPrivateProjectCount(11);
		s1.setProgramLanguagesCount(3);
		s1.setPublicProjectCount(2);
		s1.setSnapshotDate(new Date(System.currentTimeMillis() - 60000));
		s1.setTagsCount(6);
		s1.setTeamsCount(0);

		s2 = new Statistics(new Double(Math.random() * 1000).intValue(), new Date(System.currentTimeMillis() - 200));
		s3 = new Statistics(new Double(Math.random() * 1000).intValue(), new Date(System.currentTimeMillis() - 300));
		s4 = new Statistics(new Double(Math.random() * 1000).intValue(), new Date(System.currentTimeMillis() - 400));
		s5 = new Statistics(new Double(Math.random() * 1000).intValue(), new Date(System.currentTimeMillis() - 500));
		s6 = new Statistics(new Double(Math.random() * 1000).intValue(), new Date(System.currentTimeMillis() - 600));
	}

	// private void checkEquals(Statistics expected, Statistics actual) {
	// Assert.assertEquals("Number of contributors is different",
	// expected.getAllContributorsCount(),
	// actual.getAllContributorsCount());
	//
	// Assert.assertEquals("Snapshot date is different",
	// expected.getSnapshotDate(), actual.getSnapshotDate());
	//
	// Assert.assertEquals("ID is different", expected.getId(), actual.getId());
	//
	// Assert.assertEquals("Number of contributors is different",
	// expected.getAllForksCount(),
	// actual.getAllForksCount());
	//
	// Assert.assertEquals("Number of size is different",
	// expected.getAllSizeCount(), actual.getAllSizeCount());
	//
	// Assert.assertEquals("Number of stars is different",
	// expected.getAllStarsCount(), actual.getAllStarsCount());
	//
	// Assert.assertEquals("Number of members is different",
	// expected.getMembersCount(), actual.getMembersCount());
	//
	// Assert.assertEquals("Oranization names are different",
	// expected.getOrganizationName(),
	// actual.getOrganizationName());
	//
	// Assert.assertEquals("Number of projects is different",
	// expected.getPrivateProjectCount(),
	// actual.getPrivateProjectCount());
	//
	// Assert.assertEquals("Number of programming languages is different",
	// expected.getProgramLanguagesCount(),
	// actual.getProgramLanguagesCount());
	//
	// Assert.assertEquals("Number of public projects is different",
	// expected.getPublicProjectCount(),
	// actual.getPublicProjectCount());
	//
	// Assert.assertEquals("Number of tags is different",
	// expected.getTagsCount(), actual.getTagsCount());
	//
	// Assert.assertEquals("Number of teams is different",
	// expected.getTeamsCount(), actual.getTeamsCount());
	//
	// }
}

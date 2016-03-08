package com.gmail.lionelg3.nxtraces.tests;


import com.gmail.lionelg3.nxtraces.db.LogMessage;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.jboss.logging.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class SearchDatabaseTest {

    private static final Logger LOGGER = Logger.getLogger(SearchDatabaseTest.class);

    EmbeddedCacheManager cacheManager;

	@BeforeClass
	public void init() throws IOException {
		cacheManager = new DefaultCacheManager("test-database.xml");
	}

	@AfterClass
	public void stop() {
		cacheManager.stop();
	}

	@Test
	public void search() {
		Cache<Object, Object> db = cacheManager.getCache();
		for (int i = 1; i <= 3; i++) {
			LogMessage message = new LogMessage("src/test/resources/mail" + i + ".txt");
			db.put(message.getMessageID(), message);
		}
		System.out.println("maildb.size() = " + db.size());

		SearchManager searchManager = Search.getSearchManager(db);
		searchManager.getMassIndexer().start();
		QueryBuilder queryBuilder = searchManager.buildQueryBuilderForClass(LogMessage.class).get();

		BooleanJunction<BooleanJunction> search = queryBuilder.bool();

		search.must(
				queryBuilder
						.keyword()
						.onField("subject")
						.matching("os")
						.createQuery()
		);

		search.must(
				queryBuilder
						.keyword()
						.onField("subject")
						.matching("unix")
						.createQuery()
		);


        LOGGER.info("trace de test");

        CacheQuery res = searchManager.getQuery(search.createQuery(), LogMessage.class);

		Assert.assertEquals(1, res.list().size());

		res.list().forEach(System.out::println);
	}
}

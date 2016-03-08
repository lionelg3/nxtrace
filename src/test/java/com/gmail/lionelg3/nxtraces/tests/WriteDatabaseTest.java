package com.gmail.lionelg3.nxtraces.tests;

import com.gmail.lionelg3.nxtraces.db.LogMessage;
import com.gmail.lionelg3.nxtraces.engine.NXTraces;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 22/12/15, Time: 11:26
 */
public class WriteDatabaseTest {

	private static final String MAIL_FILE = "src/test/resources/mail2.txt";
	EmbeddedCacheManager cacheManager;

	@BeforeClass
	public void init() throws IOException {
		cacheManager = new DefaultCacheManager("test-database.xml");
	}

	@AfterClass
	public void stop() {
		cacheManager.stop();
	}

	@Test()
	public void write() {
		LogMessage message = new LogMessage(MAIL_FILE);
		Cache<Object, Object> db = cacheManager.getCache();
		for (int i = 0; i < 100; i++) {
			db.put("id" + i, message);
		}
		Assert.assertTrue(db.size() >= 100);
	}

}

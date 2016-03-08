package com.gmail.lionelg3.nxtraces.tests;

import com.gmail.lionelg3.nxtraces.db.LogMessage;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 22/12/15, Time: 13:33
 */
public class DatabaseInfoTest {

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
    public void size() {
        String MAIL_FILE = "src/test/resources/mail1.txt";
        LogMessage message = new LogMessage(MAIL_FILE);
        Assert.assertEquals(message.getSubject(), "Unix OS", "Compare sujet");
    }

    @Test
    public void cacheList() {
        Set<String> cacheNames = cacheManager.getCacheNames();
        Assert.assertTrue(cacheNames.contains("test"), "cacheManager contient un cache 'test'");
        System.out.println("cacheNames messages size = " + cacheManager.getCache().size());
        Assert.assertEquals(cacheManager.getCache().size(), 103, "Nb messages");

    }
}

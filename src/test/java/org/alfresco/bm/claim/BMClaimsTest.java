/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.bm.claim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.alfresco.bm.api.v1.ResultsRestAPI;
import org.alfresco.bm.api.v1.TestRestAPI;
import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventRecord;
import org.alfresco.bm.event.EventService;
import org.alfresco.bm.event.ResultService;
import org.alfresco.bm.log.LogService;
import org.alfresco.bm.log.LogService.LogLevel;
import org.alfresco.bm.session.SessionService;
import org.alfresco.bm.test.TestConstants;
import org.alfresco.bm.test.TestRunServicesCache;
import org.alfresco.bm.test.TestService;
import org.alfresco.bm.test.mongo.MongoTestDAO;
import org.alfresco.bm.tools.BMTestRunner;
import org.alfresco.bm.tools.BMTestRunnerListener;
import org.alfresco.bm.tools.BMTestRunnerListenerAdaptor;
import org.alfresco.bm.user.UserData;
import org.alfresco.bm.user.UserDataServiceImpl;
import org.alfresco.mongo.MongoDBFactory;
import org.alfresco.mongo.MongoDBForTestsFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.ApplicationContext;

import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * This class provides the primary means of testing the "Claims" load test for a developer.
 * <p>
 * MongoDB is started for each test using utilities available from the server framework.
 * Some patterns will need to be copied from existing tests; the orignal sample shows how
 * test runs can be executed.  Where a target server must exist in order for the test run
 * to succeed, you might need to just test the failure conditions.
 * <p>
 * This version of the code will start with just some basic test context checks.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
@RunWith(JUnit4.class)
public class BMClaimsTest extends BMTestRunnerListenerAdaptor implements TestConstants
{
    public static final String MONGO_TEST_DATABASE = "bm21-data";
    
    private static Log logger = LogFactory.getLog(BMClaimsTest.class);
    
    private MongoDBForTestsFactory dbFactory;
    private DB testDB;
    private String testDBHost;
    
    /**
     * Create entries for users and claims
     */
    @Before
    public void createPrerequisiteData() throws Exception
    {
        /*
         * Note that the test utilities create MongoDB instances by default.
         * However, in order to have access to the DB before the tests kick off,
         * we have to create that infrastructure here.
         */
        dbFactory = new MongoDBForTestsFactory();
        String uriWithoutDB = dbFactory.getMongoURIWithoutDB();
        testDBHost = new MongoClientURI(uriWithoutDB).getHosts().get(0);
        testDB = new MongoDBFactory(new MongoClient(testDBHost), MONGO_TEST_DATABASE).getObject();
        
        // The test needs a user
        // The test defaults point to 'cmis.alfresco.com', which we will assume is going 
        UserDataServiceImpl userDataService = new UserDataServiceImpl(testDB, "mirrors.cmis.alfresco.com.users");
        userDataService.afterPropertiesSet();
        
        UserData user = new UserData();
        user.setUsername("admin");
        user.setPassword("admin");
        user.setCreationState(DataCreationState.Created);
        // The rest is not useful for this specific test
        {
            user.setEmail("bmarley@reggae.com");
            user.setDomain("reggae");
            user.setFirstName("Bob");
            user.setLastName("Marley");
        }
        userDataService.createNewUser(user);
        
        // The test needs at least one claim
        ClaimDataDAO claimDataDAO = new ClaimDataDAO(testDB, "mirrors.cmis.alfresco.com.claims");
        String claimId = UUID.randomUUID().toString();
        claimDataDAO.createClaim(claimId);
        claimDataDAO.updateClaimState(claimId, DataCreationState.Created);
    }
    
    @After
    public void tearDown() throws Exception
    {
        if (dbFactory != null)
        {
            dbFactory.destroy();
        }
    }
    
    /**
     * Perform a basic sanity check of the test context i.e. are the beans all in order?
     * <p>
     * The test duration is set to be very short, which should cause the event processing
     * to stop as soon as it has started.
     */
    @Test
    public void runQuick() throws Exception
    {
        Properties props = new Properties();
        props.put("load.sessionDelay", "500");                  // Need enough delay for the time check to kick in
        props.put("test.duration", "1");                        // By the first check, it should say it's overdue a stop
        BMTestRunner runner = new BMTestRunner(20000L);
        runner.addListener(new BMTestRunnerListenerAdaptor()
        {
            @Override
            public void testRunFinished(ApplicationContext testCtx, String test, String run)
            {
                TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
                EventService eventService = services.getEventService(test, run);
                // Check that there are still events in the event queue, which will show that we terminated by time
                assertNotEquals("There should be events in the event queue. ", 0L, eventService.count());
            }
        });
        runner.run(testDBHost, testDBHost, props);
    }
    
    /**
     * Now we let the test run through.<br>
     * A listener is attached to the runner utility so that the results of the test can be checked.
     * Even if the test cannot do the full raft of work (if other test infrastructure is missing)
     * the failure results can still be checked.
     */
    @Test
    public void runComplete() throws Exception
    {
        BMTestRunner runner = new BMTestRunner(300000L);         // Should be done in 60s
        runner.addListener(this);
        runner.run(testDBHost, testDBHost, null);
    }

    /**
     * A listener method that allows the test to check results <b>before</b> the in-memory MongoDB instance
     * is discarded.
     * <p/>
     * Check that the exact number of results are available, as expected
     * 
     * @see BMTestRunnerListener
     */
    @Override
    public void testRunFinished(ApplicationContext testCtx, String test, String run)
    {
        TestRunServicesCache services = testCtx.getBean(TestRunServicesCache.class);
        LogService logService = testCtx.getBean(LogService.class);
        MongoTestDAO testDAO = services.getTestDAO();
        TestService testService = services.getTestService();
        ResultService resultService = services.getResultService(test, run);
        SessionService sessionService = services.getSessionService(test, run);
        assertNotNull(resultService);
        TestRestAPI testAPI = new TestRestAPI(testDAO, testService, logService, services);
        ResultsRestAPI resultsAPI = testAPI.getTestRunResultsAPI(test, run);
        // Let's check the results before the DB gets thrown away (we didn't make it ourselves)

        // Dump one of each type of event for information
        Set<String> eventNames = new TreeSet<String>(resultService.getEventNames());
        logger.info("Showing 1 of each type of event:");
        for (String eventName : eventNames)
        {
            List<EventRecord> eventRecord = resultService.getResults(eventName, 0, 1);
            logger.info("   " + eventRecord);
            assertFalse(
                    "An event was created that has no available processor or producer: " + eventRecord + ".  Use the TerminateEventProducer to absorb events.",
                    eventRecord.toString().contains("processedBy=unknown"));
        }

        // One successful START event
        assertEquals("Incorrect number of start events.", 1, resultService.countResultsByEventName(Event.EVENT_NAME_START));
        List<EventRecord> results = resultService.getResults(0L, Long.MAX_VALUE, false, 0, 1);
        if (results.size() != 1 || !results.get(0).getEvent().getName().equals(Event.EVENT_NAME_START))
        {
            Assert.fail(Event.EVENT_NAME_START + " failed: \n" + results.toString());
        }
        
        // As we are in a phase of moving from the sample 'processes' app
        // to the 'claims' app, these checks will be updated to keep up with the test as it develops.
        
        /*
         * 'start' = 1 result
         * 'claims.checkClaims' = 1 result
         * 'claims.createSessions' = 1 result
         * 'claims.startSession' = 20
         * 'claims.claimChosen' = 20
         */

        assertEquals("Incorrect number of event names: " + eventNames, 5, eventNames.size());
        assertEquals(
                "Incorrect number of events: " + "claims.checkClaims",
                1, resultService.countResultsByEventName("claims.checkClaims"));
        assertEquals(
                "Incorrect number of events: " + "claims.createSessions",
                1, resultService.countResultsByEventName("claims.createSessions"));
        assertEquals(
                "Incorrect number of events: " + "claims.startSession",
                20, resultService.countResultsByEventName("claims.startSession"));
        assertEquals(
                "Incorrect number of events: " + "claims.sessionStarted",
                20, resultService.countResultsByEventName("claims.sessionStarted"));
        // N events in total
        assertEquals("Incorrect number of results.", 43, resultService.countResults());
        
        // Get the summary CSV results for the time period and check some of the values
        String summary = BMTestRunner.getResultsCSV(resultsAPI);
        logger.info(summary);
        assertTrue(summary.contains(",,claims.startSession,    20,"));
        
        // Get the chart results and check
        String chartData = resultsAPI.getTimeSeriesResults(0L, "seconds", 1, 10, true);
        if (logger.isDebugEnabled())
        {
            logger.debug("BMClaims chart data: \n" + chartData);
        }
        
        // Make sure that events received a traceable session ID
        assertEquals("Incorrect number of sessions: ", 20, sessionService.getAllSessionsCount());
        
        // Check the log messages
        DBCursor logs = logService.getLogs(null, test, run, LogLevel.INFO, null, null, 0, 500);
        try
        {
            assertEquals("Incorrect number of log messages for this test run. ", 6, logs.size());
            logger.debug("Log messages for " + test + "." + "." + run);
            while (logs.hasNext())
            {
                DBObject log = logs.next();
                Date time = (Date) log.get("time");
                String msg = (String) log.get("msg");
                logger.debug("   " + " >>> " + time + " >>> " + msg);
            }
        }
        finally
        {
            logs.close();
        }
    }
}

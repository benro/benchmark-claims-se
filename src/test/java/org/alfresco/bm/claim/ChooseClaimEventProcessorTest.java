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
import static org.junit.Assert.assertTrue;

import org.alfresco.bm.cmis.CMISEventData;
import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.session.SessionService;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @see ChooseClaimEventProcessor
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ChooseClaimEventProcessorTest
{
    private StopWatch stopWatch;
    private ClaimDataDAO claimDataDAO;
    private SessionService sessionService;
    private ChooseClaimEventProcessor eventProcessor;
    
    @Before
    public void initDAO()
    {
        stopWatch = new StopWatch();
        claimDataDAO = Mockito.mock(ClaimDataDAO.class);
        sessionService = Mockito.mock(SessionService.class);
        eventProcessor = new ChooseClaimEventProcessor(claimDataDAO, sessionService);
    }
    
    @Test
    public void testBasicProcessing() throws Exception
    {
        String sessionId = "S1";
        CMISEventData eventData = new CMISEventData(Mockito.mock(Session.class));
        Event event = new Event("anything", eventData);
        event.setSessionId(sessionId);
        
        DBObject sessionObj = new BasicDBObject();
        sessionObj.put(ClaimSessionConstants.FIELD_USER, "U1");
        
        ClaimData claimData = new ClaimData();
        claimData.setCreationState(DataCreationState.Created);
        claimData.setClaimId("C1");
        
        Mockito.when(sessionService.getSessionData(sessionId)).thenReturn(sessionObj);
        Mockito.when(claimDataDAO.getRandomClaim(DataCreationState.Created)).thenReturn(claimData);
        
        EventResult result = eventProcessor.processEvent(event, stopWatch);
        
        // Check that the claim ID was attached to the session
        Mockito.verify(sessionService, Mockito.times(1)).setSessionData(sessionId, sessionObj);
        assertEquals("Claim not attached to session", "C1", (String) sessionObj.get(ClaimSessionConstants.FIELD_CLAIM_ID));
        
        // The output object remains unchanged
        assertTrue(result.getNextEvents().get(0).getData() == eventData);
    }
}

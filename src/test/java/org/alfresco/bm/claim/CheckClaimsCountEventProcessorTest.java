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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @see CheckClaimsCountEventProcessor
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class CheckClaimsCountEventProcessorTest
{
    private ClaimDataDAO claimDataDAO;
    private CheckClaimsCountEventProcessor eventProcessor;
    private StopWatch stopWatch;
    
    @Before
    public void initDAO()
    {
        claimDataDAO = Mockito.mock(ClaimDataDAO.class);
        eventProcessor = new CheckClaimsCountEventProcessor(claimDataDAO, 1);
        stopWatch = new StopWatch();
    }
    
    @Test
    public void testSuccess() throws Exception
    {
        Mockito.when(claimDataDAO.countClaims(DataCreationState.Created)).thenReturn(1L);
        
        Event event = new Event("not important", null);
        EventResult result = eventProcessor.processEvent(event, stopWatch);
        assertTrue(result.isSuccess());
    }
    
    @Test
    public void testFailure() throws Exception
    {
        Mockito.when(claimDataDAO.countClaims(DataCreationState.Created)).thenReturn(0L);
        
        Event event = new Event("not important", null);
        EventResult result = eventProcessor.processEvent(event, stopWatch);
        assertFalse(result.isSuccess());
    }
}

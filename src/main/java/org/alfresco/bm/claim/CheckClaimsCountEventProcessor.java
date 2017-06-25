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

import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;

/**
 * Check that enough claims exist for the test to continue
 * 
 * <h1>Input</h1>
 * 
 * None
 * 
 * <h1>Data</h1>
 * 
 * None
 * 
 * <h1>Actions</h1>
 * 
 * Checks that there are enough claims to continue
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_CLAIMS_READY}: There are claims to work with<br/>
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class CheckClaimsCountEventProcessor extends AbstractEventProcessor
{
    public static final String EVENT_NAME_CLAIMS_READY = "claims.ready";
    public static final String ERR_NOT_ENOUGH_CLAIMS = "Needed %1d created claims but only found %1d.";
    public static final String MSG_FOUND_CLAIMS = "Found %1d created claims.  Minimum was %1d.";
    
    private final ClaimDataDAO claimDataDAO;
    private final int minClaimCount;
    private String eventNameClaimsReady;

    /**
     * @param claimDataDAO              DAO for accessing stored claim data
     * @param minClaimCount             the minimum number of claims
     */
    public CheckClaimsCountEventProcessor(ClaimDataDAO claimDataDAO, int minClaimCount)
    {
        super();
        this.claimDataDAO = claimDataDAO;
        this.minClaimCount = minClaimCount;
        this.eventNameClaimsReady = EVENT_NAME_CLAIMS_READY;
    }

    /**
     * Override the {@link #EVENT_NAME_CLAIMS_READY default} event name for 'claims ready'.
     */
    public void setEventNameClaimsReady(String eventNameClaimsReady)
    {
        this.eventNameClaimsReady = eventNameClaimsReady;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        // Check the number of claims
        long createdClaimsCount = claimDataDAO.countClaims(DataCreationState.Created);
        
        if (createdClaimsCount < minClaimCount)
        {
            // report error
            String msg = String.format(ERR_NOT_ENOUGH_CLAIMS, minClaimCount, createdClaimsCount);
            return new EventResult(msg, false);
        }
        else
        {
            // We can raise an event to continue
            Event nextEvent = new Event(eventNameClaimsReady, null);
            
            String msg = String.format(MSG_FOUND_CLAIMS, minClaimCount, createdClaimsCount);
            return new EventResult(msg, nextEvent);
        }
    }
}

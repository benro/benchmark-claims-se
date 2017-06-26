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

import org.alfresco.bm.cmis.CMISEventData;
import org.alfresco.bm.data.DataCreationState;
import org.alfresco.bm.event.AbstractEventProcessor;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.session.SessionService;

import com.mongodb.DBObject;

/**
 * Select a random claim ID and attach it to the session
 * 
 * <h1>Input</h1>
 * 
 * {@link CMISEventData} and an active session.
 * 
 * <h1>Actions</h1>
 * 
 * Selects a random claim and attaches it to the current session.
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_CLAIM_CHOSEN}: A valid claim has been attached to the session.<br/>
 * The {@link CMISEventData} is passed on - without modification - as output
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ChooseClaimEventProcessor extends AbstractEventProcessor
{
    public static final String EVENT_NAME_CLAIM_CHOSEN = "claim.chosen";
    public static final String ERR_NO_SESSION = "Expected an active session in the inbound event";
    public static final String ERR_NO_USER_IN_SESSION = "There is no user in the current session";
    public static final String ERR_INCORRECT_INBOUND_TYPE = "Expected CMISEventData as input";
    public static final String ERR_NO_CLAIM = "There do not appear to be any claims available.";
    public static final String MSG_CLAIM_CHOSEN = "User %s will use claim %s (see session %s)";
    
    private final ClaimDataDAO claimDataDAO;
    private final SessionService sessionService;
    private String eventNameClaimChosen;

    /**
     * @param claimDataDAO              DAO for accessing stored claim data
     * @param sessionService            access to the persistent session for each scenario
     */
    public ChooseClaimEventProcessor(ClaimDataDAO claimDataDAO, SessionService sessionService)
    {
        super();
        this.claimDataDAO = claimDataDAO;
        this.sessionService = sessionService;
        this.eventNameClaimChosen = EVENT_NAME_CLAIM_CHOSEN;
    }

    /**
     * Override the {@link #EVENT_NAME_CLAIM_CHOSEN default} event name for 'claim chosen'.
     */
    public void setEventNameClaimChosen(String eventNameClaimChosen)
    {
        this.eventNameClaimChosen = eventNameClaimChosen;
    }

    @Override
    public EventResult processEvent(Event event) throws Exception
    {
        String sessionId = event.getSessionId();
        if (sessionId == null)
        {
            EventResult result = new EventResult(ERR_NO_SESSION, false);
            return result;
        }
        
        // Double check the input
        CMISEventData cmisEventData = null;
        try
        {
            cmisEventData = (CMISEventData) event.getData();
        }
        catch (ClassCastException e)
        {
            EventResult result = new EventResult(ERR_INCORRECT_INBOUND_TYPE, false);
            return result;
        }
        
        // Select a random claim
        ClaimData claimData = claimDataDAO.getRandomClaim(DataCreationState.Created);
        if (claimData == null)
        {
            EventResult result = new EventResult(ERR_NO_CLAIM, false);
            return result;
        }
        String claimId = claimData.getClaimId();
        // Get the current session data
        DBObject sessionObj = sessionService.getSessionData(sessionId);
        sessionObj.put(ClaimSessionConstants.FIELD_CLAIM_ID, claimId);
        // put it back
        sessionService.setSessionData(sessionId, sessionObj);
        
        // What is the user in action?
        String user = (String) sessionObj.get(ClaimSessionConstants.FIELD_USER);
        if (user == null)
        {
            EventResult result = new EventResult(ERR_NO_USER_IN_SESSION, false);
            return result;
        }
        
        // We can raise an event to continue
        Event nextEvent = new Event(eventNameClaimChosen, cmisEventData);
        
        String msg = String.format(MSG_CLAIM_CHOSEN, user, claimId, sessionId);
        return new EventResult(msg, nextEvent);
    }
}

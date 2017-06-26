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

import org.alfresco.bm.cmis.StartCMISSession;


/**
 * Constants used as keys for data passed from event to event during a claims processing session.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public interface ClaimSessionConstants
{
    /**
     * Key for the 'claimId' put into the session by the {@link ChooseClaimEventProcessor}.
     */
    public static final String FIELD_CLAIM_ID = "claimId";
    
    /**
     * Key for the 'user' put into the session by the {@link StartCMISSession}.
     */
    public static final String FIELD_USER = "user";
}

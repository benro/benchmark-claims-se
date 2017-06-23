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

/**
 * POJO representing basic <i>claim</i> data for persistence.
 * <p>
 * Note that the randomizer is simply a random number from zero to one million.
 * The same random number is used in the DAO code to select a random claim.
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ClaimData
{
    public static final String FIELD_RANDOMIZER = "randomizer";
    public static final String FIELD_CLAIM_ID = "claimId";
    public static final String FIELD_CREATION_STATE = "creationState";

    private int randomizer;
    private String claimId;
    private DataCreationState creationState;
    
    public ClaimData()
    {
        randomizer = (int)(Math.random() * 1E6);
        creationState = DataCreationState.Unknown;
    }

    public int getRandomizer()
    {
        return randomizer;
    }
    public void setRandomizer(int randomizer)
    {
        this.randomizer = randomizer;
    }
    public String getClaimId()
    {
        return claimId;
    }
    public void setClaimId(String claimId)
    {
        this.claimId = claimId;
    }
    public DataCreationState getCreationState()
    {
        return creationState;
    }
    public void setCreationState(DataCreationState creationState)
    {
        this.creationState = creationState;
    }
}

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
package org.alfresco.bm.data;

/**
 * POJO representing basic <i>claim</i> data for persistence 
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ClaimData
{
    public static final String FIELD_ID = "id";
    public static final String FIELD_STATE = "state";

    private String id;
    private DataCreationState state;
    
    public ClaimData()
    {
        state = DataCreationState.Unknown;
    }

    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public DataCreationState getState()
    {
        return state;
    }
    public void setState(DataCreationState state)
    {
        this.state = state;
    }
}

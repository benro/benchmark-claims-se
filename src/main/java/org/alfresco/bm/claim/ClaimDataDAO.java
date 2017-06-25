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

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;

/**
 * DAO for retrieving claims data
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class ClaimDataDAO
{
    private final DBCollection collection;

    /**
     * @param db                    MongoDB
     * @param collection            name of DB collection containing claim data
     */
    public ClaimDataDAO(DB db, String collection)
    {
        super();
        this.collection = db.getCollection(collection);
        
        this.collection.setWriteConcern(WriteConcern.ACKNOWLEDGED);
        
        // An index to retrieve a specific claim.
        // We could reuse the '_id' field but the necessary use of ObjectId can be forgotten sometimes.
        DBObject idxClaimId = BasicDBObjectBuilder
                .start(ClaimData.FIELD_CLAIM_ID, 1)
                .get();
        DBObject optClaimId = BasicDBObjectBuilder
                .start("name", "idxClaimId")
                .add("unique", true)
                .get();
        this.collection.createIndex(idxClaimId, optClaimId);
        
        // An index to find random claims that are in a given state
        DBObject idxCreationStateRand = BasicDBObjectBuilder
                .start(ClaimData.FIELD_CREATION_STATE, 1)
                .add(ClaimData.FIELD_RANDOMIZER, 2)
                .get();
        DBObject optCreationStateRand = BasicDBObjectBuilder
                .start("name", "idxCreationStateRand")
                .add("unique", Boolean.FALSE)
                .get();
        this.collection.createIndex(idxCreationStateRand, optCreationStateRand);
    }

    /**
     * Create a new claim
     * 
     * @return                      <tt>true</tt> if the insert was successful
     */
    public boolean createClaim(String claimId)
    {
        DBObject insertObj = BasicDBObjectBuilder
                .start()
                .add(ClaimData.FIELD_RANDOMIZER, (int) (Math.random() * (double) 1e6))
                .add(ClaimData.FIELD_CLAIM_ID, claimId)
                .add(ClaimData.FIELD_CREATION_STATE, DataCreationState.Unknown.toString())
                .get();
        try
        {
            collection.insert(insertObj);
            return true;
        }
        catch (MongoException e)
        {
            return false;
        }
    }
    
    /**
     * Build the POJO from the raw data.<br>
     * This is public for testing.
     * 
     * @return                      the fill pojo or <tt>null</tt> if the result is <tt>null</tt> as well
     */
    public ClaimData fromDBObject(DBObject resultObj)
    {
        if (resultObj == null)
        {
            return null;
        }
        else
        {
            ClaimData result = new ClaimData();
            String creationStateStr = (String) resultObj.get(ClaimData.FIELD_CREATION_STATE);
            DataCreationState creationState = DataCreationState.valueOf(creationStateStr);
            result.setCreationState(creationState);
            result.setClaimId( (String) resultObj.get(ClaimData.FIELD_CLAIM_ID));
            return result;
        }
    }
    
    public boolean updateClaimState(String claimId, DataCreationState state)
    {
        DBObject findObj = new BasicDBObject()
                .append(ClaimData.FIELD_CLAIM_ID, claimId);
        DBObject setObj = BasicDBObjectBuilder
                .start()
                .push("$set")
                    .append(ClaimData.FIELD_CREATION_STATE, state.toString())
                 .pop()
                 .get();
        DBObject foundObj = collection.findAndModify(findObj, setObj);
        return foundObj != null;
    }
    
    /**
     * @param creationState         the state to count or <tt>null</tt> to count all
     * 
     * @return                      the number of claims with the given state
     */
    public long countClaims(DataCreationState creationState)
    {
        BasicDBObjectBuilder queryObjBuilder = BasicDBObjectBuilder.start();
        if (creationState != null)
        {
            queryObjBuilder.add(ClaimData.FIELD_CREATION_STATE, creationState.toString());
        }
        DBObject queryObj = queryObjBuilder.get();
        
        return collection.count(queryObj);
    }
    
    /**
     * Find a claim by its ID
     * 
     * @param claimId               the id of the claim to find
     * @return                      Returns the data or <tt>null</tt> if not found
     */
    public ClaimData findClaimById(String claimId)
    {
        DBObject queryObj = BasicDBObjectBuilder
                .start()
                .add(ClaimData.FIELD_CLAIM_ID, claimId)
                .get();
        DBObject resultObj = collection.findOne(queryObj);
        return fromDBObject(resultObj);
    }
    
    /**
     * Retrieve a random claim that has the given state
     * 
     * @return                      a random claim or <tt>null</tt> if none exist
     */
    public ClaimData getRandomClaim(DataCreationState state)
    {
        int random = (int) (Math.random() * (double) 1e6);
        DBObject queryObj = BasicDBObjectBuilder.start()
                .add(ClaimData.FIELD_CREATION_STATE, state.toString())
                .push(ClaimData.FIELD_RANDOMIZER)
                    .add("$gte", Integer.valueOf(random))
                .pop()
                .get();
        
        DBObject claimDataObj = collection.findOne(queryObj);
        if(claimDataObj == null)
        {
            queryObj.put(ClaimData.FIELD_RANDOMIZER, new BasicDBObject("$lt", random));
            claimDataObj = collection.findOne(queryObj);
        }
        return fromDBObject(claimDataObj);
    }
}

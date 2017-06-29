/*
* Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.bm.claim
;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

import org.alfresco.bm.cmis.AbstractCMISEventProcessor;
import org.alfresco.bm.cmis.AbstractQueryCMISEventProcessor;
import org.alfresco.bm.cmis.CMISEventData;
import org.alfresco.bm.event.Event;
import org.alfresco.bm.event.EventResult;
import org.alfresco.bm.file.TestFileService;
import org.alfresco.bm.session.SessionService;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.commons.logging.Log;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

/**
 * Perform a search in the current folder
 * 
 * <h1>Input</h1>
 * 
 * A {@link CMISEventData data object } containing an existing folder.
 * 
 * <h1>Actions</h1>
 * 
 * Perform a search in the current folder using search terms from a
 * remotely-provided text file
 * 
 * <h1>Output</h1>
 * 
 * {@link #EVENT_NAME_SEARCH_COMPLETED}: The {@link CMISEventData data object}
 * without changes<br/>
 * 
 * @author Derek Hulley
 * @since 1.0
 */
public class SearchClaim extends AbstractCMISEventProcessor {
	public static final String EVENT_NAME_SEARCH_COMPLETED = "cmis.searchCompleted";
	public static final String CLAIMS_QUERY = "SELECT D.*, C.* FROM cl:claim AS D JOIN cl:claim AS C ON D.cmis:objectId = C.cmis:objectId where C.cl:claimId='%s'";

	//private final TestFileService testFileService;
	//private final String searchTermsFilename;
	//private String[] searchStrings;
	private String eventNameSearchCompleted;
	private SessionService sessionService;

	/**
	 * @param testFileService
	 *            service to provide search terms files
	 * @param searchTermsFilename
	 *            the name of the remote file containing search terms to use
	 */
	public SearchClaim(SessionService sessionService) {
		this.sessionService = sessionService;	
		this.eventNameSearchCompleted = EVENT_NAME_SEARCH_COMPLETED;
	}

	/**
	 * Override the {@link #EVENT_NAME_SEARCH_COMPLETED default} event name for
	 * 'search completed'.
	 */
	public void setEventNameSearchCompleted(String eventNameSearchCompleted) {
		this.eventNameSearchCompleted = eventNameSearchCompleted;
	}


	@Override
	@SuppressWarnings("unused")
	protected EventResult processCMISEvent(Event event) throws Exception {
		super.suspendTimer(); // Timer control
				
		CMISEventData data = (CMISEventData) event.getData();
		
		
		
		if (data == null) {
			logger.warn("Unable to query CMIS folder: no session provided.");
			return new EventResult("Unable to query CMIS folder: no session provided.", false);
		}
		
		String sessionId = event.getSessionId();
		DBObject sessionObj = sessionService.getSessionData(sessionId);

		//TODO check sessionId and sessionObj for null
		
		String claimId = (String) sessionObj.get(ClaimSessionConstants.FIELD_CLAIM_ID);
		
		//TODO check claimID for null        			
		
        
        // A quick double-check
        if (data == null)
        {
            return new EventResult("Unable to search for folder; no session provided.", false);
        }      
	
	        
		
		String query = String.format(CLAIMS_QUERY, claimId);
		Session session = data.getSession();

	
		Folder folder = null;

		// execute query
		ItemIterable<QueryResult> results = session.query(query, false);
		Iterator<QueryResult> it = results.iterator();

		// this will not work because the search returns objects not
		// accessible
		// for (QueryResult queryResult : results)

		while (it.hasNext()) {
			QueryResult queryResult = null;
			try {
				queryResult = it.next();
			} catch (Exception e) {
				logger.error("Unable to get next folder query result.", e);
				continue;
			}
			// get folder object from CMIS and store it to bread-crumb event
			// data
			//String objectId = queryResult.getPropertyValueByQueryName("D.cmis:objectId");
			 String objectId = (String)queryResult.getPropertyById("cmis:objectId").getFirstValue();
			try {
				folder = (Folder) session.getObject(session.createObjectId(objectId));
				if (null != folder) {
					data.getBreadcrumb().add(folder);
					if (logger.isDebugEnabled()) {
						logger.debug("Found folder with ID '" + objectId + "'.");
					}

					break;
				}
			} catch (Exception e) {
				logger.error("Unable to create folder from object with ID '" + objectId + "'.", e);
				folder = null;
			}
		}
		if (folder == null) {
			return new EventResult("No match found for the given Claim ID. query: " + query, false);
		}
		super.stopTimer(); // Timer control      

		// Done
		Event doneEvent = new Event(eventNameSearchCompleted, data);
		EventResult result = new EventResult(
				BasicDBObjectBuilder
					.start()
					.append("msg", "Successfully searched in folder.")
					.append("query", query)
					.push("folder")
						.append("id", folder.getId())
						.append("name", folder.getName())
					.pop()
					.get(),
				doneEvent);

		// Done
		return result;
	}
	
}

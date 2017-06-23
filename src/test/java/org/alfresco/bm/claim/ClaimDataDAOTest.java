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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.UUID;

import org.alfresco.bm.data.DataCreationState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * @see ClaimDataDAO
 * 
 * @author Derek Hulley
 * @since 1.3
 */
@RunWith(JUnit4.class)
public class ClaimDataDAOTest
{
    private final static String COLLECTION_BM_CLAIM_DAO_TEST = "BenchmarkClaimDAOTest";
    
    public final static String[] CLAIM_IDS = new String[] {"A-123", "B-234", "C-345", "D-456", "E-567"};

    private static AbstractApplicationContext ctx;
    private static ClaimDataDAO claimDataDAO;

    @Before
    public void setUp()
    {
        Properties props = new Properties();
        props.put("mongoCollection", COLLECTION_BM_CLAIM_DAO_TEST);
        
        ctx = new ClassPathXmlApplicationContext(new String[] {"test-MongoClaimDataDAOTest-context.xml"}, false);
        ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("TestProps", props));
        ctx.refresh();
        ctx.start();
        claimDataDAO = ctx.getBean(ClaimDataDAO.class);

        // Generate some random users
        for (int i = 0; i < CLAIM_IDS.length; i++)
        {
            String claimId = CLAIM_IDS[i];
            claimDataDAO.createClaim(claimId);
        }
    }
    
    @After
    public void tearDown()
    {
        ctx.close();
    }
    
    @Test
    public void testSetUp()
    {
        for (int i = 0; i < CLAIM_IDS.length; i++)
        {
            String claimId = CLAIM_IDS[i];
            ClaimData claimData = claimDataDAO.findClaimById(claimId);
            assertNotNull("Expect to find all the created claims.", claimData);
        }
    }
    
    @Test
    public void testDuplicateClaim()
    {
        String randomClaimId = UUID.randomUUID().toString();
        boolean inserted = claimDataDAO.createClaim(randomClaimId);
        assertTrue(inserted);
        // This should fail
        boolean reinserted = claimDataDAO.createClaim(randomClaimId);
        assertFalse(reinserted);
    }
    
    @Test
    public void testClaimNotExist()
    {
        boolean updated = claimDataDAO.updateClaimState("Bob", DataCreationState.Created);
        assertFalse(updated);

        ClaimData claimData = claimDataDAO.findClaimById("Bob");
        assertNull("Expected to NOT find this claim.", claimData);
    }
    
    @Test
    public void testRandomClaim()
    {
        ClaimData claimData = claimDataDAO.getRandomClaim(DataCreationState.Unknown);
        assertNotNull("Expected to find a random 'Unknown' claim.", claimData);
        
        claimData = claimDataDAO.getRandomClaim(DataCreationState.Created);
        assertNull("Expected to NOT find a random 'Created' claim.", claimData);
        
        claimDataDAO.updateClaimState("A-123", DataCreationState.Created);
        claimData = claimDataDAO.getRandomClaim(DataCreationState.Created);
        assertNotNull("Expected to find a random 'Created' claim.", claimData);
    }
}

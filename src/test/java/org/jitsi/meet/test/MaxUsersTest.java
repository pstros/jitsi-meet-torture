/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.meet.test;


import org.jitsi.meet.test.util.*;
import org.openqa.selenium.*;

import junit.framework.*;

/**
 * Test for max users prosody module.
 * 
 * NOTE: The tests assumes that the module is deployed and configured.
 *
 * @author Hristo Terezov
 */
public class MaxUsersTest
    extends TestCase
{
    /**
     * Number of participants in the call.
     */
    public static int MAX_USERS = 3;
    
    /**
     * The property to change MAX_USERS variable.
     */
    public static String MAX_USERS_PROP = "max_users_tests.max_users";

    /**
     * Constructs test
     * @param name the method name for the test.
     */
    public MaxUsersTest(String name)
    {
        super(name);
    }


    /**
     * Orders the tests.
     * @return the suite with order tests.
     */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite();

        suite.addTest(
            new MaxUsersTest("enterWithMaxParticipantsAndCheckDialog"));

        return suite;
    }
    
    
    /**
     * Scenario tests wether an error dialog is displayed when MAX_USERSth
     * participant join the conference.
     */
    public void enterWithMaxParticipantsAndCheckDialog() 
    {
        String roomName = "MaxUsersTortureTest";

        // Exit all participants
        ConferenceFixture.close(ConferenceFixture.getOwnerInstance());
        ConferenceFixture.quit(ConferenceFixture.getSecondParticipant());
        
        // Start owner with custom roomname used to set the max occupants by prosody
        WebDriver owner = ConferenceFixture.startOwner(null, roomName);

        String maxUsersString = System.getProperty(MAX_USERS_PROP);
        if(maxUsersString != null)
        {
            MAX_USERS = Integer.parseInt(maxUsersString);
        }

        boolean failed = false;

        // The owner has already been started, so MAX - 1 users are needed to hit the 
        // occupant limit of the room
        WebDriver[] participants = new WebDriver[MAX_USERS - 1];
        try
        {
            for(int i = 0; i < participants.length; i++)
            {
                // Participants join the custom room created by the owner
                participants[i] = ConferenceFixture.startParticipant(null, roomName);
            }
            // Check if the error dialog is displayed for the last participant.
            int lastParticipantIdx = participants.length - 1;
            checkDialog(participants[lastParticipantIdx]);
        } 
        catch(TimeoutException timeout)
        {
            // There was no dialog, so we fail the test !
            failed = true;
        }
        finally
        {
            // Clean up the participants in participants array
            quitParticipants(participants);
            ConferenceFixture.restartParticipants();
        }

        if (failed)
        {
            fail("There was no error dialog.");
        }
    }

    /**
     * Quits the browsers of the passed participants.
     * @param participants array with participants that are going to quited.
     */
    private void quitParticipants(WebDriver[] participants)
    {
        // Clean up the participants in participants array
        for(int i = 0; i < participants.length; i++) 
        {
            ConferenceFixture.quit(participants[i], false);
        }
    }


    /**
     * Check if the error dialog is displayed for participant.
     * @param participant the participant 
     */
    private void checkDialog(WebDriver participant)
    {
        TestUtils.waitForElementByXPath(participant, 
            "//span[@data-i18n='dialog.maxUsersLimitReached']", 5);
    }

}

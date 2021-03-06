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

import junit.framework.*;

import org.jitsi.meet.test.util.*;

import org.openqa.selenium.*;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * Launches a hook script that will launch a participant that will join
 * the conference and that participant will be sharing its screen.
 */
public class DesktopSharingTest
    extends TestCase
{
    /**
     * A property to specified the external script that will be used to
     * start the new participant.
     */
    public final static String HOOK_SCRIPT = "desktop.sharing.hook.script";

    /**
     * Exception on starting the hook, null if script has started successfully.
     */
    private Exception hookException = null;

    /**
     * Ensure we have only one participant available in the room - the owner.
     * Starts the new participants using the external script and check whether
     * the stream we are receiving from him is screen.
     * Returns at the end the state we found tests - with 2 participants.
     */
    public void testDesktopSharingInPresence()
    {
        final String hookScript = System.getProperty(HOOK_SCRIPT);

        if(hookScript == null)
            return;

        System.err.println("Start testDesktopSharingInPresence.");

        ConferenceFixture.closeAllParticipantsExceptTheOwner();

        // Counter we wait for the process execution in the thread to finish
        final CountDownLatch waitEndSignal = new CountDownLatch(1);

        // counter we wait for indication that the process has started
        // or that it ended up with an exception - hookException
        final CountDownLatch waitStartSignal = new CountDownLatch(1);

        // this will fire a hook script, which needs to launch a browser that
        // will join our room
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String URL =
                        System.getProperty(
                            ConferenceFixture.JITSI_MEET_URL_PROP) + "/"
                            + ConferenceFixture.currentRoomName
                            + "#config.requireDisplayName=false"
                            + "&config.firefox_fake_device=true"
                            + "&config.autoEnableDesktopSharing=true";
                    String[] cmd =
                        { hookScript, URL};

                    System.err.println("Start the script with param:"+ URL);
                    ProcessBuilder pb = new ProcessBuilder(cmd);

                    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                    Process p = pb.start();

                    waitStartSignal.countDown();

                    p.waitFor();
                    System.err.println("Script ended execution.");
                    waitEndSignal.countDown();
                }
                catch (IOException | InterruptedException e)
                {
                    hookException = e;
                    waitStartSignal.countDown();
                }
            }
        }).start();

        final WebDriver owner = ConferenceFixture.getOwner();

        // now lets wait satarting or error on startup
        try
        {
            waitStartSignal.await(2, TimeUnit.SECONDS);
            if(hookException != null)
            {
                hookException.printStackTrace();
                assertFalse("Error executing hook script:"
                    + hookException.getMessage(), true);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        // let's wait some time the user to joins
        TestUtils.waitForBoolean(
            owner,
            "return (APP.conference.membersCount == 2);",
            25);
        MeetUtils.waitForIceConnected(owner);
        MeetUtils.waitForSendReceiveData(owner);
        MeetUtils.waitForRemoteStreams(owner, 1);

        // now lets check whether his stream is screen
        String remoteParticipantID = owner
            .findElement(By.xpath("//span[starts-with(@id, 'participant_') " +
                " and contains(@class,'videocontainer')]")).getAttribute("id");
        remoteParticipantID
            = remoteParticipantID.replaceAll("participant_", "");

        final String expectedResult = "desktop";
        // holds the last retrieved value for the remote type
        final Object[] remoteVideoType = new Object[1];
        try
        {
            final String scriptToExecute = "return APP.UI.getRemoteVideoType('"
                + remoteParticipantID + "');";
            (new WebDriverWait(owner, 5))
                .until(new ExpectedCondition<Boolean>()
                {
                    public Boolean apply(WebDriver d)
                    {
                        Object res =
                            ((JavascriptExecutor) owner)
                                .executeScript(scriptToExecute);
                        remoteVideoType[0] = res;

                        return res != null && res.equals(expectedResult);
                    }
                });
        }
        catch (TimeoutException e)
        {
            assertEquals("Wrong video type, maybe desktop sharing didn't work",
                expectedResult, remoteVideoType[0]);
        }

        // allow the participant to leave
        try
        {
            waitEndSignal.await(10, TimeUnit.SECONDS);
            System.err.println("End DesktopSharingTest.");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        ConferenceFixture.ensureTwoParticipants();
    }
}

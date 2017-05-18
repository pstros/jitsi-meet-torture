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

import java.io.*;
import java.util.*;

/**
 * Setup conference with Shibboleth authentication.
 * 1. Clicks "authenticate" button in warning dialog.
 * 2. Redirect to Identity provider selection page.
 * 3. Selects configured identity provider.
 * 3. Redirected to Identity Provider login page.
 * 4. Login and get redirected back to Jitsi-meet room.
 *
 * Redirection is automatically done by Meet and Shibboleth(not by tests).
 *
 * Configuration is done through .properties file specified in
 * {@link #PROPERTIES_FILE_PNAME} system property(-D argument).
 *
 * @author Pawel Domas
 */
public class AuthSetupConference
    extends SetupConference
{
    /**
     * The name of system property which specifies the name of .properties file
     * which contains authentication properties.
     */
    public static final String PROPERTIES_FILE_PNAME
            = "jitsi-meet.auth.properties";

    /**
     * Base for authentication property names.
     */
    private static final String PNAME_BASE = "org.jitsi.meet.test.auth.";

    /**
     * Property for the id of input element used to enter IdP name.
     */
    public static final String IDP_SELECT_LIST_ID
            = PNAME_BASE + "idp_select_list_id";

    /**
     * Property specifies the name of Shibboleth Identity Provider.
     */
    public static final String IDP_NAME
            = PNAME_BASE + "idp_name";

    /**
     * Property for the name of submit button which confirms IdP name.
     */
    public static final String IDP_SUBMIT_BTN_NAME
            = PNAME_BASE + "idp_submit_btn_name";

    /**
     * Property for the name of username input field on IdP login page.
     */
    public static final String USERNAME_INPUT_ID
            = PNAME_BASE + "username_input_id";

    /**
     * The property specifies the name of password input field on IdP login
     * page.
     */
    public static final String PASSWORD_INPUT_ID
            = PNAME_BASE + "password_input_id";

    /**
     * The property specifies username used to login.
     */
    public static final String USERNAME = PNAME_BASE + "username";

    /**
     * The property which specifies user password.
     */
    public static final String PASSWORD = PNAME_BASE + "password";

    /**
     * Property specifies name of the login button used to confirm
     * the credentials.
     */
    public static final String LOGIN_BTN_NAME = PNAME_BASE + "login_btn_name";

    /**
     * Constructs test.
     *
     * @param name method name.
     */
    public AuthSetupConference(String name)
    {
        super(name);
    }

    /**
     * Orders tests.
     * @return the suite with order tests.
     */
    public static junit.framework.Test suite()
    {
        TestSuite parentSuite = (TestSuite) SetupConference.suite();
        Enumeration<Test> parentTests = parentSuite.tests();
        TestSuite suite = new TestSuite();

        // We want to insert "authenticate" after "startOwner"
        suite.addTest(parentTests.nextElement()); // startOwner
        suite.addTest(new AuthSetupConference("authenticate"));

        // The rest of the tests
        while (parentTests.hasMoreElements())
        {
            suite.addTest(parentTests.nextElement());
        }

        return suite;
    }

    public void authenticate() {

        String authenticationProperties
                = System.getProperty(PROPERTIES_FILE_PNAME);

        if (authenticationProperties == null
                || authenticationProperties.isEmpty())
        {
            throw new RuntimeException(
                    "Authentication properties filename " +
                            "'jitsi-meet.auth.properties' not configured");
        }

        // Identity Provider selection page
        String idpSelectListId;
        String idpName;
        String idpSubmitBtnName;

        // Identity provider login page
        String    usernameInputId;
        String    passwordInputId;
        String    loginBtnName;
        String    username;
        String    password;

        try
        {
            Properties properties
                = PropertiesUtils.loadPropertiesFile(authenticationProperties);

            properties.list(System.out);

            idpSelectListId = properties.getProperty(IDP_SELECT_LIST_ID);
            idpName = properties.getProperty(IDP_NAME);
            idpSubmitBtnName = properties.getProperty(IDP_SUBMIT_BTN_NAME);

            usernameInputId = properties.getProperty(USERNAME_INPUT_ID);
            passwordInputId = properties.getProperty(PASSWORD_INPUT_ID);
            loginBtnName = properties.getProperty(LOGIN_BTN_NAME);
            username = properties.getProperty(USERNAME);
            password = properties.getProperty(PASSWORD);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        WebDriver moderator = ConferenceFixture.getOwner();

        String authButtonName = "jqi_state0_" +
                "buttonspandatai18ndialogIamHostIamthehostspan";


        TestUtils.waitForElementByXPath(
            moderator, "//button[@name='" + authButtonName + "']", 15);

        WebElement authButton = moderator.findElement(By.name(authButtonName));

        authButton.click();

        // Redirected to Idp selection if a idp information is given.  Otherwise
        // use the default username/password authentication
        if (idpSelectListId == null && idpName == null && idpSubmitBtnName == null)
        {
          usernamePasswordAuth(moderator, username, password);
        }
        else
        {
          shibbolethIdpSelect(
            moderator, idpSelectListId, idpName, idpSubmitBtnName);

          // IdpLogin
          shibbolethIdpLogin(
            moderator, usernameInputId, passwordInputId, loginBtnName,
            username,  password);
        }

    }

    private void shibbolethIdpSelect(WebDriver participant,
                                     String idpSelectListId,
                                     String idpName,
                                     String idSubmitBtnName)
    {
        TestUtils.waitForElementByXPath(
            participant, "//input[@id='" + idpSelectListId + "']", 15);

        WebElement idpInput = participant.findElement(By.id(idpSelectListId));
        idpInput.sendKeys(idpName);

        WebElement submit = participant.findElement(By.name(idSubmitBtnName));
        submit.click();
    }

    private void usernamePasswordAuth(WebDriver participant,
                                     String username,
                                     String password)
    {
      String authLoginButtonName = "jqi_login_" +
            "buttonspandatai18ndialogOkOkspan";

      TestUtils.waitForElementByXPath(
          participant, "//input[@name='username']", 15);

      // The dialog creates multiple elements of the password required dialog box
      // so for now we will assume the second element is correct one
      List<WebElement> usernameElem = participant.findElements(By.cssSelector("[name='username']"));
      List<WebElement> passwordElem = participant.findElements(By.cssSelector("[name='password']"));

      usernameElem.get(1).sendKeys(username);
      passwordElem.get(1).sendKeys(password);

      List <WebElement> submit = participant.findElements(By.name(authLoginButtonName));
      submit.get(1).click();
    }

    private void shibbolethIdpLogin( WebDriver participant,
                                     String    usernameInputId,
                                     String    passwordInputId,
                                     String    loginBtnName,
                                     String    username,
                                     String    password )
    {
        TestUtils.waitForElementByXPath(
            participant, "//input[@id='" + usernameInputId + "']", 15);


        WebElement usernameElem
            = participant.findElement(By.id(usernameInputId));
        WebElement passwordElem
            = participant.findElement(By.id(passwordInputId));

        usernameElem.sendKeys(username);

        passwordElem.sendKeys(password);

        WebElement submit = participant.findElement(By.name(loginBtnName));

        submit.click();
    }
}

/*
 * Copyright 2011 Leonid Maslov<leonidms@gmail.com>
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

package com.blogspot.leonardinius.servlet;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.sal.api.websudo.WebSudoSessionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScriptRunnerSessionServlet extends HttpServlet
{
// ------------------------------ FIELDS ------------------------------

    private final UserManager userManager;
    private final WebSudoManager webSudoManager;
    private final LoginUriProvider loginUriProvider;

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptRunnerSessionServlet(final UserManager userManager,
                                      final WebSudoManager webSudoManager, LoginUriProvider loginUriProvider)
    {
        this.loginUriProvider = checkNotNull(loginUriProvider, "loginUriProvider");
        this.userManager = checkNotNull(userManager, "userManager");
        this.webSudoManager = checkNotNull(webSudoManager, "webSudoManager");
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // WebSudo
        try
        {
            webSudoManager.willExecuteWebSudoRequest(request);

            if (loginIfNot(request, response)) return; //just in case websudo was disabled

            if (!IsSystemAdminRequest(request))
            {
                throw new ServletException("The action could only be executed by system administrators");
            }

            //my code here
        }
        catch (WebSudoSessionException wes)
        {
            webSudoManager.enforceWebSudoProtection(request, response);
        }
    }

    private boolean loginIfNot(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        if (userManager.getRemoteUsername(request) == null)
        {
            try
            {
                URI currentUri = new URI(request.getRequestURI());
                URI loginUri = loginUriProvider.getLoginUri(currentUri);
                response.sendRedirect(loginUri.toASCIIString());
            }
            catch (URISyntaxException e)
            {
                throw new ServletException(e);
            }
            catch (IOException e)
            {
                throw new ServletException(e);
            }
            return true;
        }
        return false;
    }

    private boolean IsSystemAdminRequest(HttpServletRequest request)
    {
        return userManager.isSystemAdmin(userManager.getRemoteUsername(request));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // WebSudo
        try
        {
            webSudoManager.willExecuteWebSudoRequest(request);

            if (loginIfNot(request, response)) return; //just in case websudo was disabled

            if (!IsSystemAdminRequest(request))
            {
                throw new ServletException("The action could only be executed by system administrators");
            }

            //my code here
        }
        catch (WebSudoSessionException wes)
        {
            webSudoManager.enforceWebSudoProtection(request, response);
        }
    }
}
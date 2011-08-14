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
import com.google.common.base.Function;

import javax.annotation.Nullable;
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
                                      final WebSudoManager webSudoManager,
                                      final LoginUriProvider loginUriProvider)
    {
        this.loginUriProvider = checkNotNull(loginUriProvider, "loginUriProvider");
        this.userManager = checkNotNull(userManager, "userManager");
        this.webSudoManager = checkNotNull(webSudoManager, "webSudoManager");
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        execute(new Function<RequestHolder, RequestHolder>()
        {
            @Override
            public RequestHolder apply(@Nullable RequestHolder requestHolder)
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }, new RequestHolder(request, response));
    }

    private void execute(Function<RequestHolder, RequestHolder> function, RequestHolder holder) throws IOException, ServletException
    {
        if (userManager.getRemoteUsername(holder.getRequest()) == null)
        {
            try
            {
                URI requestUri = new URI(holder.getRequest().getRequestURI());
                URI loginUrl = loginUriProvider.getLoginUri(requestUri);
                holder.getResponse().sendRedirect(loginUrl.toASCIIString());
            }
            catch (URISyntaxException e)
            {
                throw new ServletException(e);
            }
        } else
        {
            // WebSudo
            try
            {
                webSudoManager.willExecuteWebSudoRequest(holder.getRequest());


                function.apply(holder);
            }
            catch (WebSudoSessionException wes)
            {
                webSudoManager.enforceWebSudoProtection(holder.getRequest(), holder.getResponse());
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        execute(new Function<RequestHolder, RequestHolder>()
        {
            @Override
            public RequestHolder apply(@Nullable RequestHolder requestHolder)
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }, new RequestHolder(request, response));
    }

// -------------------------- INNER CLASSES --------------------------

    private static class RequestHolder
    {
        public HttpServletRequest getRequest()
        {
            return request;
        }

        public HttpServletResponse getResponse()
        {
            return response;
        }

        private final HttpServletRequest request;
        private final HttpServletResponse response;

        public RequestHolder(HttpServletRequest request, HttpServletResponse response)
        {
            this.request = request;
            this.response = response;
        }
    }
}
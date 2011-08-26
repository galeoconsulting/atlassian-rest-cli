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

package com.galeoconsulting.leonardinius.servlet;

import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.sal.api.websudo.WebSudoSessionException;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ScriptRunnerSessionServlet extends HttpServlet {
// ------------------------------ FIELDS ------------------------------

    private static final String VELOCITY_TEMPLATES_CONFIG_PARAMETER = "velocity-templates";
    private static final String SERVLET_PATH = "servlet-path";
    private final UserManager userManager;
    private final WebSudoManager webSudoManager;
    private final LoginUriProvider loginUriProvider;
    private final TemplateRenderer templateRenderer;

    private Logger log = LoggerFactory.getLogger(ScriptRunnerSessionServlet.class);

    private Map<String, String> configuredTemplates;

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptRunnerSessionServlet(final UserManager userManager,
                                      final WebSudoManager webSudoManager, LoginUriProvider loginUriProvider, TemplateRenderer templateRenderer) {
        this.templateRenderer = checkNotNull(templateRenderer, "templateRenderer");
        this.loginUriProvider = checkNotNull(loginUriProvider, "loginUriProvider");
        this.userManager = checkNotNull(userManager, "userManager");
        this.webSudoManager = checkNotNull(webSudoManager, "webSudoManager");
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    private Map<String, String> getConfiguredTemplates() {
        if (configuredTemplates == null) {
            String parameterValue = checkNotNull(
                    getInitParameter(VELOCITY_TEMPLATES_CONFIG_PARAMETER),
                    VELOCITY_TEMPLATES_CONFIG_PARAMETER + " servlet configuration parameter");

            Collection<String> entries = new LinkedHashSet<String>(
                    Arrays.asList(StringUtils.split(parameterValue, ';')));

            entries = Collections2.filter(entries, new Predicate<String>() {
                @Override
                public boolean apply(@Nullable String s) {
                    return StringUtils.isNotBlank(s);
                }
            });

            Map<String, String> result = Maps.newHashMap();
            for (String entry : entries) {
                String data[] = StringUtils.split(entry, '|');
                result.put(StringUtils.trimToNull(data[0]), StringUtils.trimToNull(data[1]));
            }

            configuredTemplates = ImmutableMap.copyOf(result);
        }

        return configuredTemplates;
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doExecuteHttpMethod(request, response);
    }

    private void doExecuteHttpMethod(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // WebSudo
        try {
            webSudoManager.willExecuteWebSudoRequest(request);

            if (loginIfNot(request, response)) return; //just in case websudo was disabled

            if (!IsSystemAdminRequest(request)) {
                throw new ServletException("The action could only be executed by system administrators");
            }

            response.setContentType(MediaType.TEXT_HTML);
            templateRenderer.render(
                    checkNotNull(getVelocityTemplate(request), "templateName"),
                    makeContext(),
                    response.getWriter());
        } catch (WebSudoSessionException wes) {
            webSudoManager.enforceWebSudoProtection(request, response);
        }
    }

    private boolean loginIfNot(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if (userManager.getRemoteUsername(request) == null) {
            try {
                URI currentUri = new URI(request.getRequestURI());
                URI loginUri = loginUriProvider.getLoginUri(currentUri);
                response.sendRedirect(loginUri.toASCIIString());
            } catch (URISyntaxException e) {
                throw new ServletException(e);
            } catch (IOException e) {
                throw new ServletException(e);
            }
            return true;
        }
        return false;
    }

    private boolean IsSystemAdminRequest(HttpServletRequest request) {
        return userManager.isSystemAdmin(userManager.getRemoteUsername(request));
    }

    private String getVelocityTemplate(HttpServletRequest request) throws ServletException {
        try {
            String templateNamePathKey = getTemplateKey(request);
            Map<String, String> templates = getConfiguredTemplates();
            if (templates.containsKey(templateNamePathKey)) {
                String template = templates.get(templateNamePathKey);
                log.trace("Velocity template to be parsed: {}", template);
                return template;
            }

            log.warn("No templates mapped for: {} (mapping: {})",
                    templateNamePathKey,
                    MapUtils.toProperties(templates));
            return null;
        } catch (IllegalArgumentException e) {
            throw new ServletException(e);
        } catch (URISyntaxException e) {
            throw new ServletException(e);
        }
    }

    private String getTemplateKey(HttpServletRequest request) throws URISyntaxException {
        String leadingTrimPath = new StringBuilder()
                .append(request.getContextPath())
                .append(request.getServletPath())
                .toString();
        return new URI(leadingTrimPath)
                .relativize(URI.create(new URI(request.getRequestURI()).getPath()))
                .toASCIIString();
    }

    private Map<String, Object> makeContext() {
        return new MapMaker().makeMap();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doExecuteHttpMethod(request, response);
    }
}
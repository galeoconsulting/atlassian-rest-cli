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

package com.galeoconsulting.leonardinius.api.impl;

import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.galeoconsulting.leonardinius.api.ScriptService;
import com.galeoconsulting.leonardinius.api.ScriptSessionManager;
import com.galeoconsulting.leonardinius.api.ServletRequestHolder;
import com.galeoconsulting.leonardinius.api.ServletVelocityHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.script.ScriptEngineFactory;
import javax.servlet.ServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.galeoconsulting.leonardinius.api.ScriptSessionManager.ScriptSession;
import static com.galeoconsulting.leonardinius.api.ScriptSessionManager.SessionId;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: leonardinius
 * Date: 8/22/11
 * Time: 9:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServletVelocityHelperImpl implements ServletVelocityHelper
{
// ------------------------------ FIELDS ------------------------------

    private final ScriptSessionManager sessionManager;
    private final UserManager userManager;
    private final ScriptService scriptService;

    private final ServletRequestHolder requestHolder;

// --------------------------- CONSTRUCTORS ---------------------------

    public ServletVelocityHelperImpl(UserManager userManager, ScriptSessionManager sessionManager, ScriptService scriptService, ServletRequestHolder requestHolder)
    {
        this.requestHolder = checkNotNull(requestHolder, "requestHolder");
        this.scriptService = checkNotNull(scriptService, "scriptService");
        this.userManager = checkNotNull(userManager, "userManager");
        this.sessionManager = checkNotNull(sessionManager, "sessionManager");
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ServletVelocityHelper ---------------------


    @Override
    public Map<String, String[]> getAllRequestParameters()
    {
        return getRequestInstance().getParameterMap();
    }

    @Override
    public List<SessionBean> getAllSessionBeans()
    {
        List<SessionBean> list = Lists.newArrayList();

        for (Map.Entry<SessionId, ScriptSession> entry : sessionManager.listAllSessions().entrySet())
        {
            list.add(SessionBean.newInstance(entry.getKey(), entry.getValue(), getUserProfile(entry.getValue().getCreator())));
        }

        Collections.sort(list, new Comparator<SessionBean>()
        {
            @Override
            public int compare(SessionBean o1, SessionBean o2)
            {
                long cmp;
                if (o2.getCreatedAtTimestamp() < 0)
                {
                    cmp = -(o2.getCreatedAtTimestamp() - o1.getCreatedAtTimestamp());
                } else
                {
                    cmp = o1.getCreatedAtTimestamp() - o2.getCreatedAtTimestamp();
                }

                return cmp == 0 ? 0 : cmp < 0 ? -1 : 1;
            }
        });

        return list;
    }

    @Override
    public List<LanguageBean> getRegisteredLanguages()
    {
        List<LanguageBean> list = Lists.newArrayList();
        for (ScriptEngineFactory factory : scriptService.getRegisteredScriptEngines())
        {
            list.add(LanguageBean.valueOf(factory));
        }

        Collections.sort(list, new Comparator<LanguageBean>()
        {
            @Override
            public int compare(LanguageBean o1, LanguageBean o2)
            {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
            }
        });
        return list;
    }

    @Override
    public String[] getRequestParameterValues(String parameter)
    {
        return getRequestInstance().getParameterValues(parameter);
    }

    @Override
    public SessionBean getSessionBean(final String sessionId)
    {
        if (StringUtils.isBlank(sessionId))
        {
            return null;
        }

        Iterable<SessionBean> iterable = Iterables.filter(getAllSessionBeans(), new Predicate<SessionBean>()
        {
            @Override
            public boolean apply(@Nullable SessionBean input)
            {
                return StringUtils.equals(sessionId, input.getSessionId());
            }
        });

        return iterable.iterator().hasNext() ? iterable.iterator().next() : null;
    }

    @Override
    public String getRequestParameter(String parameter)
    {
        return getRequestInstance().getParameter(parameter);
    }

// -------------------------- OTHER METHODS --------------------------

    private ServletRequest getRequestInstance()
    {
        ServletRequest request = requestHolder.getRequest();
        Preconditions.checkArgument(request != null, "Request should be set previously");
        return request;
    }

    private UserProfile getUserProfile(String userId)
    {
        return userManager.getUserProfile(userId);
    }
}

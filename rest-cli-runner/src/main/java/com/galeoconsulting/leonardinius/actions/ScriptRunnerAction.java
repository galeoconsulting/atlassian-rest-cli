/*
 * Copyright 2011 Leonid M.<leonids.maslovs@galeoconsulting.com>
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

package com.galeoconsulting.leonardinius.actions;
//
//import com.atlassian.jira.ComponentManager;
//import com.atlassian.jira.web.action.JiraWebActionSupport;
//import com.atlassian.sal.api.user.UserManager;
//import com.atlassian.sal.api.user.UserProfile;
//import com.atlassian.sal.api.websudo.WebSudoRequired;
//import com.galeoconsulting.leonardinius.api.LanguageUtils;
//import com.galeoconsulting.leonardinius.api.ScriptService;
//import com.galeoconsulting.leonardinius.api.ScriptSessionManager;
//import com.google.common.base.Preconditions;
//import com.google.common.base.Predicate;
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
//import org.apache.commons.lang.StringUtils;
//
//import javax.annotation.Nullable;
//import javax.script.ScriptEngineFactory;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//import static com.galeoconsulting.leonardinius.api.ScriptSessionManager.ScriptSession;
//import static com.galeoconsulting.leonardinius.api.ScriptSessionManager.SessionId;
//
///**
// * User: leonidmaslov
// * Date: 3/13/11
// * Time: 11:34 AM
// */
//@WebSudoRequired
//public class ScriptRunnerAction extends JiraWebActionSupport
//{
//// ------------------------------ FIELDS ------------------------------
//
//    private final ScriptService scriptService;
//    private final ScriptSessionManager sessionManager;
//
//    private String sessionId;
//
//// --------------------------- CONSTRUCTORS ---------------------------
//
//    public ScriptRunnerAction(ScriptService scriptService, ScriptSessionManager sessionManager)
//    {
//        this.scriptService = scriptService;
//        this.sessionManager = sessionManager;
//    }
//
//// --------------------- GETTER / SETTER METHODS ---------------------
//
//    public String getSessionId()
//    {
//        return sessionId;
//    }
//
//    public void setSessionId(String sessionId)
//    {
//        this.sessionId = sessionId;
//    }
//
//// -------------------------- OTHER METHODS --------------------------
//
//    public String doCli()
//    {
//        return "cli";
//    }
//
//    public String doExec()
//    {
//        return "exec";
//    }
//
//    public String doList()
//    {
//        return "list";
//    }
//
//    public SessionBean getSessionBean()
//    {
//        if (StringUtils.isBlank(getSessionId()))
//        {
//            return null;
//        }
//
//        Iterable<SessionBean> iterable = Iterables.filter(getAllSessionBeans(), new Predicate<SessionBean>()
//        {
//            @Override
//            public boolean apply(@Nullable SessionBean input)
//            {
//                return StringUtils.equals(getSessionId(), input.getSessionId());
//            }
//        });
//
//        return iterable.iterator().hasNext() ? iterable.iterator().next() : null;
//    }
//
//    public List<SessionBean> getAllSessionBeans()
//    {
//        List<SessionBean> list = Lists.newArrayList();
//
//        for (Map.Entry<SessionId, ScriptSession> entry : sessionManager.listAllSessions().entrySet())
//        {
//            list.add(SessionBean.newInstance(entry.getKey(), entry.getValue()));
//        }
//
//        Collections.sort(list, new Comparator<SessionBean>()
//        {
//            @Override
//            public int compare(SessionBean o1, SessionBean o2)
//            {
//                long cmp;
//                if (o2.getCreatedAtTimestamp() < 0)
//                {
//                    cmp = -(o2.getCreatedAtTimestamp() - o1.getCreatedAtTimestamp());
//                } else
//                {
//                    cmp = o1.getCreatedAtTimestamp() - o2.getCreatedAtTimestamp();
//                }
//
//                return cmp == 0 ? 0 : cmp < 0 ? -1 : 1;
//            }
//        });
//
//        return list;
//    }
//
//    public List<LanguageBean> getRegisteredLanguages()
//    {
//        List<LanguageBean> list = Lists.newArrayList();
//        for (ScriptEngineFactory factory : scriptService.getRegisteredScriptEngines())
//        {
//            list.add(LanguageBean.valueOf(factory));
//        }
//
//        Collections.sort(list, new Comparator<LanguageBean>()
//        {
//            @Override
//            public int compare(LanguageBean o1, LanguageBean o2)
//            {
//                return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
//            }
//        });
//        return list;
//    }
//

//}

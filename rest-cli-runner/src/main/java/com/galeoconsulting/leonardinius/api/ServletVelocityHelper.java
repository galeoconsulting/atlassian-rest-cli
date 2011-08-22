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

package com.galeoconsulting.leonardinius.api;

import com.atlassian.sal.api.user.UserProfile;

import javax.script.ScriptEngineFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: leonardinius
 * Date: 8/22/11
 * Time: 10:15 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ServletVelocityHelper
{
// -------------------------- OTHER METHODS --------------------------

    List<SessionBean> getAllSessionBeans();

    List<LanguageBean> getRegisteredLanguages();

    SessionBean getSessionBean(String sessionId);

// -------------------------- INNER CLASSES --------------------------

    public final class LanguageBean
    {
        private final String name;

        private final String version;

        private LanguageBean(String name, String version)
        {
            this.name = name;
            this.version = version;
        }

        public String getName()
        {
            return name;
        }

        public String getVersion()
        {
            return version;
        }

        public static LanguageBean valueOf(ScriptEngineFactory factory)
        {
            ScriptEngineFactory arg = checkNotNull(factory, "factory");
            return new ServletVelocityHelper.LanguageBean(LanguageUtils.getLanguageName(arg), LanguageUtils.getVersionString(arg));
        }
    }

    public final class SessionBean
    {
        private static final DateFormat SIMPLE_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,
                SimpleDateFormat.LONG);

        private final String language;
        private final String version;
        private final String sessionId;
        private final UserProfile creator;
        private final long createdAt;

        private SessionBean(String sessionId, String language, String version, UserProfile creator, long createdAt)
        {
            this.language = language;
            this.version = version;
            this.sessionId = sessionId;
            this.creator = creator;
            this.createdAt = createdAt;
        }

        public static SessionBean newInstance(ScriptSessionManager.SessionId sessionId, ScriptSessionManager.ScriptSession session, UserProfile userProfile)
        {
            checkNotNull(sessionId, "sessionId");
            checkNotNull(session, "session");

            return new ServletVelocityHelper.SessionBean(sessionId.getSessionId(),
                    LanguageUtils.getLanguageName(session.getScriptEngine().getFactory()),
                    LanguageUtils.getVersionString(session.getScriptEngine().getFactory()),
                    userProfile,
                    session.getCreatedAt());
        }

        public String getLanguage()
        {
            return language;
        }

        public String getVersion()
        {
            return version;
        }

        public String getSessionId()
        {
            return sessionId;
        }

        public UserProfile getCreator()
        {
            return creator;
        }

        public long getCreatedAtTimestamp()
        {
            return createdAt;
        }

        public String getCreatedAt()
        {
            return SIMPLE_DATE_FORMAT.format(new Date(createdAt));
        }
    }
}

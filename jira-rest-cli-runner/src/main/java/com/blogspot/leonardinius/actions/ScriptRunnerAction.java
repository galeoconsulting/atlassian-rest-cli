package com.blogspot.leonardinius.actions;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.blogspot.leonardinius.api.LanguageUtils;
import com.blogspot.leonardinius.api.ScriptService;
import com.blogspot.leonardinius.api.ScriptSessionManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.script.ScriptEngineFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.blogspot.leonardinius.api.ScriptSessionManager.ScriptSession;
import static com.blogspot.leonardinius.api.ScriptSessionManager.SessionId;

/**
 * User: leonidmaslov
 * Date: 3/13/11
 * Time: 11:34 AM
 */
@WebSudoRequired
public class ScriptRunnerAction extends JiraWebActionSupport
{
// ------------------------------ FIELDS ------------------------------

    private final ScriptService scriptService;
    private final ScriptSessionManager sessionManager;

    private String sessionId;

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptRunnerAction(ScriptService scriptService, ScriptSessionManager sessionManager)
    {
        this.scriptService = scriptService;
        this.sessionManager = sessionManager;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(String sessionId)
    {
        this.sessionId = sessionId;
    }

// -------------------------- OTHER METHODS --------------------------

    public String doCli()
    {
        return "cli";
    }

    public String doExec()
    {
        return "exec";
    }

    public String doList()
    {
        return "list";
    }

    public SessionBean getLiveCliSession()
    {
        if (StringUtils.isBlank(getSessionId()))
        {
            return null;
        }

        Iterable<SessionBean> iterable = Iterables.filter(getLiveSessions(), new Predicate<SessionBean>()
        {
            @Override
            public boolean apply(@Nullable SessionBean input)
            {
                return StringUtils.equals(getSessionId(), input.getSessionId());
            }
        });

        return iterable.iterator().hasNext() ? iterable.iterator().next() : null;
    }

    public List<SessionBean> getLiveSessions()
    {
        List<SessionBean> list = Lists.newArrayList();

        for (Map.Entry<SessionId, ScriptSession> entry : sessionManager.listAllSessions().entrySet())
        {
            list.add(SessionBean.newInstance(entry.getKey(), entry.getValue()));
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

// -------------------------- INNER CLASSES --------------------------

    public static final class LanguageBean
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
            ScriptEngineFactory arg = Preconditions.checkNotNull(factory, "factory");
            return new LanguageBean(LanguageUtils.getLanguageName(arg), LanguageUtils.getVersionString(arg));
        }
    }

    public static final class SessionBean
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

        public static SessionBean newInstance(SessionId sessionId, ScriptSession session)
        {
            Preconditions.checkNotNull(sessionId, "sessionId");
            Preconditions.checkNotNull(session, "session");

            return new SessionBean(sessionId.getSessionId(),
                    LanguageUtils.getLanguageName(session.getScriptEngine().getFactory()),
                    LanguageUtils.getVersionString(session.getScriptEngine().getFactory()),
                    getUserProfile(session.getCreator()),
                    session.getCreatedAt());
        }

        private static UserProfile getUserProfile(String username)
        {
            return ComponentManager.getOSGiComponentInstanceOfType(UserManager.class).getUserProfile(username);
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

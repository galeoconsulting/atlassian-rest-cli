package com.blogspot.leonardinius.actions;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.blogspot.leonardinius.api.LanguageUtils;
import com.blogspot.leonardinius.api.ScriptService;
import com.blogspot.leonardinius.api.ScriptSessionManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.script.ScriptEngineFactory;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptRunnerAction(ScriptService scriptService, ScriptSessionManager sessionManager)
    {
        this.scriptService = scriptService;
        this.sessionManager = sessionManager;
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

    public Map<String, LanguageBean> getLiveSessions()
    {
        Map<String, LanguageBean> map = Maps.newHashMap();
        for (Map.Entry<SessionId, ScriptSession> entry : sessionManager.listAllSessions().entrySet())
        {
            map.put(entry.getKey().getSessionId(), LanguageBean.valueOf(entry.getValue().getScriptEngine().getFactory()));
        }
        return map;
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

    public static class LanguageBean
    {
        private final String name;

        private final String version;

        public LanguageBean(String name, String version)
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
}

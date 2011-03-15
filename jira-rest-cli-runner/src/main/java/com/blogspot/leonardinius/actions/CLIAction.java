package com.blogspot.leonardinius.actions;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.blogspot.leonardinius.api.ScriptService;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import javax.script.ScriptEngineFactory;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: leonidmaslov
 * Date: 3/13/11
 * Time: 11:34 AM
 */
public class CLIAction extends JiraWebActionSupport
{
// ------------------------------ FIELDS ------------------------------

    private final ScriptService scriptService;

// --------------------------- CONSTRUCTORS ---------------------------

    public CLIAction(ScriptService scriptService)
    {
        this.scriptService = scriptService;
    }

// -------------------------- OTHER METHODS --------------------------

    public List<LanguageBean> getRegisteredLanguages()
    {
        List<LanguageBean> list = Lists.newArrayList();
        for (ScriptEngineFactory factory : scriptService.getRegisteredScriptEngines())
        {
            list.add(makeBean(factory));
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

    private LanguageBean makeBean(ScriptEngineFactory factory)
    {
        String version = factory.getLanguageVersion();
        if (StringUtils.containsIgnoreCase(version, "jruby 1.5.6"))
        {
            version = "jruby 1.5.6";
        }
        String languageName = factory.getLanguageName();
        if ("ECMAScript".equals(languageName)
                && factory.getNames().contains("JavaScript"))
        {
            languageName = "JavaScript";
        }
        return new LanguageBean(StringUtils.capitalize(languageName), version);
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
    }
}

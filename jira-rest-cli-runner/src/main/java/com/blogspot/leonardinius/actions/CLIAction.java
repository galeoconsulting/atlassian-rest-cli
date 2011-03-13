package com.blogspot.leonardinius.actions;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.blogspot.leonardinius.api.ScriptService;
import com.google.common.collect.Lists;

import javax.script.ScriptEngineFactory;
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

    public List<String> getRegisteredLanguages()
    {
        List<String> list = Lists.newArrayList();
        for (ScriptEngineFactory factory : scriptService.getRegisteredScriptEngines())
        {
            list.add(factory.getLanguageName() + " - " + factory.getLanguageVersion());
        }
        return list;
    }
}

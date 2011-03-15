package com.blogspot.leonardinius.api;

import org.apache.commons.lang.StringUtils;

import javax.script.ScriptEngineFactory;

/**
 * User: leonidmaslov
 * Date: 3/15/11
 * Time: 10:02 PM
 */
public class LanguageUtils
{
    private LanguageUtils()
    {
    }

    public static String getVersionString(ScriptEngineFactory factory)
    {
        String version = factory.getLanguageVersion();
        if (StringUtils.containsIgnoreCase(version, "jruby 1.5.6"))
        {
            version = "jruby 1.5.6";
        }
        return version;
    }

    public static String getLanguageName(ScriptEngineFactory factory)
    {
        String languageName = factory.getLanguageName();
        if ("ECMAScript".equals(languageName)
                && factory.getNames().contains("JavaScript"))
        {
            languageName = "JavaScript";
        }
        return StringUtils.capitalize(languageName);
    }
}

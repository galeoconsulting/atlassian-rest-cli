package com.blogspot.leonardinius.api;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * User: leonidmaslov
 * Date: 3/12/11
 * Time: 11:01 PM
 */
public interface ScriptService
{
// -------------------------- OTHER METHODS --------------------------

    ScriptEngine getEngineByExtension(String extension);

    ScriptEngine getEngineByLanguage(String language);

    ScriptEngine getEngineByMime(String mime);

    public void registerEngineExtension(String extension, ScriptEngineFactory factory);

    public void registerEngineLanguage(String language, ScriptEngineFactory factory);

    public void registerEngineMime(String mime, ScriptEngineFactory factory);
}

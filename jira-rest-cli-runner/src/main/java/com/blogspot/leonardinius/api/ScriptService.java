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

    void defaultRegistration(ScriptEngineFactory factory);

    ScriptEngine getEngineByExtension(String extension);

    ScriptEngine getEngineByLanguage(String language);

    ScriptEngine getEngineByMime(String mime);

    Iterable<ScriptEngineFactory> getRegisteredScriptEngines();

    void registerEngineExtension(String extension, ScriptEngineFactory factory);

    void registerEngineLanguage(String language, ScriptEngineFactory factory);

    void registerEngineMime(String mime, ScriptEngineFactory factory);

    boolean removeEngine(ScriptEngineFactory factory);
}

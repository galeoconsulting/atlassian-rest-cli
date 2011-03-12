package com.blogspot.leonardinius.api;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: leonidmaslov
 * Date: 3/12/11
 * Time: 11:19 PM
 */
public class ScriptServiceImpl implements ScriptService
{
// ------------------------------ FIELDS ------------------------------

    private final ScriptEngineManager scriptEngineManager;

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptServiceImpl()
    {
        this.scriptEngineManager = new ScriptEngineManager();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ScriptService ---------------------


    @Override
    public ScriptEngine getEngineByExtension(String extension)
    {
        Preconditions.checkArgument(StringUtils.isNotBlank(extension), "Script extension should be specified!");
        return scriptEngineManager.getEngineByExtension(extension);
    }

    @Override
    public ScriptEngine getEngineByLanguage(String language)
    {
        Preconditions.checkArgument(StringUtils.isNotBlank(language), "Scripting language short name should be specified!");
        return scriptEngineManager.getEngineByName(language);
    }

    @Override
    public ScriptEngine getEngineByMime(String mime)
    {
        Preconditions.checkArgument(StringUtils.isNotBlank(mime), "Script mime should be specified!");
        return scriptEngineManager.getEngineByMimeType(mime);
    }

    @Override
    public void registerEngineExtension(String extension, ScriptEngineFactory factory)
    {
        scriptEngineManager.registerEngineExtension(checkNotNull(extension), checkNotNull(factory));
    }

    @Override
    public void registerEngineLanguage(String language, ScriptEngineFactory factory)
    {
        scriptEngineManager.registerEngineName(checkNotNull(language), checkNotNull(factory));
    }

    @Override
    public void registerEngineMime(String extension, ScriptEngineFactory factory)
    {
        scriptEngineManager.registerEngineMimeType(checkNotNull(extension), checkNotNull(factory));
    }
}

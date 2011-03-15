package com.blogspot.leonardinius.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.Map;
import java.util.WeakHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: leonidmaslov
 * Date: 3/12/11
 * Time: 11:19 PM
 */
public class ScriptServiceImpl implements ScriptService, DisposableBean
{
// ------------------------------ FIELDS ------------------------------

    private static final Object DUMMY = new Object();

    private ScriptEngineManager scriptEngineManager;

    private final Map<ScriptEngineFactory, Object> registeredEngines = new WeakHashMap<ScriptEngineFactory, Object>();

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptServiceImpl()
    {
        init();
    }

    private synchronized void init()
    {
        this.scriptEngineManager = new ScriptEngineManager();
        registeredEngines.clear();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception
    {
        init();
    }

// --------------------- Interface ScriptService ---------------------

    @Override
    public void defaultRegistration(ScriptEngineFactory engineFactory)
    {
        for (String extension : engineFactory.getExtensions())
        {
            registerEngineExtension(extension, engineFactory);
        }

        for (String mime : engineFactory.getMimeTypes())
        {
            registerEngineMime(mime, engineFactory);
        }

        registerEngineLanguage(engineFactory.getLanguageName(), engineFactory);
        registerEngineLanguage((String) engineFactory.getParameter(ScriptEngine.NAME), engineFactory);

        // do not rely on the other methods
        registeredEngines.put(engineFactory, DUMMY);
    }

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
    public Iterable<ScriptEngineFactory> getRegisteredScriptEngines()
    {
        return ImmutableList.copyOf(registeredEngines.keySet());
    }

    @Override
    public void registerEngineExtension(String extension, ScriptEngineFactory factory)
    {
        scriptEngineManager.registerEngineExtension(checkNotNull(extension), checkNotNull(factory));
        registeredEngines.put(factory, DUMMY);
    }

    @Override
    public void registerEngineLanguage(String language, ScriptEngineFactory factory)
    {
        scriptEngineManager.registerEngineName(checkNotNull(language), checkNotNull(factory));
        registeredEngines.put(factory, DUMMY);
    }

    @Override
    public void registerEngineMime(String extension, ScriptEngineFactory factory)
    {
        scriptEngineManager.registerEngineMimeType(checkNotNull(extension), checkNotNull(factory));
        registeredEngines.put(factory, DUMMY);
    }

    @Override
    public void removeEngine(ScriptEngineFactory factory)
    {
        registeredEngines.remove(factory);
    }
}

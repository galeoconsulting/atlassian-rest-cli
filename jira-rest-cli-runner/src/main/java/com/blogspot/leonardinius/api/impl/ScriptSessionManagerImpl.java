package com.blogspot.leonardinius.api.impl;

import com.blogspot.leonardinius.api.ScriptSessionManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.script.ScriptEngine;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ScriptSessionManagerImpl implements ScriptSessionManager, InitializingBean, DisposableBean
{
// ------------------------------ FIELDS ------------------------------

    private final ConcurrentMap<String, ScriptEngine> cliSessions = new ConcurrentHashMap<String, ScriptEngine>();

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception
    {
        init();
    }

// --------------------- Interface InitializingBean ---------------------

    @Override
    public void afterPropertiesSet() throws Exception
    {
        init();
    }

// --------------------- Interface ScriptSessionManager ---------------------

    @Override
    public void clear()
    {
        init();
    }

    @Override
    public ScriptEngine getSession(String sessionId)
    {
        return cliSessions.get(validSessionId(sessionId));
    }

    @Override
    public String putSession(ScriptEngine engine)
    {
        String sessionId = Preconditions.checkNotNull(nextSessionId(), "nextSessionId");
        if (cliSessions.putIfAbsent(sessionId, engine) == null)
        {
            throw new AssertionError("Internal implementation bug: UUID considered to be unique enough.");
        }
        return sessionId;
    }

    @Override
    public ScriptEngine removeSession(String sessionId)
    {
        return cliSessions.remove(validSessionId(sessionId));
    }

    @Override
    public Map<String, ScriptEngine> listAllSessions()
    {
        return ImmutableMap.copyOf(cliSessions);
    }

// -------------------------- OTHER METHODS --------------------------

    private void init()
    {
        cliSessions.clear();
    }

    private String nextSessionId()
    {
        return UUID.randomUUID().toString();
    }

    private String validSessionId(final String sessionId)
    {
        return UUID.fromString(sessionId).toString();
    }
}
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

    private final ConcurrentMap<SessionId, ScriptSession> cliSessions = new ConcurrentHashMap<SessionId, ScriptSession>();

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
    public ScriptSession getSession(SessionId sessionId)
    {
        return cliSessions.get(validSessionId(sessionId));
    }

    @Override
    public Map<SessionId, ScriptSession> listAllSessions()
    {
        return ImmutableMap.copyOf(cliSessions);
    }

// -------------------------- OTHER METHODS --------------------------

    private void init()
    {
        cliSessions.clear();
    }

    @Override
    public SessionId putSession(ScriptSession session)
    {
        SessionId sessionId = Preconditions.checkNotNull(nextSessionId(), "nextSessionId");
        if (cliSessions.putIfAbsent(sessionId, session) != null)
        {
            throw new AssertionError("Internal implementation bug: UUID considered to be unique enough.");
        }
        return sessionId;
    }

    private SessionId nextSessionId()
    {
        return SessionId.valueOf(UUID.randomUUID().toString());
    }

    @Override
    public ScriptSession removeSession(SessionId sessionId)
    {
        return cliSessions.remove(validSessionId(sessionId));
    }

    private SessionId validSessionId(final SessionId sessionId)
    {
        return SessionId.valueOf(UUID.fromString(sessionId.getSessionId()).toString());
    }
}
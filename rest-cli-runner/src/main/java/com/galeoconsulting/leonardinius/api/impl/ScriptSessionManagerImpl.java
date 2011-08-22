/*
 * Copyright 2011 Leonid Maslov<leonidms@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.galeoconsulting.leonardinius.api.impl;

import com.galeoconsulting.leonardinius.api.ScriptSessionManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ScriptSessionManagerImpl implements ScriptSessionManager, InitializingBean, DisposableBean {
// ------------------------------ FIELDS ------------------------------

    private final ConcurrentMap<SessionId, ScriptSession> cliSessions = new ConcurrentHashMap<SessionId, ScriptSession>();

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception {
        init();
    }

// --------------------- Interface InitializingBean ---------------------

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

// --------------------- Interface ScriptSessionManager ---------------------

    @Override
    public void clear() {
        init();
    }

    @Override
    public ScriptSession getSession(SessionId sessionId) {
        return cliSessions.get(validSessionId(sessionId));
    }

    @Override
    public Map<SessionId, ScriptSession> listAllSessions() {
        return ImmutableMap.copyOf(cliSessions);
    }

// -------------------------- OTHER METHODS --------------------------

    private void init() {
        cliSessions.clear();
    }

    @Override
    public SessionId putSession(ScriptSession session) {
        SessionId sessionId = Preconditions.checkNotNull(nextSessionId(), "nextSessionId");
        if (cliSessions.putIfAbsent(sessionId, session) != null) {
            throw new AssertionError("Internal implementation bug: UUID considered to be unique enough.");
        }
        return sessionId;
    }

    private SessionId nextSessionId() {
        return SessionId.valueOf(UUID.randomUUID().toString());
    }

    @Override
    public ScriptSession removeSession(SessionId sessionId) {
        return cliSessions.remove(validSessionId(sessionId));
    }

    private SessionId validSessionId(final SessionId sessionId) {
        return SessionId.valueOf(UUID.fromString(sessionId.getSessionId()).toString());
    }
}
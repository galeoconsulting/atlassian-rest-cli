package com.blogspot.leonardinius.api;

import com.google.common.base.Preconditions;

import javax.script.ScriptEngine;
import java.util.Map;

/**
 * User: leonidmaslov
 * Date: 3/15/11
 * Time: 10:11 PM
 */
public interface ScriptSessionManager
{
// -------------------------- OTHER METHODS --------------------------

    void clear();

    ScriptSession getSession(SessionId sessionId);

    Map<SessionId, ScriptSession> listAllSessions();

    SessionId putSession(ScriptSession session);

    ScriptSession removeSession(SessionId sessionId);

// -------------------------- INNER CLASSES --------------------------

    public static class SessionId
    {
        private final String sessionId;

        public String getSessionId()
        {
            return sessionId;
        }

        private SessionId(String sessionId)
        {
            this.sessionId = Preconditions.checkNotNull(sessionId, "SessionId");
        }

        public static SessionId valueOf(String sessionId)
        {
            return new SessionId(sessionId);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SessionId sessionId1 = (SessionId) o;

            if (!sessionId.equals(sessionId1.sessionId)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return sessionId.hashCode();
        }
    }

    public static class ScriptSession
    {
        private final ScriptEngine scriptEngine;

        public ScriptEngine getScriptEngine()
        {
            return scriptEngine;
        }

        private ScriptSession(ScriptEngine scriptEngine)
        {
            this.scriptEngine = Preconditions.checkNotNull(scriptEngine, "ScriptEngine");
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScriptSession that = (ScriptSession) o;

            if (!scriptEngine.equals(that.scriptEngine)) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            return scriptEngine.hashCode();
        }

        public static ScriptSession valueOf(ScriptEngine engine)
        {
            return new ScriptSession(engine);
        }
    }
}

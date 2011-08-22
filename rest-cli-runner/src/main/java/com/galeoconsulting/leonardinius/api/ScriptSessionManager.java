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

package com.galeoconsulting.leonardinius.api;

import com.google.common.base.Preconditions;

import javax.script.ScriptEngine;
import java.util.Date;
import java.util.Map;

/**
 * User: leonidmaslov
 * Date: 3/15/11
 * Time: 10:11 PM
 */
public interface ScriptSessionManager {
// -------------------------- OTHER METHODS --------------------------

    void clear();

    ScriptSession getSession(SessionId sessionId);

    Map<SessionId, ScriptSession> listAllSessions();

    SessionId putSession(ScriptSession session);

    ScriptSession removeSession(SessionId sessionId);

// -------------------------- INNER CLASSES --------------------------

    public static final class SessionId {
        private final String sessionId;

        public String getSessionId() {
            return sessionId;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        private SessionId(String sessionId) {
            this.sessionId = Preconditions.checkNotNull(sessionId, "SessionId");
        }

        public static SessionId valueOf(String sessionId) {
            return new SessionId(sessionId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SessionId sessionId1 = (SessionId) o;

            return sessionId.equals(sessionId1.sessionId);
        }

        @Override
        public int hashCode() {
            return sessionId.hashCode();
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static final class ScriptSession {
        private final ScriptEngine scriptEngine;
        private final long createdAt;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ScriptSession)) return false;

            ScriptSession that = (ScriptSession) o;

            if (createdAt != that.createdAt) return false;
            if (creator != null ? !creator.equals(that.creator) : that.creator != null) return false;
            if (scriptEngine != null ? !scriptEngine.equals(that.scriptEngine) : that.scriptEngine != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = scriptEngine != null ? scriptEngine.hashCode() : 0;
            result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
            result = 31 * result + (creator != null ? creator.hashCode() : 0);
            return result;
        }

        public String getCreator() {
            return creator;
        }

        private final String creator;

        public ScriptEngine getScriptEngine() {
            return scriptEngine;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        private ScriptSession(ScriptEngine scriptEngine, long createdAt, String userId) {
            this.createdAt = createdAt;
            this.creator = userId;
            this.scriptEngine = Preconditions.checkNotNull(scriptEngine, "ScriptEngine");
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public static ScriptSession newInstance(String creator, ScriptEngine engine) {
            return new ScriptSession(engine, new Date().getTime(), creator);
        }
    }
}

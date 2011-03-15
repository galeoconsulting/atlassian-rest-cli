package com.blogspot.leonardinius.api;

import javax.script.ScriptEngine;
import java.util.Map;

/**
 * User: leonidmaslov
 * Date: 3/15/11
 * Time: 10:11 PM
 */
public interface ScriptSessionManager
{
    void clear();

    ScriptEngine getSession(String sessionId);

    String putSession(ScriptEngine engine);

    ScriptEngine removeSession(String sessionId);

    Map<String, ScriptEngine> listAllSessions();
}

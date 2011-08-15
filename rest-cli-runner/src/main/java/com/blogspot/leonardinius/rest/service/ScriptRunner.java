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

package com.blogspot.leonardinius.rest.service;


import com.atlassian.sal.api.user.UserManager;
import com.blogspot.leonardinius.api.LanguageUtils;
import com.blogspot.leonardinius.api.ScriptService;
import com.blogspot.leonardinius.api.ScriptSessionManager;
import com.blogspot.leonardinius.rest.CacheControl;
import com.blogspot.leonardinius.rest.ErrorCollection;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.input.NullReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.plugin.util.Assertions.notNull;
import static com.blogspot.leonardinius.api.ScriptSessionManager.ScriptSession;
import static com.blogspot.leonardinius.api.ScriptSessionManager.SessionId;


@Path("/")
public class ScriptRunner implements DisposableBean {
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = LoggerFactory.getLogger(ScriptRunner.class);

    private static final String LANGUAGE = "language";
    private static final String SESSION_ID = "sessionId";

    private static final String PERMISSION_DENIED_USER_DO_NOT_HAVE_SYSTEM_ADMINISTRATOR_RIGHTS = "Permission denied: user do not have system administrator rights!";

    private final ScriptService scriptService;
    private final ScriptSessionManager sessionManager;

    private final UserManager userManager;

// --------------------------- CONSTRUCTORS ---------------------------

    @SuppressWarnings({"UnusedDeclaration"})
    public ScriptRunner(final ScriptService scriptService, final UserManager userManager, ScriptSessionManager sessionManager) {
        this.scriptService = scriptService;
        this.userManager = userManager;
        this.sessionManager = sessionManager;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception {
        sessionManager.clear();
    }

// -------------------------- OTHER METHODS --------------------------

    @POST
    @Path("/sessions/{" + SESSION_ID + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response cli(@PathParam(SESSION_ID) final String sessionId, ScriptText script) {
        if (!isAdministrator()) {
            return responseForbidden();
        }

        ScriptSessionManager.ScriptSession scriptSession;
        try {
            scriptSession = notNull("Session instance", sessionManager.getSession(SessionId.valueOf(sessionId)));
        } catch (IllegalArgumentException e) {
            return responseInternalError(Arrays.asList((e.getMessage())));
        }

        ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();
        try {
            return responseEvalOk(eval(scriptSession.getScriptEngine(), script.getScript(), consoleOutputBean));
        } catch (ScriptException e) {
            //LOG.error("Script exception", e);
            return responseScriptError(e,
                    consoleOutputBean.getOutAsString(),
                    consoleOutputBean.getErrAsString());
        }
    }

    private boolean isAdministrator() {
        return userManager.getRemoteUsername() != null
                && userManager.isSystemAdmin(userManager.getRemoteUsername());
    }

    private Response responseForbidden() {
        return Response.serverError()
                .entity(createErrorCollection(ImmutableList.of(PERMISSION_DENIED_USER_DO_NOT_HAVE_SYSTEM_ADMINISTRATOR_RIGHTS)))
                .cacheControl(CacheControl.NO_CACHE)

                .build();
    }

    private Response responseInternalError(List<String> errorMessages) {
        return responseError(createErrorCollection(errorMessages));
    }

    private <Entity> Response responseError(Entity entity) {
        return Response.serverError()
                .entity(entity)
                .cacheControl(CacheControl.NO_CACHE)

                .build();
    }

    private ErrorCollection createErrorCollection(final Iterable<String> errorMessages) {
        ErrorCollection.Builder builder = ErrorCollection.builder();
        for (String message : errorMessages) {
            builder = builder.addErrorMessage(message);
        }
        return builder.build();
    }

    private Response responseEvalOk(final ConsoleOutputBean output) {
        return responseOk(new ConsoleOutputBeanWrapper(output));
    }

    private <Entity> Response responseOk(Entity entity) {
        return Response.ok(entity).cacheControl(CacheControl.NO_CACHE).build();
    }

    private ConsoleOutputBean eval(ScriptEngine engine, String scriptText, final ConsoleOutputBean consoleOutputBean) throws ScriptException {
        updateBindings(engine, ScriptContext.ENGINE_SCOPE, new HashMap<String, Object>() {{
            put("out", new PrintWriter(consoleOutputBean.getOut(), true));
            put("err", new PrintWriter(consoleOutputBean.getErr(), true));
        }});
        engine.getContext().setWriter(consoleOutputBean.getOut());
        engine.getContext().setErrorWriter(consoleOutputBean.getErr());
        engine.getContext().setReader(new NullReader(0));

        consoleOutputBean.setEvalResult(engine.eval(scriptText, engine.getContext()));

        return consoleOutputBean;
    }

    @SuppressWarnings({"SameParameterValue"})
    private void updateBindings(ScriptEngine engine, int scope, Map<String, ?> mergeValues) {
        Bindings bindings = engine.getContext().getBindings(scope);
        if (bindings == null) {
            bindings = engine.createBindings();
            engine.getContext().setBindings(bindings, scope);
        }
        bindings.putAll(mergeValues);
    }

    private Response responseScriptError(final Throwable th, String out, String err) {
        return responseError(
                new ScriptErrors(createErrorCollection(ImmutableList.<String>of(getStackTrace(th))), out, err));
    }

    private String getStackTrace(Throwable th) {
        if (th == null) {
            return "";
        }

        List<StackTraceElement> elements = Lists.newArrayList();
        for (StackTraceElement st : th.getStackTrace()) {
            if (st.getClassName().equals(getClass().getName()))
                break;
            elements.add(st);
        }

        return new StringBuilder(ExceptionUtils.getMessage(th))
                .append(" at ")
                .append(Joiner.on("\n ").skipNulls().join(elements)).toString();
    }

    @DELETE
    @Path("/sessions/{" + SESSION_ID + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response deleteSession(@PathParam(SESSION_ID) String sessionId) {
        if (!isAdministrator()) {
            return responseForbidden();
        }

        if (sessionManager.removeSession(SessionId.valueOf(sessionId)) == null) {
            return Response.noContent().cacheControl(CacheControl.NO_CACHE).build();
        }

        return Response.ok().cacheControl(CacheControl.NO_CACHE).build();
    }

    @POST
    @Path("/execute/{" + LANGUAGE + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response execute(@PathParam(LANGUAGE) final String scriptLanguage, Script script) {
        if (!isAdministrator()) {
            return responseForbidden();
        }

        ScriptEngine engine;
        try {
            engine = createScriptEngine(scriptLanguage, script);
        } catch (IllegalArgumentException e) {
            return responseInternalError(Arrays.asList((e.getMessage())));
        }

        ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();
        try {
            return responseEvalOk(eval(engine, script.getScript(), consoleOutputBean));
        } catch (ScriptException e) {
            //LOG.error("Script exception", e);
            return responseScriptError(e,
                    consoleOutputBean.getOutAsString(),
                    consoleOutputBean.getErrAsString());
        }
    }

    private ScriptEngine createScriptEngine(String scriptLanguage, Script script) {
        ScriptEngine engine = engineByLanguage(scriptLanguage);
        if (engine == null) {
            throw new IllegalStateException(
                    String.format("Language '%s' script engine could not be found", scriptLanguage));
        }
        updateBindings(engine, ScriptContext.ENGINE_SCOPE, new HashMap<String, Object>() {{
            put("log", LOG);
            put("selfScriptRunner", ScriptRunner.this);
        }});

        engine.getContext().setAttribute(ScriptEngine.FILENAME, scriptName(script.getFilename()), ScriptContext.ENGINE_SCOPE);
        engine.getContext().setAttribute(ScriptEngine.ARGV, getArgvs(script.getArgv()), ScriptContext.ENGINE_SCOPE);

        return engine;
    }

    private ScriptEngine engineByLanguage(String language) {
        return scriptService.getEngineByLanguage(language);
    }

    private String scriptName(String filename) {
        return StringUtils.defaultIfEmpty(filename, "<unnamed script>");
    }

    private String[] getArgvs(List<String> argv) {
        return argv == null ? new String[0] : argv.toArray(new String[argv.size()]);
    }

    @GET
    @Path("/sessions")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response listSessions(@QueryParam(LANGUAGE) @DefaultValue("") String language) {
        if (!isAdministrator()) {
            return responseForbidden();
        }

        SessionIdCollectionWrapper ids = new SessionIdCollectionWrapper(Lists.<SessionIdWrapper>newArrayList());
        for (Map.Entry<SessionId, ScriptSessionManager.ScriptSession> entry : sessionManager.listAllSessions().entrySet()) {
            String languageName = LanguageUtils.getLanguageName(entry.getValue().getScriptEngine().getFactory());
            if (StringUtils.isBlank(language)
                    || StringUtils.equalsIgnoreCase(language, languageName)) {
                String sessionId = entry.getKey().getSessionId();
                String versionString = LanguageUtils.getVersionString(entry.getValue().getScriptEngine().getFactory());

                ids.addSession(new SessionIdWrapper(sessionId, languageName,
                        versionString));
            }
        }

        return responseOk(ids);
    }

    @PUT
    @Path("/sessions")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response newSession(Language language) {
        if (!isAdministrator()) {
            return responseForbidden();
        }

        ScriptEngine engine;
        try {
            engine = createScriptEngine(language.language, new Script("", "cli", ImmutableList.<String>of()));
        } catch (IllegalArgumentException e) {
            return responseInternalError(Arrays.asList((e.getMessage())));
        }

        SessionId sessionId = sessionManager.putSession(ScriptSession.newInstance(getActorName(userManager.getRemoteUsername()), engine));
        return Response.ok(new SessionIdWrapper(sessionId.getSessionId(),
                LanguageUtils.getLanguageName(engine.getFactory()),
                LanguageUtils.getVersionString(engine.getFactory())))
                .cacheControl(CacheControl.NO_CACHE).build();
    }

    private String getActorName(String username) {
        return Preconditions.checkNotNull(username);
    }

// -------------------------- INNER CLASSES --------------------------

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class Language {
        @XmlElement
        private String language;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class ScriptErrors {
        @XmlElement(name = "errors")
        private ErrorCollection errorCollection;

        @XmlElement
        private String out;

        @XmlElement
        private String err;

        public ErrorCollection getErrorCollection() {
            return errorCollection;
        }

        public void setErrorCollection(ErrorCollection errorCollection) {
            this.errorCollection = errorCollection;
        }

        public String getOut() {
            return out;
        }

        public void setOut(String out) {
            this.out = out;
        }

        public String getErr() {
            return err;
        }

        public void setErr(String err) {
            this.err = err;
        }

        public ScriptErrors() {
        }

        public ScriptErrors(ErrorCollection errorCollection, String out, String err) {
            this.errorCollection = errorCollection;
            this.out = out;
            this.err = err;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class Script {
        public Script() {
        }

        public Script(String script, String filename, List<String> argv) {
            this.script = script;
            this.filename = filename;
            this.argv = argv;
        }

        @XmlElement
        private String script;

        @XmlElement
        private String filename;

        @XmlElement
        private List<String> argv;

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public List<String> getArgv() {
            return argv;
        }

        public void setArgv(List<String> argv) {
            this.argv = argv;
        }
    }

    public static class ConsoleOutputBean {
        private StringWriter out;

        private StringWriter err;

        private Object evalResult;

        @SuppressWarnings({"UnusedDeclaration"})
        public ConsoleOutputBean(StringWriter out, StringWriter err) {
            this.out = out;
            this.err = err;
            this.evalResult = null;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public ConsoleOutputBean() {
            this(new StringWriter(), new StringWriter());
        }

        public Object getEvalResult() {
            return evalResult;
        }

        private String asString(StringWriter sw) {
            return notNull("StringWriter sw", sw)
                    .getBuffer()
                    .toString();
        }

        String getOutAsString() {
            return asString(getOut());
        }

        String getErrAsString() {
            return asString(getErr());
        }

        public void setEvalResult(Object evalResult) {
            this.evalResult = evalResult;
        }

        public StringWriter getOut() {
            return out;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setOut(StringWriter out) {
            this.out = out;
        }

        public StringWriter getErr() {
            return err;
        }

        @SuppressWarnings({"UnusedDeclaration"})
        public void setErr(StringWriter err) {
            this.err = err;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class SessionIdWrapper {
        @XmlElement
        private String sessionId;

        @XmlElement
        private String languageName;

        @XmlElement
        private String languageVersion;

        public SessionIdWrapper(String sessionId, String languageName, String languageVersion) {
            this.sessionId = sessionId;
            this.languageName = languageName;
            this.languageVersion = languageVersion;
        }

        public String getLanguageName() {
            return languageName;
        }

        public void setLanguageName(String languageName) {
            this.languageName = languageName;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class SessionIdCollectionWrapper {
        @XmlElement
        private List<SessionIdWrapper> sessions;

        public SessionIdCollectionWrapper(List<SessionIdWrapper> sessions) {
            this.sessions = sessions;
        }

        public List<SessionIdWrapper> getSessions() {
            return sessions;
        }

        public void setSessions(List<SessionIdWrapper> sessions) {
            this.sessions = sessions;
        }

        public SessionIdCollectionWrapper addSession(SessionIdWrapper id) {
            sessions.add(id);
            return this;
        }

        public SessionIdCollectionWrapper addSession(Iterable<SessionIdWrapper> ids) {
            Iterables.addAll(sessions, ids);
            return this;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class ConsoleOutputBeanWrapper {
        @XmlElement
        private String out;

        @XmlElement
        private String err;

        @XmlElement
        private String evalResult;

        public ConsoleOutputBeanWrapper(String evalResult, String out, String err) {
            this.out = out;
            this.err = err;
            this.evalResult = evalResult;
        }

        public ConsoleOutputBeanWrapper(ConsoleOutputBean bean) {
            this(String.valueOf(bean.getEvalResult()), bean.getOutAsString(), bean.getErrAsString());
        }

        public String getEvalResult() {
            return evalResult;
        }

        public void setEvalResult(String evalResult) {
            this.evalResult = evalResult;
        }

        public String getOut() {
            return out;
        }

        public void setOut(String out) {
            this.out = out;
        }

        public String getErr() {
            return err;
        }

        public void setErr(String err) {
            this.err = err;
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class ScriptText {
        @XmlElement
        private String script;

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }
    }
}


package com.blogspot.leonardinius.rest;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.dbc.Assertions;
import com.blogspot.leonardinius.api.LanguageUtils;
import com.blogspot.leonardinius.api.ScriptService;
import com.blogspot.leonardinius.api.ScriptSessionManager;
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
import java.util.*;

import static com.atlassian.jira.rest.v1.util.CacheControl.NO_CACHE;
import static com.blogspot.leonardinius.api.ScriptSessionManager.ScriptSession;
import static com.blogspot.leonardinius.api.ScriptSessionManager.SessionId;
import static com.google.common.base.Preconditions.checkNotNull;


@Path("/")
public class ScriptRunner implements DisposableBean
{
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = LoggerFactory.getLogger(ScriptRunner.class);

    private static final String LANGUAGE = "language";
    private static final String SESSION_ID = "sessionId";

    private static final String PERMISSION_DENIED_USER_DO_NOT_HAVE_SYSTEM_ADMINISTRATOR_RIGHTS = "Permission denied: user do not have system administrator rights!";

    private final ScriptService scriptService;
    private final ScriptSessionManager sessionManager;

    private final JiraAuthenticationContext context;
    private final PermissionManager permissionManager;

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptRunner(final ScriptService scriptService, final JiraAuthenticationContext context,
                        final PermissionManager permissionManager, ScriptSessionManager sessionManager)
    {
        this.scriptService = scriptService;
        this.context = context;
        this.permissionManager = permissionManager;
        this.sessionManager = sessionManager;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception
    {
        sessionManager.clear();
    }

// -------------------------- OTHER METHODS --------------------------

    @POST
    @Path("/sessions/{" + SESSION_ID + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response cli(@PathParam(SESSION_ID) final String sessionId, Script script)
    {
        if (!isAdministrator())
        {
            return responseForbidden();
        }

        ScriptSessionManager.ScriptSession scriptSession;
        try
        {
            scriptSession = Assertions.notNull("Session instance", sessionManager.getSession(SessionId.valueOf(sessionId)));
        }
        catch (IllegalArgumentException e)
        {
            return responseInternalError(Arrays.asList((e.getMessage())));
        }

        ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();
        try
        {
            return responseEvalOk(eval(scriptSession.getScriptEngine(), script, consoleOutputBean));
        }
        catch (ScriptException e)
        {
            //LOG.error("Script exception", e);
            return responseScriptError(e,
                    consoleOutputBean.getOutAsString(),
                    consoleOutputBean.getErrAsString());
        }
    }

    private boolean isAdministrator()
    {
        return context.getUser() != null && permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, context.getUser());
    }

    private Response responseForbidden()
    {
        return Response.serverError()
                .entity(createErrorCollection(ImmutableList.of(PERMISSION_DENIED_USER_DO_NOT_HAVE_SYSTEM_ADMINISTRATOR_RIGHTS)))
                .cacheControl(NO_CACHE)

                .build();
    }

    private Response responseInternalError(List<String> errorMessages)
    {
        return responseError(createErrorCollection(errorMessages));
    }

    private <Entity> Response responseError(Entity entity)
    {
        return Response.serverError()
                .entity(entity)
                .cacheControl(NO_CACHE)

                .build();
    }

    private ErrorCollection createErrorCollection(final Iterable<String> errorMessages)
    {
        ErrorCollection.Builder builder = ErrorCollection.builder();
        for (String message : errorMessages)
        {
            builder = builder.addErrorMessage(message);
        }
        return builder.build();
    }

    private Response responseEvalOk(final ConsoleOutputBean output)
    {
        return responseOk(new ConsoleOutputBeanWrapper(output));
    }

    private <Entity> Response responseOk(Entity entity)
    {
        return Response.ok(entity).cacheControl(NO_CACHE).build();
    }

    private ConsoleOutputBean eval(ScriptEngine engine, Script script, final ConsoleOutputBean consoleOutputBean) throws ScriptException
    {
        updateBindings(engine, ScriptContext.ENGINE_SCOPE, new HashMap<String, Object>()
        {{
                put("out", new PrintWriter(consoleOutputBean.getOut()));
                put("err", new PrintWriter(consoleOutputBean.getErr()));
            }});
        engine.getContext().setWriter(consoleOutputBean.getOut());
        engine.getContext().setErrorWriter(consoleOutputBean.getErr());
        engine.getContext().setReader(new NullReader(0));

        consoleOutputBean.setEvalResult(engine.eval(script.getScript(), engine.getContext()));

        return consoleOutputBean;
    }

    private void updateBindings(ScriptEngine engine, int scope, Map<String, ?> mergeValues)
    {
        Bindings bindings = engine.getContext().getBindings(scope);
        if (bindings == null)
        {
            bindings = engine.createBindings();
            engine.getContext().setBindings(bindings, scope);
        }
        bindings.putAll(mergeValues);
    }

    private Response responseScriptError(final Throwable th, String out, String err)
    {
        return responseOk(
                new ScriptErrors(createErrorCollection(ImmutableList.<String>of(ExceptionUtils.getStackTrace(th))), out, err));
    }

    @DELETE
    @Path("/sessions/{" + SESSION_ID + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response deleteSession(@PathParam(SESSION_ID) String sessionId)
    {
        if (!isAdministrator())
        {
            return responseForbidden();
        }

        if (sessionManager.removeSession(SessionId.valueOf(sessionId)) == null)
        {
            return Response.noContent().cacheControl(NO_CACHE).build();
        }

        return Response.ok().cacheControl(NO_CACHE).build();
    }

    @POST
    @Path("/execute/{" + LANGUAGE + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response execute(@PathParam(LANGUAGE) final String scriptLanguage, Script script)
    {
        if (!isAdministrator())
        {
            return responseForbidden();
        }

        ScriptEngine engine;
        try
        {
            engine = createScriptEngine(scriptLanguage, script);
        }
        catch (IllegalArgumentException e)
        {
            return responseInternalError(Arrays.asList((e.getMessage())));
        }

        ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();
        try
        {
            return responseEvalOk(eval(engine, script, consoleOutputBean));
        }
        catch (ScriptException e)
        {
            //LOG.error("Script exception", e);
            return responseScriptError(e,
                    consoleOutputBean.getOutAsString(),
                    consoleOutputBean.getErrAsString());
        }
    }

    private ScriptEngine createScriptEngine(String scriptLanguage, Script script)
    {
        ScriptEngine engine = checkNotNull(engineByLanguage(scriptLanguage), "Could not locate script engine (null)!");
        updateBindings(engine, ScriptContext.ENGINE_SCOPE, new HashMap<String, Object>()
        {{
                put("log", LOG);
                put("componentManager", ComponentManager.getInstance());
                put("selfScriptRunner", ScriptRunner.this);
            }});

        engine.getContext().setAttribute(ScriptEngine.FILENAME, scriptName(script.getFilename()), ScriptContext.ENGINE_SCOPE);
        engine.getContext().setAttribute(ScriptEngine.ARGV, getArgvs(script.getArgv()), ScriptContext.ENGINE_SCOPE);

        return engine;
    }

    private ScriptEngine engineByLanguage(String language)
    {
        return scriptService.getEngineByLanguage(language);
    }

    private String scriptName(String filename)
    {
        return StringUtils.defaultIfEmpty(filename, "<unnamed script>");
    }

    private Object getArgvs(List<String> argv)
    {
        return argv == null ? Collections.<String>emptyList() : argv.toArray(new String[argv.size()]);
    }

    @GET
    @Path("/sessions")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response listSessions(@QueryParam(LANGUAGE) @DefaultValue("") String language)
    {
        if (!isAdministrator())
        {
            return responseForbidden();
        }

        SessionIdCollectionWrapper ids = new SessionIdCollectionWrapper(Lists.<SessionIdWrapper>newArrayList());
        for (Map.Entry<SessionId, ScriptSessionManager.ScriptSession> entry : sessionManager.listAllSessions().entrySet())
        {
            String languageName = LanguageUtils.getLanguageName(entry.getValue().getScriptEngine().getFactory());
            if (StringUtils.isBlank(languageName)
                    || StringUtils.equals(language, languageName))
            {
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
    public Response newSession(Language language)
    {
        if (!isAdministrator())
        {
            return responseForbidden();
        }

        ScriptEngine engine;
        try
        {
            engine = createScriptEngine(language.language, new Script("", "cli", ImmutableList.<String>of()));
        }
        catch (IllegalArgumentException e)
        {
            return responseInternalError(Arrays.asList((e.getMessage())));
        }

        SessionId sessionId = sessionManager.putSession(ScriptSession.valueOf(engine));
        return Response.ok(new SessionIdWrapper(sessionId.getSessionId(),
                LanguageUtils.getLanguageName(engine.getFactory()),
                LanguageUtils.getVersionString(engine.getFactory())))
                .cacheControl(NO_CACHE).build();
    }

// -------------------------- INNER CLASSES --------------------------

    @XmlRootElement
    public static class Language
    {
        @XmlElement
        private String language;
    }

    @XmlRootElement
    public static class ScriptErrors
    {
        @XmlElement(name = "errors")
        private ErrorCollection errorCollection;

        @XmlElement
        private String out;

        @XmlElement
        private String err;

        public ErrorCollection getErrorCollection()
        {
            return errorCollection;
        }

        public void setErrorCollection(ErrorCollection errorCollection)
        {
            this.errorCollection = errorCollection;
        }

        public String getOut()
        {
            return out;
        }

        public void setOut(String out)
        {
            this.out = out;
        }

        public String getErr()
        {
            return err;
        }

        public void setErr(String err)
        {
            this.err = err;
        }

        public ScriptErrors()
        {
        }

        public ScriptErrors(ErrorCollection errorCollection, String out, String err)
        {
            this.errorCollection = errorCollection;
            this.out = out;
            this.err = err;
        }
    }

    @XmlRootElement
    public static class Script
    {
        public Script()
        {
        }

        public Script(String script, String filename, List<String> argv)
        {
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

        public String getScript()
        {
            return script;
        }

        public void setScript(String script)
        {
            this.script = script;
        }

        public String getFilename()
        {
            return filename;
        }

        public void setFilename(String filename)
        {
            this.filename = filename;
        }

        public List<String> getArgv()
        {
            return argv;
        }

        public void setArgv(List<String> argv)
        {
            this.argv = argv;
        }
    }

    public static class ConsoleOutputBean
    {
        private StringWriter out;

        private StringWriter err;

        private Object evalResult;

        public ConsoleOutputBean(Object evalResult, StringWriter out, StringWriter err)
        {
            this.out = out;
            this.err = err;
            this.evalResult = evalResult;
        }

        public ConsoleOutputBean()
        {
            this(null, new StringWriter(), new StringWriter());
        }

        public Object getEvalResult()
        {
            return evalResult;
        }

        private String asString(StringWriter sw)
        {
            return Assertions.notNull("StringWriter sw", sw)
                    .getBuffer()
                    .toString();
        }

        String getOutAsString()
        {
            return asString(getOut());
        }

        String getErrAsString()
        {
            return asString(getErr());
        }

        public void setEvalResult(Object evalResult)
        {
            this.evalResult = evalResult;
        }

        public StringWriter getOut()
        {
            return out;
        }

        public void setOut(StringWriter out)
        {
            this.out = out;
        }

        public StringWriter getErr()
        {
            return err;
        }

        public void setErr(StringWriter err)
        {
            this.err = err;
        }
    }

    @XmlRootElement
    public static class SessionIdWrapper
    {
        @XmlElement
        String sessionId;

        public SessionIdWrapper(String sessionId, String languageName, String languageVersion)
        {
            this.sessionId = sessionId;
            this.languageName = languageName;
            this.languageVersion = languageVersion;
        }

        public String getLanguageName()
        {
            return languageName;
        }

        public void setLanguageName(String languageName)
        {
            this.languageName = languageName;
        }

        @XmlElement
        String languageName;

        @XmlElement
        String languageVersion;

        public String getSessionId()
        {
            return sessionId;
        }

        public void setSessionId(String sessionId)
        {
            this.sessionId = sessionId;
        }
    }

    @XmlRootElement
    public static class SessionIdCollectionWrapper
    {
        public SessionIdCollectionWrapper(List<SessionIdWrapper> sessions)
        {
            this.sessions = sessions;
        }

        public List<SessionIdWrapper> getSessions()
        {
            return sessions;
        }

        public void setSessions(List<SessionIdWrapper> sessions)
        {
            this.sessions = sessions;
        }

        @XmlElement
        List<SessionIdWrapper> sessions;

        public SessionIdCollectionWrapper addSession(SessionIdWrapper id)
        {
            sessions.add(id);
            return this;
        }

        public SessionIdCollectionWrapper addSession(Iterable<SessionIdWrapper> ids)
        {
            Iterables.addAll(sessions, ids);
            return this;
        }
    }

    @XmlRootElement
    public static class ConsoleOutputBeanWrapper
    {
        @XmlElement
        private String out;

        @XmlElement
        private String err;

        @XmlElement
        private String evalResult;

        public ConsoleOutputBeanWrapper(String evalResult, String out, String err)
        {
            this.out = out;
            this.err = err;
            this.evalResult = evalResult;
        }

        public ConsoleOutputBeanWrapper(ConsoleOutputBean bean)
        {
            this(String.valueOf(bean.getEvalResult()), bean.getOutAsString(), bean.getErrAsString());
        }

        public String getEvalResult()
        {
            return evalResult;
        }

        public void setEvalResult(String evalResult)
        {
            this.evalResult = evalResult;
        }

        public String getOut()
        {
            return out;
        }

        public void setOut(String out)
        {
            this.out = out;
        }

        public String getErr()
        {
            return err;
        }

        public void setErr(String err)
        {
            this.err = err;
        }
    }
}


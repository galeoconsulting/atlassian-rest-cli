package com.blogspot.leonardinius.rest;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.blogspot.leonardinius.api.ScriptService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.input.NullReader;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.atlassian.jira.rest.v1.util.CacheControl.NO_CACHE;
import static com.google.common.base.Preconditions.checkNotNull;


@Path("/")
public class ScriptRunner
{
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = LoggerFactory.getLogger(ScriptRunner.class);

    private final ScriptService scriptService;
    private final JiraAuthenticationContext context;

    private final PermissionManager permissionManager;
    private final AtomicLong sessionCounter = new AtomicLong(0);

// --------------------------- CONSTRUCTORS ---------------------------

    public ScriptRunner(ScriptService scriptService, JiraAuthenticationContext context, PermissionManager permissionManager)
    {
        this.scriptService = scriptService;
        this.context = context;
        this.permissionManager = permissionManager;
    }

// -------------------------- OTHER METHODS --------------------------

    @POST
    @Path("/cli/{" + "language" + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response cli(@PathParam("language") final String scriptLanguage, Script script)
    {
        if (!isAdministrator())
        {
            return responseInternalError(ImmutableList.of("Permission denied: user do not have system administrator rights!"));
        }

        ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();
        try
        {
            ScriptEngine engine = createScriptEngine(scriptLanguage, script);
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

    private boolean isAdministrator()
    {
        return context.getUser() != null && permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, context.getUser());
    }

    private Response responseInternalError(List<String> errorMessages)
    {
        return makeEntityResponseRequest(createErrorCollection(errorMessages));
    }

    private <Entity> Response makeEntityResponseRequest(Entity entity)
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

    private String scriptName(String filename)
    {
        return StringUtils.defaultIfEmpty(filename, "<unnamed script>");
    }

    private Object getArgvs(List<String> argv)
    {
        return argv == null ? Collections.<String>emptyList() : argv.toArray(new String[argv.size()]);
    }

    private Response responseEvalOk(final ConsoleOutputBean output)
    {
        return Response.ok(new ConsoleOutputBeanWrapper(output)).cacheControl(NO_CACHE).build();
    }

    private ConsoleOutputBean eval(ScriptEngine engine, Script script, final ConsoleOutputBean consoleOutputBean) throws ScriptException
    {
        updateBindings(engine, ScriptContext.ENGINE_SCOPE, new HashMap<String, Object>()
        {{
                put("out", consoleOutputBean.getOut());
                put("err", consoleOutputBean.getErr());
            }});
        engine.getContext().setWriter(consoleOutputBean.getOut());
        engine.getContext().setErrorWriter(consoleOutputBean.getErr());
        engine.getContext().setReader(new NullReader(0));

        consoleOutputBean.setEvalResult(engine.eval(script.getScript(), engine.getContext()));

        return consoleOutputBean;
    }

    private Response responseScriptError(final Throwable th, String out, String err)
    {
        return makeEntityResponseRequest(
                new ScriptErrors(createErrorCollection(ImmutableList.<String>of(ExceptionUtils.getStackTrace(th))), out, err));
    }

    @POST
    @Path("/execute/{" + "language" + "}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    public Response execute(@PathParam("language") final String scriptLanguage, Script script)
    {
        if (!isAdministrator())
        {
            return responseInternalError(ImmutableList.of("Permission denied: user do not have system administrator rights!"));
        }

        ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();
        try
        {
            ScriptEngine engine = createScriptEngine(scriptLanguage, script);
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

// -------------------------- INNER CLASSES --------------------------

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
            return Preconditions.checkNotNull(sw, "Precondition failure: string writer is null")
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


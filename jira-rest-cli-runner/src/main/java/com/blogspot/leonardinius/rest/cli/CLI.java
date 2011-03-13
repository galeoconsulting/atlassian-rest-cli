package com.blogspot.leonardinius.rest.cli;

import com.atlassian.jira.rest.api.util.ErrorCollection;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.blogspot.leonardinius.api.ScriptService;
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
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.rest.v1.util.CacheControl.NO_CACHE;
import static com.google.common.base.Preconditions.checkNotNull;


@Path("/cli")
public class CLI
{
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = LoggerFactory.getLogger(CLI.class);

    private static final String SCRIPT_TYPE = "language";
    private static final String SCRIPT_CODE = "script";
    private static final String FILENAME = "filename";
    private static final String ARGV = "argv";
    private static final String UNNAMED_SCRIPT = "<unnamed script>";
    private static final String EMPTY_STRING = "";

    private final ScriptService scriptService;
    private final JiraAuthenticationContext context;
    private final PermissionManager permissionManager;

// --------------------------- CONSTRUCTORS ---------------------------

    public CLI(ScriptService scriptService, JiraAuthenticationContext context, PermissionManager permissionManager)
    {
        this.scriptService = scriptService;
        this.context = context;
        this.permissionManager = permissionManager;
    }

// -------------------------- OTHER METHODS --------------------------

    private ConsoleOutputBean eval(ScriptEngine engine, String filename, String script, List<String> argv, Map<String, ?> globalScope, Map<String, ?> localScope) throws ScriptException
    {
        final ConsoleOutputBean consoleOutputBean = new ConsoleOutputBean();

        final ScriptContext context = engine.getContext();
        context.setBindings(makeBindings(engine, new HashMap<String, Object>(localScope)
        {{
                put("out", consoleOutputBean.getOut());
                put("err", consoleOutputBean.getErr());
            }}), ScriptContext.ENGINE_SCOPE);
        context.setBindings(makeBindings(engine, globalScope), ScriptContext.GLOBAL_SCOPE);

        context.setAttribute(ScriptEngine.FILENAME, scriptName(filename), ScriptContext.ENGINE_SCOPE);
        context.setAttribute(ScriptEngine.ARGV, getArgvs(argv), ScriptContext.ENGINE_SCOPE);

        context.setWriter(consoleOutputBean.getOut());
        context.setErrorWriter(consoleOutputBean.getErr());
        context.setReader(new NullReader(0));

        consoleOutputBean.setEvalResult(engine.eval(script, context));
        return consoleOutputBean;
    }

    private Bindings makeBindings(ScriptEngine engine, Map<String, ?> scope)
    {
        Bindings bindings = engine.createBindings();
        bindings.putAll(scope);
        return bindings;
    }

    private String scriptName(String filename)
    {
        return StringUtils.defaultIfEmpty(filename, UNNAMED_SCRIPT);
    }

    private Object getArgvs(List<String> argv)
    {
        return argv == null ? Collections.<String>emptyList() : argv.toArray(new String[argv.size()]);
    }

    @POST
    @Path("/{" + SCRIPT_TYPE + "}")
    public Response execute(@PathParam(SCRIPT_TYPE) final String scriptLanguage,
                            @QueryParam(FILENAME) @DefaultValue(UNNAMED_SCRIPT) String filename,
                            @QueryParam(SCRIPT_CODE) @DefaultValue(EMPTY_STRING) String script,
                            @QueryParam(ARGV) List<String> argv)
    {
        if (!isAdministrator())
        {
            return createErrorResponse(ImmutableList.of("Permission denied: user do not have system administrator rights!"));
        }

        ScriptEngine engine = checkNotNull(engineByLanguage(scriptLanguage), "Could not locate script engine (null)!");
        try
        {
            return createResponse(eval(engine, filename, script, argv));
        }
        catch (ScriptException e)
        {
            LOG.error("Script exception", e);
            return createErrorResponse(e);
        }
    }

    private boolean isAdministrator()
    {
        return context.getUser() != null && permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, context.getUser());
    }

    private Response createErrorResponse(List<String> errorMessages)
    {
        ErrorCollection.Builder builder = ErrorCollection.builder();
        for (String message : errorMessages)
        {
            builder = builder.addErrorMessage(message);
        }
        return Response.serverError().entity(builder.build()).cacheControl(NO_CACHE).build();
    }

    private ScriptEngine engineByLanguage(String language)
    {
        return scriptService.getEngineByLanguage(language);
    }

    private Response createResponse(final ConsoleOutputBean output)
    {
        return Response.ok(new ConsoleOutputBeanWrapper(output)).cacheControl(NO_CACHE).build();
    }

    private ConsoleOutputBean eval(ScriptEngine engine, String filename, String script, List<String> argv) throws ScriptException
    {
        return eval(engine, filename, script, argv, makeGlobalScope(), makeLocalScope());
    }

    private Map<String, ?> makeGlobalScope()
    {
        return Collections.emptyMap();
    }

    private Map<String, ?> makeLocalScope()
    {
        return Collections.emptyMap();
    }

    private Response createErrorResponse(final Throwable th)
    {
        return createErrorResponse(ImmutableList.of(ExceptionUtils.getStackTrace(th)));
    }

// -------------------------- INNER CLASSES --------------------------

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
            this(String.valueOf(bean.getEvalResult()), bean.getOut().getBuffer().toString(), bean.getErr().getBuffer().toString());
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


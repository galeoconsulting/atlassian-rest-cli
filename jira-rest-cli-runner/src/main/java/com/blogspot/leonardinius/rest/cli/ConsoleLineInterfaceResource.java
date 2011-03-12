package com.blogspot.leonardinius.rest.cli;

import com.atlassian.jira.rest.v1.model.errors.ErrorCollection;
import com.atlassian.jira.rest.v1.model.errors.ValidationError;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

import static com.atlassian.jira.rest.v1.util.CacheControl.NO_CACHE;


@Path("/cli")
public class ConsoleLineInterfaceResource extends AbstractResource
{
// ------------------------------ FIELDS ------------------------------

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleLineInterfaceResource.class);

    private static final String SCRIPT_TYPE = "scriptType";
    private static final String REPL = "repl";

// -------------------------- OTHER METHODS --------------------------

    @POST
    @Path("/{" + SCRIPT_TYPE + "}")
    public Response createIssue(@PathParam(SCRIPT_TYPE) final String scriptType, @QueryParam(REPL) String input)
    {
        return createExecutionResponse(new ExecutionStatus());
    }

    private Response createExecutionResponse(final ExecutionStatus executionStatus)
    {
        // everything file: green light
        if (!executionStatus.hasAnyErrors())
        {
            // creates defaults text message if absent
            executionStatus.setMessage(StringUtils.defaultString(executionStatus.getMessage(), "Ok"));

            return Response.ok(executionStatus).cacheControl(NO_CACHE).build();
        }

        // errors detected
        final Response.ResponseBuilder builder =
                executionStatus.hasAnyErrors() ? Response.status(Response.Status.SERVICE_UNAVAILABLE) : Response.serverError();
        return builder.entity(executionStatus.getErrorCollection()).cacheControl(NO_CACHE).build();
    }

// -------------------------- INNER CLASSES --------------------------

    @XmlRootElement
    public static class ConsoleOutput
    {
        @XmlElement
        private String out;

        @XmlElement
        private String err;

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


    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class ExecutionStatus
    {
        @XmlElement
        private String message;

        @XmlElement
        private Map<String, String> bindings;

        public ConsoleOutput getIo()
        {
            return io;
        }

        public void setIo(ConsoleOutput io)
        {
            this.io = io;
        }

        @XmlElement
        private ConsoleOutput io;

        // Is not part of the element tree
        // Is used as error collection - in case errors are present - will be served as xml root
        @SuppressWarnings({"deprecation"})
        private ErrorCollection errorCollection;

        private final StringWriter out;

        private final StringWriter err;

        public StringWriter getOut()
        {
            return out;
        }

        public StringWriter getErr()
        {
            return err;
        }

        public Map<String, String> getBindings()
        {
            return bindings;
        }

        public void setBindings(final Map<String, String> bindings)
        {
            this.bindings = bindings;
        }

        public ExecutionStatus()
        {
            this.out = new StringWriter();
            this.err = new StringWriter();
        }

        @SuppressWarnings({"deprecation"})
        public void addError(final ValidationError error)
        {
            errorCollection = newErrorCollectionBuilder(errorCollection)
                    .addError(error.getField(),
                            error.getError(),
                            (String[]) (error.getParams() == null ? null : error.getParams().toArray(new String[error.getParams().size()])))
                    .build();
        }

        ExecutionStatus addErrorMessage(final Throwable th)
        {
            addErrorMessage(ExceptionUtils.getStackTrace(th));
            return this;
        }

        public ExecutionStatus addErrorMessage(final String message)
        {
            errorCollection = newErrorCollectionBuilder(errorCollection)
                    .addErrorMessage(message)
                    .build();
            return this;
        }

        @SuppressWarnings({"deprecation"})
        private ErrorCollection.Builder newErrorCollectionBuilder(final ErrorCollection errorCollection)
        {
            if (errorCollection != null)
            {
                return ErrorCollection.Builder.newBuilder(errorCollection);
            }

            return ErrorCollection.Builder.newBuilder();
        }

        @SuppressWarnings({"deprecation"})
        private ErrorCollection.Builder newErrorCollectionBuilder(final Collection<ValidationError> errorCollection)
        {
            if (errorCollection != null)
            {
                return ErrorCollection.Builder.newBuilder(errorCollection);
            }

            return ErrorCollection.Builder.newBuilder();
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(final String message)
        {
            this.message = message;
        }

        @SuppressWarnings({"deprecation"})
        public ErrorCollection getErrorCollection()
        {
            return errorCollection;
        }

        public boolean hasAnyErrors()
        {
            return getErrorCollection() != null && getErrorCollection().hasAnyErrors();
        }

        public boolean hasErrorMessages()
        {
            return getErrorCollection() != null
                    && getErrorCollection().getErrorMessages() != null
                    && !getErrorCollection().getErrorMessages().isEmpty();
        }
    }
}


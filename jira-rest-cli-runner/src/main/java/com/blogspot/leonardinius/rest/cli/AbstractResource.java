package com.blogspot.leonardinius.rest.cli;

import com.atlassian.jira.rest.api.messages.TextMessage;
import com.atlassian.jira.rest.v1.model.errors.ErrorCollection;
import com.atlassian.jira.rest.v1.model.errors.ValidationError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.atlassian.jira.rest.v1.util.CacheControl.NO_CACHE;

/**
 * Lightweight optional convenience base class for REST end points with commonly used methods.
 *
 * @since v4.0
 */
@SuppressWarnings({"deprecation"})
public abstract class AbstractResource
{
// -------------------------- OTHER METHODS --------------------------

    /**
     * @param entity
     * @return
     */
    protected Response createContentResponse(Object entity)
    {
        return Response.ok(entity).cacheControl(NO_CACHE).build();
    }

    /**
     * @param entity
     * @param type
     * @return
     */
    protected Response createContentResponse(Object entity, MediaType type)
    {
        return Response.ok(entity, type).cacheControl(NO_CACHE).build();
    }

    protected Response createIndexingUnavailableResponse(String message)
    {
        final Set<String> messages = new HashSet<String>();
        messages.add(message);
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(ErrorCollection.Builder.newBuilder(messages).build())
                .cacheControl(NO_CACHE)
                .build();
    }

    /**
     * Creates a response based on the given errors. If there are no errors, the response will be NO_CONTENT (204). If there are
     * errors, these will be used to return an error response. Many REST validate methods need to first build up a
     * possibly empty collection of {@link com.atlassian.jira.rest.v1.model.errors.ValidationError} and then just return
     * the result of calling this method.
     *
     * @param errors the possibly empty collection of errors.
     * @return a success response if the errors are empty, otherwise an error response based on these errors.
     */
    protected Response createValidationResponse(Collection<ValidationError> errors)
    {
        if (errors.isEmpty())
        {
            return Response.ok(new TextMessage("No input validation errors found."))
                    .cacheControl(NO_CACHE)
                    .build();
        }

        return createErrorResponse(errors);
    }

    /**
     * Creates an error response using the given errors.
     *
     * @param errors the errors to use for the error response. Should not be empty.
     * @return the error response.
     */
    protected Response createErrorResponse(final Collection<ValidationError> errors)
    {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorCollection.Builder.newBuilder(errors).build())
                .cacheControl(NO_CACHE)
                .build();
    }

    protected ValidationError makeError(String field, String error)
    {
        return new ValidationError(field, error);
    }

    protected ValidationError makeError(String field, String error, List<String> params)
    {
        return new ValidationError(field, error, params);
    }
}

package com.blogspot.leonardinius.rest.cli;

import com.atlassian.jira.JiraException;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.rest.v1.model.errors.ErrorCollection;
import com.atlassian.jira.rest.v1.model.errors.ValidationError;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleActors;
import com.atlassian.jira.security.roles.RoleActor;
import com.atlassian.jira.util.ImportUtils;
import com.atlassian.jira.util.dbc.Assertions;
import com.atlassian.jira.workflow.WorkflowTransitionUtil;
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.opensymphony.user.User;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.sonyericsson.bugstr.priowizard.conf.BugstrConfiguration;
import com.sonyericsson.bugstr.priowizard.utils.CommonUtils;
import com.sonyericsson.bugstr.priowizard.utils.ExceptionUtils;
import com.sonyericsson.bugstr.priowizard.utils.IssueCrudOperations;
import com.sonyericsson.bugstr.priowizard.utils.JiraErrorException;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

import static com.atlassian.jira.rest.v1.util.CacheControl.NO_CACHE;
import static com.atlassian.jira.util.dbc.Assertions.notNull;


@Path("/cli")
public class CnsoleLineInterfaceResource extends AbstractResource
{
// ------------------------------ FIELDS ------------------------------
    private static final Logger LOG = LoggerFactory.getLogger(CnsoleLineInterfaceResource.class);

    private static final int RADIX_10 = 10;



    @POST
    @Path("create/{" + "" + "}")
    public Response createIssue(@PathParam(PROJECT_ID) final String projectIdParam,
            @QueryParam(ISSUE_DATA) final Map<String, Object> issueData)
    {


        return createIssueStatusResponse(issueUpdateStatusBean);
    }

    private long prepareProjectId(final String projectIdString)
    {
        final String projectId = StringUtils.removeStart(projectIdString, "project-");
        Assertions.notBlank("Project id", projectId);
        try
        {
            return Long.parseLong(projectId, RADIX_10);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid project number specified: " + projectId, e);
        }
    }

    private User getIssueSubmitterActionUser(final User originalActionUser, final Project project)
    {
        if (hasCreatePermissions(originalActionUser, project))
        {
            return originalActionUser;
        }

        // FIXME: 1. might be very very wrong,actually Submitter is not part of default scheme anyway
        // FIXME: 2. investigate performance nuances if any.
        User roleUser = getFirstRoleUserWithCreateIssuePermission(project, Arrays.asList("Submitter"));
        if (roleUser == null)
        {
            roleUser = getFirstRoleUserWithCreateIssuePermission(project, getAllProjectRoleNames());
        }

        return roleUser;
    }

    private boolean hasCreatePermissions(final User originalActionUser, final Project project)
    {
        return permissionManager.hasPermission(Permissions.CREATE_ISSUE, project, originalActionUser, true);
    }

    private User getFirstRoleUserWithCreateIssuePermission(final Project project, final Iterable<String> projectRolesToInspect)
    {
        for (final String magicWellKnownRoleName : projectRolesToInspect)
        {
            try
            {
                final ProjectRole role = projectRoleManager.getProjectRole(magicWellKnownRoleName);
                if (role != null)
                {
                    final ProjectRoleActors actors = projectRoleManager.getProjectRoleActors(role, project);
                    if (actors != null && actors.getRoleActors() != null && actors.getRoleActors().size() > 0)
                    {
                        final RoleActor actor = actors.getRoleActors().iterator().next();
                        @SuppressWarnings({"unchecked"}) final Collection<User> users = actor.getUsers();
                        if (users != null && users.size() > 0)
                        {
                            final User u = users.iterator().next();
                            if (hasCreatePermissions(u, project))
                            {
                                return u;
                            }
                        }
                    }
                }
            }
            catch (DataAccessException e) //NOPMD - continue, return null
            {
                // continue
            }
        }

        return null;
    }

    private Iterable<String> getAllProjectRoleNames()
    {
        final Iterable<ProjectRole> projectRoles = projectRoleManager.getProjectRoles();
        if (projectRoles == null)
        {
            return Collections.emptyList();
        }

        return Iterables.transform(projectRoles, new Function<ProjectRole, String>()
        {
            @Override
            public String apply(@Nullable final ProjectRole from)
            {
                return notNull("Project role", from).getName();
            }
        });
    }

    private String getDefectIssueType() throws JiraException
    {
        return bugstrConfProvider.get().getDefectIssueTypeId();
    }

    private ExecutionStatus doUpdateIssueFields(final User actionUser, final MutableIssue issueObject,
            final Map<String, Object> issueData)
    {
        final ExecutionStatus executionStatus = new ExecutionStatus(issueObject.getKey());

        try
        {
            final Map<String, Object> fieldValues = cleanupIssueFieldsData(issueData);
            issueCrudOperations.setFieldsValues(actionUser, issueObject, fieldValues);
            executionStatus.setFieldsUpdated(ImmutableList.copyOf(fieldValues.keySet()));
        }
        catch (JiraException e)
        {
            LOG.error(UPDATE_ISSUE_VALUES_ERROR, e);
            executionStatus.addErrorMessage(e);
        }

        return executionStatus;
    }

    private Map<String, Object> cleanupIssueFieldsData(final Map<String, Object> issueData) throws JiraException
    {
        Assertions.notNull("Issue fields data", issueData);

        // for security reasons - it's allowed to update only pre-configured issue fields
        final Map<String, Object> filteredIssueData = Maps.filterEntries(issueData, new Predicate<Map.Entry<String, Object>>()
        {
            @Override
            public boolean apply(@Nullable final Map.Entry<String, Object> input)
            {
                final String key = notNull("Field mapping", input).getKey();
                return FIELD_PARAMETER_MAPPING.keySet().contains(key);
            }
        });

        final Map<String, Object> fieldValues = Maps.newHashMap();
        for (final Map.Entry<String, Object> entry : filteredIssueData.entrySet())
        {
            final String key = StringUtils.defaultString(FIELD_PARAMETER_MAPPING.get(entry.getKey()), entry.getKey());
            fieldValues.put(key, entry.getValue());
        }

        return fieldValues;
    }

    private Response createIssueStatusResponse(final ExecutionStatus executionStatus)
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

    @POST
    @Path("create-problem/{" + ISSUE_ID + "}")
    public Response createProblem(@PathParam(ISSUE_ID) final String issueKey, @QueryParam(ISSUE_DATA) final Map<String, Object> issueData)
    {
        final User actionUser = authenticationContext.getUser();
        final MutableIssue issue = issueManager.getIssueObject(StringUtils.upperCase(issueKey));
        Assertions.not("Could not locate issue object (" + issueKey + ")", issue == null);

        final ExecutionStatus executionStatus = doIssueUpdate(actionUser, issue, issueData);
        final boolean wasIndexing = ImportUtils.isIndexIssues();
        try
        {
            doTransition(actionUser, issue, notNull("transition descriptor", getNextWorkflowAction(issue)));
        }
        catch (IndexException e)
        {
            LOG.error(CREATE_DEFECT_EXCEPTION, e); // consider to remove - too verbose and breaks exception re-throwing practices
            executionStatus.addErrorMessage(e);
        }
        catch (JiraException e)
        {
            LOG.error(CREATE_DEFECT_EXCEPTION, e); // consider to remove - too verbose and breaks exception re-throwing practices
            executionStatus.addErrorMessage(e);
        }
        finally
        {
            ImportUtils.setIndexIssues(wasIndexing);
        }

        return createIssueStatusResponse(executionStatus);
    }

    private ExecutionStatus doIssueUpdate(final User actionUser, final MutableIssue issue, final Map<String, Object> issueData)
    {
        //FIXME: add update-issue permission check. Note: it should be role check not the permission scheme check.

        final ExecutionStatus executionStatus = doUpdateIssueFields(actionUser, issue, issueData);

        try
        {
            // performs database update only if some fields marked as changed
            if (!executionStatus.getFieldsUpdated().isEmpty())
            {
                issueCrudOperations.updateIssue(actionUser, issue, true);
            }
        }
        catch (JiraException e)
        {
            LOG.error(STORING_ISSUE_EXCEPTION, e); // consider to remove - too verbose and breaks exception re-throwing practices
            executionStatus.addErrorMessage(e);
        }
        return executionStatus;
    }

    private void doTransition(final User actionUser, final MutableIssue issue,
            final ActionDescriptor transition) throws JiraException
    {
        final WorkflowTransitionUtil workflowTransitionUtil = componentFactory.createObject(WorkflowTransitionUtilImpl.class);
        workflowTransitionUtil.setIssue(issue);
        workflowTransitionUtil.setUsername(actionUser.getName());
        workflowTransitionUtil.setAction(transition.getId());

        // validate and transition issue
        com.atlassian.jira.util.ErrorCollection errorCollection = workflowTransitionUtil.validate();
        throwIfErrors(WORKFLOW_VALIDATION_ERROR, errorCollection);

        errorCollection = workflowTransitionUtil.progress();
        throwIfErrors(WORKFLOW_EXECUTION_ERROR, errorCollection);

        issueIndexManager.reIndex(issue);
    }

    private void throwIfErrors(final String message,
            final com.atlassian.jira.util.ErrorCollection errorCollection) throws JiraErrorException
    {
        if (errorCollection.hasAnyErrors())
        {
            throw new JiraErrorException(message, errorCollection);
        }
    }

    @SuppressWarnings({"unchecked"})
    private ActionDescriptor getNextWorkflowAction(final Issue issue) throws JiraException
    {
        final StepDescriptor stepDescriptor = getIssueStepDescriptor(issue);
        final List commonActions = stepDescriptor.getCommonActions();
        final List actions = stepDescriptor.getActions();

        final List<ActionDescriptor> allActions = Lists.newArrayList();
        Iterables.addAll(allActions, notRecursiveActions(stepDescriptor, CommonUtils.nnulIterable(commonActions)));
        Iterables.addAll(allActions, notRecursiveActions(stepDescriptor, CommonUtils.nnulIterable(actions)));

        if (allActions.isEmpty())
        {
            throw new JiraException("Issue " + issue.getKey() + " is in final state, could not transit further");
        }

        if (allActions.size() > 1)
        {
            throw new JiraException("Issue " + issue.getKey() + ": multiple non-recursive transitions are available, " +
                    "could not chose one. (" + allActions + ")");
        }

        return allActions.get(0);
    }

    private StepDescriptor getIssueStepDescriptor(final Issue issue)
    {
        final GenericValue issueGv = notNull("issueGv", issue.getStatusObject().getGenericValue());
        return workflowManager.getWorkflow(notNull("issue", issue)).getLinkedStep(issueGv);
    }

    private Iterable<ActionDescriptor> notRecursiveActions(final StepDescriptor stepDescriptor, final Iterable<ActionDescriptor> actions)
    {
        return Iterables.filter(actions, new Predicate<ActionDescriptor>()
        {
            @Override
            public boolean apply(@Nullable final ActionDescriptor input0)
            {
                final ActionDescriptor input = notNull("Action descriptor", input0);
                return input.getUnconditionalResult() != null
                        && input.getUnconditionalResult().getStep() != stepDescriptor.getId();
            }
        });
    }

    @GET
    @Path("load/{" + ISSUE_ID + "}")
    public Response loadIssueData(@PathParam(ISSUE_ID) final String issueKeyParameter)
    {
        final String issueKey = StringUtils.upperCase(issueKeyParameter);
        final ExecutionStatus executionStatus = new ExecutionStatus(issueKey);
        final MutableIssue issue = issueManager.getIssueObject(issueKey);

        if (issue == null)
        {
            return createIssueStatusResponse(executionStatus.addErrorMessage("Could not lookup issue with issueKey: " + issueKey));
        }

        try
        {
            executionStatus.setIssueData(toStringValueMap(getIssueFieldData(issue)));
        }
        catch (JiraException e)
        {
            LOG.error(UPDATE_ISSUE_VALUES_ERROR, e);
            executionStatus.addErrorMessage(e);
        }

        return createIssueStatusResponse(executionStatus);
    }

    private Map<String, String> toStringValueMap(final Map<String, Object> fieldValues)
    {
        return Maps.newHashMap(Maps.transformValues(fieldValues, new Function<Object, String>()
        {
            @Override
            public String apply(@Nullable final Object from)
            {
                return from == null ? null : String.valueOf(from);
            }
        }));
    }

    private Map<String, Object> getIssueFieldData(final MutableIssue issue) throws JiraException
    {
        //gets data from the issue
        final Map<Field, Object> issueFieldValues = Maps.filterValues(issueCrudOperations.getFieldsValues(issue), Predicates.notNull());
        final Map<String, Object> values = Maps.newHashMap();

        if (!issueFieldValues.isEmpty())
        {
            // filters out not needed, unused data and remaps it to correct identifiers
            final BiMap<String, String> fields = Maps.unmodifiableBiMap(HashBiMap.create(FIELD_PARAMETER_MAPPING).inverse());
            for (final Map.Entry<Field, Object> entry : issueFieldValues.entrySet())
            {
                final Field field = Assertions.notNull("field", entry.getKey());
                String key = null;
                if (fields.containsKey(field.getId()))
                {
                    key = field.getId();
                }
                else if (fields.containsKey(field.getName()))
                {
                    key = field.getName();
                }

                if (key != null)
                {
                    values.put(Assertions.notNull("field reverse id", fields.get(key)), entry.getValue());
                }
            }
        }

        return values;
    }

    @POST
    @Path("update/{" + ISSUE_ID + "}")
    public Response updateIssue(@PathParam(ISSUE_ID) final String issueKey, @QueryParam(ISSUE_DATA) final Map<String, Object> issueData)
    {
        final User actionUser = authenticationContext.getUser();
        final MutableIssue issue = issueManager.getIssueObject(StringUtils.upperCase(issueKey));
        Assertions.not("Could not locate issue object (" + issueKey + ")", issue == null);

        final ExecutionStatus executionStatus = doIssueUpdate(actionUser, issue, issueData);

        return createIssueStatusResponse(executionStatus);
    }

// -------------------------- INNER CLASSES --------------------------

    @SuppressWarnings({"UnusedDeclaration"})
    @XmlRootElement
    public static class ExecutionStatus
    {
        @XmlElement
        private String message;

        @XmlElement
        private String issueKey;

        @XmlElement
        private Collection<String> fieldsUpdated;

        @XmlElement
        private Map<String, String> issueData;

        // Is not part of the element tree
        // Is used as error collection - in case errors are present - will be served as xml root
        @SuppressWarnings({"deprecation"})
        private ErrorCollection errorCollection;

        public Map<String, String> getIssueData()
        {
            return issueData;
        }

        public void setIssueData(final Map<String, String> issueData)
        {
            this.issueData = issueData;
        }

        public String getIssueKey()
        {
            return issueKey;
        }

        public void setIssueKey(final String issueKey)
        {
            this.issueKey = issueKey;
        }

        public ExecutionStatus()
        {
            this(null);
        }

        public ExecutionStatus(final String issueKey)
        {
            //noinspection deprecation
            this(issueKey,
                    Lists.<String>newArrayListWithCapacity(0),
                    Lists.<ValidationError>newArrayListWithCapacity(0),
                    null);
        }

        @SuppressWarnings({"deprecation"})
        public ExecutionStatus(final String issueKey, final List<String> fieldsUpdated, final Collection<ValidationError> errors,
                               final String message)
        {
            this.issueKey = issueKey;
            this.fieldsUpdated = fieldsUpdated;
            this.message = message;
            this.errorCollection = newErrorCollectionBuilder(errors).build();
        }

        public Collection<String> getFieldsUpdated()
        {
            return fieldsUpdated;
        }

        public Collection<String> addFieldUpdated(final String field)
        {
            fieldsUpdated.add(field);
            return fieldsUpdated;
        }

        public void setFieldsUpdated(final List<String> fieldsUpdated)
        {
            this.fieldsUpdated = fieldsUpdated;
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
            addErrorMessage(ExceptionUtils.getExceptionMessage(th));
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


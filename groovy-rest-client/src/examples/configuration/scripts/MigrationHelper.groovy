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

class MigrationHelperUtil
{

    def componentManager
    def projectManager
    def projectRoleManager
    def constantsManager
    def permSchemeManager

    MigrationHelperUtil(def componentManager)
    {
        this.componentManager = componentManager
        init()
    }

    /**
     * Gets Jira component by name
     * @param classname component interface
     * @return component (interface impl) instance
     */
    def getComponent(String classname)
    {
        componentManager.class.getComponent(componentManager.getComponentClassManager().loadClass(classname))
    }

    /**
     * Loads class and instantiates it
     * @param classname class to load and create
     * @return classname instance
     */
    def getComponentImpl(String classname) { componentManager.getComponentClassManager().newInstance(classname) }

    /**
     * loads class by name
     * @param classname class instance to load
     * @return classname Class object
     */
    def cl(String classname) { componentManager.getComponentClassManager().loadClass(classname) }

    /**
     * Instantiates class
     * @param classname class to instantiate
     * @param arguments constructor arguments
     * @return class instance
     */
    def ncl(String classname, List arguments = Collections.emptyList()) { cl(classname).newInstance(arguments as Object[]) }


    private def init()
    {
        projectManager = getComponent("com.atlassian.jira.project.ProjectManager")
        projectRoleManager = getComponent("com.atlassian.jira.security.roles.ProjectRoleManager")
        constantsManager = getComponent("com.atlassian.jira.config.ConstantsManager")
        permSchemeManager = getComponent("com.atlassian.jira.permission.PermissionSchemeManager")
        this
    }

    /**
     * Creates user role
     * @param name name of the user role
     * @param description description of the user role
     * @return created (persisted) user role object
     */
    def createUserRole(arguments)
    {
        projectRoleManager.createRole(ncl("com.atlassian.jira.security.roles.ProjectRoleImpl", [arguments.name, arguments.description]))
    }

    def createIssueType(arguments)
    {
        def errorCollection = ncl("com.atlassian.jira.util.SimpleErrorCollection")
        constantsManager.validateCreateIssueType(arguments.name, null, arguments.description, arguments.iconurl, errorCollection, "dummyfield")
        if (errorCollection.hasAnyErrors())
            throw new IllegalStateException("""Messages: ${(errorCollection.errorMessages + errorCollection.errors.values()).join(";")}""")

        constantsManager.createIssueType(arguments.name, arguments.sequence, null, arguments.description, arguments.iconurl)
    }

    def createProject(args)
    {
        def assigneeType = args.assigneeType
        if (assigneeType == null)
        {
            def properties = getComponent("com.atlassian.jira.config.properties.ApplicationProperties")
            //com.atlassian.jira.project.AssigneeTypes:
            // PROJECT_DEFAULT = 0L;
            // COMPONENT_LEAD = 1L;
            // PROJECT_LEAD = 2L;
            // UNASSIGNED = 3L;
            assigneeType = properties.getOption("jira.option.allowunassigned") ? 3L : 2L;
        }

        def permissionSchemeId = args.permissionScheme
        if (permissionSchemeId == null)
        {
            permissionSchemeId = permSchemeManager.getDefaultScheme().getLong("id")
        }


        def project = projectManager.createProject(args.name, args.key, args.description, args.lead, null, assigneeType, args.avatarId)
        permSchemeManager.addSchemeToProject(project, permSchemeManager.getSchemeObject(permissionSchemeId))
        project
    }

}

migrationHelper = new MigrationHelperUtil(componentManager)

">> Ok"
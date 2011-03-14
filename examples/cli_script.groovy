//import com.atlassian.jira.issue.CustomFieldManager
//import com.atlassian.jira.ComponentManager;

cfm = componentManager.customFieldManager;

cftypes = cfm.getCustomFieldTypes().each { item ->
   out.println("cf: ${item.key} ${item.name}")
}


issueTypes = componentManager.getIssueTypeSchemeManager().issueTypesForDefaultScheme
contexts = []
cfsearcher = null

counter = 1
cftypes.each { item ->
  cfm.createCustomField("CF ${counter} for ${item.name}", "CF ${counter} for ${item.name}", item, cfsearcher, contexts, issueTypes)
  counter++
}

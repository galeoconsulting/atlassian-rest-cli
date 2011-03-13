import com.atlassian.jira.ComponentManager
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.customfields.CustomFieldType
def CustomFieldManager cfm = ComponentManager.getInstance().customFieldManager;

cfm.getCustomFieldTypes().each { CustomFieldType item ->
   out.println("cf: ${item.key} ${item.name}")
}
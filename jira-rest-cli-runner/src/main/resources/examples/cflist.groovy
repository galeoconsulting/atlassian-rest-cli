cfm = componenManager.customFieldManager;

cfm.getCustomFieldTypes().each { item ->
   out.println("cf: ${item.key} ${item.name}")
}
(function($){
$(document).ready(function(){
    var logger = window.console;

    var console = $('<div class="console">');
    $('#cli-holder').append(console);
    var controller = console.console({
        welcomeMessage          : 'Jira REST Cli console. Click Control-enter to evaluate.',
        promptLabel             : 'cli> ',
        continuedPromptLabel    : '    > ',
        promptHistory           : true,
        autofocus               : true,
        animateScroll           : true,

        commandValidate         : function(line){
            return true;
        },

        commandHandle           : function(script, reporter,e){
             if(e.ctrlKey){

                var payload = {'script' : script};
                $.ajax({
                    url         : AJS.format("{0}/rest/1.0/rest-scripting/{1}", window.restCliBaseUrl, $("#cli-language").val()),
                    data        : JSON.stringify(payload),
                    error       : function(XMLHttpRequest, textStatus, errorThrown)
                    {
                        alert(AJS.format("Status: {0}\nError{1}", textStatus || '', errorThrown || 'none'));
                    },
                    success     : function(data) //, textStatus, XMLHttpRequest)
                    {
                        var result = new Array();
                        result.push({msg : data.evalResult, className:"jquery-console-message-value"});
                        if(data.out && data.out != ""){
                            result.push({msg : data.out, className:"jquery-console-message-out"});
                        }
                        if(data.err && data.err != ""){
                            result.push({msg : data.err, className:"jquery-console-message-err"});
                        }

                        controller.continuedPrompt = false;
                        reporter(result);
                    },
                    type        : 'POST',
                    cache       : false,
                    dataType    : 'json',
                    contentType : 'application/json'
                });
                return;
             }

             controller.continuedPrompt = true;
             return;
        }
    });
});
})(AJS.$);
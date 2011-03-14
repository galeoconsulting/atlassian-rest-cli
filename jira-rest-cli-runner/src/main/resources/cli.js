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

        commandHandle           : function(scriptText, reporter,e){
             if(e.ctrlKey){

                var payload = {script : scriptText, argv: []};
                var appendOutErr = function(result, data){
                    if(data.out && data.out != ""){
                        result.push({msg : ["OUT:\n", data.out].join(''), className:"jquery-console-message-out"});
                    }
                    if(data.err && data.err != ""){
                        result.push({msg : ["ERR:\n", data.err].join(''), className:"jquery-console-message-err"});
                    }
                    return result;
                }

                $.ajax({
                    url         : AJS.format("{0}/rest/rest-scripting/1.0/cli/{1}", window.restCliBaseUrl, $("#cli-language").val()),
                    data        : JSON.stringify(payload),
                    error       : function(XMLHttpRequest, textStatus, errorThrown)
                    {
                        var getScriptError = function (xhr) {
                           if(!xhr || !xhr.response) return null;
                           var data = xhr.response;
                           if(typeof data == 'string'){
                              data = $.parseJSON(data);
                           }
                           if(data && data.errors && data.errors.errorMessages) return data;
                           return null;
                        };

                        var scriptError = getScriptError(XMLHttpRequest);
                        if(scriptError && scriptError.errors.errorMessages.length > 0)
                        {
                           var result = [{msg : "Error:" + scriptError.errors.errorMessages.join('\n'),
                                      className:"jquery-console-message-error"}];
                           result = appendOutErr(result, scriptError);
                           reporter(result);

                        }
                        else
                            alert(AJS.format("Status: {0}\nError: {1}", textStatus || '', errorThrown || 'none'));
                        controller.continuedPrompt = false;
                        reporter();
                    },
                    success     : function(data) //, textStatus, XMLHttpRequest)
                    {
                        var result = new Array();
                        result.push({msg : data.evalResult, className:"jquery-console-message-value"});
                        result = appendOutErr(result, data);

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
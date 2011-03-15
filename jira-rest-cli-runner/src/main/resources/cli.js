(function($){
$(document).ready(function(){
    var logger = window.console;

    var tryIt = function(f, defValue) {
        try{
            return f();
        } catch(e){
            return defValue;
        }
    };

    var console = $('<div class="console">');
    $('#cli-holder').append(console);
    var controller = console.console({
        welcomeMessage          : 'Jira REST Cli console. Click Control-enter to evaluate.',
        promptLabel             : 'cli> ',
        continuedPromptLabel    : ' ==> ',
        promptHistory           : true,
        autofocus               : true,
        animateScroll           : true,

        commandValidate         : function(line){
            return true;
        },

        commandHandle           : function(scriptText, reporter,e){
             if(e.ctrlKey){

                var appendOutErr = function(result, data){
                    if(data.out && data.out != ""){
                        result.push({msg : ["OUT:\n", data.out].join(''), className:"jquery-console-message-out"});
                    }
                    if(data.err && data.err != ""){
                        result.push({msg : ["ERR:\n", data.err].join(''), className:"jquery-console-message-err"});
                    }
                    return result;
                }

                var continuePrompt = function(enable){ controller.continuedPrompt = enable; };

                var payload = {script : scriptText, argv: []};

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
                            alert(AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
                                textStatus || '',
                                errorThrown || '',
                                tryIt(function(){ return  XMLHttpRequest.status; }, 'Unknown')));
                        continuePrompt(false);
                        reporter();
                    },
                    success     : function(data) //, textStatus, XMLHttpRequest)
                    {
                        var result = new Array();
                        result.push({msg : data.evalResult, className:"jquery-console-message-value"});
                        result = appendOutErr(result, data);

                        continuePrompt(false);
                        reporter(result);
                    },
                    type        : 'POST',
                    cache       : false,
                    dataType    : 'json',
                    contentType : 'application/json'
                });

                return;
             }

             continuePrompt(true);
             return;
        }
    });
});
})(AJS.$);
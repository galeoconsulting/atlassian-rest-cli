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

                $.ajax({
                    url         : AJS.format("{0}/rest/rest-scripting/1.0/cli/{1}", window.restCliBaseUrl, $("#cli-language").val()),
                    data        : payload, //JSON.stringify(payload),
                    error       : function(XMLHttpRequest, textStatus, errorThrown)
                    {
                        var getErrorMessages = function (xhr) {
                           if(!xhr || !xhr.response) return null;
                           var data = xhr.response;
                           if(typeof data == 'string'){
                              data = $.parseJSON(data);
                           }
                           if(data && data.errorMessages) return data.errorMessages;
                           return null;
                        };

                        var errorMessages = getErrorMessages(XMLHttpRequest);
                        if(errorMessages && errorMessages.length > 0)
                        {
                           reporter([{msg : "Error:" + errorMessages.join('\n'),
                                      className:"jquery-console-message-error"}]);
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
                        if(data.out && data.out != ""){
                            result.push({msg : ["out: ", data.out].join(''), className:"jquery-console-message-out"});
                        }
                        if(data.err && data.err != ""){
                            result.push({msg : ["err: ", data.err].join(''), className:"jquery-console-message-err"});
                        }

                        controller.continuedPrompt = false;
                        reporter(result);
                    },
                    type        : 'POST',
                    cache       : false,
                    dataType    : 'json'
                    //,contentType : 'application/json'
                });
                return;
             }

             controller.continuedPrompt = true;
             return;
        }
    });
});
})(AJS.$);
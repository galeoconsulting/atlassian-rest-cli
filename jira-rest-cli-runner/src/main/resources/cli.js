(function($){
    $(document).ready(function(){
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
                     controller.continuedPrompt = false;
                     window.console.log(script);
                     return [{msg:"=> [12,42]",
                              className:"jquery-console-message-value"},
                             {msg:":: [a]",
                              className:"jquery-console-message-type"}]
                 }
                 controller.continuedPrompt = true;
                 return;
            }
        });
     });
})(AJS.$);
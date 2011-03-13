(function($){
    $(document).ready(function(){
        var console = $('<div class="console">');
        $('#cli-holder').append(console);
        var controller = console.console({
            promptLabel             : 'REPL> ',
            continuedPromptLabel    : ' -> ',
            promptHistory           : true,
            autofocus               : true,
            animateScroll           : true,

            commandValidate         : function(line){
                if (line == "") return false;
                    else return true;
            },

            commandHandle           : function(line){
             return [{msg:"=> [12,42]",
                      className:"jquery-console-message-value"},
                     {msg:":: [a]",
                      className:"jquery-console-message-type"}]
            },

            charInsertTrigger       : function(keycode,line){
                // Let you type until you press a-z
                // Never allow zero.
                return !line.match(/[a-z]+/) && keycode != '0'.charCodeAt(0);
            }
        });
     });
})(AJS.$);
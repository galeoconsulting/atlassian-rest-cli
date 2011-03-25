(function($){
  
  var tryIt = function(f, defValue) {
    try{
      return f();
    } catch(e){
      return defValue;
    }
  };
  
  $.namespace('com.blogspot.leonardinius.restcli');
  $restcli = com.blogspot.leonardinius.restcli;

  $restcli.newSession = function(ajaxOptions) {
    $.ajax($.extend({},
    {
        url         : AJS.format("{0}/rest/rest-scripting/1.0/sessions", ajaxOptions.baseUrl),
        error       : function(XMLHttpRequest, textStatus, errorThrown)
        {
          var getScriptErrors = function (xhr) {
            if(!xhr || !xhr.response) return [];
            var errors = xhr.response;
            if(typeof errors == 'string'){
              errors = tryIt(function(){ return $.parseJSON(errors)}, null);
            }
            if(errors && errors.errorMessages) return errors;
            return [];
          };

          alert(
            AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
            textStatus  || 'unknown',
            [errorThrown || ''].concat(getScriptErrors(XMLHttpRequest)).join('\n'),
            tryIt(function(){ return  XMLHttpRequest.status; }, 'Unknown'))
          );

        },
        success     : function(data) //, textStatus, XMLHttpRequest)
        {
          alert(AJS.format("Session created: {0)", data['sessionId']));
        },

        type        : 'PUT',
        cache       : false,
        dataType    : 'json',
        contentType : 'application/json'
    }, ajaxOptions));
  };

  $restcli.deleteSession = function(ajaxOptions) {
    $.ajax($.extend({},
    {
        url         : AJS.format("{0}/rest/rest-scripting/1.0/sessions/{1}", ajaxOptions.baseUrl, ajaxOptions.data.sessionId),
        error       : function(XMLHttpRequest, textStatus, errorThrown)
        {
          var getScriptErrors = function (xhr) {
            if(!xhr || !xhr.response) return [];
            var errors = xhr.response;
            if(typeof errors == 'string'){
              errors = tryIt(function(){ return $.parseJSON(errors)}, null);
            }
            if(errors && errors.errorMessages) return errors;
            return [];
          };

          alert(
            AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
            textStatus  || 'unknown',
            [errorThrown || ''].concat(getScriptErrors(XMLHttpRequest)).join('\n'),
            tryIt(function(){ return  XMLHttpRequest.status; }, 'Unknown'))
          );

        },

        type        : 'DELETE',
        cache       : false,
        dataType    : 'json',
        contentType : 'application/json'
    }, ajaxOptions));
  };
  
  $restcli.executor = function(options){
    var defaults = {
      welcomeMessage          : 'Press Ctrl-Enter/^-Enter to evaluate.',
      promptLabel             : 'cli> ',
      continuedPromptLabel    : ' => ',
      promptHistory           : true,
      autofocus               : true,
      animateScroll           : true
    };
    
    
    var controller = $('<div class="console">').appendTo($(options.container)).console(
      $.extend({}, defaults, options,
        {
          
          commandValidate         : function(line){
            return true;
          },
          
          commandHandle           : function(scriptText, reporter, e)
          {
            var continuePrompt = function(enable){ controller.continuedPrompt = enable; };
            
            var appendOutErr = function(result, data){
              if(data.out && data.out != ""){
                result.push({msg : ["OUT:\n", data.out].join(''), className:"jquery-console-message-out"});
              }
              if(data.err && data.err != ""){
                result.push({msg : ["ERR:\n", data.err].join(''), className:"jquery-console-message-err"});
              }
              return result;
            };
            
            if(e.ctrlKey)
            {
              var payload = {script : scriptText, argv: []};
              
              $.ajax({
                url         : typeof options.ajaxUrl == 'function' ? options.ajaxUrl() : options.ajaxUrl,
                data        : JSON.stringify(payload),
                error       : function(XMLHttpRequest, textStatus, errorThrown)
                {
                  var getScriptError = function (xhr) {
                    if(!xhr || !xhr.response) return null;
                    var data = xhr.response;
                    if(typeof data == 'string'){
                      data = tryIt(function(){ return $.parseJSON(data)}, null);
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
                  else alert(
                    AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
                    textStatus  || 'unknown',
                    errorThrown || '',
                    tryIt(function(){ return  XMLHttpRequest.status; }, 'Unknown'))
                  );
                  
                  continuePrompt(false);
                  reporter([]);
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
          
          }));
          
        };
})(AJS.$);
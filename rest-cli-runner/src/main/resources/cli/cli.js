/*
 * Copyright 2011 Leonid Maslov<leonidms@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//noinspection UnnecessaryReturnStatementJS
(function($)
{

    var tryIt = function(f, defValue)
    {
        try
        {
            return f();
        }
        catch(e)
        {
            return defValue;
        }
    };

    $.namespace('com.galeoconsulting.leonardinius.restcli');
    var $restcli = com.galeoconsulting.leonardinius.restcli;

    $restcli.newSession = function(ajaxOptions)
    {
        $.ajax($.extend({},
                {
                    url         : AJS.format("{0}/rest/rest-scripting/1.0/sessions", ajaxOptions.baseUrl),
                    error       : function(XMLHttpRequest, textStatus, errorThrown)
                    {
                        var getScriptErrors = function (xhr)
                        {
                            if (!xhr || !xhr.response) return [];
                            var errors = xhr.response;
                            if (typeof errors == 'string')
                            {
                                errors = tryIt(function()
                                {
                                    return $.parseJSON(errors)
                                }, null);
                            }
                            if (errors && errors.errorMessages) return errors;
                            if (errors && errors.message) return [errors.message];
                            return [];
                        };

                        alert(
                                AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
                                        textStatus || 'unknown',
                                        [errorThrown || ''].concat(getScriptErrors(XMLHttpRequest)).join('\n'),
                                        tryIt(function()
                                        {
                                            return  XMLHttpRequest.status;
                                        }, 'Unknown'))
                        );

                    },
                    success     : function(data)
                    { //, textStatus, XMLHttpRequest) {
                        alert(AJS.format("Session created: {0)", data['sessionId']));
                    },

                    type        : 'PUT',
                    cache       : false,
                    dataType    : 'json',
                    contentType : 'application/json'
                }, ajaxOptions));
    };

    $restcli.deleteSession = function(ajaxOptions)
    {
        $.ajax($.extend({},
                {
                    url         : AJS.format("{0}/rest/rest-scripting/1.0/sessions/{1}", ajaxOptions.baseUrl, ajaxOptions.data.sessionId),
                    error       : function(XMLHttpRequest, textStatus, errorThrown)
                    {
                        var getScriptErrors = function (xhr)
                        {
                            if (!xhr || !xhr.response) return [];
                            var errors = xhr.response;
                            if (typeof errors == 'string')
                            {
                                errors = tryIt(function()
                                {
                                    return $.parseJSON(errors)
                                }, null);
                            }
                            if (errors && errors.errorMessages) return errors;
                            if (errors && errors.message) return [errors.message];
                            return [];
                        };

                        alert(
                                AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
                                        textStatus || 'unknown',
                                        [errorThrown || ''].concat(getScriptErrors(XMLHttpRequest)).join('\n'),
                                        tryIt(function()
                                        {
                                            return  XMLHttpRequest.status;
                                        }, 'Unknown'))
                        );

                    },

                    type        : 'DELETE',
                    cache       : false,
                    dataType    : 'json',
                    contentType : 'application/json'
                }, ajaxOptions));
    };

    $restcli.executor = function(options)
    {
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

                            commandValidate         : function(/*line*/)
                            {
                                return true;
                            },

                            commandHandle           : function(scriptText, reporter, e)
                            {
                                var continuePrompt = function(enable)
                                {
                                    controller.continuedPrompt = enable;
                                };

                                var appendOutErr = function(result, data)
                                {
                                    if (data.out && data.out != "")
                                    {
                                        result.push({msg : ["OUT:\n", data.out].join(''), className:"jquery-console-message-out"});
                                    }
                                    if (data.err && data.err != "")
                                    {
                                        result.push({msg : ["ERR:\n", data.err].join(''), className:"jquery-console-message-err"});
                                    }
                                    return result;
                                };

                                if (e.ctrlKey)
                                {
                                    var payload = {script : scriptText};

                                    $.ajax({
                                        url         : typeof options.ajaxUrl == 'function' ? options.ajaxUrl() : options.ajaxUrl,
                                        data        : JSON.stringify(payload),
                                        error       : function(XMLHttpRequest, textStatus, errorThrown)
                                        {

                                            var getAsJson = function(xhr)
                                            {
                                                if (!xhr || !xhr.response) return null;
                                                var data = xhr.response;
                                                if (typeof data == 'string')
                                                {
                                                    data = tryIt(function()
                                                    {
                                                        return $.parseJSON(data)
                                                    }, null);
                                                }
                                                return data;
                                            };

                                            var getScriptError = function (xhr)
                                            {
                                                var data = getAsJson(xhr);
                                                if (data && data.errors && data.errors.errorMessages) return data;
                                                return null;
                                            };

                                            var scriptError = getScriptError(XMLHttpRequest);
                                            if (scriptError && scriptError.errors.errorMessages.length > 0)
                                            {
                                                var result = [
                                                    {msg : "Error:" + scriptError.errors.errorMessages.join('\n'),
                                                        className:"jquery-console-message-error"}
                                                ];
                                                result = appendOutErr(result, scriptError);
                                                reporter(result);
                                            }
                                            else alert(
                                                    AJS.format("{0}: (HTTP Status: {2})\n\n{1}",
                                                            textStatus || 'unknown',
                                                            errorThrown || tryIt(function()
                                                            {
                                                                return getAsJson(XMLHttpRequest).message
                                                            }, null) || '',
                                                            tryIt(function()
                                                            {
                                                                return  XMLHttpRequest.status;
                                                            }, 'Unknown'))
                                            );

                                            continuePrompt(false);
                                            reporter([]);
                                        },
                                        success     : function(data)
                                        { //, textStatus, XMLHttpRequest) {
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

                            }

                        }));

    };
})(AJS.$);
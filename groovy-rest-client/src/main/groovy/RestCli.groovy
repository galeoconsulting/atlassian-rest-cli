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

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.config.DefaultClientConfig
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import jline.ConsoleReader
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject

public class RestCli
{
    private def Map options = [:]
    private javax.ws.rs.core.Cookie authCookie = null;
    private Client client;

    public RestCli(Map options)
    {
        init(options)
    }

    private def init(Map options)
    {
        this.options.clear();
        this.options.putAll(options)
        authCookie = null;
        client = Client.create(new DefaultClientConfig());
    }


    private def baseUrl()
    {
        "${options.proto}://${options.host}:${options.port}/${options.context}"
    }

    private def loginUrl()
    {
        "${baseUrl()}/rest/auth/latest/session"
    }

    private def cliBaseUrl()
    {
        "${baseUrl()}/rest/rest-scripting/1.0"
    }

    RestCli login(def username, def password)
    {
        def cookie = client.resource(loginUrl())     //
                .type(MediaType.APPLICATION_JSON)    //
                .accept(MediaType.APPLICATION_JSON)  //
                .post(JSONObject.class, new JSONObject('username': username, 'password': password))['session'];

        authCookie = new NewCookie((String) cookie['name'], (String) cookie['value']);

        return this;
    }

    def logout()
    {
        ClientResponse cr = client.resource(loginUrl()) //
                .cookie(authCookie)                     //
                .type(MediaType.APPLICATION_JSON)       //
                .accept(MediaType.APPLICATION_JSON)     //
                .delete(ClientResponse.class);

        assert cr.status == 204;
        authCookie = null;
        cr
    }

    String attachSession()
    {
        client.resource(cliBaseUrl()).path('/sessions')  //
                .cookie(authCookie)                      //
                .type(MediaType.APPLICATION_JSON)        //
                .accept(MediaType.APPLICATION_JSON)      //
                .put(JSONObject.class, new JSONObject('language': 'groovy'))['sessionId']
    }

    def deleteSession(String sessionId)
    {
        client.resource(cliBaseUrl()).path('/sessions').path(sessionId).cookie(authCookie) //
                .delete(ClientResponse.class)
    }

    List<String> listSessions()
    {
        JSONObject response = client.resource(cliBaseUrl())
                .path('/sessions') //  )                 //
                .queryParam('language', 'groovy')        //
                .cookie(authCookie) //                   //
                .type(MediaType.APPLICATION_JSON)        //
                .accept(MediaType.APPLICATION_JSON)      //
                .get(JSONObject.class);
        JSONArray sessions = (JSONArray) response['sessions']
        def sessionIds = [];
        for (int i = 0; i < sessions.length(); i++)
        {
            sessionIds.add(sessions.get(i)['sessionId'])
        }
        (sessionIds as List<String>)
    }

    JSONObject eval_input(String sessionId, String scriptText)
    {
        client.resource(cliBaseUrl()).path('/sessions').path(sessionId)
                .cookie(authCookie)                  //
                .type(MediaType.APPLICATION_JSON)    //
                .accept(MediaType.APPLICATION_JSON)  //
                .post(JSONObject.class, new JSONObject('script': scriptText))
    }

    def doWithSession(sessionId = null, Closure closure)
    {
        def evaluationResult
        boolean killSessionOnExit = false

        sessionId = sessionId == null ?
            {

                def tmp = attachSession()
                println("New session acquired: ${tmp}")
                killSessionOnExit = true
                tmp
            }() : sessionId;

        try
        {
            evaluationResult = closure(sessionId)
        }
        finally
        {
            if (sessionId != null && killSessionOnExit)
            {
                println "Session state cleanup: ${sessionId}"
                deleteSession(sessionId)
            }
        }

        evaluationResult
    }

    private def defaultEval(String sessionId, String text)
    {
        try
        {
            def result = eval_input(sessionId, text)

            if (result['out'] != '') println result['out']
            if (result['err'] != '') System.err.println result['err']
            println "rest-cli=> ${result.has('evalResult') ? result['evalResult'] : null}"
            return result
        }
        catch (com.sun.jersey.api.client.UniformInterfaceException e)
        {
            defaultHandleError(e)
            return e
        }
    }

    def execute(String sessionId, String text)
    {
        doWithSession sessionId, { session ->
            defaultEval(session, text)
        }
    }

    def repl(String sessionId = null)
    {
        println """
*****************************************
**  JIRA REST-Groovy-Cli command line  **
**  Type '!q' to exit the loop         **
**  Type '!r' to eval user input       **
*****************************************
"""
        doWithSession sessionId, { session ->
            ConsoleReader console = new ConsoleReader(System.in, new OutputStreamWriter(System.out))
            def lines = [];
            def i = 1;

            loopCond: for (;;)
            {
                printf("rest-cli(%1\$05d)>> ", i++)
                String line = console.readLine();
                switch (line.trim())
                {
                    case '!q':
                    case 'quit':
                    case 'exit':
                        break loopCond; break;
                    default:
                        lines += line;
                        break;
                    case '!r':
                        def text = lines.join("\n");
                        lines = []; i = 1;
                        defaultEval(session, text)
                        break;
                }

            }
        }
    }



    private static def defaultHandleError(com.sun.jersey.api.client.UniformInterfaceException e)
    {
        JSONObject jso = e.response.getEntity(JSONObject.class)
        String message = '';
        if (jso != null)
        {
            if (jso.has('error'))                                                // server error
                message = jso['error']
            else if (jso.has('errorMessages'))                                   // app internal errors (validations, assertions)
                message = jso['errorMessages'].join('')
            else if (jso.has('errors') && jso['errors'].has('errorMessages'))    // evaluation errors
                message = jso['errors']['errorMessages'].join('')
        }
        message = StringEscapeUtils.unescapeJavaScript(message)
        message = StringUtils.defaultIfEmpty(message, e.getMessage())
        if (jso.has('out') && jso['out'] != '') println jso['out']
        if (jso.has('err') && jso['err'] != '') System.err.println jso['err']
        System.err.println("----\nError: ${message}")
    }

    public static void main(String[] args)
    {
        CliBuilder cli = new CliBuilder(usage: 'rest-cli-groovy -h <host> -u <user> -w <password> [options]')
        cli.h(required: true, longOpt: 'host', args: 1, argName: 'host', 'server hostname')
        cli.p(longOpt: 'port', args: 1, argName: 'port', 'server port. defaults to [80]')
        cli.proto(longOpt: 'protocol', args: 1, argName: 'protocol', 'http/https protocol; could be derived from port. defaults to [http]')
        cli.c(longOpt: 'context', args: 1, argName: 'context', 'application context (e.g.: jira)')
        cli.u(required: true, longOpt: 'user', args: 1, argName: 'user', 'admin user name to connect with')
        cli.w(required: true, longOpt: 'password', args: 1, argName: 'password', 'password to authenticate with')
        cli.s(longOpt: 'session', args: 1, argName: 'cli-session-id', 'cli session id to connect to')
        cli.l(longOpt: 'list-sessions', 'list cli session ids')
        cli.d(longOpt: 'drop-session', args: 1, argName: 'cli-session-id', 'will terminate cli session')
        cli.n(longOpt: 'new-session', 'will create new session and exit immediatelly')
        cli.f(longOpt: 'file', args: 1, argName: 'file', 'will read file and evaluate it\'s contents. use - for stdin')
        cli.help('print this message')

        def options = cli.parse(args)

        if (options == null || !options)
        {
            return;
        }

        if (options.help)
        {
            cli.usage()
            return;
        }

        doMain(options)
    }

    private static void doMain(def options)
    {
        RestCli repl = null;
        try
        {
            def selfOptions = [:]
            selfOptions.port = !options.port ? 80 : options.port
            selfOptions.proto = !options.proto ? (options.port == 443 ? 'https' : 'http') : options.proto
            selfOptions.host = options.host
            selfOptions.context = !options.context ? '' : options.context

            repl = new RestCli(selfOptions).login(options.user, options.password)

            if (options.'new-session')
            {
                println repl.attachSession()
                System.exit(0)
            }

            if (options.'list-sessions')
            {
                def List sessions = repl.listSessions()
                println "Active groovy sessions ($sessions.size): ${sessions.join(", ")}"
                System.exit(0)
            }

            if (options.'drop-session')
            {
                println "Deleting cli-session: ${options.'drop-session'}"
                repl.deleteSession(options.'drop-session')
                System.exit(0)
            }

            if (options.file)
            {
                doEval(options.session, options.file, repl)
                System.exit(0)
            }

            doRepl(options.session, repl)
        }
        catch (com.sun.jersey.api.client.UniformInterfaceException e)
        {
            defaultHandleError(e)
        }
        finally
        {
            if (repl != null)
            {
                repl.logout()
            }
        }
    }

    static def String readFully(InputStream is)
    {
        Reader reader = new InputStreamReader(is)
        StringWriter sw = new StringWriter()
        char[] cbuf = new char[4 * 1024]
        for (int charsRead = reader.read(cbuf); charsRead != -1; charsRead = reader.read(cbuf))
            sw.write(cbuf, 0, charsRead);
        return sw.toString()
    }

    private static String asSessionId(def optionValue)
    {
        if (!optionValue || optionValue == null) return null
        optionValue
    }

    private static def doEval(def sessionId, String file, RestCli restCli)
    {
        InputStream is = "-".equals(file) ? System.in : new FileInputStream(file)
        restCli.execute(asSessionId(sessionId), readFully(is))
    }

    private static def doRepl(def sessionId, RestCli repl)
    {
        repl.repl(asSessionId(sessionId))
    }

}

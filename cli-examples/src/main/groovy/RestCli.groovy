import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.config.DefaultClientConfig
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import jline.ConsoleReader
import org.apache.commons.lang.StringEscapeUtils
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject

public class RestCli
{
    private def options = [:];
    private javax.ws.rs.core.Cookie authCookie = null;
    private Client client;

    public RestCli(Map opts)
    {
        init(opts);

    }

    def reset()
    {
        init([:]);
    }

    private def init(Map opts)
    {
        options.putAll(opts);
        authCookie = null;
        client = Client.create(new DefaultClientConfig());
    }

    private def baseUrl()
    {
        "${options['proto']}://${options['host']}:${options['port']}/${options['context']}"
    }

    private def loginUrl()
    {
        "${baseUrl()}/rest/auth/latest/session"
    }

    private def cliBaseUrl()
    {
        "${baseUrl()}/rest/rest-scripting/1.0"
    }

    def login(def username, def password)
    {
        def cookie = client.resource(loginUrl()).type(MediaType.APPLICATION_JSON)    //
                .accept(MediaType.APPLICATION_JSON)  //
                .post(JSONObject.class, new JSONObject('username': username, 'password': password))['session'];
        authCookie = new NewCookie(cookie['name'], cookie['value']);

        return this;
    }


    def attachSession()
    {
        client.resource(cliBaseUrl()).path('/sessions') //
                .cookie(authCookie) //
                .type(MediaType.APPLICATION_JSON)    //
                .accept(MediaType.APPLICATION_JSON)  //
                .put(JSONObject.class, new JSONObject('language': 'groovy'))['sessionId']
    }

    def deleteSession(String sessionId)
    {
        client.resource(cliBaseUrl()).path('/sessions').path(sessionId).cookie(authCookie) //
                .delete(ClientResponse.class)
    }

    List listSessions()
    {
        JSONObject response = client.resource(cliBaseUrl()).path('/sessions') //
                .cookie(authCookie) //
                .type(MediaType.APPLICATION_JSON)    //
                .accept(MediaType.APPLICATION_JSON)  //
                .get(JSONObject.class)
        JSONArray sessions = response['sessions']
        def sessionIds = [];
        for (int i = 0; i < sessions.length(); i++)
        {
            if (sessions.get(i)['languageName'].toLowerCase().equals('groovy'))
            {
                sessionIds.add(sessions.get(i)['sessionId'])
            }
        }
        sessionIds
    }

    def eval_input(String sessionId, String input)
    {
        client.resource(cliBaseUrl()).path('/sessions').path(sessionId).cookie(authCookie) //
                .type(MediaType.APPLICATION_JSON)    //
                .accept(MediaType.APPLICATION_JSON)  //
                .post(JSONObject.class, new JSONObject('script': input))
    }

    def repl(sessionId = null, killSessionOnExit = true)
    {

        System.out.println """
*****************************************
**  JIRA REST-Groovy-Cli command line  **
**  Type '!q' to exit the loop         **
**  Type '!r' to eval user input       **
*****************************************
"""
        sessionId = sessionId == null ?
            {

                def tmp = attachSession()
                System.out.println("New session acquired: ${tmp}")
                tmp
            }() : sessionId;

        try
        {
            ConsoleReader console = new ConsoleReader(System.in, new OutputStreamWriter(System.out))
            def lines = [];

            def loopCond = true;
            for (def i = 1; loopCond;)
            {

                System.out.printf("rest-cli(%1\$05d)>> ", i++)
                String line = console.readLine();
                switch (line.trim())
                {
                    case '!q':
                    case 'quit':
                    case 'exit':
                        loopCond = false; break;
                    default:
                        lines += line;
                        break;
                    case '!r':
                        def text = lines.join("\n");
                        lines = []
                        i = 1

                        try
                        {
                            JSONObject result = eval_input(sessionId, text)

                            if (result['out'] != '') System.out.println result['out']
                            if (result['err'] != '') System.err.println result['err']
                            System.out.println "rest-cli=> ${result.has('evalResult') ? result['evalResult'] : null}"
                        }
                        catch (com.sun.jersey.api.client.UniformInterfaceException e)
                        {
                            defaultHandleError(e)
                        }
                        break;
                }

            }
        }
        finally
        {
            if (sessionId != null && killSessionOnExit)
            {
                System.out.println "Session state cleanup: ${sessionId}"
                this.deleteSession(sessionId)
            }
        }
    }

    public static void main(String[] args)
    {
        try
        {
            def cli = new RestCli([
                    'proto': 'http', //
                    'host': 'localhost', //
                    'port': '2990', //
                    'context': 'jira', //
            ]).login('admin', 'admin')

            if (!cli.listSessions().isEmpty())
            {
                System.out.println "Active groovy sessions: ${cli.listSessions().join(", ")}"
            }

            cli.repl()
        }
        catch (com.sun.jersey.api.client.UniformInterfaceException e)
        {
            defaultHandleError(e)
        }
    }

    private static def defaultHandleError(com.sun.jersey.api.client.UniformInterfaceException e)
    {
        JSONObject jso = e.response.getEntity(JSONObject.class)
        String message = "${jso.has('error') ? jso['error'] : ''}${jso.has('errors') ? jso['errors']['errorMessages'].join('') : ''}";
        message = StringEscapeUtils.unescapeJavaScript(message)
        if (jso['out'] != '') System.out.println jso['out']
        if (jso['err'] != '') System.err.println jso['err']
        System.err.println("----\nError: ${message}")
    }
}

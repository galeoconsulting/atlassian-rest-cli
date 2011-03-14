import groovyx.net.http.RESTClient
import org.apache.http.HttpResponse
import groovyx.net.http.ContentType
import net.sf.json.JSON
import net.sf.json.JSONSerializer

def PrintStream out = System.out

def (proto, host, port, context) = ["http", "localhost", 2990, "jira/"]

def url = "${proto}://${host}:${port}/${context}rest/rest-scripting/1.0/cli/groovy"

RESTClient client = new RESTClient(url)
//client.auth.basic("admin", "admin")
client.contentType = ContentType.JSON
client.headers.put("Cookie", "jira.conglomerate.cookie=; JSESSIONID=283C0269F172BC30EEE865B52449D0AB")


File file = new File(System.getProperty("user.dir"), "cflist.groovy");

def response = client.post(
        [filename: file.name,
        script: file.readLines().join("\n"),
        argv: []])

assert response.status == 200
out.println response.data


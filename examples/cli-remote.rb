require 'rubygems'
require 'restclient'
require 'json'

def from_json(str)
    JSON::parse str
end

(proto, host, port, context) = ["http", "localhost", 2990, "jira/"]
(username, password) = ["admin", "admin"]

AUTH_BASE_URL = "#{proto}://#{host}:#{port}/#{context}rest/auth/latest/session"
def login(username, password)
    logindata = RestClient.post(AUTH_BASE_URL, {"username" => username, "password" => password}.to_json, { :content_type => :json,
    :accept => :json })
    from_json(logindata)["session"]
end

CLI_BASE_URL = "#{proto}://#{host}:#{port}/#{context}rest/rest-scripting/1.0"
OPTS = { :content_type => :json, :accept => :json }

def newSessionId
    sessionId = RestClient.put("#{CLI_BASE_URL}/sessions", {"language" => "ruby"}.to_json, OPTS)
    from_json(sessionId)["sessionId"]
end

def deleteSession(sessionId)
    raise StandardError, "SessionId should be specified. Empty value passed in" if sessionId.nil? or sessionId == ""
    RestClient.delete("#{CLI_BASE_URL}/sessions/#{sessionId}", OPTS)
end

def cli(sessionId, script)
    evalResult = RestClient.post("#{CLI_BASE_URL}/sessions/#{sessionId}", {"script" => script}.to_json, OPTS)
    from_json(evalResult)
end

def listSessions
    sessions = RestClient.get("#{CLI_BASE_URL}/sessions", OPTS)
    from_json(sessions)["sessions"].select{|i| i["languageName"]["Ruby"] }.map{ |e| e["sessionId"]}
end

def repl(sessionId = nil, killSessionOnExit = true)
    sessionId ||= newSessionId()
    begin
        puts " ***************************************** "
        puts " **  JIRA REST-Ruby-Cli command line    ** "
        puts " **  Type 'quit' to exit the loop       ** "
        puts " ***************************************** "
        while true
            $stdout.print "JIRA-CLI>> "
            input = gets()
            case input.strip
            when "quit" then break;
            else
                out = cli(sessionId, input)
            end
            $stdout.puts  out["out"] unless out["out"] == ""
            $stderr.puts  out["err"] unless out["err"] == ""
            $stdout.print "JIRA-CLI=> ", out["evalResult"], "\n" 
        end
    ensure
        deleteSession(sessionId) unless sessionId.nil? and not killSessionOnExit
    end
end


authData = login(username, password)
OPTS[:cookies] = {authData["name"] => authData["value"] }
repl()



#
# Copyright 2011 Leonid Maslov<leonidms@gmail.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require 'rubygems'
require 'restclient'
require 'json'

module RESTCli
  class Cli
    public

    attr_accessor :options

    def initialize(options)
      @options = options || {}
      @sessionOpts = {:content_type => :json, :accept => :json}
    end

    def loginUrl
      "#{@options[:proto]}://#{@options[:host]}:#{@options[:port]}/#{@options[:context]}rest/auth/latest/session"
    end

    def cliBaseUrl
      "#{@options[:proto]}://#{@options[:host]}:#{@options[:port]}/#{@options[:context]}rest/rest-scripting/1.0"
    end

    def login(username, password)
      logindata = RestClient.post(loginUrl, {"username" => username, "password" => password}.to_json, {:content_type => :json,
          :accept => :json})
      session = from_json(logindata)["session"]
      @sessionOpts.merge!({:cookies => {session["name"] => session["value"]}})
    end

    def logout()
      cr = RestClient.delete(loginUrl, sessionOpts)
      @sessionOpts.merge!({:cookies => nil})
      cr
    end

    def attachSession
      sessionId = RestClient.put("#{cliBaseUrl}/sessions", {"language" => "ruby"}.to_json, sessionOpts)
      from_json(sessionId)["sessionId"]
    end

    def deleteSession(sessionId)
      raise StandardError, "SessionId should be specified. Empty value passed in" if sessionId.nil? or sessionId == ""
      RestClient.delete("#{cliBaseUrl}/sessions/#{sessionId}", sessionOpts)
    end

    def eval_input(sessionId, script)
      evalResult = RestClient.post("#{cliBaseUrl}/sessions/#{sessionId}", {"script" => script}.to_json, sessionOpts)
      from_json(evalResult)
    end

    def listSessions
      sessions = RestClient.get("#{cliBaseUrl}/sessions", sessionOpts.merge(:params => {"language" => "ruby"}))
      from_json(sessions)["sessions"].map { |e| e["sessionId"] }
    end

    def repl(sessionId = nil, killSessionOnExit = true)
      puts " ***************************************** "
      puts " **  JIRA REST-Ruby-Cli command line    ** "
      puts " **  Type '!q' to exit the loop         ** "
      puts " **  Type '!r' to eval user input       ** "
      puts " ***************************************** "
      sessionId ||= (
      tmp = attachSession()
      $stdout.puts "New session acquired: #{tmp}";
      tmp
      )
      begin
        while true
          case stdinGets.strip
            when "exit" then
              break;
            when "quit" then
              break;
            when "!q" then
              break;
            when "!r" then
              begin
                out = eval_input(sessionId, stdinReset)
                $stdout.puts out["out"] unless out["out"] == ""
                $stderr.puts out["err"] unless out["err"] == ""
                $stdout.print "rest-cli=> ", out["evalResult"], "\n"
              rescue RestClient::Exception => e
                out = from_json(e.http_body)
                message = if out['error'] then
                            out['error'] ## server errors
                          elsif out['errorMessages'] then
                            out['errorMessages'] ## internal app errors (validations, assertions)
                          else
                            out['errors'] && out['errors']['errorMessages'] || ''
                          end ## evaluation errors
                message = message.join("\n") if message.is_a? Array
                $stdout.puts out["out"] unless out["out"] == ""
                $stderr.puts out["err"] unless out["err"] == ""
                $stderr.puts "----\n#{e.message}: #{message}"
              end
          end
        end
      ensure
        if !sessionId.nil? and killSessionOnExit
          $stdout.puts "Session state cleanup: #{sessionId}";
          deleteSession(sessionId)
        end
      end
    end

    private
    def from_json(str)
      JSON::parse str
    end

    attr_accessor :sessionOpts

    def stdin
      @io ||= (
      require 'irb'
      @io = IRB::ReadlineInputMethod.new if defined? IRB::ReadlineInputMethod
      @io ||= IRB::StdioInputMethod.new
      update_prompt(@io)
      )
    end

    def stdinGets
      s = stdin.gets
      update_prompt(stdin)
      s
    end

    def stdinReset
      text = stdin.instance_variable_get('@line')
      stdin.instance_variable_set('@line_no', 0)
      stdin.instance_variable_set('@line', [])
      update_prompt(stdin)
      text.pop ## removing command line input
      text.join('')
    end

    def update_prompt(io)
      s = (io.instance_variable_get('@line_no') + 1).to_s
      io.prompt= "rest-cli(#{"0" * (5-s.length)}#{s})>> "
      io
    end
  end
end

cli = RESTCli::Cli.new({
    :proto => 'http',
    :host => 'localhost',
    :port => 2990,
    :context => 'jira/'
})

cli.login('admin', 'admin')
sessions = cli.listSessions
puts "Active ruby sessions: " + sessions.join(', ') unless sessions.empty?
begin
  cli.repl()
ensure
  cli.logout()
end

require 'rest_client'
require 'json'

(proto, host, port, context) = ["http", "localhost", 2990, "jira/"]

url = "#{proto}://#{host}:#{port}/#{context}rest/rest-scripting/1.0/execute/groovy"


script = File.new(Dir.pwd + "/" + "cli_script.groovy").read
payload = { 'script' => script, 'filename' => 'filename', 'argv' => [] }.to_json
data = RestClient.post(url, payload,
                       :content_type => :json, :accept => :json,
                       :cookies => {"jira.conglomerate.cookie" => "", "JSESSIONID" => "283C0269F172BC30EEE865B52449D0AB"})
pp data
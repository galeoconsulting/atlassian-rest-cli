/*
 * for some reason doesn't work
 *
 * for the sake of argument - implemented the same with the help of Ruby 'rest_client'
 */

import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient

def (proto, host, port, context) = ["http", "localhost", 2990, "jira/"]

def loginUrl = "${proto}://${host}:${port}/${context}rest/"

RESTClient client = new RESTClient(loginUrl)
client.contentType = ContentType.JSON

def HttpResponseDecorator response = client.post(
        path : 'auth/latest/session',
        body : [username: 'admin', password: 'admin'],
        requestContentType : ContentType.JSON)

println response.status
println response.data
println response.data['session']
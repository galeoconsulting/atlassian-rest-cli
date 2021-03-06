##Introduction
Atlassian plugin which provides a possibility to use your favorite programming language to script and interact with Atlassian product server realtime. 
###Supported products

* Jira 4.3
* Confluence 3.5
* Bamboo 3.2.2

###Working modes

* Web-executor interface - allows to execute script input from admin interface, no continuous working session support and no
working context preservation between invocations.
* Web-cli interface - allows to create and manage working scripting sessions from admin interface, connect to them and execute
script code in the scripting session context - state is preserved between invocations.
* The are sample console clients available (Ruby, Groovy) which work similar to interactive language shells (irb, groovysh).
* Sample JIRA configuration scripts.

###Scripting languages supported

* JavaScript (Rhino) shipped with Oracle JDK - default
* Groovy 1.8.0 - separate
* JRuby 1.5.6 - separate.

Languages are implemented as standalone plug-able components, installed separately, except for Rhino available by default.

The target is to come as close to Firebug / IRB / Groovysh as possible :)

You can see some screenshots in action [here](http://leonardinius.blogspot.com/2011/03/release-announcement-jira-rest-cli-05.html).

##Alternatives
* [Python CLI for JIRA](https://plugins.atlassian.com/plugin/details/16346) - basically cli interface to JIRA SOAP interface;
* [Jira Scripting Suite](https://plugins.atlassian.com/plugin/details/16346) - provides a convenient way to put custom conditions,
validators and post-functions into workflow in a form of
Jython scripts..
* [Script Runner](https://plugins.atlassian.com/plugin/details/6820) - provide ability to script (JSR-223 capable) workflow validators,
conditions, etc..

What's wrong with? Actually - nothing. They are great pieces of software and they excellently do they should do - extend JIRA functionality
and provide possibility to easily extend workflows (Jira Scripting Suite, Script Runner) w/o need to restart Jira server, or provide access
to built-in remote access (Python CLI for JIRA).
The thing I've missed here - is to play with Jira API in the realtime, see what's inside of teddy bear looks like and the ability to use
the same approach to automate certain operations (e.g. configuration deployment).

The  atlassian-rest-cli tool at the moment makes a step little bit further - it allows to use the same REST cli infrastructure with the Atlassian product line (see supported products above). 

##How could I use it?
When working with it I have several use-cases in mind:

* Use it as console-tool to script and automate certain configuration changes (local development; staging etc development deployment)
* Use it as a tool to play with Atlassian product system API at realtime (local development needs)

I would really appreciate if you will think out other use-cases and will report them back to me. So do it :)

##How to start?
*  Build project

        git clone git@github.com:leonardinius/atlassian-rest-cli.git
        cd atlassian-rest-cli/
        git submodule init
        git submodule update
        cd rest-cli-parent/
        atlas-mvn clean install

* To start play with the REST Cli - you need to install rest-cli-runner plugin, which is a main entry point and Rhino language
provider.
* To try out JRuby or Groovy language support - install rest-cli-jruby or rest-cli-groovy accordingly. <br/>

**OR** you could get all this artifacts here

* [rest-cli-runner-1.1-SNAPSHOT.jar](http://dl.dropbox.com/u/379506/rest-cli-1.1/rest-cli-runner-1.1-SNAPSHOT.jar) - Script runner, web-console,
session mgmt admin interface, Rhino language support.
* [rest-cli-jruby-1.1-SNAPSHOT.jar](http://dl.dropbox.com/u/379506/rest-cli-1.1/rest-cli-jruby-1.1-SNAPSHOT.jar) - JRuby support
* [rest-cli-groovy-1.1-SNAPSHOT.jar](http://dl.dropbox.com/u/379506/rest-cli-1.1/rest-cli-groovy-1.1-SNAPSHOT.jar) - Groovy support
* [groovy-rest-client-1.1-SNAPSHOT.jar](http://dl.dropbox.com/u/379506/rest-cli-1.1/groovy-rest-client-1.1-SNAPSHOT.jar) - Groovy rest-cli-groovy.sh client, with command line options to execute arbitrary scripts, list/create/delete sessions, start REPL mode. 
* [ruby-rest-client-1.1-SNAPSHOT.jar](http://dl.dropbox.com/u/379506/rest-cli-1.1/ruby-rest-client-1.1-SNAPSHOT.jar) - JRuby rest-cli-jruby.sh client, with command line options to execute arbitrary scripts, list/create/delete sessions, start REPL mode. 
 
NB: You can see some screenshots in action [here](http://leonardinius.blogspot.com/2011/03/release-announcement-jira-rest-cli-05.html).

## License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/galeoconsulting/atlassian-rest-cli/trend.png)](https://bitdeli.com/free "Bitdeli Badge")


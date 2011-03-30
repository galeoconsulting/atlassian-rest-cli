Introduction
=============
JIRA plugin which provides a possibility to use your favorite programming language to script and interact with JIRA server realtime. The plugin provide following working modes:
* JIRA web-executor interface - allows to execute script input from Jira admin interface, no continuos working session support and no working context preservation between invocations.
* JIRA web-cli interface - allows to create and manage working scripting sessions from Jira admin interface, connect to them and execute script code in the scripting session context - state is preserved between invocations.
* The are sample console clients available (Ruby, Groovy) which works similar to interactive language shells (irb, groovysh).

At the moment following programming languages are supported:
* JavaScript (Rhino) shipped with Oracle JDK - default
* Groovy 1.7.9 - separate
* JRuby 1.5.6 - separate.

Languages are implemented as standalone plug-able components, installed separately, except for Rhino available by default.

The target is to come as close to Firebug / IRB / Groovysh as possible :)

Alternatives
=============
* [Python CLI for JIRA](https://plugins.atlassian.com/plugin/details/16346) - basically cli interface to JIRA SOAP interface;
* [Jira Scripting Suite](https://plugins.atlassian.com/plugin/details/16346) - provides a convenient way to put custom conditions, validators and post-functions into workflow in a form of
Jython scripts..
* [Script Runner](https://plugins.atlassian.com/plugin/details/6820) - provide ability to script (JSR-223 capable) workflow validators, conditions, etc..

What's wrong with? Actually - nothing. They are great pieces of software and they excellently do they should do - extend JIRA functionality and provide possibility to easily extend workflows (Jira Scripting Suite, Script Runner) w/o need to restart Jira server, or provide access to built-in remote access (Python CLI for JIRA).
The thing I've missed here - is to play with Jira API in the realtime, see what's inside of teddy bear looks like (latvian: kas lācītim vēderā) and the ability to use the same approach to automate certain operations.

How could I use it?
=============
When working with it I have several use-cases in mind:
Use it as console-tool to script and automate certain configuration changes (local development; staging etc development rollout)
Use it as a tool to play with JIRA system API at realtime (local development needs)
I would really appreciate if you will think out other use-cases and will report them back to me. So do it :)

How to start?
=============
1.  Build project
    git clone git@github.com:leonardinius/jira-rest-cli.git
    cd jira-rest-cli/
    git submodule init
    cd jira-rest-cli-parent/
    atlas-mvn clean install
1. To start play with the REST Cli - you need to install jira-rest-cli-runner plugin, which is a main entry point and Rhino language
provider.
1. If you want to try out JRuby or Groovy language support - then you should install jira-rest-cli-jruby or jira-rest-cli-groovy
accordingly.
NB: on my local dev environment I install all the plugins using atlas-cli since I launch JIRA using atlas-run / atlas-debug commands.

Ok, it's useful. How could I help?
=============
* High priority: Documentation - both creating one and understanding what actually is missing. Even GitHub issues for this are welcome.
* Medium priority: Upload plugin to Atlassian Plugin Exchange
* Medium priority: Improving console-cli modes sample applications (JRuby, Groovy) - cleaning up, since I'm not expert in those languages; adding command line options.
* Low priority: improving web-interface (more Ajax-like) etc...
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

class Deployer
{
    @Lazy
    Properties properties = {
        Properties props = new Properties();
        props.load(new FileInputStream('deploy.properties'));
        props
    }()

    def args(def arguments)
    {
        def quote = { return "\"${it}\"" }
        def allarguments = [
                '-proto', quote(properties.proto), //
                '-h', quote(properties.host), //
                '-p', quote(properties.port), //
                '-c', quote(properties.context), //
                '-u', quote(properties.username), //
                '-w', quote(properties.password), //
        ];

        allarguments.addAll(arguments.collect { it.startsWith('-') ? it : quote(it) })
        allarguments
    }

    def cmd(def arguments)
    {
        "${properties.shell} ${properties.'groovy-rest-client.path'} ${args(arguments).join(' ')}"
    }

    def exec(def arguments)
    {
        def command = cmd(arguments)
        println "Executing: ${command}"
        Process process = command.execute()

        StringBuffer out = new StringBuffer()
        StringBuffer err = new StringBuffer()
        process.waitForProcessOutput(out, err)
        [out.toString(), err.toString()]
    }

    def pp(def outs, boolean outputOut = true)
    {
        if (outputOut) println outs[0]
        if (outs[1] && outs[1] != '')
        {
            throw new IllegalStateException(outs[1])
        }
        outs
    }

    def getSessionId()
    {
        def outs = exec(["-n"])
        pp(outs, false)
        def sessionId = outs[0].trim()
        println "Session id: ${sessionId}"; sessionId
    }

    def deleteSessionId(def sessionId)
    {
        println "Cleaning up / closing up session."
        pp exec(["-d", sessionId])
    }

    def eval_file(String file, def sessionId)
    {
        pp exec(["-s", sessionId, "-f", file])
    }

    def listSripts(Number version, String path)
    {
        File dir = new File(path)
        if (!dir.isDirectory()) throw new IllegalArgumentException("Path `${path}` is not a directory.")

        dir.listFiles({File d, String name -> return name.endsWith(".groovy")} as FilenameFilter)  //
                .findAll { it.name.matches("\\d{1,}_.*\\.groovy") && scriptVersion(it.name) > version}  //
                .sort({File f1, File f2 -> f1.name.compareTo(f2.name)} as Comparator)  //
    }


    def scriptVersion(String filename)
    {
        Long.parseLong(filename.split('_')[0].trim(), 10)
    }

    def runtimeConfigVersion(def versionScript, String sessionId)
    {
        return Long.parseLong(eval_file(versionScript, sessionId)[0].trim().substring("rest-cli=>".length()).trim(), 10)
    }

    def deploy(Map options)
    {
        def sessionId = getSessionId()
        try
        {
            for (def lib in options.toolkit)
                eval_file(lib, sessionId)

            def version = runtimeConfigVersion(options.versionScript, sessionId)
            println "Configuration version in the runtime: ${version}"

            for (File script in listSripts(version, options.scripts))
                run_script(script, version, sessionId)
        }
        finally
        {
            deleteSessionId(sessionId)
        }
    }

    def run_script(File script, long version, String sessionId)
    {
        def parts = { it.split('\\.') }
        File tmp = File.createTempFile(parts(script.name)[0..-2].join('.'), '.groovy')
        try
        {
            tmp.write("""
            def configuration = ${new String(script.readBytes())};
            println ">> Applying ${scriptVersion(script.name)} configuration (${script.name})"
            configuration.${version < scriptVersion(script.name) ? 'execute' : 'undo'}();
            ">> Ok."
            """)
            return eval_file(tmp.path, sessionId)
        }
        finally
        {
            tmp.delete()
        }
    }
}


Deployer deployer = new Deployer()
deployer.deploy(
        toolkit: ['scripts/MigrationHelper.groovy'],
        versionScript: 'scripts/Version.groovy',
        scripts: 'scripts')




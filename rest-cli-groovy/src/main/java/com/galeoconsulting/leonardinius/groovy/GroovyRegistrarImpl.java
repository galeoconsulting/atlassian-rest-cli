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

package com.galeoconsulting.leonardinius.groovy;

import com.atlassian.sal.api.user.UserManager;
import com.galeoconsulting.leonardinius.api.Registrar;
import com.galeoconsulting.leonardinius.api.ScriptService;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.script.ScriptEngine;

/**
 * User: leonidmaslov
 * Date: 3/13/11
 * Time: 12:37 AM
 */
public class GroovyRegistrarImpl implements Registrar, InitializingBean, DisposableBean {
// ------------------------------ FIELDS ------------------------------

    private final ScriptService scriptService;
    private final GroovyScriptEngineFactory scriptEngineFactory;

    private GroovyClassLoader gcl;
    private final Object lock = new Object();

// --------------------------- CONSTRUCTORS ---------------------------

    public GroovyRegistrarImpl(ScriptService scriptService) {
        this.scriptService = scriptService;

        this.scriptEngineFactory = new GroovyScriptEngineFactory() {
            @Override
            public ScriptEngine getScriptEngine() {
                GroovyScriptEngineImpl engine = new GroovyScriptEngineImpl();
                engine.setClassLoader(getClassLoader());
                return engine;
            }
        };
    }

    private GroovyClassLoader getClassLoader() {
        if (gcl == null) {
            synchronized (lock) {
                if (gcl == null) {
                    final ClassLoader chainedClassLoader = this.scriptService.getClassLoader(
                            getClass().getClassLoader(),
                            Script.class.getClassLoader(),
                            UserManager.class.getClassLoader(),
                            ClassLoader.getSystemClassLoader());


                    gcl = new GroovyClassLoader(chainedClassLoader);
                }
            }
        }
        return gcl;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception {
        scriptService.removeEngine(scriptEngineFactory);
    }

// --------------------- Interface InitializingBean ---------------------

    @Override
    public void afterPropertiesSet() throws Exception {
        scriptService.defaultRegistration(scriptEngineFactory);
    }
}


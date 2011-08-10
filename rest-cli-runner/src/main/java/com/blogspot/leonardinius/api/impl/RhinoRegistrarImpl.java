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

package com.blogspot.leonardinius.api.impl;

import com.atlassian.plugin.util.ClassLoaderUtils;
import com.blogspot.leonardinius.api.Registrar;
import com.blogspot.leonardinius.api.ScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.script.ScriptEngineFactory;

/**
 * User: 23059892
 * Date: 3/15/11
 * Time: 3:31 PM
 */
public class RhinoRegistrarImpl implements Registrar, InitializingBean, DisposableBean
{
// ------------------------------ FIELDS ------------------------------

    private static final String RHINO_ENGINE_CLASS = "com.sun.script.javascript.RhinoScriptEngineFactory";
    private static final Logger LOG = LoggerFactory.getLogger(RhinoRegistrarImpl.class);

    private final ScriptService scriptService;
    private ScriptEngineFactory engineFactory;

    private final Object lock = new Object();

// --------------------------- CONSTRUCTORS ---------------------------

    public RhinoRegistrarImpl(ScriptService scriptService)
    {
        this.scriptService = scriptService;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    private ScriptEngineFactory getEngineFactory() throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        if (engineFactory == null)
        {
            synchronized (lock)
            {
                if (engineFactory == null)
                {
                    Class<? extends ScriptEngineFactory> klazz = ClassLoaderUtils.loadClass(RHINO_ENGINE_CLASS, getClass());
                    engineFactory = klazz.newInstance();
                }
            }
        }

        return engineFactory;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------

    @Override
    public void destroy() throws Exception
    {
        synchronized (lock)
        {
            if (engineFactory != null)
            {
                scriptService.removeEngine(engineFactory);
            }
            engineFactory = null;
        }
    }

// --------------------- Interface InitializingBean ---------------------

    @Override
    public void afterPropertiesSet() throws Exception
    {
        ScriptEngineFactory factory;
        try
        {
            factory = getEngineFactory();
        }
        catch (ClassNotFoundException e)
        {
            reportInstantiateEngineError(e);
            return;
        }
        catch (IllegalAccessException e)
        {
            reportInstantiateEngineError(e);
            return;
        }
        catch (InstantiationException e)
        {
            reportInstantiateEngineError(e);
            return;
        }
        scriptService.defaultRegistration(factory);
    }

// -------------------------- OTHER METHODS --------------------------

    private void reportInstantiateEngineError(Throwable e)
    {
        LOG.error(String.format("" +
                "Could not instantiate shipped with Sun/Oracle JRE " +
                "javascript jsr223 engine factory." +
                "Engine class: %1$s.", RHINO_ENGINE_CLASS), e);
    }
}

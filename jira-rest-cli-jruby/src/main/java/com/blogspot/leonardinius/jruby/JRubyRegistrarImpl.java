package com.blogspot.leonardinius.jruby;

import com.blogspot.leonardinius.api.Registrar;
import com.blogspot.leonardinius.api.ScriptService;
import org.jruby.embed.jsr223.JRubyEngineFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * User: leonidmaslov
 * Date: 3/13/11
 * Time: 12:37 AM
 */
public class JRubyRegistrarImpl implements Registrar, InitializingBean, DisposableBean
{
// ------------------------------ FIELDS ------------------------------

    private final ScriptService scriptService;
    private final JRubyEngineFactory engineFactory;

// --------------------------- CONSTRUCTORS ---------------------------

    public JRubyRegistrarImpl(ScriptService scriptService)
    {
        this.scriptService = scriptService;
        engineFactory = new JRubyEngineFactory();
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DisposableBean ---------------------


    @Override
    public void destroy() throws Exception
    {
        scriptService.removeEngine(engineFactory);
    }

// --------------------- Interface InitializingBean ---------------------

    @Override
    public void afterPropertiesSet() throws Exception
    {
        scriptService.defaultRegistration(engineFactory);
    }
}

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
        /* the following property is necessary for ruby, otherwise we can't retrieve the variables set in ruby
               http://yokolet.blogspot.com/2009/08/redbridge-and-jruby-embed-api-update.html
        */
        System.setProperty("org.jruby.embed.localvariable.behavior", "persistent");
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

package com.blogspot.leonardinius.jruby;

import com.blogspot.leonardinius.api.Registrar;
import com.blogspot.leonardinius.api.ScriptService;
import org.jruby.embed.jsr223.JRubyEngineFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * User: leonidmaslov
 * Date: 3/13/11
 * Time: 12:37 AM
 */
public class JRubyRegistrarImpl implements Registrar, InitializingBean
{
// ------------------------------ FIELDS ------------------------------

    private final ScriptService scriptService;

// --------------------------- CONSTRUCTORS ---------------------------

    public JRubyRegistrarImpl(ScriptService scriptService)
    {
        this.scriptService = scriptService;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface InitializingBean ---------------------

    @Override
    public void afterPropertiesSet() throws Exception
    {
        scriptService.defaultRegistration(new JRubyEngineFactory());
    }
}

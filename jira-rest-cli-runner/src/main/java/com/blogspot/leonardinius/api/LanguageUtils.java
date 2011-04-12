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

package com.blogspot.leonardinius.api;

import org.apache.commons.lang.StringUtils;

import javax.script.ScriptEngineFactory;

/**
 * User: leonidmaslov
 * Date: 3/15/11
 * Time: 10:02 PM
 */
public class LanguageUtils
{
// -------------------------- STATIC METHODS --------------------------

    public static String getVersionString(ScriptEngineFactory factory)
    {
        return factory.getLanguageVersion();
    }

    public static String getLanguageName(ScriptEngineFactory factory)
    {
        String languageName = factory.getLanguageName();
        if ("ECMAScript".equals(languageName)
                && factory.getNames().contains("JavaScript"))
        {
            languageName = "JavaScript (Rhino)";
        }
        return StringUtils.capitalize(languageName);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private LanguageUtils()
    {
    }
}

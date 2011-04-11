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

package com.blogspot.leonardinius.groovy;

/**
 * User: 23059892
 * Date: 4/11/11
 * Time: 12:21 PM
 */

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * A class loader that delegates to a list of class loaders. The order is important as classes and resources will be
 * loaded from the first classloader that can load them.
 */
public class ChainingClassLoader extends ClassLoader
{
// ------------------------------ FIELDS ------------------------------

    private static final Logger log = LoggerFactory.getLogger(ChainingClassLoader.class);

    /**
     * The list of classloader to delegate to.
     */
    private final List<ClassLoader> classLoaders;

// --------------------------- CONSTRUCTORS ---------------------------

    public ChainingClassLoader(ClassLoader... classLoaders)
    {
        Validate.noNullElements(classLoaders, "ClassLoader arguments cannot be null");
        this.classLoaders = Arrays.asList(classLoaders);
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public synchronized void clearAssertionStatus()
    {
        for (ClassLoader classloader : classLoaders)
        {
            classloader.clearAssertionStatus();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        for (ClassLoader classloader : classLoaders)
        {
            try
            {
                return callFindClass(classloader, name);
            }
            catch (ClassNotFoundException e)
            {
                // ignoring until we reach the end of the list since we are chaining
            }
        }

        throw new ClassNotFoundException(name);
    }

    private Class<?> callFindClass(ClassLoader classloader, String name) throws ClassNotFoundException
    {
        try
        {
            Class<?> classInstance = (Class<?>) MethodsHolder.findClassMethod.invoke(classloader, name);
            if (classInstance != null)
            {
                return classInstance;
            }
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError(e); //unexpected
        }
        catch (InvocationTargetException e)
        {
            throw new AssertionError(e); //unexpected
        }
    }

    @Override
    public URL getResource(String name)
    {
        for (ClassLoader classloader : classLoaders)
        {
            final URL url = classloader.getResource(name);
            if (url != null)
            {
                return url;
            }
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        for (ClassLoader classloader : classLoaders)
        {
            final InputStream inputStream = classloader.getResourceAsStream(name);
            if (inputStream != null)
            {
                return inputStream;
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        return new ResourcesEnumeration(name, classLoaders);
    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException
    {
        for (ClassLoader classloader : classLoaders)
        {
            try
            {
                return classloader.loadClass(name);
            }
            catch (ClassNotFoundException e)
            {
                // ignoring until we reach the end of the list since we are chaining
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public synchronized void setClassAssertionStatus(String className, boolean enabled)
    {
        for (ClassLoader classloader : classLoaders)
        {
            classloader.setClassAssertionStatus(className, enabled);
        }
    }

    @Override
    public synchronized void setDefaultAssertionStatus(boolean enabled)
    {
        for (ClassLoader classloader : classLoaders)
        {
            classloader.setDefaultAssertionStatus(enabled);
        }
    }

    @Override
    public synchronized void setPackageAssertionStatus(String packageName, boolean enabled)
    {
        for (ClassLoader classloader : classLoaders)
        {
            classloader.setPackageAssertionStatus(packageName, enabled);
        }
    }

// -------------------------- INNER CLASSES --------------------------

    private static final class MethodsHolder
    {
        private static final Method findClassMethod;

        static
        {
            // single check idiom 
            // it's safe to recalculate same method several times (on concurrent access)
            try
            {
                findClassMethod = ClassLoader.class.getDeclaredMethod("findClass", String.class);
                findClassMethod.setAccessible(true);
            }
            catch (NoSuchMethodException e)
            {
                throw new AssertionError(e); //unexpected
            }
        }
    }

    private static final class ResourcesEnumeration implements Enumeration<URL>
    {
        private final List<Enumeration<URL>> resources;
        private final String resourceName;

        ResourcesEnumeration(String resourceName, List<ClassLoader> classLoaders) throws IOException
        {
            this.resourceName = resourceName;
            this.resources = new LinkedList<Enumeration<URL>>();
            for (ClassLoader classLoader : classLoaders)
            {
                resources.add(classLoader.getResources(resourceName));
            }
        }

        public boolean hasMoreElements()
        {
            for (Enumeration<URL> resource : resources)
            {
                if (resource.hasMoreElements())
                {
                    return true;
                }
            }

            return false;
        }

        public URL nextElement()
        {
            for (Enumeration<URL> resource : resources)
            {
                if (resource.hasMoreElements())
                {
                    return resource.nextElement();
                }
            }
            throw new NoSuchElementException(resourceName);
        }
    }
}
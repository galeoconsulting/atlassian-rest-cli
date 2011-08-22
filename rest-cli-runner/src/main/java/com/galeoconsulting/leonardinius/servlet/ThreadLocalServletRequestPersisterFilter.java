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

package com.galeoconsulting.leonardinius.servlet;

import com.galeoconsulting.leonardinius.api.ServletRequestHolder;

import javax.servlet.*;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: leonidmaslov
 * Date: 8/22/11
 * Time: 11:55 PM
 */
public class ThreadLocalServletRequestPersisterFilter implements Filter
{
    private final ServletRequestHolder httpServletRequestHolder;

    public ThreadLocalServletRequestPersisterFilter(ServletRequestHolder httpServletRequestHolder)
    {
        this.httpServletRequestHolder =
                checkNotNull(httpServletRequestHolder, "httpServletRequestHolder");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        //no-op
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        try
        {
            httpServletRequestHolder.set(servletRequest, servletResponse);
            filterChain.doFilter(servletRequest, servletResponse);
        }
        finally
        {
            httpServletRequestHolder.remove();
        }
    }

    @Override
    public void destroy()
    {
        httpServletRequestHolder.remove();
    }
}

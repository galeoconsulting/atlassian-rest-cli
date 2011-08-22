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

package com.galeoconsulting.leonardinius.api.impl;

import com.galeoconsulting.leonardinius.api.ServletRequestHolder;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * User: leonidmaslov
 * Date: 8/23/11
 * Time: 12:00 AM
 */
public class ServletRequestHolderImpl implements ServletRequestHolder
{
    private final ThreadLocal<ServletRequest> requestHolder = new ThreadLocal<ServletRequest>();
    private final ThreadLocal<ServletResponse> responseHolder = new ThreadLocal<ServletResponse>();

    @Override
    public void set(ServletRequest request, ServletResponse response)
    {
        requestHolder.set(request);
        responseHolder.set(response);
    }

    @Override
    public ServletRequest getRequest()
    {
        return requestHolder.get();
    }

    @Override
    public ServletResponse getResponse()
    {
        return responseHolder.get();
    }

    @Override
    public void remove()
    {
        requestHolder.remove();
        responseHolder.remove();
    }
}

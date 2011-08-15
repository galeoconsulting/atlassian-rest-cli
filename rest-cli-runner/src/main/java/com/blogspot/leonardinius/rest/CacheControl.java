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

package com.blogspot.leonardinius.rest;

@SuppressWarnings({"UnusedDeclaration"})
public class CacheControl
{
// ------------------------------ FIELDS ------------------------------

    /**
     * Provides a cacheControl with noStore and noCache set to true
     */
    public static final javax.ws.rs.core.CacheControl NO_CACHE = new javax.ws.rs.core.CacheControl();

    /**
     * Provides a cacheControl with a 1 year limit.  Effectively forever.
     */
    public static final javax.ws.rs.core.CacheControl CACHE_FOREVER = new javax.ws.rs.core.CacheControl();
    // HTTP spec limits the max-age directive to one year.
    private static final int ONE_YEAR = 60 * 60 * 24 * 365;

// -------------------------- STATIC METHODS --------------------------

    static
    {
        NO_CACHE.setNoStore(true);
        NO_CACHE.setNoCache(true);
    }

    static
    {
        CACHE_FOREVER.setPrivate(false);
        CACHE_FOREVER.setMaxAge(ONE_YEAR);
    }
}

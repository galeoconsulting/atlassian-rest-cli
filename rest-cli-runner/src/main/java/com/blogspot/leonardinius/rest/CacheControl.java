package com.blogspot.leonardinius.rest;

@SuppressWarnings({"UnusedDeclaration"})
public class CacheControl {
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

    static {
        NO_CACHE.setNoStore(true);
        NO_CACHE.setNoCache(true);
    }

    static {
        CACHE_FOREVER.setPrivate(false);
        CACHE_FOREVER.setMaxAge(ONE_YEAR);
    }
}

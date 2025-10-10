// Core/Cache/BolsaCache.java
package com.farenet.descuentos.Core.Cache;

public final class BolsaCache {
    // Guardamos el objeto crudo que devuelva Retrofit (DTO real)
    private static volatile Object snapshot;
    private static volatile long fetchedAtMs = -1L;

    private BolsaCache() {}

    public static void set(Object dto) {
        snapshot = dto;
        fetchedAtMs = System.currentTimeMillis();
    }

    public static Object get() {
        return snapshot;
    }

    public static long getFetchedAtMs() {
        return fetchedAtMs;
    }

    public static boolean hasData() {
        return snapshot != null;
    }

    public static void clear() {
        snapshot = null;
        fetchedAtMs = -1L;
    }
}

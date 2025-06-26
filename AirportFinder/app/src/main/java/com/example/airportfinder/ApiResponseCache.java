package com.example.airportfinder;

import android.util.LruCache;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache for API responses to reduce network calls and improve app performance.
 * Implements time-based expiration for cached data.
 */
public class ApiResponseCache {
    private static final String TAG = "ApiResponseCache";
    
    // Cache expiration times (in minutes)
    private static final long METAR_CACHE_EXPIRY_MINUTES = 15; // METARs typically updated every 30-60 minutes
    private static final long TAF_CACHE_EXPIRY_MINUTES = 60;   // TAFs typically updated every 6 hours
    private static final long NOTAM_CACHE_EXPIRY_MINUTES = 120; // NOTAMs can be less frequent
    
    // Cache size (number of entries)
    private static final int CACHE_SIZE = 50;
    
    // Singleton instance
    private static ApiResponseCache instance;
    
    // LRU Caches for different data types
    private final LruCache<String, CacheEntry<WeatherData>> metarCache;
    private final LruCache<String, CacheEntry<WeatherData>> tafCache;
    private final LruCache<String, CacheEntry<List<NotamData>>> notamCache;
    
    /**
     * Get the singleton instance of the cache
     */
    public static synchronized ApiResponseCache getInstance() {
        if (instance == null) {
            instance = new ApiResponseCache();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private ApiResponseCache() {
        metarCache = new LruCache<>(CACHE_SIZE);
        tafCache = new LruCache<>(CACHE_SIZE);
        notamCache = new LruCache<>(CACHE_SIZE);
    }
    
    /**
     * Cache a METAR response
     * 
     * @param icaoCode The airport ICAO code
     * @param weatherData The METAR data to cache
     */
    public void cacheMetar(String icaoCode, WeatherData weatherData) {
        if (icaoCode == null || weatherData == null) return;
        
        metarCache.put(icaoCode, new CacheEntry<>(weatherData, 
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(METAR_CACHE_EXPIRY_MINUTES)));
    }
    
    /**
     * Get cached METAR data if available and not expired
     * 
     * @param icaoCode The airport ICAO code
     * @return The cached METAR data, or null if not found or expired
     */
    public WeatherData getCachedMetar(String icaoCode) {
        if (icaoCode == null) return null;
        
        CacheEntry<WeatherData> entry = metarCache.get(icaoCode);
        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }
        
        // Remove expired entry
        if (entry != null && entry.isExpired()) {
            metarCache.remove(icaoCode);
        }
        
        return null;
    }
    
    /**
     * Cache a TAF response
     * 
     * @param icaoCode The airport ICAO code
     * @param weatherData The TAF data to cache
     */
    public void cacheTaf(String icaoCode, WeatherData weatherData) {
        if (icaoCode == null || weatherData == null) return;
        
        tafCache.put(icaoCode, new CacheEntry<>(weatherData, 
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(TAF_CACHE_EXPIRY_MINUTES)));
    }
    
    /**
     * Get cached TAF data if available and not expired
     * 
     * @param icaoCode The airport ICAO code
     * @return The cached TAF data, or null if not found or expired
     */
    public WeatherData getCachedTaf(String icaoCode) {
        if (icaoCode == null) return null;
        
        CacheEntry<WeatherData> entry = tafCache.get(icaoCode);
        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }
        
        // Remove expired entry
        if (entry != null && entry.isExpired()) {
            tafCache.remove(icaoCode);
        }
        
        return null;
    }
    
    /**
     * Cache a NOTAM response
     * 
     * @param icaoCode The airport ICAO code
     * @param notamList The NOTAM data to cache
     */
    public void cacheNotams(String icaoCode, List<NotamData> notamList) {
        if (icaoCode == null || notamList == null) return;
        
        notamCache.put(icaoCode, new CacheEntry<>(notamList, 
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(NOTAM_CACHE_EXPIRY_MINUTES)));
    }
    
    /**
     * Get cached NOTAM data if available and not expired
     * 
     * @param icaoCode The airport ICAO code
     * @return The cached NOTAM data, or null if not found or expired
     */
    public List<NotamData> getCachedNotams(String icaoCode) {
        if (icaoCode == null) return null;
        
        CacheEntry<List<NotamData>> entry = notamCache.get(icaoCode);
        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }
        
        // Remove expired entry
        if (entry != null && entry.isExpired()) {
            notamCache.remove(icaoCode);
        }
        
        return null;
    }
    
    /**
     * Clear all cached data
     */
    public void clearAllCaches() {
        metarCache.evictAll();
        tafCache.evictAll();
        notamCache.evictAll();
    }
    
    /**
     * Clear cached data for a specific ICAO code
     * 
     * @param icaoCode The airport ICAO code
     */
    public void clearCacheForIcao(String icaoCode) {
        if (icaoCode == null) return;
        
        metarCache.remove(icaoCode);
        tafCache.remove(icaoCode);
        notamCache.remove(icaoCode);
    }
    
    /**
     * Generic cache entry with expiration time
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long expiryTimeMillis;
        
        CacheEntry(T data, long expiryTimeMillis) {
            this.data = data;
            this.expiryTimeMillis = expiryTimeMillis;
        }
        
        T getData() {
            return data;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTimeMillis;
        }
    }
}

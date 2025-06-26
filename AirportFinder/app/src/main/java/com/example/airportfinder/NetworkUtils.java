package com.example.airportfinder;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Utility class for network-related operations
 */
public class NetworkUtils {
    
    /**
     * Check if the device has an active internet connection
     * 
     * @param context Application context
     * @return true if connected to the internet, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());
            
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            // For older Android versions
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
    
    /**
     * Check if the device is connected to WiFi
     * 
     * @param context Application context
     * @return true if connected to WiFi, false otherwise
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());
            
            return capabilities != null && 
                   capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            // For older Android versions
            NetworkInfo wifiNetworkInfo = connectivityManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetworkInfo != null && wifiNetworkInfo.isConnected();
        }
    }
}

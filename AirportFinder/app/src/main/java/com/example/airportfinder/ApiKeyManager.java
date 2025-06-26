package com.example.airportfinder;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Utility class for securely storing and retrieving API keys using Android's
 * EncryptedSharedPreferences backed by the Android Keystore system.
 */
public class ApiKeyManager {
    private static final String TAG = "ApiKeyManager";
    private static final String ENCRYPTED_PREFS_FILE_NAME = "secure_api_keys";
    private static final String AVWX_API_KEY = "avwx_api_key";
    private static final String AUTOROUTER_API_KEY = "autorouter_api_key";

    private final Context context;
    private EncryptedSharedPreferences encryptedPrefs;

    public ApiKeyManager(Context context) {
        this.context = context;
        initEncryptedSharedPreferences();
    }

    /**
     * Initialize EncryptedSharedPreferences with a master key from the Android Keystore
     */
    private void initEncryptedSharedPreferences() {
        try {
            // Create or retrieve the Master Key for encryption/decryption
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // Create the EncryptedSharedPreferences using the MasterKey
            encryptedPrefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences", e);
            // Fallback to regular SharedPreferences is not recommended for sensitive data
            // In a production app, you might want to implement a fallback strategy or notify the user
        }
    }

    /**
     * Store the AVWX API key securely
     * @param apiKey The API key to store
     * @return true if successful, false otherwise
     */
    public boolean storeAvwxApiKey(String apiKey) {
        if (encryptedPrefs == null) {
            Log.e(TAG, "EncryptedSharedPreferences not initialized");
            return false;
        }
        
        try {
            encryptedPrefs.edit().putString(AVWX_API_KEY, apiKey).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error storing AVWX API key", e);
            return false;
        }
    }

    /**
     * Retrieve the stored AVWX API key
     * @return The stored API key, or null if not found or error
     */
    public String getAvwxApiKey() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "EncryptedSharedPreferences not initialized");
            return null;
        }
        
        try {
            return encryptedPrefs.getString(AVWX_API_KEY, null);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving AVWX API key", e);
            return null;
        }
    }

    /**
     * Store the Autorouter API key securely
     * @param apiKey The API key to store
     * @return true if successful, false otherwise
     */
    public boolean storeAutoRouterApiKey(String apiKey) {
        if (encryptedPrefs == null) {
            Log.e(TAG, "EncryptedSharedPreferences not initialized");
            return false;
        }
        
        try {
            encryptedPrefs.edit().putString(AUTOROUTER_API_KEY, apiKey).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error storing Autorouter API key", e);
            return false;
        }
    }

    /**
     * Retrieve the stored Autorouter API key
     * @return The stored API key, or null if not found or error
     */
    public String getAutoRouterApiKey() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "EncryptedSharedPreferences not initialized");
            return null;
        }
        
        try {
            return encryptedPrefs.getString(AUTOROUTER_API_KEY, null);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving Autorouter API key", e);
            return null;
        }
    }

    /**
     * Check if the AVWX API key is stored
     * @return true if the key exists, false otherwise
     */
    public boolean hasAvwxApiKey() {
        return getAvwxApiKey() != null;
    }

    /**
     * Check if the Autorouter API key is stored
     * @return true if the key exists, false otherwise
     */
    public boolean hasAutoRouterApiKey() {
        return getAutoRouterApiKey() != null;
    }

    /**
     * Remove all stored API keys
     */
    public void clearAllApiKeys() {
        if (encryptedPrefs == null) {
            Log.e(TAG, "EncryptedSharedPreferences not initialized");
            return;
        }
        
        try {
            encryptedPrefs.edit().clear().apply();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing API keys", e);
        }
    }
}

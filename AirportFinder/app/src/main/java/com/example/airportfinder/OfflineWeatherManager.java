package com.example.airportfinder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages offline storage and retrieval of aviation weather data
 * to ensure app functionality when internet is not available
 */
public class OfflineWeatherManager {
    private static final String TAG = "OfflineWeatherManager";
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "offline_weather_data";
    private static final String KEY_METAR_PREFIX = "metar_";
    private static final String KEY_TAF_PREFIX = "taf_";
    private static final String KEY_NOTAM_PREFIX = "notam_";
    private static final String KEY_LAST_UPDATED_PREFIX = "last_updated_";
    
    private final Context context;
    private final SharedPreferences sharedPreferences;
    
    /**
     * Constructor
     * 
     * @param context Application context
     */
    public OfflineWeatherManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Save METAR data for offline use
     * 
     * @param icaoCode Airport ICAO code
     * @param weatherData METAR data to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveMetarOffline(String icaoCode, WeatherData weatherData) {
        if (icaoCode == null || weatherData == null) {
            return false;
        }
        
        try {
            JSONObject json = new JSONObject();
            json.put("icaoCode", weatherData.getIcaoCode());
            json.put("rawText", weatherData.getRawText());
            json.put("decodedText", weatherData.getDecodedText());
            json.put("timestamp", weatherData.getTimestamp());
            json.put("type", weatherData.getType());
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_METAR_PREFIX + icaoCode, json.toString());
            editor.putLong(KEY_LAST_UPDATED_PREFIX + icaoCode, System.currentTimeMillis());
            editor.apply();
            
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Error saving METAR data offline", e);
            return false;
        }
    }
    
    /**
     * Retrieve saved METAR data for offline use
     * 
     * @param icaoCode Airport ICAO code
     * @return Saved METAR data, or null if not found
     */
    public WeatherData getOfflineMetar(String icaoCode) {
        if (icaoCode == null) {
            return null;
        }
        
        String jsonString = sharedPreferences.getString(KEY_METAR_PREFIX + icaoCode, null);
        if (jsonString == null) {
            return null;
        }
        
        try {
            JSONObject json = new JSONObject(jsonString);
            
            WeatherData weatherData = new WeatherData();
            weatherData.setIcaoCode(json.optString("icaoCode", ""));
            weatherData.setRawText(json.optString("rawText", ""));
            weatherData.setDecodedText(json.optString("decodedText", ""));
            weatherData.setTimestamp(json.optString("timestamp", ""));
            weatherData.setType(json.optInt("type", WeatherData.TYPE_METAR));
            
            return weatherData;
        } catch (JSONException e) {
            Log.e(TAG, "Error retrieving offline METAR data", e);
            return null;
        }
    }
    
    /**
     * Save TAF data for offline use
     * 
     * @param icaoCode Airport ICAO code
     * @param weatherData TAF data to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveTafOffline(String icaoCode, WeatherData weatherData) {
        if (icaoCode == null || weatherData == null) {
            return false;
        }
        
        try {
            JSONObject json = new JSONObject();
            json.put("icaoCode", weatherData.getIcaoCode());
            json.put("rawText", weatherData.getRawText());
            json.put("decodedText", weatherData.getDecodedText());
            json.put("timestamp", weatherData.getTimestamp());
            json.put("type", weatherData.getType());
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_TAF_PREFIX + icaoCode, json.toString());
            editor.putLong(KEY_LAST_UPDATED_PREFIX + icaoCode, System.currentTimeMillis());
            editor.apply();
            
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Error saving TAF data offline", e);
            return false;
        }
    }
    
    /**
     * Retrieve saved TAF data for offline use
     * 
     * @param icaoCode Airport ICAO code
     * @return Saved TAF data, or null if not found
     */
    public WeatherData getOfflineTaf(String icaoCode) {
        if (icaoCode == null) {
            return null;
        }
        
        String jsonString = sharedPreferences.getString(KEY_TAF_PREFIX + icaoCode, null);
        if (jsonString == null) {
            return null;
        }
        
        try {
            JSONObject json = new JSONObject(jsonString);
            
            WeatherData weatherData = new WeatherData();
            weatherData.setIcaoCode(json.optString("icaoCode", ""));
            weatherData.setRawText(json.optString("rawText", ""));
            weatherData.setDecodedText(json.optString("decodedText", ""));
            weatherData.setTimestamp(json.optString("timestamp", ""));
            weatherData.setType(json.optInt("type", WeatherData.TYPE_TAF));
            
            return weatherData;
        } catch (JSONException e) {
            Log.e(TAG, "Error retrieving offline TAF data", e);
            return null;
        }
    }
    
    /**
     * Save NOTAM data for offline use
     * 
     * @param icaoCode Airport ICAO code
     * @param notamList List of NOTAM data to save
     * @return true if saved successfully, false otherwise
     */
    public boolean saveNotamsOffline(String icaoCode, List<NotamData> notamList) {
        if (icaoCode == null || notamList == null) {
            return false;
        }
        
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (NotamData notam : notamList) {
                JSONObject notamJson = new JSONObject();
                notamJson.put("id", notam.getId());
                notamJson.put("text", notam.getText());
                notamJson.put("startTime", notam.getStartTime());
                notamJson.put("endTime", notam.getEndTime());
                notamJson.put("type", notam.getType());
                
                jsonArray.put(notamJson);
            }
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_NOTAM_PREFIX + icaoCode, jsonArray.toString());
            editor.putLong(KEY_LAST_UPDATED_PREFIX + icaoCode, System.currentTimeMillis());
            editor.apply();
            
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Error saving NOTAMs offline", e);
            return false;
        }
    }
    
    /**
     * Retrieve saved NOTAM data for offline use
     * 
     * @param icaoCode Airport ICAO code
     * @return List of saved NOTAM data, or empty list if none found
     */
    public List<NotamData> getOfflineNotams(String icaoCode) {
        List<NotamData> notamList = new ArrayList<>();
        
        if (icaoCode == null) {
            return notamList;
        }
        
        String jsonString = sharedPreferences.getString(KEY_NOTAM_PREFIX + icaoCode, null);
        if (jsonString == null) {
            return notamList;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject notamJson = jsonArray.getJSONObject(i);
                
                NotamData notam = new NotamData();
                notam.setId(notamJson.optString("id", ""));
                notam.setText(notamJson.optString("text", ""));
                notam.setStartTime(notamJson.optString("startTime", ""));
                notam.setEndTime(notamJson.optString("endTime", ""));
                notam.setType(notamJson.optString("type", ""));
                
                notamList.add(notam);
            }
            
            return notamList;
        } catch (JSONException e) {
            Log.e(TAG, "Error retrieving offline NOTAMs", e);
            return notamList;
        }
    }
    
    /**
     * Check if offline data is available for an airport
     * 
     * @param icaoCode Airport ICAO code
     * @return true if any offline data is available, false otherwise
     */
    public boolean hasOfflineData(String icaoCode) {
        if (icaoCode == null) {
            return false;
        }
        
        return sharedPreferences.contains(KEY_METAR_PREFIX + icaoCode) ||
               sharedPreferences.contains(KEY_TAF_PREFIX + icaoCode) ||
               sharedPreferences.contains(KEY_NOTAM_PREFIX + icaoCode);
    }
    
    /**
     * Get the last update time for offline data
     * 
     * @param icaoCode Airport ICAO code
     * @return Timestamp of last update in milliseconds, or 0 if not found
     */
    public long getLastUpdateTime(String icaoCode) {
        if (icaoCode == null) {
            return 0;
        }
        
        return sharedPreferences.getLong(KEY_LAST_UPDATED_PREFIX + icaoCode, 0);
    }
    
    /**
     * Clear all offline data
     */
    public void clearAllOfflineData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
    
    /**
     * Clear offline data for a specific airport
     * 
     * @param icaoCode Airport ICAO code
     */
    public void clearOfflineData(String icaoCode) {
        if (icaoCode == null) {
            return;
        }
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_METAR_PREFIX + icaoCode);
        editor.remove(KEY_TAF_PREFIX + icaoCode);
        editor.remove(KEY_NOTAM_PREFIX + icaoCode);
        editor.remove(KEY_LAST_UPDATED_PREFIX + icaoCode);
        editor.apply();
    }
}

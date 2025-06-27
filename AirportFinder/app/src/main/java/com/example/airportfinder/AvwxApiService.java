package com.example.airportfinder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/**
 * Service class for interacting with the AVWX REST API to fetch
 * aviation weather (METAR/TAF) data.
 * 
 * API Documentation: https://avwx.docs.apiary.io
 */
public class AvwxApiService {
    private static final String TAG = "AvwxApiService";
    private static final String BASE_URL = "https://avwx.rest/api/";
    private static final String METAR_ENDPOINT = "metar/";
    private static final String TAF_ENDPOINT = "taf/";
    
    private final ApiKeyManager apiKeyManager;
    private final Context context;
    private final ApiResponseCache cache;
    
    public AvwxApiService(Context context) {
        this.context = context;
        this.apiKeyManager = new ApiKeyManager(context);
        this.cache = ApiResponseCache.getInstance();
    }
    
    /**
     * Interface for API response callbacks
     */
    public interface ApiResponseCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }
    
    /**
     * Fetch METAR data for the specified ICAO code
     * 
     * @param icaoCode The airport ICAO code
     * @param callback Callback to handle the response
     * @param forceRefresh Whether to force a refresh from the API (ignore cache)
     */
    public void fetchMetar(String icaoCode, ApiResponseCallback<WeatherData> callback, boolean forceRefresh) {
        // Check for network connectivity first
        if (!NetworkUtils.isNetworkAvailable(context)) {
            ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.NETWORK, 0);
            callback.onError(ApiErrorHandler.ERROR_NO_NETWORK);
            return;
        }
        
        // Check for API key
        String apiKey = apiKeyManager.getAvwxApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.API_KEY, 0);
            callback.onError(ApiErrorHandler.ERROR_API_KEY_MISSING);
            return;
        }
        
        // Check cache first if not forcing refresh
        if (!forceRefresh) {
            WeatherData cachedData = cache.getCachedMetar(icaoCode);
            if (cachedData != null) {
                callback.onSuccess(cachedData);
                return;
            }
        }
        
        // Fetch from API
        new FetchWeatherTask(METAR_ENDPOINT, apiKey, callback).execute(icaoCode);
    }
    
    /**
     * Fetch METAR data (default behavior - use cache if available)
     */
    public void fetchMetar(String icaoCode, ApiResponseCallback<WeatherData> callback) {
        fetchMetar(icaoCode, callback, false);
    }
    
    /**
     * Fetch TAF data for the specified ICAO code
     * 
     * @param icaoCode The airport ICAO code
     * @param callback Callback to handle the response
     * @param forceRefresh Whether to force a refresh from the API (ignore cache)
     */
    public void fetchTaf(String icaoCode, ApiResponseCallback<WeatherData> callback, boolean forceRefresh) {
        // Check for network connectivity first
        if (!NetworkUtils.isNetworkAvailable(context)) {
            ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.NETWORK, 0);
            callback.onError(ApiErrorHandler.ERROR_NO_NETWORK);
            return;
        }
        
        // Check for API key
        String apiKey = apiKeyManager.getAvwxApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.API_KEY, 0);
            callback.onError(ApiErrorHandler.ERROR_API_KEY_MISSING);
            return;
        }
        
        // Check cache first if not forcing refresh
        if (!forceRefresh) {
            WeatherData cachedData = cache.getCachedTaf(icaoCode);
            if (cachedData != null) {
                callback.onSuccess(cachedData);
                return;
            }
        }
        
        // Fetch from API
        new FetchWeatherTask(TAF_ENDPOINT, apiKey, callback).execute(icaoCode);
    }
    
    /**
     * Fetch TAF data (default behavior - use cache if available)
     */
    public void fetchTaf(String icaoCode, ApiResponseCallback<WeatherData> callback) {
        fetchTaf(icaoCode, callback, false);
    }
    
    /**
     * AsyncTask to fetch weather data (METAR/TAF) from AVWX API
     */
    private class FetchWeatherTask extends AsyncTask<String, Void, WeatherData> {
        private final String endpoint;
        private final String apiKey;
        private final ApiResponseCallback<WeatherData> callback;
        private String errorMessage = null;
        private int responseCode = 0;
        private String icaoCode;
        
        FetchWeatherTask(String endpoint, String apiKey, ApiResponseCallback<WeatherData> callback) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.callback = callback;
        }
        
        @Override
        protected WeatherData doInBackground(String... params) {
            icaoCode = params[0];
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            
            try {
                URI uri = new URI(BASE_URL + endpoint + icaoCode);
                URL url = uri.toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", apiKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                
                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    WeatherData result = parseWeatherResponse(response.toString(), endpoint.equals(METAR_ENDPOINT));
                    
                    // Cache the result
                    if (result != null) {
                        if (endpoint.equals(METAR_ENDPOINT)) {
                            cache.cacheMetar(icaoCode, result);
                        } else {
                            cache.cacheTaf(icaoCode, result);
                        }
                    }
                    
                    return result;
                } else {
                    errorMessage = "API request failed with response code: " + responseCode;
                    Log.e(TAG, errorMessage);
                    return null;
                }
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                    errorMessage = ApiErrorHandler.ERROR_TIMEOUT;
                } else {
                    errorMessage = "Network error: " + e.getMessage();
                }
                Log.e(TAG, errorMessage, e);
                return null;
            } catch (JSONException e) {
                errorMessage = ApiErrorHandler.ERROR_PARSING;
                Log.e(TAG, errorMessage, e);
                return null;
            } catch (URISyntaxException e) {
                errorMessage = "Invalid URL: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
                return null;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        @Override
        protected void onPostExecute(WeatherData result) {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                // Determine error type based on response code
                if (responseCode >= 500) {
                    ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.SERVER, responseCode);
                } else if (responseCode >= 400) {
                    ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.CLIENT, responseCode);
                } else if (errorMessage != null && errorMessage.contains("timeout")) {
                    ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.TIMEOUT, 0);
                } else if (errorMessage != null && errorMessage.contains("parsing")) {
                    ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.PARSING, 0);
                } else {
                    ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.UNKNOWN, 0);
                }
                
                callback.onError(errorMessage != null ? errorMessage : ApiErrorHandler.ERROR_UNKNOWN);
            }
        }
        
        /**
         * Parse the JSON response from the AVWX API into a WeatherData object
         */
        private WeatherData parseWeatherResponse(String jsonResponse, boolean isMetar) throws JSONException {
            JSONObject json = new JSONObject(jsonResponse);
            
            WeatherData weatherData = new WeatherData();
            weatherData.setIcaoCode(json.optString("station", ""));
            weatherData.setRawText(json.optString("raw", ""));
            
            // Parse the decoded/translated sections
            if (json.has("translate") && !json.isNull("translate")) {
                JSONObject translate = json.getJSONObject("translate");
                StringBuilder decodedText = new StringBuilder();
                
                // Different structure for METAR vs TAF
                if (isMetar) {
                    // Process METAR translation
                    Iterator<String> keys = translate.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        decodedText.append(key).append(": ").append(translate.getString(key)).append("\n");
                    }
                } else {
                    // Process TAF translation which may have forecast periods
                    if (translate.has("forecast") && !translate.isNull("forecast")) {
                        JSONArray forecasts = translate.getJSONArray("forecast");
                        for (int i = 0; i < forecasts.length(); i++) {
                            JSONObject forecast = forecasts.getJSONObject(i);
                            decodedText.append("Period ").append(i + 1).append(":\n");
                            Iterator<String> forecastKeys = forecast.keys();
                            while (forecastKeys.hasNext()) {
                                String key = forecastKeys.next();
                                decodedText.append("  ").append(key).append(": ")
                                        .append(forecast.getString(key)).append("\n");
                            }
                        }
                    }
                    
                    // Add any other TAF translation elements
                    Iterator<String> keys = translate.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (!key.equals("forecast")) {
                            decodedText.append(key).append(": ").append(translate.getString(key)).append("\n");
                        }
                    }
                }
                
                weatherData.setDecodedText(decodedText.toString());
            }
            
            // Set weather type (METAR or TAF)
            weatherData.setType(isMetar ? WeatherData.TYPE_METAR : WeatherData.TYPE_TAF);
            
            // Add timestamp if available
            if (json.has("time") && !json.isNull("time")) {
                JSONObject time = json.getJSONObject("time");
                String timestamp = time.optString("dt", "");
                if (timestamp != null && !timestamp.isEmpty()) {
                    try {
                        // Try to parse ISO-8601 timestamp to milliseconds
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date date = sdf.parse(timestamp);
                        weatherData.setTimestamp(date.getTime());
                    } catch (ParseException e) {
                        // If parsing fails, use current time
                        Log.e(TAG, "Error parsing timestamp: " + timestamp, e);
                        weatherData.setTimestamp(System.currentTimeMillis());
                    }
                } else {
                    weatherData.setTimestamp(System.currentTimeMillis());
                }
            } else {
                weatherData.setTimestamp(System.currentTimeMillis());
            }
            
            return weatherData;
        }
    }
}

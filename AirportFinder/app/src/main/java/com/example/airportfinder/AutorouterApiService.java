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
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for interacting with the Autorouter API to fetch NOTAM data.
 * 
 * API Documentation: https://www.autorouter.aero/wiki/api/
 */
public class AutorouterApiService {
    private static final String TAG = "AutorouterApiService";
    private static final String BASE_URL = "https://api.autorouter.aero/";
    private static final String NOTAM_ENDPOINT = "notam/";
    
    private final ApiKeyManager apiKeyManager;
    private final Context context;
    private final ApiResponseCache cache;
    
    public AutorouterApiService(Context context) {
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
     * Fetch NOTAM data for the specified ICAO code
     * 
     * @param icaoCode The airport ICAO code
     * @param callback Callback to handle the response
     * @param forceRefresh Whether to force a refresh from the API (ignore cache)
     */
    public void fetchNotams(String icaoCode, ApiResponseCallback<List<NotamData>> callback, boolean forceRefresh) {
        // Check for network connectivity first
        if (!NetworkUtils.isNetworkAvailable(context)) {
            ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.NETWORK, 0);
            callback.onError(ApiErrorHandler.ERROR_NO_NETWORK);
            return;
        }
        
        // Check for API key
        String apiKey = apiKeyManager.getAutoRouterApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            ApiErrorHandler.handleApiError(context, ApiErrorHandler.ErrorType.API_KEY, 0);
            callback.onError(ApiErrorHandler.ERROR_API_KEY_MISSING);
            return;
        }
        
        // Check cache first if not forcing refresh
        if (!forceRefresh) {
            List<NotamData> cachedData = cache.getCachedNotams(icaoCode);
            if (cachedData != null) {
                callback.onSuccess(cachedData);
                return;
            }
        }
        
        // Fetch from API
        new FetchNotamTask(apiKey, callback).execute(icaoCode);
    }
    
    /**
     * Fetch NOTAM data (default behavior - use cache if available)
     */
    public void fetchNotams(String icaoCode, ApiResponseCallback<List<NotamData>> callback) {
        fetchNotams(icaoCode, callback, false);
    }
    
    /**
     * AsyncTask to fetch NOTAM data from Autorouter API
     */
    private class FetchNotamTask extends AsyncTask<String, Void, List<NotamData>> {
        private final String apiKey;
        private final ApiResponseCallback<List<NotamData>> callback;
        private String errorMessage = null;
        private int responseCode = 0;
        private String icaoCode;
        
        FetchNotamTask(String apiKey, ApiResponseCallback<List<NotamData>> callback) {
            this.apiKey = apiKey;
            this.callback = callback;
        }
        
        @Override
        protected List<NotamData> doInBackground(String... params) {
            icaoCode = params[0];
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            
            try {
                URI uri = new URI(BASE_URL + NOTAM_ENDPOINT + icaoCode);
                URL url = uri.toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                
                responseCode = connection.getResponseCode();
                Log.d(TAG, "Autorouter API Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String jsonResponse = response.toString();
                    Log.d(TAG, "Autorouter API Response: " + jsonResponse);
                    
                    List<NotamData> notamList = parseNotamResponse(jsonResponse);
                    
                    // Cache the successful response
                    if (notamList != null && !notamList.isEmpty()) {
                        cache.cacheNotams(icaoCode, notamList);
                    }
                    
                    return notamList;
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    
                    errorMessage = "HTTP " + responseCode + ": " + errorResponse;
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
        protected void onPostExecute(List<NotamData> result) {
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
         * Parse the JSON response from the Autorouter API into a list of NotamData objects
         */
        private List<NotamData> parseNotamResponse(String jsonResponse) throws JSONException {
            JSONObject json = new JSONObject(jsonResponse);
            List<NotamData> notamList = new ArrayList<>();
            
            if (json.has("data") && !json.isNull("data")) {
                JSONArray notams = json.getJSONArray("data");
                
                for (int i = 0; i < notams.length(); i++) {
                    JSONObject notamJson = notams.getJSONObject(i);
                    
                    NotamData notam = new NotamData();
                    notam.setId(notamJson.optString("id", "NOTAM #" + (i + 1)));
                    notam.setText(notamJson.optString("text", ""));
                    
                    // Extract start and end time if available
                    if (notamJson.has("validFrom") && !notamJson.isNull("validFrom")) {
                        notam.setStartTime(notamJson.getString("validFrom"));
                    }
                    
                    if (notamJson.has("validTo") && !notamJson.isNull("validTo")) {
                        notam.setEndTime(notamJson.getString("validTo"));
                    }
                    
                    // Add any other relevant NOTAM data
                    if (notamJson.has("type") && !notamJson.isNull("type")) {
                        notam.setType(notamJson.getString("type"));
                    }
                    
                    notamList.add(notam);
                }
            }
            
            return notamList;
        }
    }
}

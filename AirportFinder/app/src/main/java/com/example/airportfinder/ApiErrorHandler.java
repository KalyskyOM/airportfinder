package com.example.airportfinder;

import android.content.Context;
import android.widget.Toast;

/**
 * Utility class for handling API errors and providing consistent user feedback
 */
public class ApiErrorHandler {
    
    // Error message constants
    public static final String ERROR_NO_NETWORK = "No internet connection available. Please check your network settings.";
    public static final String ERROR_API_KEY_MISSING = "API key not configured. Please set up your API keys in the settings.";
    public static final String ERROR_SERVER = "Server error. Please try again later.";
    public static final String ERROR_TIMEOUT = "Request timed out. Please try again.";
    public static final String ERROR_PARSING = "Error processing the response. Please try again.";
    public static final String ERROR_UNKNOWN = "An unknown error occurred. Please try again.";
    
    /**
     * Display an error message to the user as a Toast
     * 
     * @param context Application context
     * @param message Error message to display
     */
    public static void showError(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Handle common API errors and display appropriate messages
     * 
     * @param context Application context
     * @param errorType The type of error that occurred
     * @param statusCode HTTP status code (if applicable)
     */
    public static void handleApiError(Context context, ErrorType errorType, int statusCode) {
        String message;
        
        switch (errorType) {
            case NETWORK:
                message = ERROR_NO_NETWORK;
                break;
            case API_KEY:
                message = ERROR_API_KEY_MISSING;
                break;
            case SERVER:
                message = ERROR_SERVER + " (Status: " + statusCode + ")";
                break;
            case TIMEOUT:
                message = ERROR_TIMEOUT;
                break;
            case PARSING:
                message = ERROR_PARSING;
                break;
            default:
                message = ERROR_UNKNOWN;
                break;
        }
        
        showError(context, message);
    }
    
    /**
     * Enum representing different types of API errors
     */
    public enum ErrorType {
        NETWORK,    // No network connection
        API_KEY,    // Missing or invalid API key
        SERVER,     // Server error (5xx)
        CLIENT,     // Client error (4xx)
        TIMEOUT,    // Request timeout
        PARSING,    // Error parsing response
        UNKNOWN     // Unknown error
    }
}

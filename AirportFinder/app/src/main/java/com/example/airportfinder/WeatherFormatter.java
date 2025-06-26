package com.example.airportfinder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for formatting aviation weather data in user-friendly formats
 */
public class WeatherFormatter {

    /**
     * Format a raw METAR or TAF string with proper line breaks and indentation
     * 
     * @param rawText The raw METAR or TAF text
     * @return Formatted text with proper line breaks
     */
    public static String formatRawWeatherText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }
        
        // Add line breaks for readability
        return rawText.replace(" TEMPO ", "\n  TEMPO ")
                     .replace(" BECMG ", "\n  BECMG ")
                     .replace(" FM", "\n  FM")
                     .replace(" PROB", "\n  PROB");
    }
    
    /**
     * Format a decoded weather text for better readability
     * 
     * @param decodedText The decoded weather text
     * @return Formatted text with proper styling
     */
    public static String formatDecodedWeatherText(String decodedText) {
        if (decodedText == null || decodedText.isEmpty()) {
            return "";
        }
        
        // Already formatted by the API service, just return as is
        return decodedText;
    }
    
    /**
     * Format an ISO-8601 timestamp into a user-friendly date/time string
     * 
     * @param timestamp ISO-8601 timestamp (e.g., "2025-06-26T08:30:00Z")
     * @return Formatted date/time string (e.g., "Jun 26, 2025 08:30 UTC")
     */
    public static String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm 'UTC'", Locale.US);
            outputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // If parsing fails, return the original timestamp
            return timestamp;
        }
    }
    
    /**
     * Format a timestamp in milliseconds into a user-friendly date/time string
     * 
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date/time string (e.g., "Jun 26, 2025 08:30 UTC")
     */
    public static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm 'UTC'", Locale.US);
        outputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        Date date = new Date(timestamp);
        return outputFormat.format(date);
    }
    
    /**
     * Format a NOTAM validity period
     * 
     * @param startTime Start time of the NOTAM
     * @param endTime End time of the NOTAM (may be null for permanent NOTAMs)
     * @return Formatted validity period string
     */
    public static String formatNotamValidity(String startTime, String endTime) {
        String start = formatTimestamp(startTime);
        
        if (endTime == null || endTime.isEmpty() || endTime.equals("PERM")) {
            return "From " + start + " (PERMANENT)";
        } else {
            String end = formatTimestamp(endTime);
            return "From " + start + " until " + end;
        }
    }
    
    /**
     * Extract and format the most important weather information from a METAR
     * for display in a summary view
     * 
     * @param metar The METAR weather data
     * @return A concise summary of the most important weather information
     */
    public static String extractMetarSummary(WeatherData metar) {
        if (metar == null || metar.getRawText() == null) {
            return "No METAR data available";
        }
        
        String rawText = metar.getRawText();
        StringBuilder summary = new StringBuilder();
        
        // Extract wind information (e.g., 12008KT)
        String windRegex = "\\b\\d{3}\\d{2,3}(G\\d{2,3})?KT\\b";
        java.util.regex.Pattern windPattern = java.util.regex.Pattern.compile(windRegex);
        java.util.regex.Matcher windMatcher = windPattern.matcher(rawText);
        
        if (windMatcher.find()) {
            String wind = windMatcher.group();
            String direction = wind.substring(0, 3);
            String speed = wind.substring(3, 5);
            
            summary.append("Wind: ").append(direction).append("° at ").append(speed).append(" knots");
            
            // Check for gusts
            if (wind.contains("G")) {
                String gust = wind.substring(wind.indexOf("G") + 1, wind.indexOf("KT"));
                summary.append(" (gusts ").append(gust).append(" knots)");
            }
            summary.append("\n");
        }
        
        // Extract visibility (e.g., 10SM)
        String visRegex = "\\b\\d+SM\\b";
        java.util.regex.Pattern visPattern = java.util.regex.Pattern.compile(visRegex);
        java.util.regex.Matcher visMatcher = visPattern.matcher(rawText);
        
        if (visMatcher.find()) {
            String visibility = visMatcher.group();
            summary.append("Visibility: ").append(visibility).append("\n");
        }
        
        // Extract temperature/dew point (e.g., 21/12)
        String tempRegex = "\\b\\d{2}/\\d{2}\\b";
        java.util.regex.Pattern tempPattern = java.util.regex.Pattern.compile(tempRegex);
        java.util.regex.Matcher tempMatcher = tempPattern.matcher(rawText);
        
        if (tempMatcher.find()) {
            String temp = tempMatcher.group();
            String[] parts = temp.split("/");
            summary.append("Temp: ").append(parts[0]).append("°C, Dew point: ").append(parts[1]).append("°C\n");
        }
        
        // Extract altimeter setting (e.g., A2992)
        String altRegex = "\\bA\\d{4}\\b";
        java.util.regex.Pattern altPattern = java.util.regex.Pattern.compile(altRegex);
        java.util.regex.Matcher altMatcher = altPattern.matcher(rawText);
        
        if (altMatcher.find()) {
            String altimeter = altMatcher.group();
            summary.append("Altimeter: ").append(altimeter).append("\n");
        }
        
        // Extract weather phenomena and clouds
        String[] tokens = rawText.split(" ");
        boolean foundWeather = false;
        
        for (String token : tokens) {
            // Cloud conditions (e.g., FEW020, SCT045, BKN080, OVC100)
            if (token.startsWith("FEW") || token.startsWith("SCT") || 
                token.startsWith("BKN") || token.startsWith("OVC")) {
                
                String cloudType = "";
                switch (token.substring(0, 3)) {
                    case "FEW": cloudType = "Few"; break;
                    case "SCT": cloudType = "Scattered"; break;
                    case "BKN": cloudType = "Broken"; break;
                    case "OVC": cloudType = "Overcast"; break;
                }
                
                String height = token.substring(3);
                if (height.length() == 3) {
                    height = height + "00"; // Convert to feet
                }
                
                if (!foundWeather) {
                    summary.append("Clouds: ");
                    foundWeather = true;
                } else {
                    summary.append(", ");
                }
                
                summary.append(cloudType).append(" at ").append(height).append(" ft");
            }
        }
        
        if (foundWeather) {
            summary.append("\n");
        }
        
        // Add timestamp
        if (metar.getTimestamp() > 0) {
            summary.append("Observed: ").append(formatTimestamp(metar.getTimestamp()));
        }
        
        return summary.toString();
    }
}

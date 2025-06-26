package com.example.airportfinder;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for visualizing weather data with color-coding and formatting
 */
public class WeatherVisualizationHelper {
    
    // Color constants for different weather conditions
    private static final int COLOR_VFR = Color.rgb(0, 128, 0);      // Green
    private static final int COLOR_MVFR = Color.rgb(0, 0, 255);     // Blue
    private static final int COLOR_IFR = Color.rgb(255, 0, 0);      // Red
    private static final int COLOR_LIFR = Color.rgb(128, 0, 128);   // Purple
    
    private static final int COLOR_WIND_NORMAL = Color.rgb(0, 128, 0);      // Green
    private static final int COLOR_WIND_MODERATE = Color.rgb(255, 165, 0);  // Orange
    private static final int COLOR_WIND_STRONG = Color.rgb(255, 0, 0);      // Red
    
    private static final int COLOR_CLOUD_FEW = Color.rgb(0, 128, 0);        // Green
    private static final int COLOR_CLOUD_SCT = Color.rgb(0, 128, 128);      // Teal
    private static final int COLOR_CLOUD_BKN = Color.rgb(0, 0, 255);        // Blue
    private static final int COLOR_CLOUD_OVC = Color.rgb(128, 0, 128);      // Purple
    
    /**
     * Applies color-coding to a TextView displaying METAR data
     * 
     * @param context Application context
     * @param textView TextView to apply formatting to
     * @param metar METAR data to visualize
     */
    public static void applyMetarFormatting(Context context, TextView textView, WeatherData metar) {
        if (context == null || textView == null || metar == null || metar.getRawText() == null) {
            return;
        }
        
        String rawText = metar.getRawText();
        SpannableStringBuilder builder = new SpannableStringBuilder(rawText);
        
        // Color-code visibility
        colorCodeVisibility(builder, rawText);
        
        // Color-code wind
        colorCodeWind(builder, rawText);
        
        // Color-code clouds
        colorCodeClouds(builder, rawText);
        
        // Color-code weather phenomena
        colorCodeWeatherPhenomena(builder, rawText);
        
        textView.setText(builder);
    }
    
    /**
     * Applies color-coding to a TextView displaying TAF data
     * 
     * @param context Application context
     * @param textView TextView to apply formatting to
     * @param taf TAF data to visualize
     */
    public static void applyTafFormatting(Context context, TextView textView, WeatherData taf) {
        if (context == null || textView == null || taf == null || taf.getRawText() == null) {
            return;
        }
        
        String rawText = taf.getRawText();
        
        // Format TAF with line breaks for readability
        String formattedText = WeatherFormatter.formatRawWeatherText(rawText);
        SpannableStringBuilder builder = new SpannableStringBuilder(formattedText);
        
        // Color-code TAF change indicators (TEMPO, BECMG, FM, PROB)
        Pattern changePattern = Pattern.compile("\\b(TEMPO|BECMG|FM\\d{6}|PROB\\d{2})\\b");
        Matcher changeMatcher = changePattern.matcher(formattedText);
        
        while (changeMatcher.find()) {
            builder.setSpan(
                new ForegroundColorSpan(Color.rgb(0, 102, 204)),
                changeMatcher.start(),
                changeMatcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            builder.setSpan(
                new RelativeSizeSpan(1.1f),
                changeMatcher.start(),
                changeMatcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        
        // Color-code visibility
        colorCodeVisibility(builder, formattedText);
        
        // Color-code wind
        colorCodeWind(builder, formattedText);
        
        // Color-code clouds
        colorCodeClouds(builder, formattedText);
        
        // Color-code weather phenomena
        colorCodeWeatherPhenomena(builder, formattedText);
        
        textView.setText(builder);
    }
    
    /**
     * Creates a flight category indicator for METAR data
     * 
     * @param metar METAR data
     * @return Spannable string with colored flight category
     */
    public static SpannableString createFlightCategoryIndicator(WeatherData metar) {
        if (metar == null || metar.getRawText() == null) {
            return new SpannableString("");
        }
        
        String rawText = metar.getRawText();
        String category;
        int color;
        
        if (hasLifrConditions(rawText)) {
            category = "LIFR";
            color = COLOR_LIFR;
        } else if (hasIfrConditions(rawText)) {
            category = "IFR";
            color = COLOR_IFR;
        } else if (hasMvfrConditions(rawText)) {
            category = "MVFR";
            color = COLOR_MVFR;
        } else {
            category = "VFR";
            color = COLOR_VFR;
        }
        
        SpannableString spannableString = new SpannableString(category);
        spannableString.setSpan(
            new ForegroundColorSpan(color),
            0,
            category.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        spannableString.setSpan(
            new BackgroundColorSpan(Color.rgb(240, 240, 240)),
            0,
            category.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        return spannableString;
    }
    
    /**
     * Creates a wind indicator for METAR data
     * 
     * @param metar METAR data
     * @return Spannable string with colored wind information
     */
    public static SpannableString createWindIndicator(WeatherData metar) {
        if (metar == null || metar.getRawText() == null) {
            return new SpannableString("No Wind Data");
        }
        
        String rawText = metar.getRawText();
        
        // Extract wind information (e.g., 12008KT)
        Pattern windPattern = Pattern.compile("\\b(\\d{3})(\\d{2,3})(G\\d{2,3})?KT\\b");
        Matcher windMatcher = windPattern.matcher(rawText);
        
        if (windMatcher.find()) {
            String direction = windMatcher.group(1);
            String speed = windMatcher.group(2);
            String gust = windMatcher.group(3);
            
            int windSpeed = Integer.parseInt(speed);
            int color;
            
            // Color based on wind speed
            if (windSpeed >= 25 || (gust != null && gust.length() > 0)) {
                color = COLOR_WIND_STRONG;
            } else if (windSpeed >= 15) {
                color = COLOR_WIND_MODERATE;
            } else {
                color = COLOR_WIND_NORMAL;
            }
            
            String windText = direction + "° at " + speed + " KT";
            if (gust != null && gust.length() > 0) {
                windText += " (G" + gust.substring(1) + ")";
            }
            
            SpannableString spannableString = new SpannableString(windText);
            spannableString.setSpan(
                new ForegroundColorSpan(color),
                0,
                windText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            return spannableString;
        }
        
        return new SpannableString("Calm");
    }
    
    /**
     * Determines if METAR indicates LIFR conditions
     * (visibility < 1 mile or ceiling < 500 ft)
     */
    private static boolean hasLifrConditions(String rawText) {
        boolean lowVisibility = false;
        boolean lowCeiling = false;
        
        // Check visibility
        Pattern visPattern = Pattern.compile("\\b([0-9]{1}/[0-9]{1}|[0-9]/[0-9]{2}|M?([0-9]/[0-9]|[0-9]))SM\\b");
        Matcher visMatcher = visPattern.matcher(rawText);
        
        if (visMatcher.find()) {
            String visText = visMatcher.group();
            if (visText.startsWith("M") || visText.equals("1/4SM") || visText.equals("1/2SM") || 
                visText.equals("3/4SM") || visText.equals("1SM")) {
                lowVisibility = true;
            }
        }
        
        // Check ceiling
        Pattern ceilingPattern = Pattern.compile("\\b(OVC|BKN)([0-9]{3})\\b");
        Matcher ceilingMatcher = ceilingPattern.matcher(rawText);
        
        while (ceilingMatcher.find()) {
            int height = Integer.parseInt(ceilingMatcher.group(2));
            if (height < 5) {  // Less than 500 feet (reported in hundreds of feet)
                lowCeiling = true;
                break;
            }
        }
        
        return lowVisibility || lowCeiling;
    }
    
    /**
     * Determines if METAR indicates IFR conditions
     * (visibility 1-3 miles or ceiling 500-1000 ft)
     */
    private static boolean hasIfrConditions(String rawText) {
        boolean lowVisibility = false;
        boolean lowCeiling = false;
        
        // Check visibility
        Pattern visPattern = Pattern.compile("\\b([1-2]SM|[1-2]\\s[1-9]/[0-9]SM|3SM)\\b");
        Matcher visMatcher = visPattern.matcher(rawText);
        
        if (visMatcher.find()) {
            lowVisibility = true;
        }
        
        // Check ceiling
        Pattern ceilingPattern = Pattern.compile("\\b(OVC|BKN)([0-9]{3})\\b");
        Matcher ceilingMatcher = ceilingPattern.matcher(rawText);
        
        while (ceilingMatcher.find()) {
            int height = Integer.parseInt(ceilingMatcher.group(2));
            if (height >= 5 && height < 10) {  // 500-1000 feet
                lowCeiling = true;
                break;
            }
        }
        
        return lowVisibility || lowCeiling;
    }
    
    /**
     * Determines if METAR indicates MVFR conditions
     * (visibility 3-5 miles or ceiling 1000-3000 ft)
     */
    private static boolean hasMvfrConditions(String rawText) {
        boolean moderateVisibility = false;
        boolean moderateCeiling = false;
        
        // Check visibility
        Pattern visPattern = Pattern.compile("\\b([3-5]SM)\\b");
        Matcher visMatcher = visPattern.matcher(rawText);
        
        if (visMatcher.find()) {
            moderateVisibility = true;
        }
        
        // Check ceiling
        Pattern ceilingPattern = Pattern.compile("\\b(OVC|BKN)([0-9]{3})\\b");
        Matcher ceilingMatcher = ceilingPattern.matcher(rawText);
        
        while (ceilingMatcher.find()) {
            int height = Integer.parseInt(ceilingMatcher.group(2));
            if (height >= 10 && height < 30) {  // 1000-3000 feet
                moderateCeiling = true;
                break;
            }
        }
        
        return moderateVisibility || moderateCeiling;
    }
    
    /**
     * Color-codes visibility in a SpannableStringBuilder
     */
    private static void colorCodeVisibility(SpannableStringBuilder builder, String text) {
        Pattern visPattern = Pattern.compile("\\b\\d+SM\\b|\\b\\d/\\d+SM\\b|\\b\\d+\\s\\d/\\d+SM\\b");
        Matcher visMatcher = visPattern.matcher(text);
        
        while (visMatcher.find()) {
            String visText = visMatcher.group();
            int color;
            
            if (visText.startsWith("1") && !visText.contains(" ")) {
                color = COLOR_IFR;  // 1SM
            } else if (visText.startsWith("2") && !visText.contains(" ")) {
                color = COLOR_IFR;  // 2SM
            } else if (visText.startsWith("3") && !visText.contains(" ")) {
                color = COLOR_MVFR;  // 3SM
            } else if (visText.startsWith("4") && !visText.contains(" ")) {
                color = COLOR_MVFR;  // 4SM
            } else if (visText.startsWith("5") && !visText.contains(" ")) {
                color = COLOR_MVFR;  // 5SM
            } else if (visText.contains("/")) {
                color = COLOR_LIFR;  // Fractional visibility
            } else {
                color = COLOR_VFR;  // 6+ SM
            }
            
            builder.setSpan(
                new ForegroundColorSpan(color),
                visMatcher.start(),
                visMatcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * Color-codes wind in a SpannableStringBuilder
     */
    private static void colorCodeWind(SpannableStringBuilder builder, String text) {
        Pattern windPattern = Pattern.compile("\\b\\d{3}\\d{2,3}(G\\d{2,3})?KT\\b");
        Matcher windMatcher = windPattern.matcher(text);
        
        while (windMatcher.find()) {
            String windText = windMatcher.group();
            String speedPart = windText.substring(3, windText.indexOf("KT"));
            
            if (speedPart.contains("G")) {
                speedPart = speedPart.substring(0, speedPart.indexOf("G"));
            }
            
            int speed = Integer.parseInt(speedPart);
            int color;
            
            if (speed >= 25 || windText.contains("G")) {
                color = COLOR_WIND_STRONG;
            } else if (speed >= 15) {
                color = COLOR_WIND_MODERATE;
            } else {
                color = COLOR_WIND_NORMAL;
            }
            
            builder.setSpan(
                new ForegroundColorSpan(color),
                windMatcher.start(),
                windMatcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * Color-codes cloud layers in a SpannableStringBuilder
     */
    private static void colorCodeClouds(SpannableStringBuilder builder, String text) {
        Pattern cloudPattern = Pattern.compile("\\b(FEW|SCT|BKN|OVC)(\\d{3})\\b");
        Matcher cloudMatcher = cloudPattern.matcher(text);
        
        while (cloudMatcher.find()) {
            String cloudType = cloudMatcher.group(1);
            int height = Integer.parseInt(cloudMatcher.group(2));
            int color;
            
            // Color based on cloud type and height
            if (cloudType.equals("OVC")) {
                if (height < 5) {
                    color = COLOR_LIFR;  // Overcast below 500 feet
                } else if (height < 10) {
                    color = COLOR_IFR;   // Overcast 500-1000 feet
                } else if (height < 30) {
                    color = COLOR_MVFR;  // Overcast 1000-3000 feet
                } else {
                    color = COLOR_CLOUD_OVC;
                }
            } else if (cloudType.equals("BKN")) {
                if (height < 5) {
                    color = COLOR_LIFR;  // Broken below 500 feet
                } else if (height < 10) {
                    color = COLOR_IFR;   // Broken 500-1000 feet
                } else if (height < 30) {
                    color = COLOR_MVFR;  // Broken 1000-3000 feet
                } else {
                    color = COLOR_CLOUD_BKN;
                }
            } else if (cloudType.equals("SCT")) {
                color = COLOR_CLOUD_SCT;
            } else {  // FEW
                color = COLOR_CLOUD_FEW;
            }
            
            builder.setSpan(
                new ForegroundColorSpan(color),
                cloudMatcher.start(),
                cloudMatcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    
    /**
     * Color-codes weather phenomena in a SpannableStringBuilder
     */
    private static void colorCodeWeatherPhenomena(SpannableStringBuilder builder, String text) {
        // Weather phenomena patterns
        String[] phenomena = {
            "\\b(\\+|-)?RA\\b",     // Rain
            "\\b(\\+|-)?SN\\b",     // Snow
            "\\b(\\+|-)?DZ\\b",     // Drizzle
            "\\b(\\+|-)?SH\\b",     // Shower
            "\\b(\\+|-)?TS\\b",     // Thunderstorm
            "\\b(\\+|-)?FG\\b",     // Fog
            "\\b(\\+|-)?BR\\b",     // Mist
            "\\b(\\+|-)?HZ\\b",     // Haze
            "\\b(\\+|-)?FU\\b",     // Smoke
            "\\b(\\+|-)?SA\\b",     // Sand
            "\\b(\\+|-)?DU\\b",     // Dust
            "\\b(\\+|-)?GR\\b",     // Hail
            "\\b(\\+|-)?IC\\b"      // Ice crystals
        };
        
        for (String phenomenon : phenomena) {
            Pattern pattern = Pattern.compile(phenomenon);
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                String match = matcher.group();
                int color;
                
                if (match.contains("TS") || match.contains("+")) {
                    // Thunderstorms or heavy precipitation
                    color = Color.rgb(128, 0, 0);  // Dark red
                } else if (match.contains("FG") || match.contains("BR")) {
                    // Fog or mist (visibility reducers)
                    color = Color.rgb(128, 128, 128);  // Gray
                } else {
                    // Other weather
                    color = Color.rgb(0, 0, 128);  // Dark blue
                }
                
                builder.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                
                // Make it bold by using a slightly larger relative size
                builder.setSpan(
                    new RelativeSizeSpan(1.1f),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
}

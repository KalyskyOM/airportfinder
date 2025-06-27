package com.example.airportfinder;

/**
 * Model class for airport weather data (METAR and TAF)
 */
public class WeatherData {
    public static final int TYPE_METAR = 1;
    public static final int TYPE_TAF = 2;
    
    private String icaoCode;
    private String metarRaw;
    private String metarDecoded;
    private String tafRaw;
    private String tafDecoded;
    private long timestamp;
    private String rawText;
    private String decodedText;
    private int type;

    public WeatherData() {
        this.timestamp = System.currentTimeMillis();
    }

    public WeatherData(String icaoCode) {
        this.icaoCode = icaoCode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getIcaoCode() {
        return icaoCode;
    }
    
    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }

    public String getMetarRaw() {
        return metarRaw;
    }

    public void setMetarRaw(String metarRaw) {
        this.metarRaw = metarRaw;
    }

    public String getMetarDecoded() {
        return metarDecoded;
    }

    public void setMetarDecoded(String metarDecoded) {
        this.metarDecoded = metarDecoded;
    }

    public String getTafRaw() {
        return tafRaw;
    }

    public void setTafRaw(String tafRaw) {
        this.tafRaw = tafRaw;
    }

    public String getTafDecoded() {
        return tafDecoded;
    }

    public void setTafDecoded(String tafDecoded) {
        this.tafDecoded = tafDecoded;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getTimestampAsString() {
        return String.valueOf(timestamp);
    }
    
    public void setTimestamp(String timestamp) {
        try {
            this.timestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public String getRawText() {
        return rawText != null ? rawText : (type == TYPE_METAR ? metarRaw : tafRaw);
    }
    
    public void setRawText(String rawText) {
        this.rawText = rawText;
        if (type == TYPE_METAR) {
            this.metarRaw = rawText;
        } else if (type == TYPE_TAF) {
            this.tafRaw = rawText;
        }
    }
    
    public String getDecodedText() {
        return decodedText != null ? decodedText : (type == TYPE_METAR ? metarDecoded : tafDecoded);
    }
    
    public void setDecodedText(String decodedText) {
        this.decodedText = decodedText;
        if (type == TYPE_METAR) {
            this.metarDecoded = decodedText;
        } else if (type == TYPE_TAF) {
            this.tafDecoded = decodedText;
        }
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }

    /**
     * For demo purposes, generate sample weather data
     */
    public static WeatherData getSampleData(android.content.Context context, String icaoCode) {
        WeatherData data = new WeatherData(icaoCode);
        
        // Current date and time for realistic sample data
        java.text.SimpleDateFormat zulu = new java.text.SimpleDateFormat("ddHHmm");
        zulu.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String dateTime = zulu.format(new java.util.Date());
        
        // Sample METAR
        data.setMetarRaw(String.format(context.getString(R.string.metar_raw_format), icaoCode, dateTime));
        data.setMetarDecoded(
            context.getString(R.string.metar_decoded_wind, 280, 7) + "\n" +
            context.getString(R.string.metar_decoded_visibility, 10) + "\n" +
            context.getString(R.string.metar_decoded_clouds, "Few at 5000 feet") + "\n" +
            context.getString(R.string.metar_decoded_temperature, 23, 73) + "\n" +
            context.getString(R.string.metar_decoded_dew_point, 16, 61) + "\n" +
            context.getString(R.string.metar_decoded_altimeter, 30.02f)
        );
        
        // Sample TAF
        String day = dateTime.substring(0, 2);
        String nextDay = String.valueOf(Integer.parseInt(day) + 1);
        data.setTafRaw(String.format(context.getString(R.string.taf_raw_format), icaoCode, dateTime, day, nextDay));
        data.setTafDecoded(
            context.getString(R.string.taf_decoded_period, day, "12", nextDay, "18") + "\n" +
            context.getString(R.string.taf_decoded_wind, "280°", 8) + "\n" +
            context.getString(R.string.taf_decoded_visibility, 6) + "\n" +
            context.getString(R.string.taf_decoded_clouds_multi, "Few", 5000, "Scattered", 15000) + "\n\n" +
            context.getString(R.string.taf_decoded_period, day, "20", day, "20") + "\n" +
            context.getString(R.string.taf_decoded_wind, "Variable", 3) + "\n" +
            context.getString(R.string.taf_decoded_visibility, 6) + "\n" +
            context.getString(R.string.taf_decoded_clouds_clear)
        );
        
        return data;
    }
}

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
    public static WeatherData getSampleData(String icaoCode) {
        WeatherData data = new WeatherData(icaoCode);
        
        // Current date and time for realistic sample data
        java.text.SimpleDateFormat zulu = new java.text.SimpleDateFormat("ddHHmm");
        zulu.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String dateTime = zulu.format(new java.util.Date());
        
        // Sample METAR
        data.setMetarRaw(icaoCode + " " + dateTime + "Z 28007KT 10SM FEW050 23/16 A3002 RMK AO2 SLP166 T02280161");
        data.setMetarDecoded(
            "Wind: 280° at 7 knots\n" +
            "Visibility: 10 statute miles\n" +
            "Clouds: Few at 5000 feet\n" +
            "Temperature: 23°C (73°F)\n" +
            "Dew Point: 16°C (61°F)\n" +
            "Altimeter: 30.02 inHg"
        );
        
        // Sample TAF
        data.setTafRaw(icaoCode + " " + dateTime + "Z " + dateTime.substring(0, 2) + "12/" + 
                      (Integer.parseInt(dateTime.substring(0, 2)) + 1) + "18 28008KT P6SM FEW050 SCT150 " +
                      "FM" + dateTime.substring(0, 2) + "2000 VRB03KT P6SM SKC");
        data.setTafDecoded(
            "From " + dateTime.substring(0, 2) + "th 12:00Z to " + (Integer.parseInt(dateTime.substring(0, 2)) + 1) + "th 18:00Z:\n" +
            "Wind: 280° at 8 knots\n" +
            "Visibility: More than 6 statute miles\n" +
            "Clouds: Few at 5000 feet, Scattered at 15000 feet\n\n" +
            "From " + dateTime.substring(0, 2) + "th 20:00Z:\n" +
            "Wind: Variable at 3 knots\n" +
            "Visibility: More than 6 statute miles\n" +
            "Clouds: Clear"
        );
        
        return data;
    }
}

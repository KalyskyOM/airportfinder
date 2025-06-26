package com.example.airportfinder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Model class for NOTAM data
 */
public class NotamData {
    private String id;
    private String type;
    private String text;
    private long validFrom;
    private long validTo;
    private String startTime;
    private String endTime;
    
    public NotamData() {
        // Default constructor
    }
    
    public NotamData(String id, String type, String text, long validFrom, long validTo) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public long getValidFrom() {
        return validFrom;
    }
    
    public void setValidFrom(long validFrom) {
        this.validFrom = validFrom;
    }
    
    public long getValidTo() {
        return validTo;
    }
    
    public void setValidTo(long validTo) {
        this.validTo = validTo;
    }
    
    public String getStartTime() {
        return startTime;
    }
    
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    
    public String getEndTime() {
        return endTime;
    }
    
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
    
    public String getFormattedValidity() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return "From: " + sdf.format(new Date(validFrom)) + " To: " + sdf.format(new Date(validTo));
    }
    
    /**
     * For demo purposes, generate sample NOTAM data for an airport
     */
    public static List<NotamData> getSampleData(String icaoCode) {
        List<NotamData> notams = new ArrayList<>();
        Random random = new Random();
        
        // Current time for realistic sample data
        long now = System.currentTimeMillis();
        
        // Sample NOTAM types
        String[] types = {"RUNWAY", "TAXIWAY", "APRON", "NAVAID", "AIRSPACE", "OBSTACLE"};
        
        // Sample NOTAM texts based on type
        String[][] sampleTexts = {
            // RUNWAY
            {
                "RWY 13L/31R CLSD",
                "RWY 04/22 WIP CONST. CAUTION",
                "RWY 09/27 EDGE LGT U/S",
                "RWY 18/36 DECLARED DIST CHANGED. TORA 2500M, TODA 2600M, ASDA 2500M, LDA 2300M"
            },
            // TAXIWAY
            {
                "TWY A CLSD BTN TWY B AND TWY C",
                "TWY D LGT U/S",
                "TWY E RESTRICTED TO ACFT WITH WINGSPAN LT 36M",
                "TWY F WIP CONST. CAUTION"
            },
            // APRON
            {
                "APRON 1 STANDS 10-15 CLSD",
                "APRON 2 REDUCED BEARING STRENGTH. MAX WT 50T",
                "APRON 3 LGT U/S",
                "APRON 4 WIP CONST. CAUTION"
            },
            // NAVAID
            {
                "VOR " + icaoCode.substring(1) + " U/S",
                "ILS RWY 27 GP U/S",
                "DME " + icaoCode.substring(1) + " OTS FOR MAINT",
                "NDB " + icaoCode.substring(1) + " FREQ CHANGED TO 375KHZ"
            },
            // AIRSPACE
            {
                "TMA SECTOR 3 CLSD DUE MIL EXER",
                "CTR ACT H24 DUE INCREASED TFC",
                "ATZ ACTIVE 0600-2200",
                "RESTRICTED AREA R123 ACT"
            },
            // OBSTACLE
            {
                "CRANE ERECTED 2NM E AD. HGT 300FT AGL, 1200FT AMSL",
                "NEW MAST 1NM W RWY 27 THR. HGT 150FT AGL",
                "UNMARKED OBSTACLE 3NM S AD. HGT 250FT AGL",
                "TEMPORARY OBSTACLE 0.5NM N RWY 09 THR. HGT 100FT AGL"
            }
        };
        
        // Generate 3-5 random NOTAMs
        int count = random.nextInt(3) + 3;
        for (int i = 0; i < count; i++) {
            // Generate a random NOTAM ID (letter + 4 digits + year)
            char letter = (char)('A' + random.nextInt(26));
            int number = 1000 + random.nextInt(9000);
            int year = 23 + random.nextInt(3);
            String id = letter + String.valueOf(number) + "/" + year;
            
            // Pick a random type
            int typeIndex = random.nextInt(types.length);
            String type = types[typeIndex];
            
            // Pick a random text for that type
            String text = sampleTexts[typeIndex][random.nextInt(sampleTexts[typeIndex].length)];
            
            // Generate validity period (starting between 7 days ago and now, ending between now and 30 days from now)
            long validFrom = now - (random.nextInt(7) * 24 * 60 * 60 * 1000L);
            long validTo = now + ((random.nextInt(30) + 1) * 24 * 60 * 60 * 1000L);
            
            notams.add(new NotamData(id, type, text, validFrom, validTo));
        }
        
        return notams;
    }
}

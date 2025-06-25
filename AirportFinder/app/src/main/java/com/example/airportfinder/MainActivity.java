package com.example.airportfinder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // Coordinate Format Constants
    private static final int FORMAT_DMS = 0;    // Degrees Minutes Seconds.Decimal
    private static final int FORMAT_DECIMAL = 1; // Decimal Degrees
    private static final int FORMAT_GPS = 2;     // GPS Format
    
    // Format Names
    private static final String[] FORMAT_NAMES = {
        "DMS.SS (40°45'30.25\"N)",
        "Decimal (40.758958)",
        "GPS (40°45'30.25\"N 73°58'45.75\"W)"
    };
    
    private static final String[] FORMAT_DESCRIPTIONS = {
        "Degrees Minutes Seconds.Decimal (40°45'30.25\"N)",
        "Decimal Degrees (40.758958, -73.985130)",
        "GPS Coordinates (40°45'30.25\"N 73°58'45.75\"W)"
    };

    private EditText etLatDegrees, etLatMinutes, etLatSecondsWhole, etLatSecondsDecimal;
    private EditText etLonDegrees, etLonMinutes, etLonSecondsWhole, etLonSecondsDecimal;
    private EditText etRadiusWhole, etRadiusDecimal;
    private RadioGroup rgLatDirection, rgLonDirection;
    private Button btnFindAirport;
    private TextView tvResult;
    private Spinner spinnerCoordinateFormat;
    private TextView tvFormatDescription;

    private List<Airport> airports = new ArrayList<>();
    private SharedPreferences prefs;
    private DecimalFormat decimalFormat = new DecimalFormat("#.######");
    
    // State management
    private boolean isUpdatingFields = false;
    private int currentFormat = FORMAT_DMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AirportFinder", MODE_PRIVATE);

        initializeViews();
        setupFormatSpinner();
        loadLastValues();
        loadAirportData();
        setupClickListener();
        setupInputFieldListeners();
    }

    private void initializeViews() {
        etLatDegrees = findViewById(R.id.etLatDegrees);
        etLatMinutes = findViewById(R.id.etLatMinutes);
        etLatSecondsWhole = findViewById(R.id.etLatSecondsWhole);
        etLatSecondsDecimal = findViewById(R.id.etLatSecondsDecimal);

        etLonDegrees = findViewById(R.id.etLonDegrees);
        etLonMinutes = findViewById(R.id.etLonMinutes);
        etLonSecondsWhole = findViewById(R.id.etLonSecondsWhole);
        etLonSecondsDecimal = findViewById(R.id.etLonSecondsDecimal);

        etRadiusWhole = findViewById(R.id.etRadiusWhole);
        etRadiusDecimal = findViewById(R.id.etRadiusDecimal);

        rgLatDirection = findViewById(R.id.rgLatDirection);
        rgLonDirection = findViewById(R.id.rgLonDirection);
        btnFindAirport = findViewById(R.id.btnFindAirport);
        tvResult = findViewById(R.id.tvResult);
        
        spinnerCoordinateFormat = findViewById(R.id.spinnerCoordinateFormat);
        tvFormatDescription = findViewById(R.id.tvFormatDescription);
    }
    
    private void setupFormatSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, FORMAT_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCoordinateFormat.setAdapter(adapter);
        
        spinnerCoordinateFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentFormat != position) {
                    switchCoordinateFormat(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupInputFieldListeners() {
        TextWatcher conversionWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                if (!isUpdatingFields && currentFormat != FORMAT_DMS) {
                    // Convert input to DMS format for display consistency
                    convertToCurrentFormat();
                }
            }
        };
        
        etLatDegrees.addTextChangedListener(conversionWatcher);
        etLatMinutes.addTextChangedListener(conversionWatcher);
        etLatSecondsWhole.addTextChangedListener(conversionWatcher);
        etLatSecondsDecimal.addTextChangedListener(conversionWatcher);
        etLonDegrees.addTextChangedListener(conversionWatcher);
        etLonMinutes.addTextChangedListener(conversionWatcher);
        etLonSecondsWhole.addTextChangedListener(conversionWatcher);
        etLonSecondsDecimal.addTextChangedListener(conversionWatcher);
    }

    private void loadLastValues() {
        etLatDegrees.setText(prefs.getString("latDegrees", ""));
        etLatMinutes.setText(prefs.getString("latMinutes", ""));
        etLatSecondsWhole.setText(prefs.getString("latSecondsWhole", ""));
        etLatSecondsDecimal.setText(prefs.getString("latSecondsDecimal", ""));

        etLonDegrees.setText(prefs.getString("lonDegrees", ""));
        etLonMinutes.setText(prefs.getString("lonMinutes", ""));
        etLonSecondsWhole.setText(prefs.getString("lonSecondsWhole", ""));
        etLonSecondsDecimal.setText(prefs.getString("lonSecondsDecimal", ""));

        etRadiusWhole.setText(prefs.getString("radiusWhole", "30"));
        etRadiusDecimal.setText(prefs.getString("radiusDecimal", "0"));

        int latDirection = prefs.getInt("latDirection", R.id.rbLatNorth);
        int lonDirection = prefs.getInt("lonDirection", R.id.rbLonEast);
        rgLatDirection.check(latDirection);
        rgLonDirection.check(lonDirection);
        
        // Load saved format
        currentFormat = prefs.getInt("coordinateFormat", FORMAT_DMS);
        spinnerCoordinateFormat.setSelection(currentFormat);
        tvFormatDescription.setText(FORMAT_DESCRIPTIONS[currentFormat]);
        updateFieldLabelsAndTypes(currentFormat);
    }

    private void saveValues() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("latDegrees", etLatDegrees.getText().toString());
        editor.putString("latMinutes", etLatMinutes.getText().toString());
        editor.putString("latSecondsWhole", etLatSecondsWhole.getText().toString());
        editor.putString("latSecondsDecimal", etLatSecondsDecimal.getText().toString());

        editor.putString("lonDegrees", etLonDegrees.getText().toString());
        editor.putString("lonMinutes", etLonMinutes.getText().toString());
        editor.putString("lonSecondsWhole", etLonSecondsWhole.getText().toString());
        editor.putString("lonSecondsDecimal", etLonSecondsDecimal.getText().toString());

        editor.putString("radiusWhole", etRadiusWhole.getText().toString());
        editor.putString("radiusDecimal", etRadiusDecimal.getText().toString());

        editor.putInt("latDirection", rgLatDirection.getCheckedRadioButtonId());
        editor.putInt("lonDirection", rgLonDirection.getCheckedRadioButtonId());
        
        // Save format
        editor.putInt("coordinateFormat", currentFormat);
        
        editor.apply();
    }

    private void loadAirportData() {
        try {
            InputStream inputStream = getAssets().open("airports.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean isFirstLine = true;
            int totalLines = 0;
            int validAirports = 0;
            
            while ((line = reader.readLine()) != null) {
                totalLines++;
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                
                // Safety check: ensure we have enough columns
                if (parts.length < 14) {
                    continue; // Skip lines with insufficient data
                }
                
                try {
                    String type = parts.length > 2 ? parts[2].replace("\"", "") : "";
                    
                    // Filter only small, medium, large airports
                    if (type.equals("small_airport") || type.equals("medium_airport") || type.equals("large_airport")) {
                        String name = parts.length > 3 ? parts[3].replace("\"", "") : "";
                        String iataCode = parts.length > 13 ? parts[13].replace("\"", "") : "";
                        String icaoCode = parts.length > 12 ? parts[12].replace("\"", "") : "";
                        String municipality = parts.length > 10 ? parts[10].replace("\"", "") : "";
                        String country = parts.length > 8 ? parts[8].replace("\"", "") : "";
                        
                        if (parts.length > 5 && !parts[4].isEmpty() && !parts[5].isEmpty()) {
                            double latitude = Double.parseDouble(parts[4]);
                            double longitude = Double.parseDouble(parts[5]);
                            
                            if (Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180 && !name.isEmpty()) {
                                airports.add(new Airport(name, latitude, longitude, type, 
                                                       iataCode, icaoCode, municipality, country));
                                validAirports++;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
            reader.close();
            
            Toast.makeText(this, "Loaded " + validAirports + " airports from " + totalLines + " lines", Toast.LENGTH_LONG).show();
            
        } catch (IOException e) {
            Toast.makeText(this, "Error loading airport data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupClickListener() {
        btnFindAirport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNearestAirport();
            }
        });
    }

    private void findNearestAirport() {
        try {
            saveValues(); // Save current input

            double[] coordinates = getCurrentCoordinatesAsDecimal();
            double latitude = coordinates[0];
            double longitude = coordinates[1];

            // Get search radius
            double searchRadius = getSearchRadius();
            double radiusDegrees = searchRadius / 3600.0; // Convert seconds to degrees

            List<AirportDistance> nearestAirports = findNearestAirports(latitude, longitude, radiusDegrees, 5);

            if (nearestAirports.isEmpty()) {
                tvResult.setText("No airports found in the database.");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append("Nearest Airports:\n\n");

            for (int i = 0; i < nearestAirports.size(); i++) {
                AirportDistance ad = nearestAirports.get(i);
                Airport airport = ad.airport;

                result.append(String.format("%d. %s\n", i + 1, airport.name));
                result.append(String.format("   Type: %s\n", airport.type));
                result.append(String.format("   Location: %s, %s\n", airport.municipality, airport.country));

                if (!airport.iataCode.isEmpty()) {
                    result.append(String.format("   IATA: %s", airport.iataCode));
                }
                if (!airport.icaoCode.isEmpty()) {
                    result.append(String.format("   ICAO: %s", airport.icaoCode));
                }
                result.append("\n");

                result.append(String.format("   Distance: %.2f km\n\n", ad.distance));
            }

            tvResult.setText(result.toString());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for all coordinate fields", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private double getSearchRadius() throws Exception {
        try {
            String wholeStr = etRadiusWhole.getText().toString().trim();
            String decStr = etRadiusDecimal.getText().toString().trim();
            
            int whole = wholeStr.isEmpty() ? 30 : Integer.parseInt(wholeStr); // Default 30
            int decimal = decStr.isEmpty() ? 0 : Integer.parseInt(decStr);
            
            return whole + (decimal / 100.0);
            
        } catch (NumberFormatException e) {
            throw new Exception("Please enter valid search radius");
        }
    }

    private List<AirportDistance> findNearestAirports(double targetLat, double targetLon, double radiusDegrees, int count) {
        List<AirportDistance> distances = new ArrayList<>();

        for (Airport airport : airports) {
            double distance = calculateDistance(targetLat, targetLon, airport.latitude, airport.longitude);
            
            // Filter airports by radius (convert km to degrees approximately)
            double distanceDegrees = distance / 111.0; // Rough conversion: 1 degree ≈ 111 km
            if (distanceDegrees <= radiusDegrees) {
                distances.add(new AirportDistance(airport, distance));
            }
        }

        Collections.sort(distances, new Comparator<AirportDistance>() {
            @Override
            public int compare(AirportDistance a, AirportDistance b) {
                return Double.compare(a.distance, b.distance);
            }
        });

        return distances.subList(0, Math.min(count, distances.size()));
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    // Format conversion methods
    
    private void switchCoordinateFormat(int newFormat) {
        if (isUpdatingFields) return;
        
        try {
            // Get current coordinates in decimal format
            double[] currentCoords = getCurrentCoordinatesAsDecimal();
            
            // Update format
            currentFormat = newFormat;
            tvFormatDescription.setText(FORMAT_DESCRIPTIONS[newFormat]);
            
            // Update field labels and input types based on format
            updateFieldLabelsAndTypes(newFormat);
            
            // Convert and display coordinates in new format
            if (currentCoords != null) {
                displayCoordinatesInFormat(currentCoords[0], currentCoords[1], newFormat);
            }
            
        } catch (Exception e) {
            // If conversion fails, just update the format without converting
            currentFormat = newFormat;
            tvFormatDescription.setText(FORMAT_DESCRIPTIONS[newFormat]);
            updateFieldLabelsAndTypes(newFormat);
        }
    }

    private void updateFieldLabelsAndTypes(int format) {
        switch (format) {
            case FORMAT_DMS:
                etLatDegrees.setHint("Deg");
                etLatMinutes.setHint("Min");
                etLatSecondsWhole.setHint("Sec");
                etLatSecondsDecimal.setHint("Dec");
                etLonDegrees.setHint("Deg");
                etLonMinutes.setHint("Min");
                etLonSecondsWhole.setHint("Sec");
                etLonSecondsDecimal.setHint("Dec");
                break;
                
            case FORMAT_DECIMAL:
                etLatDegrees.setHint("Latitude");
                etLatMinutes.setHint("(unused)");
                etLatSecondsWhole.setHint("(unused)");
                etLatSecondsDecimal.setHint("(unused)");
                etLonDegrees.setHint("Longitude");
                etLonMinutes.setHint("(unused)");
                etLonSecondsWhole.setHint("(unused)");
                etLonSecondsDecimal.setHint("(unused)");
                break;
                
            case FORMAT_GPS:
                etLatDegrees.setHint("GPS Coordinates");
                etLatMinutes.setHint("(use Deg field)");
                etLatSecondsWhole.setHint("(use Deg field)");
                etLatSecondsDecimal.setHint("(use Deg field)");
                etLonDegrees.setHint("(unused)");
                etLonMinutes.setHint("(unused)");
                etLonSecondsWhole.setHint("(unused)");
                etLonSecondsDecimal.setHint("(unused)");
                break;
        }
    }

    private void displayCoordinatesInFormat(double lat, double lon, int format) {
        isUpdatingFields = true;
        
        switch (format) {
            case FORMAT_DMS:
                displayAsDMS(lat, lon);
                break;
            case FORMAT_DECIMAL:
                displayAsDecimal(lat, lon);
                break;
            case FORMAT_GPS:
                displayAsGPS(lat, lon);
                break;
        }
        
        isUpdatingFields = false;
    }

    private void displayAsDMS(double lat, double lon) {
        // Convert latitude to DMS
        DMSCoordinate latDMS = decimalToDMS(Math.abs(lat));
        etLatDegrees.setText(String.valueOf(latDMS.degrees));
        etLatMinutes.setText(String.valueOf(latDMS.minutes));
        etLatSecondsWhole.setText(String.valueOf(latDMS.secondsWhole));
        etLatSecondsDecimal.setText(String.valueOf(latDMS.secondsDecimal));
        
        // Set latitude direction
        ((RadioButton)findViewById(lat >= 0 ? R.id.rbLatNorth : R.id.rbLatSouth)).setChecked(true);
        
        // Convert longitude to DMS
        DMSCoordinate lonDMS = decimalToDMS(Math.abs(lon));
        etLonDegrees.setText(String.valueOf(lonDMS.degrees));
        etLonMinutes.setText(String.valueOf(lonDMS.minutes));
        etLonSecondsWhole.setText(String.valueOf(lonDMS.secondsWhole));
        etLonSecondsDecimal.setText(String.valueOf(lonDMS.secondsDecimal));
        
        // Set longitude direction
        ((RadioButton)findViewById(lon >= 0 ? R.id.rbLonEast : R.id.rbLonWest)).setChecked(true);
    }

    private void displayAsDecimal(double lat, double lon) {
        etLatDegrees.setText(decimalFormat.format(lat));
        etLatMinutes.setText("");
        etLatSecondsWhole.setText("");
        etLatSecondsDecimal.setText("");
        
        etLonDegrees.setText(decimalFormat.format(lon));
        etLonMinutes.setText("");
        etLonSecondsWhole.setText("");
        etLonSecondsDecimal.setText("");
        
        // Set directions based on sign
        ((RadioButton)findViewById(lat >= 0 ? R.id.rbLatNorth : R.id.rbLatSouth)).setChecked(true);
        ((RadioButton)findViewById(lon >= 0 ? R.id.rbLonEast : R.id.rbLonWest)).setChecked(true);
    }

    private void displayAsGPS(double lat, double lon) {
        String gpsString = formatAsGPS(lat, lon);
        etLatDegrees.setText(gpsString);
        etLatMinutes.setText("");
        etLatSecondsWhole.setText("");
        etLatSecondsDecimal.setText("");
        
        etLonDegrees.setText("");
        etLonMinutes.setText("");
        etLonSecondsWhole.setText("");
        etLonSecondsDecimal.setText("");
        
        // Set directions
        ((RadioButton)findViewById(lat >= 0 ? R.id.rbLatNorth : R.id.rbLatSouth)).setChecked(true);
        ((RadioButton)findViewById(lon >= 0 ? R.id.rbLonEast : R.id.rbLonWest)).setChecked(true);
    }

    private String formatAsGPS(double lat, double lon) {
        DMSCoordinate latDMS = decimalToDMS(Math.abs(lat));
        DMSCoordinate lonDMS = decimalToDMS(Math.abs(lon));
        
        String latDir = lat >= 0 ? "N" : "S";
        String lonDir = lon >= 0 ? "E" : "W";
        
        return String.format("%d°%d'%.2f\"%s %d°%d'%.2f\"%s",
            latDMS.degrees, latDMS.minutes, latDMS.secondsWhole + (latDMS.secondsDecimal / 100.0), latDir,
            lonDMS.degrees, lonDMS.minutes, lonDMS.secondsWhole + (lonDMS.secondsDecimal / 100.0), lonDir);
    }

    private void convertToCurrentFormat() {
        try {
            double[] coords = getCurrentCoordinatesAsDecimal();
            if (coords != null) {
                displayCoordinatesInFormat(coords[0], coords[1], currentFormat);
            }
        } catch (Exception e) {
            // Ignore conversion errors during typing
        }
    }

    private double[] getCurrentCoordinatesAsDecimal() throws Exception {
        switch (currentFormat) {
            case FORMAT_DMS:
                return getDMSCoordinates();
            case FORMAT_DECIMAL:
                return getDecimalCoordinates();
            case FORMAT_GPS:
                return getGPSCoordinates();
            default:
                throw new Exception("Invalid coordinate format");
        }
    }

    private double[] getDMSCoordinates() throws Exception {
        try {
            // Latitude
            String latDegStr = etLatDegrees.getText().toString().trim();
            String latMinStr = etLatMinutes.getText().toString().trim();
            String latSecWholeStr = etLatSecondsWhole.getText().toString().trim();
            String latSecDecStr = etLatSecondsDecimal.getText().toString().trim();
            
            if (latDegStr.isEmpty() || latMinStr.isEmpty() || latSecWholeStr.isEmpty()) {
                throw new Exception("Please fill in all latitude fields");
            }
            
            int latDeg = Integer.parseInt(latDegStr);
            int latMin = Integer.parseInt(latMinStr);
            int latSecWhole = Integer.parseInt(latSecWholeStr);
            int latSecDec = latSecDecStr.isEmpty() ? 0 : Integer.parseInt(latSecDecStr);
            boolean isNorth = ((RadioButton)findViewById(R.id.rbLatNorth)).isChecked();
            
            // Longitude
            String lonDegStr = etLonDegrees.getText().toString().trim();
            String lonMinStr = etLonMinutes.getText().toString().trim();
            String lonSecWholeStr = etLonSecondsWhole.getText().toString().trim();
            String lonSecDecStr = etLonSecondsDecimal.getText().toString().trim();
            
            if (lonDegStr.isEmpty() || lonMinStr.isEmpty() || lonSecWholeStr.isEmpty()) {
                throw new Exception("Please fill in all longitude fields");
            }
            
            int lonDeg = Integer.parseInt(lonDegStr);
            int lonMin = Integer.parseInt(lonMinStr);
            int lonSecWhole = Integer.parseInt(lonSecWholeStr);
            int lonSecDec = lonSecDecStr.isEmpty() ? 0 : Integer.parseInt(lonSecDecStr);
            boolean isEast = ((RadioButton)findViewById(R.id.rbLonEast)).isChecked();
            
            // Validate ranges
            if (latDeg > 90 || latMin > 59 || latSecWhole > 59 || latSecDec > 99) {
                throw new Exception("Invalid latitude values");
            }
            if (lonDeg > 180 || lonMin > 59 || lonSecWhole > 59 || lonSecDec > 99) {
                throw new Exception("Invalid longitude values");
            }
            
            // Convert to decimal
            double lat = dmsToDecimal(latDeg, latMin, latSecWhole, latSecDec, isNorth);
            double lon = dmsToDecimal(lonDeg, lonMin, lonSecWhole, lonSecDec, isEast);
            
            return new double[]{lat, lon};
            
        } catch (NumberFormatException e) {
            throw new Exception("Please fill in all coordinate fields with valid numbers");
        }
    }

    private double[] getDecimalCoordinates() throws Exception {
        try {
            String latStr = etLatDegrees.getText().toString().trim();
            String lonStr = etLonDegrees.getText().toString().trim();
            
            if (latStr.isEmpty() || lonStr.isEmpty()) {
                throw new Exception("Please enter both latitude and longitude");
            }
            
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            
            // Apply direction if needed (for backward compatibility)
            boolean isNorth = ((RadioButton)findViewById(R.id.rbLatNorth)).isChecked();
            boolean isEast = ((RadioButton)findViewById(R.id.rbLonEast)).isChecked();
            
            if (!isNorth && lat > 0) lat = -lat;
            if (!isEast && lon > 0) lon = -lon;
            
            // Validate ranges
            if (lat < -90 || lat > 90) {
                throw new Exception("Latitude must be between -90 and 90 degrees");
            }
            if (lon < -180 || lon > 180) {
                throw new Exception("Longitude must be between -180 and 180 degrees");
            }
            
            return new double[]{lat, lon};
            
        } catch (NumberFormatException e) {
            throw new Exception("Please enter valid decimal coordinates");
        }
    }

    private double[] getGPSCoordinates() throws Exception {
        String gpsText = etLatDegrees.getText().toString().trim();
        if (gpsText.isEmpty()) {
            throw new Exception("Please enter GPS coordinates");
        }
        
        return parseGPSCoordinates(gpsText);
    }

    private double[] parseGPSCoordinates(String gpsText) throws Exception {
        // Pattern for GPS coordinates: 40°45'30.25"N 73°58'45.75"W
        Pattern pattern = Pattern.compile(
            "(\\d+)°(\\d+)'([\\d.]+)\"([NS])\\s+(\\d+)°(\\d+)'([\\d.]+)\"([EW])"
        );
        
        Matcher matcher = pattern.matcher(gpsText);
        if (!matcher.find()) {
            throw new Exception("Invalid GPS format. Use: 40°45'30.25\"N 73°58'45.75\"W");
        }
        
        // Parse latitude
        int latDeg = Integer.parseInt(matcher.group(1));
        int latMin = Integer.parseInt(matcher.group(2));
        double latSec = Double.parseDouble(matcher.group(3));
        boolean isNorth = matcher.group(4).equals("N");
        
        // Parse longitude
        int lonDeg = Integer.parseInt(matcher.group(5));
        int lonMin = Integer.parseInt(matcher.group(6));
        double lonSec = Double.parseDouble(matcher.group(7));
        boolean isEast = matcher.group(8).equals("E");
        
        // Convert to decimal
        double lat = latDeg + (latMin / 60.0) + (latSec / 3600.0);
        double lon = lonDeg + (lonMin / 60.0) + (lonSec / 3600.0);
        
        if (!isNorth) lat = -lat;
        if (!isEast) lon = -lon;
        
        return new double[]{lat, lon};
    }

    private DMSCoordinate decimalToDMS(double decimal) {
        int degrees = (int) decimal;
        double minutesFloat = (decimal - degrees) * 60;
        int minutes = (int) minutesFloat;
        double secondsFloat = (minutesFloat - minutes) * 60;
        int secondsWhole = (int) secondsFloat;
        int secondsDecimal = (int) Math.round((secondsFloat - secondsWhole) * 100);
        
        // Handle rounding edge case
        if (secondsDecimal >= 100) {
            secondsDecimal = 0;
            secondsWhole++;
            if (secondsWhole >= 60) {
                secondsWhole = 0;
                minutes++;
                if (minutes >= 60) {
                    minutes = 0;
                    degrees++;
                }
            }
        }
        
        return new DMSCoordinate(degrees, minutes, secondsWhole, secondsDecimal);
    }

    private double dmsToDecimal(int degrees, int minutes, int secondsWhole, int secondsDecimal, boolean isPositive) {
        double seconds = secondsWhole + (secondsDecimal / 100.0);
        double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);
        return isPositive ? decimal : -decimal;
    }
    
    // Helper class for DMS coordinates
    private static class DMSCoordinate {
        int degrees;
        int minutes;
        int secondsWhole;
        int secondsDecimal;
        
        DMSCoordinate(int degrees, int minutes, int secondsWhole, int secondsDecimal) {
            this.degrees = degrees;
            this.minutes = minutes;
            this.secondsWhole = secondsWhole;
            this.secondsDecimal = secondsDecimal;
        }
    }

    private static class Airport {
        String name, type, iataCode, icaoCode, municipality, country;
        double latitude, longitude;

        Airport(String name, double latitude, double longitude, String type,
                String iataCode, String icaoCode, String municipality, String country) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.type = type;
            this.iataCode = iataCode;
            this.icaoCode = icaoCode;
            this.municipality = municipality;
            this.country = country;
        }
    }

    private static class AirportDistance {
        Airport airport;
        double distance;

        AirportDistance(Airport airport, double distance) {
            this.airport = airport;
            this.distance = distance;
        }
    }
}

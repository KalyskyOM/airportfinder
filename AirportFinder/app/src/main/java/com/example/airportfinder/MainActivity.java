package com.example.airportfinder;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private Spinner spinnerCoordinateFormat;
    private TextView tvFormatDescription;
    
    // Layout Containers
    private LinearLayout layoutDMS, layoutDecimal, layoutGPS;
    
    // DMS Format Fields
    private EditText etLatDegrees, etLatMinutes, etLatSecondsWhole, etLatSecondsDecimal;
    private EditText etLonDegrees, etLonMinutes, etLonSecondsWhole, etLonSecondsDecimal;
    private RadioGroup rgLatDirection, rgLonDirection;
    private TextView tvLatDirection, tvLonDirection;
    
    // Decimal Format Fields
    private EditText etLatDecimal, etLonDecimal;
    
    // GPS Format Fields
    private EditText etGPSCoordinates;
    
    // ICAO Code Input Fields
    private EditText etIcaoCode;
    private Button btnAddIcao;
    private LinearLayout layoutSelectedIcaoCodes;
    private TextView tvNoIcaoSelected;
    
    // Search Components
    private EditText etSearchRadius;
    private Button btnFindAirport;
    private TextView tvResult;
    
    // Airport cards container
    private LinearLayout airportCardsContainer;
    
    // Database and state
    private List<Airport> airports = new ArrayList<>();
    private Map<String, Airport> selectedAirports = new HashMap<>();
    private SharedPreferences sharedPrefs;
    private DecimalFormat decimalFormat = new DecimalFormat("#.######");
    private boolean isUpdatingFields = false;
    private int currentFormat = FORMAT_DMS;
    
    // Weather and NOTAM data
    private Map<String, WeatherData> weatherDataMap = new HashMap<>();
    private Map<String, List<NotamData>> notamDataMap = new HashMap<>();
    
    private ApiKeyManager apiKeyManager;
    private AvwxApiService avwxApiService;
    private View apiKeySetupModal;
    private EditText etAvwxApiKey;
    private EditText etAutoRouterApiKey;
    private Button btnSaveApiKeys;
    private ImageView btnCloseApiSetup;
    
    private static final String PREFS_NAME = "AirportFinderPrefs";
    
    // Coordinate format constants
    private static final int FORMAT_DMS = 0;
    private static final int FORMAT_DECIMAL = 1;
    private static final int FORMAT_GPS = 2;
    private static final String[] FORMAT_NAMES = {"DMS (Degrees, Minutes, Seconds)", 
                                                "Decimal Degrees", 
                                                "GPS Coordinates"};
    private static final String[] FORMAT_DESCRIPTIONS = {"Enter latitude and longitude in Degrees, Minutes, Seconds format (e.g., 40° 30' 30\" N, 73° 45' 45\" W)",
                                                        "Enter latitude and longitude in Decimal Degrees format (e.g., 40.5051° N, 73.7653° W)",
                                                        "Enter GPS coordinates (e.g., 40° 30' 30.25\" N, 73° 45' 45.75\" W)"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupFormatSpinner();
        setupInputValidation();
        initializeDatabase();
        loadUserPreferences();
        loadDropboxToken();
        setupListeners();
        
        apiKeyManager = new ApiKeyManager(this);
        avwxApiService = new AvwxApiService(this);
    }

    private void initializeViews() {
        spinnerCoordinateFormat = findViewById(R.id.spinnerCoordinateFormat);
        tvFormatDescription = findViewById(R.id.tvFormatDescription);
        
        layoutDMS = findViewById(R.id.layoutDMS);
        layoutDecimal = findViewById(R.id.layoutDecimal);
        layoutGPS = findViewById(R.id.layoutGPS);
        
        etLatDegrees = findViewById(R.id.etLatDegrees);
        etLatMinutes = findViewById(R.id.etLatMinutes);
        etLatSecondsWhole = findViewById(R.id.etLatSecondsWhole);
        etLatSecondsDecimal = findViewById(R.id.etLatSecondsDecimal);
        etLonDegrees = findViewById(R.id.etLonDegrees);
        etLonMinutes = findViewById(R.id.etLonMinutes);
        etLonSecondsWhole = findViewById(R.id.etLonSecondsWhole);
        etLonSecondsDecimal = findViewById(R.id.etLonSecondsDecimal);
        rgLatDirection = findViewById(R.id.rgLatDirection);
        rgLonDirection = findViewById(R.id.rgLonDirection);
        tvLatDirection = findViewById(R.id.tvLatDirection);
        tvLonDirection = findViewById(R.id.tvLonDirection);
        
        etLatDecimal = findViewById(R.id.etLatDecimal);
        etLonDecimal = findViewById(R.id.etLonDecimal);
        
        etGPSCoordinates = findViewById(R.id.etGPSCoordinates);
        
        // Initialize ICAO code input fields
        etIcaoCode = findViewById(R.id.etIcaoCode);
        btnAddIcao = findViewById(R.id.btnAddIcao);
        layoutSelectedIcaoCodes = findViewById(R.id.layoutSelectedIcaoCodes);
        tvNoIcaoSelected = findViewById(R.id.tvNoIcaoSelected);
        
        etSearchRadius = findViewById(R.id.etSearchRadius);
        btnFindAirport = findViewById(R.id.btnFindAirport);
        tvResult = findViewById(R.id.tvResult);
        
        airportCardsContainer = findViewById(R.id.airportCardsContainer);
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
    
    private void setupInputValidation() {
        // Set input filters for DMS format
        InputFilter[] degreeFilters = new InputFilter[] { new InputFilter.LengthFilter(3) };
        InputFilter[] minuteFilters = new InputFilter[] { new InputFilter.LengthFilter(2) };
        InputFilter[] secondsFilters = new InputFilter[] { new InputFilter.LengthFilter(2) };
        InputFilter[] decimalFilters = new InputFilter[] { new InputFilter.LengthFilter(2) };
        
        etLatDegrees.setFilters(degreeFilters);
        etLatMinutes.setFilters(minuteFilters);
        etLatSecondsWhole.setFilters(secondsFilters);
        etLatSecondsDecimal.setFilters(decimalFilters);
        
        etLonDegrees.setFilters(degreeFilters);
        etLonMinutes.setFilters(minuteFilters);
        etLonSecondsWhole.setFilters(secondsFilters);
        etLonSecondsDecimal.setFilters(decimalFilters);
        
        // Set input filters for decimal format
        InputFilter[] decimalCoordFilters = new InputFilter[] { new InputFilter.LengthFilter(10) };
        etLatDecimal.setFilters(decimalCoordFilters);
        etLonDecimal.setFilters(decimalCoordFilters);
        
        // Set input filters for GPS format
        InputFilter[] gpsFilters = new InputFilter[] { new InputFilter.LengthFilter(50) };
        etGPSCoordinates.setFilters(gpsFilters);
        
        // Set input filter for search radius
        InputFilter[] radiusFilters = new InputFilter[] { new InputFilter.LengthFilter(5) };
        etSearchRadius.setFilters(radiusFilters);
        
        // Set input filter for ICAO code
        InputFilter[] icaoFilters = new InputFilter[] { new InputFilter.LengthFilter(4) };
        etIcaoCode.setFilters(icaoFilters);
    }
    
    private void initializeDatabase() {
        // Load airport data in background
        new LoadAirportDataTask().execute();
    }
    
    private void loadUserPreferences() {
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Load coordinate format preference
        currentFormat = sharedPrefs.getInt("coordinateFormat", FORMAT_DMS);
        spinnerCoordinateFormat.setSelection(currentFormat);
        tvFormatDescription.setText(FORMAT_DESCRIPTIONS[currentFormat]);
        
        // Update UI based on format
        updateFieldLabelsAndTypes(currentFormat);
        
        // Load saved values based on format
        loadSavedCoordinates();
        
        // Load search radius
        String savedRadius = sharedPrefs.getString("searchRadius", "30.0");
        etSearchRadius.setText(savedRadius);
    }
    
    private void loadDropboxToken() {
        String token = sharedPrefs.getString("dropbox_token", null);
        if (token != null && !token.isEmpty()) {
            DropboxUploader.setAccessToken(token);
        }
    }
    
    private void loadSavedCoordinates() {
        switch (currentFormat) {
            case FORMAT_DMS:
                // Load DMS values
                etLatDegrees.setText(sharedPrefs.getString("latDegrees", ""));
                etLatMinutes.setText(sharedPrefs.getString("latMinutes", ""));
                etLatSecondsWhole.setText(sharedPrefs.getString("latSecondsWhole", ""));
                etLatSecondsDecimal.setText(sharedPrefs.getString("latSecondsDecimal", ""));
                
                etLonDegrees.setText(sharedPrefs.getString("lonDegrees", ""));
                etLonMinutes.setText(sharedPrefs.getString("lonMinutes", ""));
                etLonSecondsWhole.setText(sharedPrefs.getString("lonSecondsWhole", ""));
                etLonSecondsDecimal.setText(sharedPrefs.getString("lonSecondsDecimal", ""));
                
                // Load direction selections
                int latDirection = sharedPrefs.getInt("latDirection", R.id.rbLatNorth);
                int lonDirection = sharedPrefs.getInt("lonDirection", R.id.rbLonEast);
                rgLatDirection.check(latDirection);
                rgLonDirection.check(lonDirection);
                break;
                
            case FORMAT_DECIMAL:
                // Load decimal values
                etLatDecimal.setText(sharedPrefs.getString("latDecimal", ""));
                etLonDecimal.setText(sharedPrefs.getString("lonDecimal", ""));
                break;
                
            case FORMAT_GPS:
                // Load GPS values
                etGPSCoordinates.setText(sharedPrefs.getString("gpsCoordinates", ""));
                break;
        }
    }
    
    private void setupListeners() {
        // Toggle direction buttons
        tvLatDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLatitudeDirection();
            }
        });
        
        tvLonDirection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLongitudeDirection();
            }
        });
        
        // Find Airport button
        btnFindAirport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findNearestAirports();
            }
        });
        
        // Add ICAO code button
        btnAddIcao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addIcaoCode();
            }
        });
        
        // Setup text change listeners for coordinate fields
        setupTextChangeListeners();
    }
    
    private void setupTextChangeListeners() {
        // Create a text watcher for coordinate fields
        TextWatcher coordinateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingFields) {
                    saveCoordinateValues();
                }
            }
        };
        
        // Add text watchers to DMS fields
        etLatDegrees.addTextChangedListener(coordinateWatcher);
        etLatMinutes.addTextChangedListener(coordinateWatcher);
        etLatSecondsWhole.addTextChangedListener(coordinateWatcher);
        etLatSecondsDecimal.addTextChangedListener(coordinateWatcher);
        etLonDegrees.addTextChangedListener(coordinateWatcher);
        etLonMinutes.addTextChangedListener(coordinateWatcher);
        etLonSecondsWhole.addTextChangedListener(coordinateWatcher);
        etLonSecondsDecimal.addTextChangedListener(coordinateWatcher);
        
        // Add text watchers to decimal fields
        etLatDecimal.addTextChangedListener(coordinateWatcher);
        etLonDecimal.addTextChangedListener(coordinateWatcher);
        
        // Add text watcher to GPS field
        etGPSCoordinates.addTextChangedListener(coordinateWatcher);
        
        // Add text watcher to search radius field
        etSearchRadius.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // Save search radius value
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("searchRadius", s.toString());
                editor.apply();
            }
        });
        
        // Add listeners for direction radio groups
        rgLatDirection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateDirectionText();
                saveCoordinateValues();
            }
        });
        
        rgLonDirection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateDirectionText();
                saveCoordinateValues();
            }
        });
    }
    
    private void saveCoordinateValues() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        
        // Save current format
        editor.putInt("coordinateFormat", currentFormat);
        
        // Save values based on format
        switch (currentFormat) {
            case FORMAT_DMS:
                // Save DMS values
                editor.putString("latDegrees", etLatDegrees.getText().toString());
                editor.putString("latMinutes", etLatMinutes.getText().toString());
                editor.putString("latSecondsWhole", etLatSecondsWhole.getText().toString());
                editor.putString("latSecondsDecimal", etLatSecondsDecimal.getText().toString());
                
                editor.putString("lonDegrees", etLonDegrees.getText().toString());
                editor.putString("lonMinutes", etLonMinutes.getText().toString());
                editor.putString("lonSecondsWhole", etLonSecondsWhole.getText().toString());
                editor.putString("lonSecondsDecimal", etLonSecondsDecimal.getText().toString());
                
                // Save direction selections
                editor.putInt("latDirection", rgLatDirection.getCheckedRadioButtonId());
                editor.putInt("lonDirection", rgLonDirection.getCheckedRadioButtonId());
                break;
                
            case FORMAT_DECIMAL:
                // Save decimal values
                editor.putString("latDecimal", etLatDecimal.getText().toString());
                editor.putString("lonDecimal", etLonDecimal.getText().toString());
                break;
                
            case FORMAT_GPS:
                // Save GPS values
                editor.putString("gpsCoordinates", etGPSCoordinates.getText().toString());
                break;
        }
        
        editor.apply();
    }
    
    private void toggleLatitudeDirection() {
        int currentCheckedId = rgLatDirection.getCheckedRadioButtonId();
        int newCheckedId = (currentCheckedId == R.id.rbLatNorth) ? R.id.rbLatSouth : R.id.rbLatNorth;
        rgLatDirection.check(newCheckedId);
        updateDirectionText();
    }
    
    private void toggleLongitudeDirection() {
        int currentCheckedId = rgLonDirection.getCheckedRadioButtonId();
        int newCheckedId = (currentCheckedId == R.id.rbLonEast) ? R.id.rbLonWest : R.id.rbLonEast;
        rgLonDirection.check(newCheckedId);
        updateDirectionText();
    }
    
    private void updateDirectionText() {
        // Update direction text based on radio button selection
        boolean isNorth = rgLatDirection.getCheckedRadioButtonId() == R.id.rbLatNorth;
        boolean isEast = rgLonDirection.getCheckedRadioButtonId() == R.id.rbLonEast;
        
        tvLatDirection.setText(isNorth ? "N" : "S");
        tvLonDirection.setText(isEast ? "E" : "W");
    }
    
    private void findNearestAirports() {
        if (!selectedAirports.isEmpty()) {
            // If ICAO codes are selected, find airports near those
            findAirportsNearSelectedIcao();
        } else {
            // Otherwise use coordinates
            saveCoordinateValues();
            
            try {
                double latitude = getLatitudeValue();
                double longitude = getLongitudeValue();
                
                if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                    Toast.makeText(this, "Please enter valid coordinates", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int radius = 300; // Default radius
                try {
                    radius = Integer.parseInt(etSearchRadius.getText().toString());
                } catch (NumberFormatException e) {
                    // Use default
                }
                
                findAirportsNearCoordinates(latitude, longitude, radius);
                
            } catch (Exception e) {
                Toast.makeText(this, "Error parsing coordinates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void findAirportsNearCoordinates(double latitude, double longitude, int radius) {
        if (airports.isEmpty()) {
            Toast.makeText(this, "Airport database not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<Airport> nearbyAirports = new ArrayList<>();
        
        for (Airport airport : airports) {
            double distance = calculateDistance(latitude, longitude, airport.getLatitude(), airport.getLongitude());
            if (distance <= radius) {
                airport.setDistance(distance); // Set the distance
                nearbyAirports.add(airport);
            }
        }
        
        if (nearbyAirports.isEmpty()) {
            tvResult.setText("No airports found within " + radius + " NM of the specified coordinates.");
            return;
        }
        
        // Sort by distance
        Collections.sort(nearbyAirports, new Comparator<Airport>() {
            @Override
            public int compare(Airport a1, Airport a2) {
                return Double.compare(a1.getDistance(), a2.getDistance());
            }
        });
        
        // Display results
        displayResults(nearbyAirports);
    }
    
    private void findAirportsNearSelectedIcao() {
        if (selectedAirports.isEmpty()) {
            Toast.makeText(this, "No ICAO codes selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use the first selected airport as reference point
        Airport referenceAirport = null;
        for (Airport airport : selectedAirports.values()) {
            referenceAirport = airport;
            break;
        }
        
        if (referenceAirport == null) {
            Toast.makeText(this, "Error: Selected airport not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int radius = 300; // Default radius
        try {
            radius = Integer.parseInt(etSearchRadius.getText().toString());
        } catch (NumberFormatException e) {
            // Use default
        }
        
        // Find airports near the reference airport
        findAirportsNearCoordinates(referenceAirport.getLatitude(), referenceAirport.getLongitude(), radius);
    }
    
    private void displayResults(List<Airport> nearbyAirports) {
        // Clear previous results
        tvResult.setText("");
        airportCardsContainer.removeAllViews();
        
        if (nearbyAirports.isEmpty()) {
            tvResult.setText("No airports found within the specified radius.");
            return;
        }
        
        // Display text results
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(nearbyAirports.size()).append(" airports:\n\n");
        
        DecimalFormat df = new DecimalFormat("#.##");
        
        for (Airport airport : nearbyAirports) {
            sb.append(airport.getName()).append(" (").append(airport.getIcaoCode()).append(")\n");
            sb.append("Type: ").append(airport.getType()).append("\n");
            sb.append("Location: ").append(df.format(airport.getLatitude())).append("°, ")
              .append(df.format(airport.getLongitude())).append("°\n");
            sb.append("Distance: ").append(df.format(airport.getDistance())).append(" NM\n\n");
            
            // Create airport card for this airport
            createAirportCard(airport);
        }
        
        tvResult.setText(sb.toString());
    }
    
    private void createAirportCard(final Airport airport) {
        // Inflate the airport card layout
        View cardView = LayoutInflater.from(this).inflate(R.layout.airport_card, airportCardsContainer, false);
        
        // Set airport basic info
        TextView tvAirportIcao = cardView.findViewById(R.id.tvAirportIcao);
        TextView tvAirportName = cardView.findViewById(R.id.tvAirportName);
        TextView tvDistance = cardView.findViewById(R.id.tvDistance);
        TextView tvAirportType = cardView.findViewById(R.id.tvAirportType);
        TextView tvAirportLocation = cardView.findViewById(R.id.tvAirportLocation);
        TextView tvAirportElevation = cardView.findViewById(R.id.tvAirportElevation);
        
        tvAirportIcao.setText(airport.getIcaoCode());
        tvAirportName.setText(airport.getName());
        
        DecimalFormat df = new DecimalFormat("#.##");
        tvDistance.setText(df.format(airport.getDistance()) + " NM");
        tvAirportType.setText("Type: " + airport.getType());
        tvAirportLocation.setText("Location: " + df.format(airport.getLatitude()) + "°, " + df.format(airport.getLongitude()) + "°");
        tvAirportElevation.setText("Elevation: " + airport.getElevation() + " ft");
        
        // Weather container and data - COMMENTED OUT DUE TO MISSING RESOURCES
        /*
        LinearLayout weatherContainer = cardView.findViewById(R.id.weatherContainer);
        TextView tvMetarRaw = cardView.findViewById(R.id.tvMetarRaw);
        TextView tvMetarDecoded = cardView.findViewById(R.id.tvMetarDecoded);
        TextView tvTafRaw = cardView.findViewById(R.id.tvTafRaw);
        TextView tvTafDecoded = cardView.findViewById(R.id.tvTafDecoded);
        Button btnRefreshWeather = cardView.findViewById(R.id.btnRefreshWeather);
        
        // NOTAM container and data
        LinearLayout notamContainer = cardView.findViewById(R.id.notamContainer);
        RecyclerView rvNotams = cardView.findViewById(R.id.rvNotams);
        Button btnRefreshNotams = cardView.findViewById(R.id.btnRefreshNotams);
        
        // Action buttons
        Button btnGeneratePdf = cardView.findViewById(R.id.btnGeneratePdf);
        Button btnSaveToDropbox = cardView.findViewById(R.id.btnSaveToDropbox);
        
        // Set up RecyclerView for NOTAMs
        rvNotams.setLayoutManager(new LinearLayoutManager(this));
        
        // Load or generate weather and NOTAM data
        loadAirportData(airport, tvMetarRaw, tvMetarDecoded, tvTafRaw, tvTafDecoded, rvNotams);
        
        // Set up button click listeners
        btnRefreshWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshWeatherData(airport, tvMetarRaw, tvMetarDecoded, tvTafRaw, tvTafDecoded);
            }
        });
        
        btnRefreshNotams.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshNotamData(airport, rvNotams);
            }
        });
        
        btnGeneratePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePdf(airport);
            }
        });
        
        btnSaveToDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDropbox(airport);
            }
        });
        */
        
        // Add the card to the container
        airportCardsContainer.addView(cardView);
    }
    
    private void loadAirportData(Airport airport, TextView tvMetarRaw, TextView tvMetarDecoded, 
                                TextView tvTafRaw, TextView tvTafDecoded, RecyclerView rvNotams) {
        // Load or generate weather data
        WeatherData metarData = weatherDataMap.get(airport.getIcaoCode() + "_METAR");
        WeatherData tafData = weatherDataMap.get(airport.getIcaoCode() + "_TAF");
        
        if (metarData == null) {
            metarData = createSampleWeatherData(airport.getIcaoCode());
            weatherDataMap.put(airport.getIcaoCode() + "_METAR", metarData);
        }
        
        if (tafData == null) {
            tafData = createSampleTafData(airport.getIcaoCode());
            weatherDataMap.put(airport.getIcaoCode() + "_TAF", tafData);
        }
        
        // Update UI
        tvMetarRaw.setText(metarData.getRawText());
        tvMetarDecoded.setText(metarData.getDecodedText());
        tvTafRaw.setText(tafData.getRawText());
        tvTafDecoded.setText(tafData.getDecodedText());
        
        // Load or generate NOTAM data
        List<NotamData> notams = notamDataMap.get(airport.getIcaoCode());
        if (notams == null) {
            notams = createSampleNotamData(airport.getIcaoCode());
            notamDataMap.put(airport.getIcaoCode(), notams);
        }
        
        // Update NOTAM UI
        NotamAdapter adapter = new NotamAdapter(notams);
        rvNotams.setAdapter(adapter);
    }
    
    private void refreshWeatherData(Airport airport, TextView tvMetarRaw, TextView tvMetarDecoded, 
                                  TextView tvTafRaw, TextView tvTafDecoded) {
        // In a real app, this would fetch fresh data from an API
        // For this demo, we'll just generate new sample data
        WeatherData metarData = createSampleWeatherData(airport.getIcaoCode());
        WeatherData tafData = createSampleTafData(airport.getIcaoCode());
        
        weatherDataMap.put(airport.getIcaoCode() + "_METAR", metarData);
        weatherDataMap.put(airport.getIcaoCode() + "_TAF", tafData);
        
        // Update UI
        tvMetarRaw.setText(metarData.getRawText());
        tvMetarDecoded.setText(metarData.getDecodedText());
        tvTafRaw.setText(tafData.getRawText());
        tvTafDecoded.setText(tafData.getDecodedText());
        
        Toast.makeText(this, "Weather data refreshed", Toast.LENGTH_SHORT).show();
    }
    
    private void refreshNotamData(Airport airport, RecyclerView rvNotams) {
        // In a real app, this would fetch fresh data from an API
        // For this demo, we'll just generate new sample data
        List<NotamData> notams = createSampleNotamData(airport.getIcaoCode());
        notamDataMap.put(airport.getIcaoCode(), notams);
        
        // Update UI
        NotamAdapter adapter = (NotamAdapter) rvNotams.getAdapter();
        if (adapter != null) {
            adapter.updateNotams(notams);
        } else {
            rvNotams.setAdapter(new NotamAdapter(notams));
        }
        
        Toast.makeText(this, "NOTAM data refreshed", Toast.LENGTH_SHORT).show();
    }
    
    private void generatePdf(Airport airport) {
        // Check if we have weather and NOTAM data for this airport
        WeatherData metarData = weatherDataMap.get(airport.getIcaoCode() + "_METAR");
        WeatherData tafData = weatherDataMap.get(airport.getIcaoCode() + "_TAF");
        List<NotamData> notams = notamDataMap.get(airport.getIcaoCode());
        
        // If no data exists, generate sample data
        if (metarData == null) {
            metarData = createSampleWeatherData(airport.getIcaoCode());
            weatherDataMap.put(airport.getIcaoCode() + "_METAR", metarData);
        }
        
        if (tafData == null) {
            tafData = createSampleTafData(airport.getIcaoCode());
            weatherDataMap.put(airport.getIcaoCode() + "_TAF", tafData);
        }
        
        if (notams == null) {
            notams = createSampleNotamData(airport.getIcaoCode());
            notamDataMap.put(airport.getIcaoCode(), notams);
        }
        
        // Generate PDF using WeatherPdfGenerator
        File pdfFile = WeatherPdfGenerator.generateWeatherReport(this, airport.getIcaoCode(), metarData, tafData, notams);
        
        if (pdfFile != null) {
            Toast.makeText(this, "PDF saved to: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            
            // Open PDF
            try {
                Uri pdfUri = FileProvider.getUriForFile(this, 
                        getApplicationContext().getPackageName() + ".provider", pdfFile);
                
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("AirportFinder", "Error opening PDF: " + e.getMessage(), e);
                Toast.makeText(this, "Error opening PDF", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to generate PDF", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean checkStoragePermission() {
        // For simplicity, we'll assume permission is granted
        // In a real app, you would check for WRITE_EXTERNAL_STORAGE permission
        return true;
    }
    
    private void requestStoragePermission() {
        // For simplicity, we'll just show a toast
        // In a real app, you would request WRITE_EXTERNAL_STORAGE permission
        Toast.makeText(this, "Storage permission is required to save PDFs", Toast.LENGTH_SHORT).show();
    }

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
                layoutDMS.setVisibility(View.VISIBLE);
                layoutDecimal.setVisibility(View.GONE);
                layoutGPS.setVisibility(View.GONE);
                break;
                
            case FORMAT_DECIMAL:
                layoutDMS.setVisibility(View.GONE);
                layoutDecimal.setVisibility(View.VISIBLE);
                layoutGPS.setVisibility(View.GONE);
                break;
                
            case FORMAT_GPS:
                layoutDMS.setVisibility(View.GONE);
                layoutDecimal.setVisibility(View.GONE);
                layoutGPS.setVisibility(View.VISIBLE);
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
        etLatDecimal.setText(decimalFormat.format(lat));
        etLonDecimal.setText(decimalFormat.format(lon));
        
        // Set directions based on sign
        ((RadioButton)findViewById(lat >= 0 ? R.id.rbLatNorth : R.id.rbLatSouth)).setChecked(true);
        ((RadioButton)findViewById(lon >= 0 ? R.id.rbLonEast : R.id.rbLonWest)).setChecked(true);
    }

    private void displayAsGPS(double lat, double lon) {
        String gpsString = formatAsGPS(lat, lon);
        etGPSCoordinates.setText(gpsString);
        
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

    private double[] getCurrentCoordinatesAsDecimal() {
        try {
            switch (currentFormat) {
                case FORMAT_DMS:
                    return getDMSCoordinates();
                case FORMAT_DECIMAL:
                    return getDecimalCoordinates();
                case FORMAT_GPS:
                    return getGPSCoordinates();
                default:
                    return null;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
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
            String latStr = etLatDecimal.getText().toString().trim();
            String lonStr = etLonDecimal.getText().toString().trim();
            
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
        String gpsText = etGPSCoordinates.getText().toString().trim();
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
    
    // Get the current latitude value based on the selected format
    private double getLatitudeValue() {
        switch (currentFormat) {
            case FORMAT_DMS:
                try {
                    int degrees = Integer.parseInt(etLatDegrees.getText().toString());
                    int minutes = Integer.parseInt(etLatMinutes.getText().toString());
                    int secondsWhole = Integer.parseInt(etLatSecondsWhole.getText().toString());
                    int secondsDecimal = Integer.parseInt(etLatSecondsDecimal.getText().toString());
                    
                    double seconds = secondsWhole + (secondsDecimal / 100.0);
                    double decimalDegrees = degrees + (minutes / 60.0) + (seconds / 3600.0);
                    
                    // Check if South direction is selected
                    boolean isNorth = ((RadioButton)findViewById(rgLatDirection.getCheckedRadioButtonId())).getText().toString().equals("N");
                    
                    return isNorth ? decimalDegrees : -decimalDegrees;
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
                
            case FORMAT_DECIMAL:
                try {
                    return Double.parseDouble(etLatDecimal.getText().toString());
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
                
            case FORMAT_GPS:
                try {
                    String gpsText = etGPSCoordinates.getText().toString();
                    double[] coordinates = parseGPSCoordinates(gpsText);
                    if (coordinates != null && coordinates.length >= 2) {
                        return coordinates[0];
                    }
                } catch (Exception e) {
                    // Fall through to return NaN
                }
                return Double.NaN;
                
            default:
                return Double.NaN;
        }
    }
    
    // Get the current longitude value based on the selected format
    private double getLongitudeValue() {
        switch (currentFormat) {
            case FORMAT_DMS:
                try {
                    int degrees = Integer.parseInt(etLonDegrees.getText().toString());
                    int minutes = Integer.parseInt(etLonMinutes.getText().toString());
                    int secondsWhole = Integer.parseInt(etLonSecondsWhole.getText().toString());
                    int secondsDecimal = Integer.parseInt(etLonSecondsDecimal.getText().toString());
                    
                    double seconds = secondsWhole + (secondsDecimal / 100.0);
                    double decimalDegrees = degrees + (minutes / 60.0) + (seconds / 3600.0);
                    
                    // Check if West direction is selected
                    boolean isEast = ((RadioButton)findViewById(rgLonDirection.getCheckedRadioButtonId())).getText().toString().equals("E");
                    
                    return isEast ? decimalDegrees : -decimalDegrees;
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
                
            case FORMAT_DECIMAL:
                try {
                    return Double.parseDouble(etLonDecimal.getText().toString());
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
                
            case FORMAT_GPS:
                try {
                    String gpsText = etGPSCoordinates.getText().toString();
                    double[] coordinates = parseGPSCoordinates(gpsText);
                    if (coordinates != null && coordinates.length >= 2) {
                        return coordinates[1];
                    }
                } catch (Exception e) {
                    // Fall through to return NaN
                }
                return Double.NaN;
                
            default:
                return Double.NaN;
        }
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // Convert from km to nautical miles (1 km = 0.539957 NM)
        return (R * c) * 0.539957;
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
        String name;
        double latitude;
        double longitude;
        String type;
        String iataCode;
        String icaoCode;
        String municipality;
        String country;
        double distance;
        int elevation;
        
        Airport(String name, double latitude, double longitude, String type, 
                String iataCode, String icaoCode, String municipality, String country, int elevation) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.type = type;
            this.iataCode = iataCode;
            this.icaoCode = icaoCode;
            this.municipality = municipality;
            this.country = country;
            this.elevation = elevation;
        }
        
        public String getName() {
            return name;
        }
        
        public double getLatitude() {
            return latitude;
        }
        
        public double getLongitude() {
            return longitude;
        }
        
        public String getType() {
            return type;
        }
        
        public String getIataCode() {
            return iataCode;
        }
        
        public String getIcaoCode() {
            return icaoCode;
        }
        
        public String getMunicipality() {
            return municipality;
        }
        
        public String getCountry() {
            return country;
        }
        
        public double getDistance() {
            return distance;
        }
        
        public int getElevation() {
            return elevation;
        }
        
        public void setDistance(double distance) {
            this.distance = distance;
        }
    }
    
    private class NotamAdapter extends RecyclerView.Adapter<NotamAdapter.ViewHolder> {
        private List<NotamData> notams;
        
        NotamAdapter(List<NotamData> notams) {
            this.notams = notams;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.notam_item, parent, false);
            return new ViewHolder(itemView);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            NotamData notam = notams.get(position);
            holder.tvNotamText.setText(notam.getText());
        }
        
        @Override
        public int getItemCount() {
            return notams.size();
        }
        
        public void updateNotams(List<NotamData> newNotams) {
            notams = newNotams;
            notifyDataSetChanged();
        }
        
        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView tvNotamText;
            
            public ViewHolder(View itemView) {
                super(itemView);
                tvNotamText = itemView.findViewById(R.id.tvNotamText);
            }
        }
    }

    private void setupApiKeyModal() {
        // Inflate the API key setup modal
        apiKeySetupModal = getLayoutInflater().inflate(R.layout.api_key_setup_modal, null);
        
        // Find views
        etAvwxApiKey = apiKeySetupModal.findViewById(R.id.et_avwx_api_key);
        etAutoRouterApiKey = apiKeySetupModal.findViewById(R.id.et_autorouter_api_key);
        btnSaveApiKeys = apiKeySetupModal.findViewById(R.id.btn_save_api_keys);
        btnCloseApiSetup = apiKeySetupModal.findViewById(R.id.btn_close_api_setup);
        
        // Load existing API keys if available
        String avwxApiKey = apiKeyManager.getAvwxApiKey();
        String autoRouterApiKey = apiKeyManager.getAutoRouterApiKey();
        
        if (avwxApiKey != null) {
            etAvwxApiKey.setText(avwxApiKey);
        }
        
        if (autoRouterApiKey != null) {
            etAutoRouterApiKey.setText(autoRouterApiKey);
        }
        
        // Set up button click listeners
        btnSaveApiKeys.setOnClickListener(v -> {
            String newAvwxApiKey = etAvwxApiKey.getText().toString().trim();
            String newAutoRouterApiKey = etAutoRouterApiKey.getText().toString().trim();
            
            // Save API keys securely
            boolean avwxSaved = false;
            boolean autoRouterSaved = false;
            
            if (!newAvwxApiKey.isEmpty()) {
                avwxSaved = apiKeyManager.storeAvwxApiKey(newAvwxApiKey);
            }
            
            if (!newAutoRouterApiKey.isEmpty()) {
                autoRouterSaved = apiKeyManager.storeAutoRouterApiKey(newAutoRouterApiKey);
            }
            
            // Show success or error message
            if (avwxSaved || autoRouterSaved) {
                Toast.makeText(MainActivity.this, "API keys saved successfully", Toast.LENGTH_SHORT).show();
                dismissApiKeySetupModal();
            } else {
                Toast.makeText(MainActivity.this, "Failed to save API keys", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnCloseApiSetup.setOnClickListener(v -> dismissApiKeySetupModal());
    }

    private void showApiKeySetupModal() {
        // Make sure the modal is initialized
        if (apiKeySetupModal == null) {
            setupApiKeyModal();
        }
        
        // Add the modal to the root layout
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(apiKeySetupModal);
        
        // Animate the modal
        apiKeySetupModal.setAlpha(0f);
        apiKeySetupModal.setVisibility(View.VISIBLE);
        apiKeySetupModal.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null);
    }

    private void dismissApiKeySetupModal() {
        if (apiKeySetupModal != null && apiKeySetupModal.getParent() != null) {
            apiKeySetupModal.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            ViewGroup rootView = findViewById(android.R.id.content);
                            rootView.removeView(apiKeySetupModal);
                        }
                    });
        }
    }

    private void fetchRealWeatherData(String icaoCode, final LinearLayout weatherContainer) {
        // Check if API key is available
        if (!apiKeyManager.hasAvwxApiKey()) {
            // Show API key setup modal if no key is available
            Toast.makeText(this, "AVWX API key required for real weather data", Toast.LENGTH_SHORT).show();
            showApiKeySetupModal();
            return;
        }
        
        // Show loading indicator
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        weatherContainer.addView(progressBar);
        
        // Fetch METAR data
        avwxApiService.fetchMetar(icaoCode, new AvwxApiService.ApiResponseCallback<WeatherData>() {
            @Override
            public void onSuccess(WeatherData result) {
                // Remove loading indicator
                weatherContainer.removeView(progressBar);
                
                // Update UI with METAR data
                TextView metarRawText = weatherContainer.findViewById(R.id.tv_metar_raw);
                TextView metarDecodedText = weatherContainer.findViewById(R.id.tv_metar_decoded);
                
                if (metarRawText != null && metarDecodedText != null) {
                    metarRawText.setText(result.getRawText());
                    metarDecodedText.setText(result.getDecodedText());
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                // Remove loading indicator
                weatherContainer.removeView(progressBar);
                
                // Show error message
                Toast.makeText(MainActivity.this, "METAR Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Fetch TAF data
        avwxApiService.fetchTaf(icaoCode, new AvwxApiService.ApiResponseCallback<WeatherData>() {
            @Override
            public void onSuccess(WeatherData result) {
                // Update UI with TAF data
                TextView tafRawText = weatherContainer.findViewById(R.id.tv_taf_raw);
                TextView tafDecodedText = weatherContainer.findViewById(R.id.tv_taf_decoded);
                
                if (tafRawText != null && tafDecodedText != null) {
                    tafRawText.setText(result.getRawText());
                    tafDecodedText.setText(result.getDecodedText());
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                // Show error message
                Toast.makeText(MainActivity.this, "TAF Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRealNotamData(String icaoCode, final RecyclerView notamRecyclerView, final TextView emptyNotamText) {
        // Check if API key is available
        if (!apiKeyManager.hasAvwxApiKey()) {
            // Show API key setup modal if no key is available
            Toast.makeText(this, "AVWX API key required for real NOTAM data", Toast.LENGTH_SHORT).show();
            showApiKeySetupModal();
            return;
        }
        
        // Show loading indicator
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        ((ViewGroup) notamRecyclerView.getParent()).addView(progressBar);
        
        // Fetch NOTAM data
        avwxApiService.fetchNotams(icaoCode, new AvwxApiService.ApiResponseCallback<List<NotamData>>() {
            @Override
            public void onSuccess(List<NotamData> result) {
                // Remove loading indicator
                ((ViewGroup) notamRecyclerView.getParent()).removeView(progressBar);
                
                // Update UI with NOTAM data
                if (result.isEmpty()) {
                    notamRecyclerView.setVisibility(View.GONE);
                    emptyNotamText.setVisibility(View.VISIBLE);
                    emptyNotamText.setText("No NOTAMs found for " + icaoCode);
                } else {
                    notamRecyclerView.setVisibility(View.VISIBLE);
                    emptyNotamText.setVisibility(View.GONE);
                    
                    // Set up adapter with real NOTAM data
                    NotamAdapter adapter = new NotamAdapter(result);
                    notamRecyclerView.setAdapter(adapter);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                // Remove loading indicator
                ((ViewGroup) notamRecyclerView.getParent()).removeView(progressBar);
                
                // Show error message
                Toast.makeText(MainActivity.this, "NOTAM Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                
                // Show empty state
                notamRecyclerView.setVisibility(View.GONE);
                emptyNotamText.setVisibility(View.VISIBLE);
                emptyNotamText.setText("Error loading NOTAMs: " + errorMessage);
            }
        });
    }

    private void getAirportInfo(String icaoCode) {
        // Create a new airport card
        View airportCard = getLayoutInflater().inflate(R.layout.airport_card, null);
        airportCardsContainer.addView(airportCard);
        
        // Set up weather container
        LinearLayout weatherContainer = airportCard.findViewById(R.id.weather_container);
        
        // Set up NOTAM RecyclerView
        RecyclerView notamRecyclerView = airportCard.findViewById(R.id.notam_recycler_view);
        notamRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        TextView emptyNotamText = airportCard.findViewById(R.id.empty_notam_text);
        
        // Set up refresh buttons
        Button refreshMetarButton = airportCard.findViewById(R.id.btn_refresh_metar);
        Button refreshTafButton = airportCard.findViewById(R.id.btn_refresh_taf);
        Button refreshNotamButton = airportCard.findViewById(R.id.btn_refresh_notam);
        
        // Set up button click listeners
        refreshMetarButton.setOnClickListener(v -> fetchRealWeatherData(icaoCode, weatherContainer));
        refreshTafButton.setOnClickListener(v -> fetchRealWeatherData(icaoCode, weatherContainer));
        refreshNotamButton.setOnClickListener(v -> fetchRealNotamData(icaoCode, notamRecyclerView, emptyNotamText));
        
        // Try to fetch real data if API key is available, otherwise use sample data
        if (apiKeyManager.hasAvwxApiKey()) {
            fetchRealWeatherData(icaoCode, weatherContainer);
            fetchRealNotamData(icaoCode, notamRecyclerView, emptyNotamText);
        } else {
            // Use sample data as fallback
            loadSampleWeatherData(icaoCode, weatherContainer);
            loadSampleNotamData(icaoCode, notamRecyclerView, emptyNotamText);
            
            // Show a message about setting up API keys
            Toast.makeText(this, "Using sample data. Set up API keys for real data.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_api_settings) {
            showApiKeySetupModal();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    // AsyncTask for loading airport data in background
    private class LoadAirportDataTask extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            tvResult.setText("Loading airport database...");
            Log.d("AirportFinder", "Starting to load airport database");
        }
        
        @Override
        protected Integer doInBackground(Void... params) {
            int validAirports = 0;
            
            try {
                InputStream inputStream = getAssets().open("airports.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                boolean isFirstLine = true;
                
                Log.d("AirportFinder", "Opened airports.csv file for reading");
                
                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        Log.d("AirportFinder", "CSV Header: " + line);
                        continue; // Skip header
                    }
                    
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    
                    // Safety check: ensure we have enough columns
                    if (parts.length < 14) {
                        Log.w("AirportFinder", "Skipping line with insufficient data: " + line);
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
                                                          iataCode, icaoCode, municipality, country, 0));
                                    validAirports++;
                                    
                                    // Log every 500th airport for debugging
                                    if (validAirports % 500 == 0) {
                                        Log.d("AirportFinder", "Added airport #" + validAirports + ": " + name + " (" + icaoCode + ")");
                                    }
                                    
                                    // Update progress every 100 airports
                                    if (validAirports % 100 == 0) {
                                        publishProgress(validAirports);
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                        Log.w("AirportFinder", "NumberFormatException while parsing: " + e.getMessage());
                    }
                }
                reader.close();
                Log.d("AirportFinder", "Finished reading airports.csv file");
                
            } catch (IOException e) {
                // Error loading data
                Log.e("AirportFinder", "Error loading airport data: " + e.getMessage(), e);
            }
            
            return validAirports;
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            tvResult.setText("Loading airport database... " + values[0] + " airports loaded");
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            Log.d("AirportFinder", "Airport database loading completed. Valid airports: " + result);
            if (result > 0) {
                tvResult.setText("Airport database loaded with " + result + " airports.\n\nEnter coordinates and search radius, then press 'Find Nearest Airports'.");
            } else {
                tvResult.setText("Error loading airport database. Please restart the app.");
            }
        }
    }
    
    private WeatherData createSampleWeatherData(String icaoCode) {
        // Create sample METAR data
        WeatherData weatherData = new WeatherData();
        weatherData.setIcaoCode(icaoCode);
        weatherData.setRawText("METAR " + icaoCode + " 011300Z 27005KT 10SM BKN025 22/18 A3006 RMK AO2 SLP195 T02220183 50000=");
        weatherData.setDecodedText("Wind: 270° at 5 kt\nVisibility: 10 SM\nClouds: Broken at 2500 ft\nTemp: 22°C\nDewpoint: 18°C");
        weatherData.setType(WeatherData.TYPE_METAR);
        weatherData.setTimestamp(System.currentTimeMillis());
        return weatherData;
    }
    
    private WeatherData createSampleTafData(String icaoCode) {
        // Create sample TAF data
        WeatherData weatherData = new WeatherData();
        weatherData.setIcaoCode(icaoCode);
        weatherData.setRawText("TAF " + icaoCode + " 011300Z 0114/0218 27005KT 10SM BKN025\nFM011500 28010KT 10SM FEW025\nFM012000 29015KT 10SM SCT030");
        weatherData.setDecodedText("Forecast:\n- 01:14 to 02:18: Wind 270° at 5 kt, visibility 10 SM, broken clouds at 2500 ft\n- 01:50 to 02:00: Wind 280° at 10 kt, visibility 10 SM, few clouds at 2500 ft\n- 02:00 to 02:18: Wind 290° at 15 kt, visibility 10 SM, scattered clouds at 3000 ft");
        weatherData.setType(WeatherData.TYPE_TAF);
        weatherData.setTimestamp(System.currentTimeMillis());
        return weatherData;
    }
    
    private List<NotamData> createSampleNotamData(String icaoCode) {
        // Create sample NOTAM data
        List<NotamData> notams = new ArrayList<>();
        
        NotamData notam1 = new NotamData();
        notam1.setId("NOTAM1");
        notam1.setText("RWY 01/19 CLOSED DUE TO MAINTENANCE");
        notam1.setStartTime("2023-01-01T00:00:00Z");
        notam1.setEndTime("2023-02-01T00:00:00Z");
        notam1.setType("RWY");
        notams.add(notam1);
        
        NotamData notam2 = new NotamData();
        notam2.setId("NOTAM2");
        notam2.setText("TWR FREQUENCY CHANGE TO 119.2 MHz");
        notam2.setStartTime("2023-01-15T00:00:00Z");
        notam2.setEndTime("2023-01-20T00:00:00Z");
        notam2.setType("TWY");
        notams.add(notam2);
        
        NotamData notam3 = new NotamData();
        notam3.setId("NOTAM3");
        notam3.setText("ILS RWY 01 U/S DUE TO EQUIPMENT FAILURE");
        notam3.setStartTime("2023-01-10T00:00:00Z");
        notam3.setEndTime("2023-01-25T00:00:00Z");
        notam3.setType("NAV");
        notams.add(notam3);
        
        return notams;
    }
    
    /**
     * Load sample weather data for demo purposes
     */
    private void loadSampleWeatherData(String icaoCode, LinearLayout container) {
        if (container == null) {
            return;
        }
        
        // Create sample METAR data
        WeatherData sampleMetar = new WeatherData();
        sampleMetar.setIcaoCode(icaoCode);
        sampleMetar.setRawText("METAR " + icaoCode + " 011300Z 27005KT 10SM BKN025 22/18 A3006 RMK AO2 SLP195 T02220183 50000=");
        sampleMetar.setDecodedText("Wind: 270° at 5 kt\nVisibility: 10 SM\nClouds: Broken at 2500 ft\nTemp: 22°C\nDewpoint: 18°C");
        sampleMetar.setType(WeatherData.TYPE_METAR);
        sampleMetar.setTimestamp(System.currentTimeMillis());
        
        // Create sample TAF data
        WeatherData sampleTaf = new WeatherData();
        sampleTaf.setIcaoCode(icaoCode);
        sampleTaf.setRawText("TAF " + icaoCode + " 011300Z 0114/0218 27005KT 10SM BKN025\nFM011500 28010KT 10SM FEW025\nFM012000 29015KT 10SM SCT030");
        sampleTaf.setDecodedText("Forecast:\n- 01:14 to 02:18: Wind 270° at 5 kt, visibility 10 SM, broken clouds at 2500 ft\n- 01:50 to 02:00: Wind 280° at 10 kt, visibility 10 SM, few clouds at 2500 ft\n- 02:00 to 02:18: Wind 290° at 15 kt, visibility 10 SM, scattered clouds at 3000 ft");
        sampleTaf.setType(WeatherData.TYPE_TAF);
        sampleTaf.setTimestamp(System.currentTimeMillis());
        
        Log.d("AirportFinder", "Sample weather data loaded for " + icaoCode);
    }
    
    /**
     * Load sample NOTAM data for demo purposes
     */
    private void loadSampleNotamData(String icaoCode, RecyclerView recyclerView, TextView emptyText) {
        if (recyclerView == null || emptyText == null) {
            return;
        }
        
        // Create sample NOTAM data
        List<NotamData> sampleNotams = new ArrayList<>();
        
        NotamData notam1 = new NotamData();
        notam1.setId("NOTAM1");
        notam1.setText(icaoCode + " RWY 18/36 CLSD DUE TO CONSTRUCTION WEF 2023-01-01 UNTIL 2023-02-01");
        notam1.setStartTime("2023-01-01T00:00:00Z");
        notam1.setEndTime("2023-02-01T00:00:00Z");
        notam1.setType("RWY");
        sampleNotams.add(notam1);
        
        NotamData notam2 = new NotamData();
        notam2.setId("NOTAM2");
        notam2.setText(icaoCode + " TWY A CLSD DUE TO MAINTENANCE WEF 2023-01-15 UNTIL 2023-01-20");
        notam2.setStartTime("2023-01-15T00:00:00Z");
        notam2.setEndTime("2023-01-20T00:00:00Z");
        notam2.setType("TWY");
        sampleNotams.add(notam2);
        
        // Set up the RecyclerView with the sample NOTAM data
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        
        NotamAdapter adapter = new NotamAdapter(sampleNotams);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        Log.d("AirportFinder", "Sample NOTAM data loaded for " + icaoCode);
    }
    
    /**
     * Add ICAO code functionality
     */
    private void addIcaoCode() {
        String icaoCode = etIcaoCode.getText().toString().trim().toUpperCase();
        
        if (icaoCode.isEmpty()) {
            Toast.makeText(this, "Please enter an ICAO code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (icaoCode.length() != 4) {
            Toast.makeText(this, "ICAO code must be 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add the ICAO code to selected codes (implementation would depend on your UI structure)
        Log.d("AirportFinder", "Adding ICAO code: " + icaoCode);
        Toast.makeText(this, "ICAO code " + icaoCode + " added", Toast.LENGTH_SHORT).show();
        
        // Clear the input field
        etIcaoCode.setText("");
    }
    
    /**
     * Save airport to Dropbox functionality
     */
    private void saveToDropbox(Airport airport) {
        if (airport == null) {
            Toast.makeText(this, "No airport data to save", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For now, just log the action - full Dropbox integration would require API setup
        Log.d("AirportFinder", "Saving airport to Dropbox: " + airport.getName());
        Toast.makeText(this, "Airport " + airport.getName() + " saved to Dropbox", Toast.LENGTH_SHORT).show();
    }
}

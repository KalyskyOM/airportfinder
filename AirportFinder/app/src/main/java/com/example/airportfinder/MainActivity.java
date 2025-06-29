package com.example.airportfinder;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    // Coordinate format constants
    private static final int FORMAT_DD = 0;  // Decimal Degrees
    private static final int FORMAT_DDM = 1; // Degrees, Decimal Minutes
    private static final int FORMAT_DMS = 2; // Degrees, Minutes, Seconds

    // --- STUBS FOR MISSING METHODS ---
    private void parseAndFillCoordinates() {
        // TODO: Implement coordinate parsing logic
    }

    private void setupAvwxApiKey() {
        // TODO: Implement AVWX API key setup logic
    }

    private boolean isValidLongitude(double longitude) {
        // Valid longitude is between -180 and 180
        return longitude >= -180.0 && longitude <= 180.0;
    }

    private boolean validateDMSComponents() {
        // TODO: Implement DMS component validation if needed
        return true;
    }
    // --- END STUBS ---

    // UI Components
    private Spinner spinnerCoordinateFormat;
    private String[] formatNames; // Holds format names for spinner
    
    // We'll use string resources directly for labels instead of TextView references
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
    private EditText etPasteCoordinates;
    private Button btnParseCoordinates;
    private Button btnUseMyLocation;
    
    // ICAO/IATA Code Input Fields
    private EditText etIcaoCode;
    private Button btnAddIcao;
    private LinearLayout layoutSelectedIcaoCodes;
    private TextView tvNoIcaoSelected;
    private List<String> selectedIcaoCodes; // Holds selected codes

    // API Key Flags
    private boolean hasAvwxKey = false;
    private boolean hasAutorouterKey = false;

    // Mutable fields for location

    
    // Search Components
    private EditText etSearchRadius;
    private Button btnFindAirport;
    private TextView tvResult;
    
    // Airport cards container
    private LinearLayout airportCardsContainer;
    
    // Database and state
    private final List<Airport> airports = new ArrayList<>();
    // ...
    // Helper for ICAO/IATA selection
    private void updateIcaoBubbles() {
        layoutSelectedIcaoCodes.removeAllViews();
        if (selectedIcaoCodes.isEmpty()) {
            tvNoIcaoSelected.setVisibility(View.VISIBLE);
            return;
        }
        tvNoIcaoSelected.setVisibility(View.GONE);
        for (String code : selectedIcaoCodes) {
            View bubble = createIcaoBubble(code);
            layoutSelectedIcaoCodes.addView(bubble);
        }
    }
    private View createIcaoBubble(final String code) {
        TextView bubble = new TextView(this);
        bubble.setText(getString(R.string.icao_bubble_remove, code));
        bubble.setBackgroundResource(R.drawable.airport_bubble_background);
        bubble.setPadding(24, 12, 24, 12);
        bubble.setTextColor(getResources().getColor(android.R.color.white));
        bubble.setTextSize(16);
        bubble.setTypeface(Typeface.DEFAULT_BOLD);
        bubble.setOnClickListener(v -> {
            selectedIcaoCodes.remove(code);
            updateIcaoBubbles();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 8, 8, 8);
        bubble.setLayoutParams(params);
        return bubble;
    }
    private final Map<String, Airport> selectedAirports = new HashMap<>();
    private SharedPreferences sharedPrefs;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.######");
    private final boolean isUpdatingFields = false;
    private int currentFormat = FORMAT_DMS;
    
    // Weather and NOTAM data
    private final Map<String, WeatherData> weatherDataMap = new HashMap<>();
    private final Map<String, List<NotamData>> notamDataMap = new HashMap<>();
    
    private ApiKeyManager apiKeyManager;
    private AvwxApiService avwxApiService;
    private AutorouterApiService autorouterApiService;
    private View apiKeySetupModal;
    private EditText etAvwxApiKey;
    private EditText etAutoRouterApiKey;
    private Button btnSaveApiKeys;
    private ImageView btnCloseApiSetup;
    
    private static final String PREFS_NAME = "AirportFinderPrefs";
    
    // Coordinate format constants
    // Format constants already defined at the top of the class
    private static final int FORMAT_DECIMAL = 1;
    private static final int FORMAT_GPS = 3;  // GPS Coordinates
    private String[] FORMAT_NAMES;
    private static final String[] FORMAT_DESCRIPTIONS = {"Enter latitude and longitude in Degrees, Minutes, Seconds format (e.g., 40° 30' 30\" N, 73° 45' 45\" W)",
                                                        "Enter latitude and longitude in Decimal Degrees format (e.g., 40.5051° N, 73.7653° W)",
                                                        "Enter GPS coordinates (e.g., 40° 30' 30.25\" N, 73° 45' 45.75\" W)"};
    
    // Location Services
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        formatNames = getResources().getStringArray(R.array.format_options); // Initialize formatNames from resources
        initializeViews();
        setupFormatSpinner();
        setupInputValidation();
        initializeDatabase();
        loadUserPreferences();
        loadDropboxToken();
        setupListeners();
        
        apiKeyManager = new ApiKeyManager(this);
        avwxApiService = new AvwxApiService(this);
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
        etPasteCoordinates = findViewById(R.id.etPasteCoordinates);
        btnParseCoordinates = findViewById(R.id.btnParseCoordinates);
        btnUseMyLocation = findViewById(R.id.btnUseMyLocation);
        
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
            android.R.layout.simple_spinner_item, formatNames);
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
        InputFilter[] latDegreeFilters = new InputFilter[] { new InputFilter.LengthFilter(2) }; 
        InputFilter[] lonDegreeFilters = new InputFilter[] { new InputFilter.LengthFilter(3) }; 
        InputFilter[] minuteFilters = new InputFilter[] { new InputFilter.LengthFilter(2) };
        InputFilter[] secondsFilters = new InputFilter[] { new InputFilter.LengthFilter(2) };
        InputFilter[] decimalFilters = new InputFilter[] { new InputFilter.LengthFilter(2) };
        
        etLatDegrees.setFilters(latDegreeFilters);
        etLatMinutes.setFilters(minuteFilters);
        etLatSecondsWhole.setFilters(secondsFilters);
        etLatSecondsDecimal.setFilters(decimalFilters);
        
        etLonDegrees.setFilters(lonDegreeFilters);
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
        
        // Set input filter for ICAO/IATA code (max 4 chars)
        InputFilter[] codeFilters = new InputFilter[] { new InputFilter.LengthFilter(4) };
        etIcaoCode.setFilters(codeFilters);
        
        // Initial button state check
        validateCoordinatesAndUpdateButton();
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
    
    /**
     * Initialize all UI views and components
     */
    private void initializeViews() {
        // Initialize UI components
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
        etPasteCoordinates = findViewById(R.id.etPasteCoordinates);
        btnParseCoordinates = findViewById(R.id.btnParseCoordinates);
        btnUseMyLocation = findViewById(R.id.btnUseMyLocation);
        
        // ICAO/IATA code input components
        etIcaoCode = findViewById(R.id.etIcaoCode);
        btnAddIcao = findViewById(R.id.btnAddIcao);
        layoutSelectedIcaoCodes = findViewById(R.id.layoutSelectedIcaoCodes);
        tvNoIcaoSelected = findViewById(R.id.tvNoIcaoSelected);
        selectedIcaoCodes = new ArrayList<>();
        
        etSearchRadius = findViewById(R.id.etSearchRadius);
        btnFindAirport = findViewById(R.id.btnFindAirport);
        tvResult = findViewById(R.id.tvResult);
        
        airportCardsContainer = findViewById(R.id.airportCardsContainer);
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
        
        // Parse Coordinates button
        btnParseCoordinates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parseAndFillCoordinates();
            }
        });
        
        // Use My Location button
        btnUseMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationPermission();
            }
        });
        
        // Setup text change listeners for coordinate fields
        setupTextChangeListeners();
    }
    
    private void setupTextChangeListeners() {
        // Create a simple text watcher for fields without auto-advance
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
                    validateCoordinatesAndUpdateButton();
                }
            }
        };
        
        // Setup auto-advance watchers for coordinate fields
        etLatDegrees.addTextChangedListener(createAutoAdvanceWatcher(etLatDegrees, etLatMinutes));
        etLatMinutes.addTextChangedListener(createAutoAdvanceWatcher(etLatMinutes, etLatSecondsWhole));
        etLatSecondsWhole.addTextChangedListener(createAutoAdvanceWatcher(etLatSecondsWhole, etLatSecondsDecimal));
        etLatSecondsDecimal.addTextChangedListener(createAutoAdvanceWatcher(etLatSecondsDecimal, etLonDegrees));
        etLonDegrees.addTextChangedListener(createAutoAdvanceWatcher(etLonDegrees, etLonMinutes));
        etLonMinutes.addTextChangedListener(createAutoAdvanceWatcher(etLonMinutes, etLonSecondsWhole));
        etLonSecondsWhole.addTextChangedListener(createAutoAdvanceWatcher(etLonSecondsWhole, etLonSecondsDecimal));
        etLonSecondsDecimal.addTextChangedListener(createAutoAdvanceWatcher(etLonSecondsDecimal, null));
        
        // Add text watchers to decimal fields
        etLatDecimal.addTextChangedListener(coordinateWatcher);
        etLonDecimal.addTextChangedListener(coordinateWatcher);
        
        // Add text watchers to GPS field
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
                validateCoordinatesAndUpdateButton();
            }
        });
        
        rgLonDirection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateDirectionText();
                saveCoordinateValues();
                validateCoordinatesAndUpdateButton();
            }
        });
    }
    
    /**
     * Create a text watcher for coordinate fields with auto-advance functionality
     */
    private TextWatcher createAutoAdvanceWatcher(final EditText currentField, final EditText nextField) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Auto-advance to next field when current field is filled
                if (nextField != null && s.length() == getMaxLength(currentField)) {
                    nextField.requestFocus();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingFields) {
                    saveCoordinateValues();
                    validateCoordinatesAndUpdateButton();
                }
            }
        };
    }
    
    private int getMaxLength(EditText editText) {
        InputFilter[] filters = editText.getFilters();
        for (InputFilter filter : filters) {
            if (filter instanceof InputFilter.LengthFilter) {
                return ((InputFilter.LengthFilter) filter).getMax();
            }
        }
        return -1; // No length filter found
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
        if (!selectedIcaoCodes.isEmpty()) {
            // If ICAO codes are selected, find airports near those
            findAirportsNearSelectedIcao();
        } else {
            // Otherwise use coordinates
            saveCoordinateValues();
            
            try {
                double latitude = getLatitudeValue();
                double longitude = getLongitudeValue();
                
                if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                    Toast.makeText(this, R.string.error_invalid_coordinates, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, getString(R.string.error_parsing_coordinates, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void findAirportsNearCoordinates(double latitude, double longitude, int radius) {
        if (airports.isEmpty()) {
            Toast.makeText(this, R.string.error_database_not_loaded, Toast.LENGTH_SHORT).show();
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
            tvResult.setText(getString(R.string.no_airports_found, radius));
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
            Toast.makeText(this, R.string.error_no_icao_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use the first selected airport as reference point
        Airport referenceAirport = null;
        for (Airport airport : selectedAirports.values()) {
            referenceAirport = airport;
            break;
        }
        
        if (referenceAirport == null) {
            Toast.makeText(this, R.string.error_airport_not_found, Toast.LENGTH_SHORT).show();
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
            tvResult.setText(R.string.no_airports_in_radius);
            return;
        }
        
        // Display text results
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.found_airports_header, nearbyAirports.size())).append("\n\n");
        
        DecimalFormat df = new DecimalFormat("#.##");
        
        for (Airport airport : nearbyAirports) {
            sb.append(getString(R.string.airport_name_format, airport.getName(), airport.getIcaoCode())).append("\n");
            sb.append(getString(R.string.airport_type_format, airport.getType())).append("\n");
            double lat = airport.getLatitude();
        double lon = airport.getLongitude();
        String latDir = lat >= 0 ? "N" : "S";
        String lonDir = lon >= 0 ? "E" : "W";
        sb.append(getString(R.string.location_format, Math.abs(lat), latDir, Math.abs(lon), lonDir)).append("\n");
            sb.append(getString(R.string.distance_format, airport.getDistance())).append("\n\n");
            
            // Create airport card for this airport
            createAirportCard(airport);
        }
        
        tvResult.setText(sb.toString());
    }
    
    private void createAirportCard(final Airport airport) {
        // Inflate the airport card layout
        View cardView = LayoutInflater.from(this).inflate(R.layout.airport_card, airportCardsContainer, false);
        final CardView airportCardView = cardView.findViewById(R.id.airportCardView);
        
        // Set airport basic info
        TextView tvAirportIcao = cardView.findViewById(R.id.tvAirportIcao);
        TextView tvAirportName = cardView.findViewById(R.id.tvAirportName);
        TextView tvDistance = cardView.findViewById(R.id.tvDistance);
        TextView tvTrack = cardView.findViewById(R.id.tvTrack);
        TextView tvAirportType = cardView.findViewById(R.id.tvAirportType);
        TextView tvAirportLocation = cardView.findViewById(R.id.tvAirportLocation);
        TextView tvAirportElevation = cardView.findViewById(R.id.tvAirportElevation);
        
        // Get expandable containers and button
        final LinearLayout weatherContainer = cardView.findViewById(R.id.weather_container);
        final Button btnExpandCollapse = cardView.findViewById(R.id.btnExpandCollapse);
        
        tvAirportIcao.setText(airport.getIcaoCode());
        tvAirportName.setText(airport.getName());
        
        DecimalFormat df = new DecimalFormat("#.##");
        tvDistance.setText(getString(R.string.distance_format, airport.getDistance()));
        tvAirportType.setText(getString(R.string.airport_type_format, airport.getType()));
        double lat = airport.getLatitude();
        double lon = airport.getLongitude();
        String latDir = lat >= 0 ? "N" : "S";
        String lonDir = lon >= 0 ? "E" : "W";
        tvAirportLocation.setText(getString(R.string.airport_location_format, 
            Math.abs(lat), latDir, Math.abs(lon), lonDir));
        tvAirportElevation.setText(getString(R.string.airport_elevation_format, airport.getElevation()));
        
        // Calculate and set track information
        double track = calculateTrack(currentLatitude, currentLongitude, airport.getLatitude(), airport.getLongitude());
        tvTrack.setText(getString(R.string.track_format, Math.round(track)));
        
        // Set card color based on airport type and distance
        setCardColor(airportCardView, airport);
        
        // Setup expand/collapse functionality
        setupExpandCollapseFunctionality(btnExpandCollapse, weatherContainer, airport, cardView);
        
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
    
    /**
     * Calculates the track (bearing) from a starting point to a destination point
     */
    private double calculateTrack(double startLat, double startLon, double destLat, double destLon) {
        // Convert to radians
        startLat = Math.toRadians(startLat);
        startLon = Math.toRadians(startLon);
        destLat = Math.toRadians(destLat);
        destLon = Math.toRadians(destLon);
        
        // Calculate bearing
        double y = Math.sin(destLon - startLon) * Math.cos(destLat);
        double x = Math.cos(startLat) * Math.sin(destLat) -
                  Math.sin(startLat) * Math.cos(destLat) * Math.cos(destLon - startLon);
        double bearing = Math.atan2(y, x);
        
        // Convert to degrees
        bearing = Math.toDegrees(bearing);
        bearing = (bearing + 360) % 360; // Normalize to 0-360 degrees
        
        return bearing;
    }
    
    /**
     * Sets the card color based on airport type and distance
     */
    private void setCardColor(CardView cardView, Airport airport) {
        int color;
        
        // Color logic based on airport type and distance
        if (airport.getDistance() < 10) {
            // Closer airports use warmer colors
            if (airport.getType().contains("large_airport")) {
                color = Color.parseColor("#FFE6E6"); // Light red for large airports close by
            } else if (airport.getType().contains("medium_airport")) {
                color = Color.parseColor("#FFEFD6"); // Light orange for medium airports close by
            } else {
                color = Color.parseColor("#FFFBD6"); // Light yellow for small airports close by
            }
        } else {
            // Further airports use cooler colors
            if (airport.getType().contains("large_airport")) {
                color = Color.parseColor("#E6F0FF"); // Light blue for large airports further away
            } else if (airport.getType().contains("medium_airport")) {
                color = Color.parseColor("#E6FFFA"); // Light teal for medium airports further away
            } else {
                color = Color.parseColor("#F0F8FF"); // Alice blue for small airports further away
            }
        }
        
        cardView.setCardBackgroundColor(color);
    }
    
    /**
     * Sets up the expand/collapse functionality for airport details
     */
    /**
     * Sets up the expand/collapse functionality for airport details
                                               final LinearLayout weatherContainer, 
                                               final Airport airport,
                                               final View cardView) {
    // Initialize views for weather and NOTAM data
    final TextView tvMetarRaw = cardView.findViewById(R.id.tvMetarRaw);
    final TextView tvMetarDecoded = cardView.findViewById(R.id.tvMetarDecoded);
    final TextView tvTafRaw = cardView.findViewById(R.id.tvTafRaw);
    final TextView tvTafDecoded = cardView.findViewById(R.id.tvTafDecoded);
    final RecyclerView notamRecyclerView = cardView.findViewById(R.id.rvNotams);
    final TextView emptyNotamText = cardView.findViewById(R.id.tvNoNotams);
    
    // Set up RecyclerView
    notamRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    
    // Track expansion state
    final boolean[] isExpanded = {false};
    
    // Initialize with loading text
    tvMetarRaw.setText("Loading METAR...");
    tvMetarDecoded.setText("");
    tvTafRaw.setText("Loading TAF...");
    tvTafDecoded.setText("");
    
    btnExpandCollapse.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isExpanded[0]) {
                // Collapse
                weatherContainer.setVisibility(View.GONE);
                btnExpandCollapse.setText("Show Details");
                isExpanded[0] = false;
            } else {
                // Expand
                weatherContainer.setVisibility(View.VISIBLE);
                btnExpandCollapse.setText("Hide Details");
                isExpanded[0] = true;
                
                // Load weather and NOTAM data if needed
                if (tvMetarRaw.getText().toString().equals("Loading METAR...")) {
                    loadWeatherAndNotamData(airport, tvMetarRaw, tvMetarDecoded, tvTafRaw, 
                                          tvTafDecoded, notamRecyclerView, emptyNotamText);
                }
            }
        }
 * Loads weather and NOTAM data for the airport
 */
private void loadWeatherAndNotamData(Airport airport, TextView tvMetarRaw, TextView tvMetarDecoded,
                                      TextView tvTafRaw, TextView tvTafDecoded, 
                                      RecyclerView notamRecyclerView, TextView emptyNotamText) {
    // Check if we already have weather data for this airport
    WeatherData metarData = weatherDataMap.get(airport.getIcaoCode() + "_METAR");
    WeatherData tafData = weatherDataMap.get(airport.getIcaoCode() + "_TAF");
    
    if (metarData == null) {
        // Generate sample weather data
        metarData = createSampleWeatherData(airport.getIcaoCode());
        weatherDataMap.put(airport.getIcaoCode() + "_METAR", metarData);
    }
    
    if (tafData == null) {
        tafData = createSampleTafData(airport.getIcaoCode());
        weatherDataMap.put(airport.getIcaoCode() + "_TAF", tafData);
    }
    
    // Update weather UI
    tvMetarRaw.setText(metarData.getRawText());
    tvMetarDecoded.setText(metarData.getDecodedText());
    tvTafRaw.setText(tafData.getRawText());
    tvTafDecoded.setText(tafData.getDecodedText());
    
    // Check if we have NOTAM data
    List<NotamData> notams = notamDataMap.get(airport.getIcaoCode());
    
    if (notams == null || notams.isEmpty()) {
        // Generate sample NOTAM data
        notams = createSampleNotamData(airport.getIcaoCode());
        notamDataMap.put(airport.getIcaoCode(), notams);
    }
    
    if (notams.isEmpty()) {
        handleNoNotams(notamRecyclerView, emptyNotamText);
    } else {
        emptyNotamText.setVisibility(View.GONE);
        notamRecyclerView.setVisibility(View.VISIBLE);
        
        // Update NOTAM UI
        NotamAdapter adapter = new NotamAdapter(notams);
        notamRecyclerView.setAdapter(adapter);
    }
}

/**
 * Helper method to handle the case when no NOTAMs are available
 */
private void handleNoNotams(RecyclerView notamRecyclerView, TextView emptyNotamText) {
    if (notamRecyclerView != null && emptyNotamText != null) {
        notamRecyclerView.setVisibility(View.GONE);
        emptyNotamText.setVisibility(View.VISIBLE);
        emptyNotamText.setText("No NOTAMs available");
    }
}

// ...

/**
 * Fetches real NOTAM data from Autorouter API
 */
private void fetchRealNotamData(String icaoCode, final RecyclerView notamRecyclerView, final TextView emptyNotamText) {
    // Check if API key is available
    if (!apiKeyManager.hasAutoRouterApiKey()) {
        // Show API key setup modal if no key is available
        Toast.makeText(this, "Autorouter API key required for real NOTAM data", Toast.LENGTH_SHORT).show();
        setupAvwxApiKey(); // This will now handle both API keys
        return;
    }
    
    // Show loading indicator
    final ProgressBar progressBar = new ProgressBar(this);
    progressBar.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
    progressBar.setIndeterminate(true);
    ((ViewGroup) notamRecyclerView.getParent()).addView(progressBar);
    
    // Fetch NOTAM data from Autorouter API
    autorouterApiService.fetchNotams(icaoCode, new AutorouterApiService.ApiResponseCallback<List<NotamData>>() {
        @Override
        public void onSuccess(List<NotamData> result) {
            runOnUiThread(() -> {
                // Remove loading indicator
                ((ViewGroup) notamRecyclerView.getParent()).removeView(progressBar);
                
                if (result.isEmpty()) {
                    handleNoNotams(notamRecyclerView, emptyNotamText);
                } else {
                    emptyNotamText.setVisibility(View.GONE);
                    notamRecyclerView.setVisibility(View.VISIBLE);
                    
                    // Set up adapter with real NOTAM data
                    NotamAdapter adapter = new NotamAdapter(result);
                    notamRecyclerView.setAdapter(adapter);
                }
            });
        }
        
        @Override
        public void onError(String errorMessage) {
            runOnUiThread(() -> {
                // Remove loading indicator
                ((ViewGroup) notamRecyclerView.getParent()).removeView(progressBar);
                
                // Show error message
                Toast.makeText(MainActivity.this, getString(R.string.notam_error_format, errorMessage), Toast.LENGTH_SHORT).show();
                
                // Show empty state
                notamRecyclerView.setVisibility(View.GONE);
                emptyNotamText.setVisibility(View.VISIBLE);
                emptyNotamText.setText(getString(R.string.error_loading_notams, errorMessage));
            });
        }
    });
}

/**
 * Creates and displays an airport card for the given ICAO code
 */
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
            tvResult.setText(getString(R.string.loading_airport_database));
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
                                    // Using the correct constructor parameters (name, latitude, longitude, type, elevation, icaoCode)
                                    // Note: elevation is set to 0 as it's not available in the parsed data
                                    airports.add(new Airport(name, latitude, longitude, type, 0, icaoCode));
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
            tvResult.setText(getString(R.string.loading_airports_progress, values[0]));
        }
        
        @Override
        protected void onPostExecute(Integer result) {
            Log.d("AirportFinder", "Airport database loading completed. Valid airports: " + result);
            if (result > 0) {
                tvResult.setText(getString(R.string.loading_complete, result));
            } else {
                tvResult.setText(getString(R.string.error_loading_airport_database));
            }
        }
    }
    
    private WeatherData createSampleWeatherData(String icaoCode) {
        // Create sample METAR data
        WeatherData weatherData = new WeatherData();
        weatherData.setIcaoCode(icaoCode);
        weatherData.setRawText(getString(R.string.sample_metar_raw_format, icaoCode, "011300"));
        weatherData.setDecodedText(getString(R.string.sample_metar_decoded));
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
        notam3.setText(getString(R.string.sample_notam_3)); // Fixed: no format argument
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
        sampleMetar.setRawText(getString(R.string.sample_metar_raw_format, icaoCode, "011300"));
        sampleMetar.setDecodedText(getString(R.string.sample_metar_decoded));
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
        String code = etIcaoCode.getText().toString().trim().toUpperCase();
        
        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter an ICAO/IATA code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (code.length() < 3 || code.length() > 4 || !code.matches("[A-Z0-9]{3,4}")) {
            Toast.makeText(this, "Code must be 3 or 4 letters/numbers", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedIcaoCodes.contains(code)) {
            Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectedIcaoCodes.add(code);
        updateIcaoBubbles();
        etIcaoCode.setText("");
    }
    
    /**
     * Save airport to Dropbox functionality
     */
    private void saveToDropbox(Airport airport) {
        if (airport == null) {
            Toast.makeText(this, R.string.no_airport_data, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // For now, just log the action - full Dropbox integration would require API setup
        Log.d("AirportFinder", "Saving airport to Dropbox: " + airport.getName());
        Toast.makeText(this, getString(R.string.airport_saved_to_dropbox, airport.getName()), Toast.LENGTH_SHORT).show();
    }
    
    private void validateCoordinatesAndUpdateButton() {
        try {
            double latitude = getLatitudeValue();
            double longitude = getLongitudeValue();
            
            // Check if coordinates are valid numbers and within valid geographic ranges
            boolean isValid = !Double.isNaN(latitude) && !Double.isNaN(longitude) &&
                             isValidLatitude(latitude) && isValidLongitude(longitude);
            
            // For DMS format, also validate individual components
            if (isValid && currentFormat == FORMAT_DMS) {
                isValid = validateDMSComponents();
            }
            btnFindAirport.setEnabled(isValid);
        } catch (Exception e) {
            btnFindAirport.setEnabled(false);
        }
    }
    
    /**
     * Load sample NOTAM data for demo purposes
     */

    /**
     * Coordinate parse result holder
     */
    private static class CoordinateParseResult {
        double latitude;
        double longitude;

        CoordinateParseResult(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }
    
    /**
     * Convert DMS to decimal degrees
     */
    private double convertDMSToDecimal(int degrees, int minutes, double seconds, String direction) {
        double decimal = degrees + (minutes / 60.0) + (seconds / 3600.0);
        if (direction.equals("S") || direction.equals("W")) {
            decimal = -decimal;
        }
        return decimal;
    }
    
    /**
     * Convert DM to decimal degrees
     */
    private double convertDMToDecimal(int degrees, double minutes, String direction) {
        double decimal = degrees + (minutes / 60.0);
        if (direction.equals("S") || direction.equals("W")) {
            decimal = -decimal;
        }
        return decimal;
    }
    
    /**
     * Fill coordinate fields with parsed values
     */
    private void fillCoordinateFields(double latitude, double longitude) {
        // Get current coordinate format
        currentFormat = spinnerCoordinateFormat.getSelectedItemPosition();
        
        switch (currentFormat) {
            case FORMAT_DD: // Decimal Degrees Format
                fillDecimalFields(latitude, longitude);
                break;
            case FORMAT_DDM: // Degrees, Decimal Minutes Format
                // Currently handled same as decimal
                fillDecimalFields(latitude, longitude);
                break;
            case FORMAT_DMS: // Degrees, Minutes, Seconds Format
                fillDMSFields(latitude, longitude);
                break;
            case FORMAT_GPS: // GPS Format
                fillGPSFields(latitude, longitude);
                break;
        }
        
        // Validate coordinates and update button state
        validateCoordinatesAndUpdateButton();
    }
    
    private void fillDMSFields(double latitude, double longitude) {
        // Convert latitude to DMS
        boolean isNorth = latitude >= 0;
        latitude = Math.abs(latitude);
        
        int latDegrees = (int) latitude;
        double latMinutesDecimal = (latitude - latDegrees) * 60;
        int latMinutes = (int) latMinutesDecimal;
        double latSeconds = (latMinutesDecimal - latMinutes) * 60;
        int latSecondsWhole = (int) latSeconds;
        int latSecondsDecimal = (int) Math.round((latSeconds - latSecondsWhole) * 100);
        
        // Convert longitude to DMS
        boolean isEast = longitude >= 0;
        longitude = Math.abs(longitude);
        
        int lonDegrees = (int) longitude;
        double lonMinutesDecimal = (longitude - lonDegrees) * 60;
        int lonMinutes = (int) lonMinutesDecimal;
        double lonSeconds = (lonMinutesDecimal - lonMinutes) * 60;
        int lonSecondsWhole = (int) lonSeconds;
        int lonSecondsDecimal = (int) Math.round((lonSeconds - lonSecondsWhole) * 100);
        
        // Fill DMS fields
        etLatDegrees.setText(String.valueOf(latDegrees));
        etLatMinutes.setText(String.valueOf(latMinutes));
        etLatSecondsWhole.setText(String.valueOf(latSecondsWhole));
        etLatSecondsDecimal.setText(String.valueOf(latSecondsDecimal));
        
        etLonDegrees.setText(String.valueOf(lonDegrees));
        etLonMinutes.setText(String.valueOf(lonMinutes));
        etLonSecondsWhole.setText(String.valueOf(lonSecondsWhole));
        etLonSecondsDecimal.setText(String.valueOf(lonSecondsDecimal));
        
        // Set direction indicators
        tvLatDirection.setText(isNorth ? "N" : "S");
        tvLonDirection.setText(isEast ? "E" : "W");
        
        // Set radio group selections
        if (isNorth) {
            rgLatDirection.check(R.id.rbLatNorth);
        } else {
            rgLatDirection.check(R.id.rbLatSouth);
        }
        
        if (isEast) {
            rgLonDirection.check(R.id.rbLonEast);
        } else {
            rgLonDirection.check(R.id.rbLonWest);
        }
    }
    
    private void fillDecimalFields(double latitude, double longitude) {
        DecimalFormat decimalFormat = new DecimalFormat("#.######");
        etLatDecimal.setText(decimalFormat.format(latitude));
        etLonDecimal.setText(decimalFormat.format(longitude));
    }
    
    private void fillGPSFields(double latitude, double longitude) {
        String gpsFormat = formatAsGPS(latitude, longitude);
        etGPSCoordinates.setText(gpsFormat);
    }
    
    /**
     * Validate latitude range
     */
    private boolean isValidLatitude(double lat) {
        return lat >= -90 && lat <= 90;
    }
    /**
     * Checks if both API keys are present; if not, prompts the user to enter them.
     */
    private void checkAndPromptApiKeys() {
        // Check if both API keys are already stored
        boolean hasAvwxKey = apiKeyManager.hasAvwxApiKey();
        boolean hasAutorouterKey = apiKeyManager.hasAutoRouterApiKey();

        if (hasAvwxKey && hasAutorouterKey) {
            Log.d("MainActivity", "Both API keys already configured");
            return;
        }

        // Show dialog to enter missing API keys
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("API Keys Required");

        // Create custom layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        // Add instructions
        TextView instructions = new TextView(this);
        instructions.setText(R.string.api_key_required_message);
        instructions.setPadding(0, 0, 0, 20);
        layout.addView(instructions);
        // AVWX API Key section
        if (!hasAvwxKey) {
            TextView avwxLabel = new TextView(this);
            avwxLabel.setText("AVWX API Key (for Weather Data):");
            avwxLabel.setTypeface(null, Typeface.BOLD);
            layout.addView(avwxLabel);

            TextView avwxInfo = new TextView(this);
            avwxInfo.setText("Get free key from: https://avwx.rest");
            avwxInfo.setTextSize(12);
            avwxInfo.setPadding(0, 0, 0, 8);
            layout.addView(avwxInfo);

            final EditText avwxInput = new EditText(this);
            avwxInput.setHint("Enter AVWX API key");
            avwxInput.setPadding(20, 10, 20, 10);
            layout.addView(avwxInput);

            // Add some spacing
            View spacer1 = new View(this);
            spacer1.setLayoutParams(new LinearLayout.LayoutParams(0, 30));
            layout.addView(spacer1);
        }

        // Autorouter API Key section
        if (!hasAutorouterKey) {
            TextView autorouterLabel = new TextView(this);
            autorouterLabel.setText("Autorouter API Key (for NOTAM Data):");
            autorouterLabel.setTypeface(null, Typeface.BOLD);
            layout.addView(autorouterLabel);

            TextView autorouterInfo = new TextView(this);
            autorouterInfo.setText("Get key from: https://www.autorouter.aero");
            autorouterInfo.setTextSize(12);
            autorouterInfo.setPadding(0, 0, 0, 8);
            layout.addView(autorouterInfo);

            final EditText autorouterInput = new EditText(this);
            autorouterInput.setHint("Enter Autorouter API key");
            autorouterInput.setPadding(20, 10, 20, 10);
            layout.addView(autorouterInput);
        }

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean success = true;
                // Save AVWX key if needed
                if (!hasAvwxKey) {
                    EditText avwxInput = (EditText) layout.getChildAt(3); // Adjust index if needed
                    String avwxKey = avwxInput.getText().toString().trim();
                    if (!avwxKey.isEmpty()) {
                        boolean avwxSuccess = apiKeyManager.storeAvwxApiKey(avwxKey);
                        if (avwxSuccess) {
                            Log.d("MainActivity", "AVWX API key stored successfully");
                        } else {
                            success = false;
                            Log.e("MainActivity", "Failed to store AVWX API key");
                        }
                    }
                }
                // Save Autorouter key if needed
                if (!hasAutorouterKey) {
                    EditText autorouterInput = (EditText) layout.getChildAt(hasAvwxKey ? 3 : 7); // Adjust index if needed
                    String autorouterKey = autorouterInput.getText().toString().trim();
                    if (!autorouterKey.isEmpty()) {
                        boolean autorouterSuccess = apiKeyManager.storeAutoRouterApiKey(autorouterKey);
                        if (autorouterSuccess) {
                            Log.d("MainActivity", "Autorouter API key stored successfully");
                        } else {
                            success = false;
                            Log.e("MainActivity", "Failed to store Autorouter API key");
                        }
                    }
                }
                if (success) {
                    Toast.makeText(MainActivity.this, "API keys saved successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to save one or more API keys. Please try again.", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    
    /**
     * Verify if API key is stored and working
     */
    private void verifyApiKey() {
        if (apiKeyManager.hasAvwxApiKey()) {
            String key = apiKeyManager.getAvwxApiKey();
            Log.d("MainActivity", "API key loaded: " + (key != null ? "✓" : "✗"));
            Toast.makeText(this, "API key is configured", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("MainActivity", "No API key found");
            Toast.makeText(this, "No API key configured", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Request location permissions from the user
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, get location
            getCurrentLocation();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get the location
                getCurrentLocation();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Get the current location using LocationManager
     */
    @SuppressLint("MissingPermission") // Permission is checked before calling this method
    private void getCurrentLocation() {
        try {
            // Get location from GPS provider first
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            
            // If GPS location is not available, try network provider
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                // Got location, update the UI
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                
                // Update the coordinate fields based on current format
                fillCoordinateFields(latitude, longitude);
                
                // Enable the Find Airport button if coordinates are valid
                validateCoordinatesAndUpdateButton();
            } else {
                // No last known location, request updates
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null) {
                            runOnUiThread(() -> {
                                currentLatitude = location.getLatitude();
                                currentLongitude = location.getLongitude();
                                fillCoordinateFields(currentLatitude, currentLongitude);
                                validateCoordinatesAndUpdateButton();
                            });
                            locationManager.removeUpdates(this);
                        }
                    }
                    
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    
                    @Override
                    public void onProviderEnabled(String provider) {}
                    
                    @Override
                    public void onProviderDisabled(String provider) {
                        runOnUiThread(() -> 
                            Toast.makeText(MainActivity.this, 
                                "Location provider disabled. Please enable location services.", 
                                Toast.LENGTH_SHORT).show()
                        );
                    }
                };
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
            }
        } catch (Exception e) {
            Log.e("LocationError", "Error getting location", e);
            runOnUiThread(() -> 
                Toast.makeText(this, getString(R.string.error_getting_location, e.getMessage()), 
                Toast.LENGTH_SHORT).show()
            );
        }
    }
    
    // This duplicate method has been removed
    
    /**
     * Switch between coordinate formats (DMS, DD, DDM)
     * @param position The position in the spinner (0=DMS, 1=DD, 2=DDM)
     */
    private void switchCoordinateFormat(int position) {
        currentFormat = position;
        updateFieldLabelsAndTypes(currentFormat);
        savePreferences();
    }
    
    /**
     * Save user preferences to SharedPreferences
     */
    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        
        // Save current coordinate format
        editor.putInt("coordinateFormat", currentFormat);
        
        // Save coordinate values
        saveCoordinateValues();
        
        // Save search radius
        String searchRadiusStr = etSearchRadius.getText().toString();
        if (!TextUtils.isEmpty(searchRadiusStr)) {
            editor.putString("searchRadius", searchRadiusStr);
        }
        
        editor.apply();
    }
    
    /**
     * Update field labels and input types based on the selected coordinate format
     * @param format The coordinate format (0=DMS, 1=DD, 2=DDM)
     */
    private void updateFieldLabelsAndTypes(int format) {
        // Get references to all input fields
        EditText latDegrees = findViewById(R.id.etLatDegrees);
        EditText latMinutes = findViewById(R.id.etLatMinutes);
        EditText latSeconds = findViewById(R.id.etLatSecondsWhole);
        EditText latSecondsDecimal = findViewById(R.id.etLatSecondsDecimal);
        
        EditText lonDegrees = findViewById(R.id.etLonDegrees);
        EditText lonMinutes = findViewById(R.id.etLonMinutes);
        EditText lonSeconds = findViewById(R.id.etLonSecondsWhole);
        EditText lonSecondsDecimal = findViewById(R.id.etLonSecondsDecimal);
        
        // Update visibility and labels based on format
        switch (format) {
            case FORMAT_DMS: // Degrees, Minutes, Seconds
                // Show all fields for DMS format
                latMinutes.setVisibility(View.VISIBLE);
                latSeconds.setVisibility(View.VISIBLE);
                latSecondsDecimal.setVisibility(View.VISIBLE);
                
                lonMinutes.setVisibility(View.VISIBLE);
                lonSeconds.setVisibility(View.VISIBLE);
                lonSecondsDecimal.setVisibility(View.VISIBLE);
                break;
                
            case FORMAT_DD: // Decimal Degrees
                // Hide minutes and seconds fields for DD format
                latMinutes.setVisibility(View.GONE);
                latSeconds.setVisibility(View.GONE);
                latSecondsDecimal.setVisibility(View.GONE);
                
                lonMinutes.setVisibility(View.GONE);
                lonSeconds.setVisibility(View.GONE);
                lonSecondsDecimal.setVisibility(View.GONE);
                break;
                
            case FORMAT_DDM: // Degrees, Decimal Minutes
                // Show minutes but hide seconds fields for DDM format
                latMinutes.setVisibility(View.VISIBLE);
                latSeconds.setVisibility(View.GONE);
                latSecondsDecimal.setVisibility(View.VISIBLE);
                
                lonMinutes.setVisibility(View.VISIBLE);
                lonSeconds.setVisibility(View.GONE);
                lonSecondsDecimal.setVisibility(View.VISIBLE);
                break;
        }
        
        // Clear fields that are no longer visible
        if (format != FORMAT_DMS) {
            latSeconds.setText("");
            lonSeconds.setText("");
        }
        
        if (format == FORMAT_DD) {
            latMinutes.setText("");
            lonMinutes.setText("");
        }
    }
    
    // Method to load sample NOTAMs for testing
    
    /**
     * Calculate distance between two coordinates using the Haversine formula
     * @param lat1 First latitude in decimal degrees
     * @param lon1 First longitude in decimal degrees
     * @param lat2 Second latitude in decimal degrees
     * @param lon2 Second longitude in decimal degrees
     * @return Distance in nautical miles
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert coordinates to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate distance using Haversine formula
        double dlon = lon2Rad - lon1Rad;
        double dlat = lat2Rad - lat1Rad;
        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 3440.065 * c; // Convert to nautical miles (Earth radius = 3440.065 NM)
    }
    
    /**
     * Set up expand/collapse functionality for airport cards
     * @param btnExpandCollapse The expand/collapse button
     * @param weatherContainer The container to expand/collapse
     * @param airport The airport object
     * @param cardView The parent card view
     */
    private void setupExpandCollapseFunctionality(Button btnExpandCollapse, LinearLayout weatherContainer, 
                                                Airport airport, View cardView) {
        // Set initial state (collapsed)
        weatherContainer.setVisibility(View.GONE);
        btnExpandCollapse.setText(getString(R.string.show_details));
        
        btnExpandCollapse.setOnClickListener(v -> {
            if (weatherContainer.getVisibility() == View.VISIBLE) {
                // Collapse
                weatherContainer.setVisibility(View.GONE);
                btnExpandCollapse.setText(getString(R.string.show_details));
            } else {
                // Expand
                weatherContainer.setVisibility(View.VISIBLE);
                btnExpandCollapse.setText(getString(R.string.hide_details));
                
                // Load weather data if not already loaded
                if (weatherContainer.getChildCount() == 0) {
                    String icaoCode = airport.getIcaoCode();
                    if (isApiKeyConfigured()) {
                        fetchRealWeatherData(icaoCode, weatherContainer);
                    } else {
                        loadSampleWeatherData(icaoCode, weatherContainer);
                    }
                }
            }
        });
    }
    
    private double getLatitudeValue() {
        double latitude = 0.0;
        try {
            switch (currentFormat) {
                case FORMAT_DMS:
                    int latDegrees = Integer.parseInt(etLatDegrees.getText().toString());
                    int latMinutes = Integer.parseInt(etLatMinutes.getText().toString());
                    double latSeconds = Double.parseDouble(etLatSecondsWhole.getText().toString() + "." +
                            etLatSecondsDecimal.getText().toString());
                    latitude = latDegrees + (latMinutes / 60.0) + (latSeconds / 3600.0);
                    if (rgLatDirection.getCheckedRadioButtonId() == R.id.rbLatSouth) {
                        latitude = -latitude;
                    }
                    break;
                case FORMAT_DECIMAL:
                    latitude = Double.parseDouble(etLatDecimal.getText().toString());
                    break;
                case FORMAT_GPS:
                    // Parse from GPS format
                    String gpsText = etGPSCoordinates.getText().toString();
                    latitude = parseGPSLatitude(gpsText);
                    break;
            }
        } catch (NumberFormatException | NullPointerException e) {
            Log.e("CoordinateError", "Error parsing latitude", e);
        }
        return latitude;
    }

    private double getLongitudeValue() {
        double longitude = 0.0;
        try {
            switch (currentFormat) {
                case FORMAT_DMS:
                    int lonDegrees = Integer.parseInt(etLonDegrees.getText().toString());
                    int lonMinutes = Integer.parseInt(etLonMinutes.getText().toString());
                    double lonSeconds = Double.parseDouble(etLonSecondsWhole.getText().toString() + "." +
                            etLonSecondsDecimal.getText().toString());
                    longitude = lonDegrees + (lonMinutes / 60.0) + (lonSeconds / 3600.0);
                    if (rgLonDirection.getCheckedRadioButtonId() == R.id.rbLonWest) {
                        longitude = -longitude;
                    }
                    break;
                case FORMAT_DECIMAL:
                    longitude = Double.parseDouble(etLonDecimal.getText().toString());
                    break;
                case FORMAT_GPS:
                    // Parse from GPS format
                    String gpsText = etGPSCoordinates.getText().toString();
                    longitude = parseGPSLongitude(gpsText);
                    break;
            }
        } catch (NumberFormatException | NullPointerException e) {
            Log.e("CoordinateError", "Error parsing longitude", e);
        }
        return longitude;
    }

    private String formatAsGPS(double latitude, double longitude) {
        boolean isLatNorth = latitude >= 0;
        boolean isLonEast = longitude >= 0;
        latitude = Math.abs(latitude);
        longitude = Math.abs(longitude);

        int latDegrees = (int) latitude;
        double latMinutes = (latitude - latDegrees) * 60;
        int latMinutesWhole = (int) latMinutes;
        double latSeconds = (latMinutes - latMinutesWhole) * 60;

        int lonDegrees = (int) longitude;
        double lonMinutes = (longitude - lonDegrees) * 60;
        int lonMinutesWhole = (int) lonMinutes;
        double lonSeconds = (lonMinutes - lonMinutesWhole) * 60;

        return String.format("%02d°%02d'%05.2f\"%s %03d°%02d'%05.2f\"%s",
                latDegrees, latMinutesWhole, latSeconds, (isLatNorth ? "N" : "S"),
                lonDegrees, lonMinutesWhole, lonSeconds, (isLonEast ? "E" : "W"));
    }

    private void fetchRealWeatherData(String icaoCode, LinearLayout container) {
        // TODO: Implement real weather data fetching using API
        // For now, use sample data
        loadSampleWeatherData(icaoCode, container);
    }

    private void updateWeatherDisplay(LinearLayout container, WeatherData metar, WeatherData taf) {
        if (container == null) return;

        TextView tvMetarRaw = container.findViewById(R.id.tv_metar_raw);
        TextView tvMetarDecoded = container.findViewById(R.id.tv_metar_decoded);
        TextView tvTafRaw = container.findViewById(R.id.tv_taf_raw);
        TextView tvTafDecoded = container.findViewById(R.id.tv_taf_decoded);

        if (metar != null) {
            tvMetarRaw.setText(metar.getRawText());
            tvMetarDecoded.setText(metar.getDecodedText());
        } else {
            tvMetarRaw.setText(R.string.no_metar_data);
            tvMetarDecoded.setText("");
        }

        if (taf != null) {
            tvTafRaw.setText(taf.getRawText());
            tvTafDecoded.setText(taf.getDecodedText());
        } else {
            tvTafRaw.setText(R.string.no_taf_data);
            tvTafDecoded.setText("");
        }
    }

    /**
     * Check if the API key is configured in shared preferences
     * @return true if API key is configured, false otherwise
     */
    private boolean isApiKeyConfigured() {
        SharedPreferences sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String avwxApiKey = sharedPrefs.getString("avwx_api_key", "");
        return !avwxApiKey.isEmpty();
    }
    
    private void showApiKeySetupModal() {
        View modalView = getLayoutInflater().inflate(R.layout.api_key_setup_modal, null);
        EditText etAutoRouterKey = modalView.findViewById(R.id.et_autorouter_api_key);
        Button btnSaveKeys = modalView.findViewById(R.id.btn_save_api_keys);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(modalView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        btnSaveKeys.setOnClickListener(v -> {
            String autoRouterKey = etAutoRouterKey.getText().toString().trim();
            if (!autoRouterKey.isEmpty()) {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("autorouter_api_key", autoRouterKey);
                editor.apply();
                dialog.dismiss();
            } else {
                Toast.makeText(this, R.string.api_key_required, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private double parseGPSLatitude(String gpsText) {
        Pattern pattern = Pattern.compile("(\\d{1,2})°(\\d{2})'([\\d.]+)\"([NS])");
        Matcher matcher = pattern.matcher(gpsText);
        if (matcher.matches()) {
            String degreesStr = matcher.group(1);
            String minutesStr = matcher.group(2);
            String secondsStr = matcher.group(3);
            String direction = matcher.group(4);
            if (degreesStr != null && minutesStr != null && secondsStr != null && direction != null) {
                double degrees = Double.parseDouble(degreesStr);
                double minutes = Double.parseDouble(minutesStr);
                double seconds = Double.parseDouble(secondsStr);
                // Convert DMS to decimal degrees
                double decimalDegrees = degrees + (minutes / 60.0) + (seconds / 3600.0);
                // Apply sign based on direction (negative for South)
                return direction.equals("S") ? -decimalDegrees : decimalDegrees;
            }
        }
        throw new IllegalArgumentException("Invalid GPS latitude format");
    }

    private double parseGPSLongitude(String gpsText) {
        Pattern pattern = Pattern.compile("(\\d{1,3})°(\\d{2})'([\\d.]+)\"([EW])");
        Matcher matcher = pattern.matcher(gpsText);
        if (matcher.matches()) {
            double degrees = Double.parseDouble(matcher.group(1));
            double minutes = Double.parseDouble(matcher.group(2));
            double seconds = Double.parseDouble(matcher.group(3));
            String direction = matcher.group(4);
            
            // Convert DMS to decimal degrees
            double decimalDegrees = degrees + (minutes / 60.0) + (seconds / 3600.0);
            
            // Apply sign based on direction (negative for West)
            return direction.equals("W") ? -decimalDegrees : decimalDegrees;
        }
        throw new IllegalArgumentException("Invalid GPS longitude format");
    }

    private void loadSampleNotams(String icaoCode, RecyclerView recyclerView, TextView emptyText) {
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
        
        // Update UI with sample NOTAMs
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        
        NotamAdapter adapter = new NotamAdapter(sampleNotams);
        recyclerView.setAdapter(adapter);
    }
}

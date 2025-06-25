package com.example.airportfinder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etLatDegrees, etLatMinutes, etLatSecondsWhole, etLatSecondsDecimal;
    private EditText etLonDegrees, etLonMinutes, etLonSecondsWhole, etLonSecondsDecimal;
    private RadioGroup rgLatDirection, rgLonDirection;
    private Button btnFindAirport;
    private TextView tvResult;

    private List<Airport> airports = new ArrayList<>();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AirportFinder", MODE_PRIVATE);

        initializeViews();
        loadLastValues();
        loadAirportData();
        setupClickListener();
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

        rgLatDirection = findViewById(R.id.rgLatDirection);
        rgLonDirection = findViewById(R.id.rgLonDirection);
        btnFindAirport = findViewById(R.id.btnFindAirport);
        tvResult = findViewById(R.id.tvResult);
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

        int latDirection = prefs.getInt("latDirection", R.id.rbLatNorth);
        int lonDirection = prefs.getInt("lonDirection", R.id.rbLonEast);
        rgLatDirection.check(latDirection);
        rgLonDirection.check(lonDirection);
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

        editor.putInt("latDirection", rgLatDirection.getCheckedRadioButtonId());
        editor.putInt("lonDirection", rgLonDirection.getCheckedRadioButtonId());
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

            double latDegrees = Double.parseDouble(etLatDegrees.getText().toString());
            double latMinutes = Double.parseDouble(etLatMinutes.getText().toString());
            double latSecondsWhole = Double.parseDouble(etLatSecondsWhole.getText().toString());
            double latSecondsDecimal = Double.parseDouble("0." + etLatSecondsDecimal.getText().toString());

            double lonDegrees = Double.parseDouble(etLonDegrees.getText().toString());
            double lonMinutes = Double.parseDouble(etLonMinutes.getText().toString());
            double lonSecondsWhole = Double.parseDouble(etLonSecondsWhole.getText().toString());
            double lonSecondsDecimal = Double.parseDouble("0." + etLonSecondsDecimal.getText().toString());

            double latitude = latDegrees + (latMinutes / 60.0) + ((latSecondsWhole + latSecondsDecimal) / 3600.0);
            double longitude = lonDegrees + (lonMinutes / 60.0) + ((lonSecondsWhole + lonSecondsDecimal) / 3600.0);

            RadioButton selectedLatDirection = findViewById(rgLatDirection.getCheckedRadioButtonId());
            RadioButton selectedLonDirection = findViewById(rgLonDirection.getCheckedRadioButtonId());

            if (selectedLatDirection.getText().toString().equals("S")) {
                latitude = -latitude;
            }
            if (selectedLonDirection.getText().toString().equals("W")) {
                longitude = -longitude;
            }

            List<AirportDistance> nearestAirports = findNearestAirports(latitude, longitude, 5);

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

    private List<AirportDistance> findNearestAirports(double targetLat, double targetLon, int count) {
        List<AirportDistance> distances = new ArrayList<>();

        for (Airport airport : airports) {
            double distance = calculateDistance(targetLat, targetLon, airport.latitude, airport.longitude);
            distances.add(new AirportDistance(airport, distance));
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

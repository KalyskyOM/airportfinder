# Airport Finder Android App

## 🎯 Project Overview
Complete Android Studio project implementing an Airport Finder app with all requested features:

### ✅ Implemented Features
1. **SharedPreferences Integration**: App remembers last used input values
2. **Filtered Airport Database**: Only small_airport, medium_airport, and large_airport types
3. **Split Seconds Input**: Separate fields for whole seconds and decimal parts
4. **No Spinner Arrows**: Custom styles remove spinner arrows from number inputs
5. **Real Airport Data**: Uses actual airports.csv data from OurAirports
6. **Complete Project Structure**: Ready for Android Studio import
7. **ICAO Code Input**: Search for airports by ICAO code
8. **Airport Bubble UI**: Select multiple airports with visual bubbles
9. **Airport Info Cards**: Detailed airport information display
10. **Weather Data (METAR/TAF)**: View weather information for airports
11. **NOTAM Display**: View NOTAMs for selected airports
12. **PDF Generation**: Create PDF reports with airport information
13. **Dropbox Integration**: Upload PDF reports to Dropbox

## 📁 Project Structure
```
AirportFinder/
├── app/
│   ├── src/main/
│   │   ├── java/com/airportfinder/
│   │   │   ├── MainActivity.java
│   │   │   ├── PdfGenerator.java
│   │   │   ├── DropboxUploader.java
│   │   │   ├── NotamAdapter.java
│   │   │   ├── WeatherData.java
│   │   │   └── NotamData.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── airport_bubble.xml
│   │   │   │   ├── airport_card.xml
│   │   │   │   ├── notam_item.xml
│   │   │   │   └── dropbox_setup_modal.xml
│   │   │   ├── drawable/
│   │   │   │   ├── ic_close.xml
│   │   │   │   ├── ic_dropbox.xml
│   │   │   │   ├── edit_text_background.xml
│   │   │   │   └── results_background.xml
│   │   │   ├── values/
│   │   │   │   ├── styles.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── strings.xml
│   │   │   └── raw/airports_filtered.csv
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

## 🚀 Import Instructions
1. **Extract**: Unzip AirportFinder_AndroidStudio_Project.zip
2. **Open Android Studio**
3. **Import**: Select "Open an existing Android Studio project"
4. **Navigate**: Choose the extracted `AirportFinder` folder
5. **Sync**: Wait for Gradle sync to complete (may take a few minutes)
6. **Run**: Connect device/emulator and run the app

## 📱 App Usage Guide
1. **Enter Coordinates or ICAO Code**:
   - Input latitude and longitude in your preferred format (DMS, Decimal, or GPS)
   - OR enter ICAO code(s) and add them as airport bubbles
2. **Set Search Radius**: Define the search radius in nautical miles
3. **Find Airports**: Tap the search button
4. **View Results**: See nearby airports sorted by distance
5. **View Airport Details**: Detailed cards show airport information, weather, and NOTAMs
6. **Generate Reports**: Create PDF reports with airport information
7. **Upload to Dropbox**: Save reports to your Dropbox account
8. **Automatic Save**: Your inputs are remembered for next time

## 🔧 Technical Specifications
- **Package Name**: com.airportfinder
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 34 (Android 14)
- **Language**: Java
- **UI Framework**: Material Design Components
- **Storage**: SharedPreferences for settings, External storage for PDFs
- **APIs**: Sample weather (METAR/TAF) and NOTAM data (simulated)
- **Cloud Integration**: Dropbox for file storage

## 📋 Requirements Verification
✅ **SharedPreferences**: Implemented for input persistence  
✅ **Split Seconds Input**: Separate whole + decimal fields  
✅ **No Spinner Arrows**: Custom Material Design styles  
✅ **Real Airport Data**: Filtered OurAirports CSV data  
✅ **Complete Structure**: All Android Studio files included  
✅ **Ready for Import**: Proper Gradle configuration  
✅ **ICAO Input**: Search by ICAO code with airport bubbles  
✅ **Airport Info Display**: Detailed cards with weather and NOTAMs  
✅ **PDF Generation**: Create reports for selected airports  
✅ **Dropbox Integration**: Upload reports to cloud storage  

## 🎨 Key Features
- **Material Design UI**: Modern, clean interface with aviation blue theme
- **Input Validation**: Handles invalid input gracefully
- **Distance Calculation**: Accurate distance in nautical miles
- **Data Filtering**: Only relevant airport types included
- **Persistent Storage**: SharedPreferences for user convenience
- **Multi-Selection**: Select multiple airports by ICAO code
- **Detailed Information**: View comprehensive airport data
- **Weather Information**: METAR and TAF data display
- **NOTAM Alerts**: Critical airport notices
- **PDF Reports**: Generate professional reports
- **Cloud Storage**: Dropbox integration for sharing

## 🔜 Future Enhancements
- Real-time weather data from aviation APIs
- Live NOTAM data integration
- Map view for airport visualization
- Flight planning features
- Route optimization
- Expanded airport database with more details
- User accounts and favorites

## API Integration
The application integrates with aviation weather APIs to provide real-time data:

### AVWX REST API
- Real-time METAR (current weather) data
- TAF (forecast) information
- NOTAM (Notice to Airmen) alerts
- Secure API key storage using Android Keystore and EncryptedSharedPreferences
- Visit [AVWX](https://account.avwx.rest/tokens) to obtain your API key

### Autorouter API
- Integration prepared for future flight planning features
- Visit [Autorouter](https://www.autorouter.aero/userhome) to obtain your API key

## Weather Data Utilities

The AirportFinder app includes several utility classes to handle aviation weather data from the AVWX API:

### Data Handling and Formatting

- **WeatherFormatter**: Formats raw METAR, TAF, and NOTAM data into user-friendly text. Includes methods for timestamp formatting, NOTAM validity period formatting, and extracting concise METAR summaries.

- **WeatherVisualizationHelper**: Provides color-coded visualization of aviation weather data, including flight category indicators (VFR, MVFR, IFR, LIFR), wind information, and highlighting of significant weather phenomena in METAR and TAF reports.

- **WeatherPdfGenerator**: Creates professional PDF reports containing METAR, TAF, and NOTAM data for selected airports. Reports include formatted timestamps, color-coded weather information, and organized sections for different data types.

### API Integration and Caching

- **ApiKeyManager**: Securely stores and manages AVWX API keys using Android Keystore and EncryptedSharedPreferences, ensuring sensitive credentials are never stored in plain text.

- **AvwxApiService**: Handles all communication with the AVWX REST API, including fetching METAR, TAF, and NOTAM data for airports. Implements proper error handling and response parsing.

- **ApiResponseCache**: Implements intelligent caching of API responses to reduce network usage and improve app performance. Includes configurable expiration times for different data types.

- **ApiErrorHandler**: Provides centralized error handling for API-related issues, with user-friendly error messages and logging.

### Network and Offline Support

- **NetworkUtils**: Checks for internet connectivity before making API requests to prevent unnecessary network calls and provide appropriate user feedback.

- **OfflineWeatherManager**: Enables offline access to previously fetched weather data, allowing the app to function without an internet connection. Includes methods for storing, retrieving, and managing cached weather data.

## Usage

To use the aviation weather features:

1. Obtain an API key from [AVWX](https://account.avwx.rest/tokens)
2. Enter your API key in the app settings
3. Search for airports by coordinates or ICAO code
4. View real-time METAR, TAF, and NOTAM data for selected airports
5. Generate PDF reports for flight planning
6. Upload reports to Dropbox for sharing

---
**Project Updated**: 2025-06-26  
**Version**: 2.0  
**Status**: Enhanced with ICAO search, airport info cards, weather/NOTAM display, PDF generation, Dropbox integration, and real-time aviation weather API

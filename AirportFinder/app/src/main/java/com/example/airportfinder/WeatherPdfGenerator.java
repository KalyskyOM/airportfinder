package com.example.airportfinder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for generating PDF reports of aviation weather data
 */
public class WeatherPdfGenerator {
    private static final String TAG = "WeatherPdfGenerator";
    
    /**
     * Generate a PDF report containing METAR, TAF, and NOTAM data for an airport
     * 
     * @param context Application context
     * @param icaoCode Airport ICAO code
     * @param metar METAR weather data
     * @param taf TAF weather data
     * @param notams List of NOTAM data
     * @return The generated PDF file, or null if generation failed
     */
    public static File generateWeatherReport(Context context, String icaoCode, 
                                            WeatherData metar, WeatherData taf, 
                                            List<NotamData> notams) {
        if (context == null || icaoCode == null || icaoCode.isEmpty()) {
            return null;
        }
        
        // Create PDF document
        PdfDocument document = new PdfDocument();
        
        // Page info (A4 size)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        
        // Start page
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        
        // Define paints
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(0, 51, 102)); // Aviation blue
        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        
        Paint subtitlePaint = new Paint();
        subtitlePaint.setColor(Color.rgb(0, 51, 102));
        subtitlePaint.setTextSize(18);
        subtitlePaint.setFakeBoldText(true);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(12);
        
        Paint smallTextPaint = new Paint();
        smallTextPaint.setColor(Color.DKGRAY);
        smallTextPaint.setTextSize(10);
        
        // Draw title
        String title = "Aviation Weather Report - " + icaoCode;
        canvas.drawText(title, 50, 50, titlePaint);
        
        // Draw timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US);
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = "Generated: " + dateFormat.format(new Date());
        canvas.drawText(timestamp, 50, 80, smallTextPaint);
        
        int yPosition = 120;
        
        // Draw METAR section
        if (metar != null) {
            canvas.drawText("METAR", 50, yPosition, subtitlePaint);
            yPosition += 30;
            
            if (metar.getTimestamp() > 0) {
                canvas.drawText("Observed: " + WeatherFormatter.formatTimestamp(metar.getTimestamp()), 
                        50, yPosition, smallTextPaint);
                yPosition += 20;
            }
            
            if (metar.getRawText() != null && !metar.getRawText().isEmpty()) {
                canvas.drawText("Raw METAR:", 50, yPosition, textPaint);
                yPosition += 20;
                
                String formattedRaw = WeatherFormatter.formatRawWeatherText(metar.getRawText());
                String[] rawLines = formattedRaw.split("\n");
                for (String line : rawLines) {
                    canvas.drawText(line, 60, yPosition, textPaint);
                    yPosition += 15;
                }
                yPosition += 10;
            }
            
            // Add METAR summary
            String metarSummary = WeatherFormatter.extractMetarSummary(metar);
            if (metarSummary != null && !metarSummary.isEmpty()) {
                canvas.drawText("Decoded METAR:", 50, yPosition, textPaint);
                yPosition += 20;
                
                String[] summaryLines = metarSummary.split("\n");
                for (String line : summaryLines) {
                    canvas.drawText(line, 60, yPosition, textPaint);
                    yPosition += 15;
                }
            }
            
            yPosition += 30;
        }
        
        // Draw TAF section
        if (taf != null) {
            canvas.drawText("TAF", 50, yPosition, subtitlePaint);
            yPosition += 30;
            
            if (taf.getTimestamp() > 0) {
                canvas.drawText("Issued: " + WeatherFormatter.formatTimestamp(taf.getTimestamp()), 
                        50, yPosition, smallTextPaint);
                yPosition += 20;
            }
            
            if (taf.getRawText() != null && !taf.getRawText().isEmpty()) {
                canvas.drawText("Raw TAF:", 50, yPosition, textPaint);
                yPosition += 20;
                
                String formattedRaw = WeatherFormatter.formatRawWeatherText(taf.getRawText());
                String[] rawLines = formattedRaw.split("\n");
                for (String line : rawLines) {
                    canvas.drawText(line, 60, yPosition, textPaint);
                    yPosition += 15;
                }
            }
            
            yPosition += 30;
        }
        
        // Draw NOTAMs section
        if (notams != null && !notams.isEmpty()) {
            canvas.drawText("NOTAMs", 50, yPosition, subtitlePaint);
            yPosition += 30;
            
            canvas.drawText("Total NOTAMs: " + notams.size(), 50, yPosition, smallTextPaint);
            yPosition += 20;
            
            for (int i = 0; i < notams.size(); i++) {
                NotamData notam = notams.get(i);
                
                // Check if we need a new page
                if (yPosition > 750) {
                    // Finish current page
                    document.finishPage(page);
                    
                    // Start new page
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = 50;
                }
                
                canvas.drawText("NOTAM #" + (i + 1) + ": " + notam.getId(), 50, yPosition, textPaint);
                yPosition += 20;
                
                if (notam.getStartTime() != null) {
                    String validity = WeatherFormatter.formatNotamValidity(
                            notam.getStartTime(), notam.getEndTime());
                    canvas.drawText(validity, 60, yPosition, smallTextPaint);
                    yPosition += 15;
                }
                
                if (notam.getText() != null && !notam.getText().isEmpty()) {
                    String[] lines = notam.getText().split("\n");
                    for (String line : lines) {
                        // Wrap long lines
                        int maxChars = 80;
                        for (int start = 0; start < line.length(); start += maxChars) {
                            String segment = line.substring(
                                    start, Math.min(line.length(), start + maxChars));
                            canvas.drawText(segment, 60, yPosition, textPaint);
                            yPosition += 15;
                        }
                    }
                }
                
                yPosition += 20;
            }
        }
        
        // Finish page
        document.finishPage(page);
        
        // Create output file
        File outputDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "AirportFinder");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String fileName = icaoCode + "_Weather_" + fileNameFormat.format(new Date()) + ".pdf";
        File outputFile = new File(outputDir, fileName);
        
        try {
            FileOutputStream fos = new FileOutputStream(outputFile);
            document.writeTo(fos);
            document.close();
            fos.close();
            return outputFile;
        } catch (IOException e) {
            Log.e(TAG, "Error writing PDF file", e);
            document.close();
            return null;
        }
    }
}

package com.example.airportfinder;

/**
 * Represents an airport with its basic information and location
 */
public class Airport {
    private String name;
    private double latitude;
    private double longitude;
    private String type;
    private int elevation;
    private String icaoCode;
    private double distance;
    private int track;

    public Airport(String name, double latitude, double longitude, String type, int elevation, String icaoCode) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.elevation = elevation;
        this.icaoCode = icaoCode;
        this.distance = 0.0;
        this.track = 0;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getElevation() {
        return elevation;
    }

    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    public String getIcaoCode() {
        return icaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        this.icaoCode = icaoCode;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    /**
     * Calculate distance and track from given coordinates
     * @param fromLat Latitude to calculate from
     * @param fromLon Longitude to calculate from
     */
    public void calculateDistanceAndTrack(double fromLat, double fromLon) {
        // Convert coordinates to radians
        double lat1 = Math.toRadians(fromLat);
        double lon1 = Math.toRadians(fromLon);
        double lat2 = Math.toRadians(latitude);
        double lon2 = Math.toRadians(longitude);

        // Calculate distance using Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dlon/2) * Math.sin(dlon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        distance = 3440.065 * c; // Convert to nautical miles (Earth radius = 3440.065 NM)

        // Calculate initial bearing (track)
        double y = Math.sin(dlon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dlon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        track = (int)((brng + 360) % 360); // Normalize to 0-359 degrees
    }
}

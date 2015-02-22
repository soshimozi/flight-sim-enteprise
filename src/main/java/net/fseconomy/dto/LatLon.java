package net.fseconomy.dto;

public class LatLon
{
    public double lat;
    public double lon;

    public LatLon(String slat, String slon)
    {
        lat = Double.parseDouble(slat);
        lon = Double.parseDouble(slon);
    }
    public LatLon(double latitude, double longitude)
    {
        lat = latitude;
        lon = longitude;
    }
}

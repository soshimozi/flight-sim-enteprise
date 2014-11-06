package net.fseconomy.dto;

public class LatLonSize
{
    public double lat;
    public double lon;
    public int size;
    public int type;

    public LatLonSize(double latitude, double longitude, int sz, int t)
    {
        lat = latitude;
        lon = longitude;
        size = sz;
        type = t;
    }
}

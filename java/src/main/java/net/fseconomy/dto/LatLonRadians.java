package net.fseconomy.dto;

public class LatLonRadians
{
    public double rlat;
    public double rlon;
    public LatLonRadians(double latitude, double longitude)
    {
        rlat = Math.toRadians(latitude);
        rlon = Math.toRadians(longitude);
    }
}



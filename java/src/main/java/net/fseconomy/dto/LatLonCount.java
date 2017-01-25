package net.fseconomy.dto;

import java.io.Serializable;

public class LatLonCount implements Serializable
{
    public double a;
    public double b;
    public int c;

    public LatLonCount(double latitude, double longitude, int cnt)
    {
        a = latitude;
        b = longitude;
        c = cnt;
    }
}
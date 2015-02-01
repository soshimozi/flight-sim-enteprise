package net.fseconomy.dto;

import net.fseconomy.beans.CachedAirportBean;

public class AirportInfo
{
    public String icao;
    public String title;
    public LatLon latlon;
    public int size;
    public int longestRwy;
    public int type;
    public int surface;

    public AirportInfo(String icaocode, String airportname, double latitude, double longitude, int sz, int longestrwy, int pType, int surf)
    {
        icao = icaocode;
        title = airportname;
        latlon = new LatLon(latitude, longitude);
        size = sz;
        longestRwy = longestrwy;
        type = pType;
        surface = surf;
    }

    public AirportInfo(CachedAirportBean cab)
    {
        icao = cab.getIcao();
        title = cab.getTitle();
        latlon = cab.getLatLon();
        size = cab.getSize();
        longestRwy = cab.getLongestRunway();
        type = cab.getType();
        surface = cab.getSurfaceType();
    }
}

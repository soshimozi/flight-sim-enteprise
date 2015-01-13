package net.fseconomy.dto;

public class AirportInfo
{
    public String icao;
    public String name;
    public LatLon latlon;
    public int size;
    public int type;
    public int surface;

    public AirportInfo(String icaocode, String airportname, double latitude, double longitude, int sz, int t, int surf)
    {
        icao = icaocode;
        name = airportname;
        latlon = new LatLon(latitude, longitude);
        size = sz;
        type = t;
        surface = surf;
    }
}

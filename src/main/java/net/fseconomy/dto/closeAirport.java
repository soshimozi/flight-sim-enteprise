package net.fseconomy.dto;

import net.fseconomy.data.Data;

public class CloseAirport implements Comparable<CloseAirport>
{
    public static String icao;
    public static double distance;
    public static double bearing;

    public CloseAirport(String icao, double distance, double bearing)
    {
        this.icao = icao;
        this.distance = distance;
        this.bearing = bearing;
    }

    public CloseAirport(String icao, double distance)
    {
        this.icao = icao;
        this.distance = distance;
        this.bearing = Double.NaN;
    }

    public int compareTo(CloseAirport ca)
    {
        return ca.distance == distance ? 0 : ca.distance < distance ? 1 : -1;
    }
}

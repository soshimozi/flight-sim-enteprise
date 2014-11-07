package net.fseconomy.dto;

public class CloseAirport implements Comparable<CloseAirport>
{
    public String icao;
    public double distance;
    public double bearing;

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

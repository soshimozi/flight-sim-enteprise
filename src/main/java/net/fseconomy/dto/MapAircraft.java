package net.fseconomy.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapAircraft
{
    public AirportInfo location;
    public List<MapAircraftInfo> aircraft;
    public MapAircraft(AirportInfo loc)
    {
        location = loc;
        aircraft = new ArrayList<>();
    }
}

package net.fseconomy.dto;

import java.util.ArrayList;
import java.util.List;

public class MapAssignments
{
    public AirportInfo departure;
    public List<AirportInfo> destinations;
    public List<MapAssignment> assignments;

    public MapAssignments(AirportInfo depart)
    {
        departure = depart;
        destinations = new ArrayList<>();
        assignments = new ArrayList<>();
    }
}

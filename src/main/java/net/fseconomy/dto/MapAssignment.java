package net.fseconomy.dto;

import java.util.Map;

public class MapAssignment
{
    public String icao;
    public String cargo;
    public String pay;
    public String distance;

    public MapAssignment(String sIcao, String sCargo, String sPay, String sDistance)
    {
        icao = sIcao;
        cargo = sCargo;
        pay = sPay;
        distance = sDistance;
    }
}

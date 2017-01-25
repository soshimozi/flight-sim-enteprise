package net.fseconomy.dto;

public class MapAircraftInfo
{
    public String icao;
    public LatLon latlon;
    public String registration;
    public String makemodel;
    public String equipment;
    public String fuel;
    public String sellprice;
    public String buybackprice;

    public MapAircraftInfo(String locationIcao, LatLon platlon, String reg, String pMakeModel, String pEquipment, String pTotalFuel, String pSellprice, String pBuybackprice)
    {
        icao = locationIcao;
        latlon = platlon;
        registration = reg;
        makemodel = pMakeModel;
        equipment = pEquipment;
        fuel = pTotalFuel;
        sellprice = pSellprice;
        buybackprice = pBuybackprice;
    }
}

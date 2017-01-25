package net.fseconomy.dto;

//New class to hold a single data point for flight operations at an ICAO
public class FlightOp
{
    public int year;
    public int month;
    public String icao;
    public int ops;

    public FlightOp(int opyear, int opmonth, String opicao, int opcount)
    {
        year = opyear;
        month = opmonth;
        icao = opicao;
        ops = opcount;
    }
}



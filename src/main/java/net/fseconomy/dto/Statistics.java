package net.fseconomy.dto;

import net.fseconomy.beans.AircraftBean;

import java.sql.Timestamp;
import java.util.Set;

public class Statistics implements Comparable<Statistics>
{
    public int accountId;
    public String accountName;
    public String owner;
    public int money;
    public int flights, totalMiles, totalFlightTime;
    public Timestamp firstFlight;
    public Set<AircraftBean> aircraft;
    public boolean group;

    public Statistics(int accountId, String accountName, String owner, int money, int flights, int totalMiles, int totalFlightTime, Timestamp firstFlight, Set<AircraftBean> aircraft, boolean group)
    {
        this.accountId = accountId;
        this.accountName = accountName;
        this.owner = owner;
        this.money = money;
        this.flights = flights;
        this.totalMiles = totalMiles;
        this.totalFlightTime = totalFlightTime;
        this.firstFlight = firstFlight;
        this.aircraft = aircraft;
        this.group = group;
    }

    public int compareTo(Statistics s)
    {
        return accountName.compareToIgnoreCase(s.accountName);
    }
}


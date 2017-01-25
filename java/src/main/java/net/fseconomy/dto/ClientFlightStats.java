package net.fseconomy.dto;

import java.sql.Timestamp;

/**
 * Created by smobley on 2/8/2015.
 */
public class ClientFlightStats implements Comparable<ClientFlightStats>
{
    public int userId;
    public String client;
    public Timestamp startTime;
    public Timestamp endTime;
    public String fromIcao;
    public String toIcao;
    public String makeModel;
    public double distance;
    public double stopDistance;
    public int flightTimeSeconds;
    public int realTimeSeconds;
    public int estimatedTC;

    public ClientFlightStats(int pUserId, String pClient, Timestamp pStartTime, Timestamp pEndTime, String pFromIcao, String pToIcao, String pMakeModel, double pDistance, double pStopDistance, int pFlightTimeSeconds, int pRealTimeSeconds, int pEstimatedTC)
    {
        userId = pUserId;
        client = pClient;
        startTime = pStartTime;
        endTime = pEndTime;
        fromIcao = pFromIcao;
        toIcao = pToIcao;
        makeModel = pMakeModel;
        distance = pDistance;
        stopDistance = pStopDistance;
        flightTimeSeconds = pFlightTimeSeconds;
        realTimeSeconds = pRealTimeSeconds;
        estimatedTC = pEstimatedTC;
    }

    public int compareTo(ClientFlightStats s)
    {
        return startTime.compareTo(s.startTime);
    }
}

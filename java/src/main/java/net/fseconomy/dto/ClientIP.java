package net.fseconomy.dto;

public class ClientIP
{
    public String ip;
    public int count;
    public String users;
    public ClientIP(String pIp, int pCount, String pUsers)
    {
        ip = pIp;
        count = pCount;
        users = pUsers;
    }
}

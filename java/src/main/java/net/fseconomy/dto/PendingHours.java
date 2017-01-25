package net.fseconomy.dto;


public class PendingHours
{
    public float phours;
    public String phourtime;
    public String pminutetime;

    public PendingHours(float phours, String phourtime, String pminutetime)
    {
        this.phours=phours;
        this.phourtime=phourtime;
        this.pminutetime=pminutetime;
    }
}
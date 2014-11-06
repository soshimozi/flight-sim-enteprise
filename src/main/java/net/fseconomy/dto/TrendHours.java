package net.fseconomy.dto;

public class TrendHours
{
    public String logdate;
    public String duration;
    public float last48Hours;

    public TrendHours(String logdate, int duration, int last48Hours)
    {
        this.logdate = logdate;
        this.duration = "" + (float)Math.round(duration/3600.0 *10)/10;
        this.last48Hours = (float)Math.round(last48Hours/3600.0*10)/10;
    }
}

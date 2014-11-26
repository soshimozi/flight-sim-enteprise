package net.fseconomy.dto;

import java.sql.Timestamp;

public class DbLog
{
    public Timestamp timestamp;
    public String level;
    public String callerClass;
    public String message;
}

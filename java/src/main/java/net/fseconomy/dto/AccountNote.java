package net.fseconomy.dto;

import java.sql.Timestamp;

public class AccountNote
{
    public int accountId;
    public Timestamp created;
    public int createdBy;
    public String note;

    public String accountName;
    public String createdByName;
}

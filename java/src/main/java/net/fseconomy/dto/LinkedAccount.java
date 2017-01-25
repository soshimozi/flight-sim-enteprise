package net.fseconomy.dto;

public class LinkedAccount
{
    public int linkId;
    public int accountId;
    public int status;
    public String accountName;

    public String getAccountName()
    {
        return accountName + " (" + accountId + ")";
    }
    public String getStatus()
    {
        if(status == 0)
            return "inactive";
        else
            return "active";
    }
}

package net.fseconomy.beans;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;

public class ServiceAccessBean
{
    private int id;
    private int serviceid;
    private String servicename;
    private int accountid;
    private String cashaccess;
    private String bankaccess;
    private String aircraftaccess;
    private String fboaccess;
    private String goodsaccess;

    private ServiceAccessBean() {}

    public ServiceAccessBean(CachedRowSet rs)
    {
        try
        {
            id = rs.getInt("id");
            serviceid = rs.getInt("serviceid");
            setServicename(rs.getString("servicename"));
            accountid = rs.getInt("accountid");
            cashaccess = rs.getString("cash");
            bankaccess = rs.getString("bank");
            aircraftaccess = rs.getString("aircraft");
            fboaccess = rs.getString("fbo");
            goodsaccess = rs.getString("goods");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getServiceid()
    {
        return serviceid;
    }

    public void setServiceid(int serviceid)
    {
        this.serviceid = serviceid;
    }

    public int getAccountid()
    {
        return accountid;
    }

    public void setAccountid(int accountid)
    {
        this.accountid = accountid;
    }

    public String getCashaccess()
    {
        return cashaccess;
    }

    public void setCashaccess(String cashaccess)
    {
        this.cashaccess = cashaccess;
    }

    public String getBankaccess()
    {
        return bankaccess;
    }

    public void setBankaccess(String bankaccess)
    {
        this.bankaccess = bankaccess;
    }

    public String getAircraftaccess()
    {
        return aircraftaccess;
    }

    public void setAircraftaccess(String aircraftaccess)
    {
        this.aircraftaccess = aircraftaccess;
    }

    public String getFboaccess()
    {
        return fboaccess;
    }

    public void setFboaccess(String fboaccess)
    {
        this.fboaccess = fboaccess;
    }

    public String getGoodsaccess()
    {
        return goodsaccess;
    }

    public void setGoodsaccess(String goodsaccess)
    {
        this.goodsaccess = goodsaccess;
    }

    public String getServicename()
    {
        return servicename;
    }

    public void setServicename(String servicename)
    {
        this.servicename = servicename;
    }
}

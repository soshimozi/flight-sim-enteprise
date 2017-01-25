package net.fseconomy.data;

import net.fseconomy.beans.ServiceAccessBean;
import net.fseconomy.beans.ServiceProviderBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.util.Formatters;

import javax.sql.rowset.CachedRowSet;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ServiceProviders implements Serializable
{
    public static String createAccessKey()
    {
        return createAccessKey(10);
    }

    public static String createAccessKey(int len)
    {
        StringBuilder result = new StringBuilder();

        for (int loop = 0; loop < len; loop++)
        {
            int ran=(int)Math.round(Math.random()*35);
            if (ran < 10)
                result.append(ran);
            else
                result.append((char)('A'+ran-10));
        }

        return result.toString();
    }

    public static ServiceProviderBean getServiceProviderById(int serviceid)
    {
        List<ServiceProviderBean> result = null;

        try
        {
            String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id  WHERE s.id = ? order by status, a1.name";
            CachedRowSet crs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, serviceid);
            result = getServiceProviderList(crs);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result == null || result.size() == 0 ? null : result.get(0);
    }

    public static ServiceProviderBean getServiceProviderByOwner(int userid)
    {
        List<ServiceProviderBean> result = null;

        try
        {
            String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id  WHERE s.owner = ? order by status, a1.name";
            CachedRowSet crs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, userid);
            result = getServiceProviderList(crs);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result == null || result.size() == 0 ? null : result.get(0);
    }

    public static List<ServiceProviderBean> getServiceProviders()
    {
        List<ServiceProviderBean> result = null;

        try
        {
            String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id";
            CachedRowSet crs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            result = getServiceProviderList(crs);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static ServiceProviderBean getServiceProviderByKey(String key)
    {
        List<ServiceProviderBean> result = null;

        try
        {
            String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id where s.key = ?";
            CachedRowSet crs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, key);
            result = getServiceProviderList(crs);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result == null || result.size() == 0 ? null : result.get(0);
    }

//    public static String getServiceProviderNameByKey(String key)
//    {
//        String result = null;
//
//        try
//        {
//            String qry = "select name from serviceproviders where `key`= ?";
//            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), key);
//        }
//        catch (SQLException e)
//        {
//            e.printStackTrace();
//        }
//
//        return result;
//    }

    static List<ServiceProviderBean> getServiceProviderList(ResultSet rs)
    {
        ArrayList<ServiceProviderBean> result = new ArrayList<>();
        try
        {
            while (rs.next())
            {
                ServiceProviderBean item = new ServiceProviderBean(rs);
                result.add(item);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public static void addServiceProvider(ServiceProviderBean service)
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        Date date = new Date();
        String note = Formatters.dateyyyymmddhhmmss.format(date) + " - Created";
        service.setNotes(note);
        service.setKey(createAccessKey());
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("select * from serviceproviders where id=-1");

            rs.moveToInsertRow();
            service.writeBean(rs);
            rs.insertRow();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void updateServiceProvider(ServiceProviderBean service)
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM serviceproviders WHERE id=" + service.getId());

            rs.first();
            service.writeBean(rs);
            rs.updateRow();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void doServiceProviderNotification(ServiceProviderBean service, String subject, String messageText, boolean adminonly) throws DataError
    {
        List<String> toList = new ArrayList<>();

        Emailer emailer = Emailer.getInstance();

        if (!adminonly)
        {
            UserBean owner = Accounts.getAccountById(service.getOwner());
            UserBean alt = null;
            if (service.getAlternate() != -1)
            {
                alt = Accounts.getAccountById(service.getAlternate());
            }

            toList.add(owner.getEmail());

            if (alt != null)
            {
                toList.add(alt.getEmail());
            }
        }

        toList.add("fseadmin@gmail.com");

        emailer.sendEmail("no-reply@fseconomy.net", "FSEconomy Service Notification System", subject, messageText, toList, Emailer.ADDRESS_TO);
    }

    public static ServiceAccessBean getServiceProviderAccess(int serviceId, int accountId)
    {
        ServiceAccessBean result = null;

        try
        {
            String qry = "SELECT sa.*, sp.servicename FROM serviceaccess sa, serviceproviders sp WHERE sa.serviceid=sp.id AND serviceId = ? AND accountId = ?;";
            CachedRowSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, serviceId, accountId);
            if (rs.next())
            {
                result = new ServiceAccessBean(rs);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static ServiceAccessBean[] getCurrentServiceProviderAccess(int accountId)
    {
        ServiceAccessBean[] result = null;

        try
        {
            String qry = "SELECT sa.*, sp.servicename FROM serviceaccess sa, serviceproviders sp WHERE sa.serviceid=sp.id AND accountId = ?;";
            CachedRowSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, accountId);
            result = getServiceAccessArray(rs);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    static ServiceAccessBean[] getServiceAccessArray(CachedRowSet rs)
    {
        ArrayList<ServiceAccessBean> result = new ArrayList<>();
        try
        {
            while (rs.next())
            {
                ServiceAccessBean item = new ServiceAccessBean(rs);
                result.add(item);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return result.toArray(new ServiceAccessBean[result.size()]);
    }

    public static void updateServiceProviderAccess(int accountId, int serviceId, HashMap<String, String> map)
    {
        try
        {
            String qry = "UPDATE serviceaccess Set cash=?, bank=?, aircraft=? WHERE serviceid=? AND accountid=?;";
            DALHelper.getInstance().ExecuteUpdate(qry, map.get("cash"), map.get("bank"), map.get("aircraft"), serviceId, accountId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void addServiceProviderAccess(int accountId, int serviceId)
    {
        try
        {
            String qry = "INSERT INTO serviceaccess (serviceid, accountid) VALUES(?, ?);";
            DALHelper.getInstance().ExecuteUpdate(qry, serviceId, accountId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void deleteServiceProviderAccess(int accountId, int serviceId)
    {
        try
        {
            String qry = "DELETE FROM serviceaccess WHERE serviceId = ? AND accountId = ?;";
            DALHelper.getInstance().ExecuteUpdate(qry, serviceId, accountId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<ServiceProviderBean> getAccountAvailableServiceProviders(int accountId)
    {
        List<ServiceProviderBean> result = null;

        try
        {
            String qry = "SELECT s.id, s.owner, a1.name AS ownername, s.alternate, a2.name AS alternatename, s.servicename, s.ip, s.url, s.description, s.status, s.key, notes FROM serviceproviders AS s LEFT JOIN accounts AS a1 ON owner=a1.id LEFT JOIN accounts AS a2 on alternate=a2.id LEFT JOIN serviceaccess as sa on s.id=sa.serviceId AND sa.accountId=? where s.status=1 AND sa.serviceId is null";
            CachedRowSet crs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, accountId);
            result = getServiceProviderList(crs);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
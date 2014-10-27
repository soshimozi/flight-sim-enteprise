package net.fseconomy.services;

import net.fseconomy.data.DALHelper;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
//import java.util.EnumSet;
import java.util.List;

public class common
{
    public enum PermissionCategory
    {
        CASH,
        BANK,
        AIRCRAFT,
        FBO,
        GOODS
    }

    public enum PermissionSet
    {
        READ(1), WITHDRAW(2), DEPOSIT(4), TRANSFER(8), SELL(16), PURCHASE(32), LEASE(64);

        //public static final EnumSet<PermissionSet> ALL_OPTS = EnumSet.allOf(PermissionSet.class);

        private final int id;
        PermissionSet(int id) { this.id = id; }
        public int getValue() { return id; }
        public static List<PermissionSet> parsePermissionSet(String val)
        {
            List<PermissionSet> apList = new ArrayList<>();
            for (PermissionSet ap : values())
            {
                if (val.contains(ap.name()))
                    apList.add(ap);
            }
            return apList;
        }
    }

    public static boolean hasPermission(String servicekey, int account, PermissionCategory check, PermissionSet required)
    {
        String permissions = getPermissions(servicekey, account, check);

        List<PermissionSet> list = PermissionSet.parsePermissionSet(permissions);
        for(PermissionSet val: list)
        {
            if (val.equals(required))
                return true;
        }

        return false;
    }

    public static String getPermissions(String servicekey, int account, PermissionCategory check)
    {
        try
        {
            String qry = "SELECT sa.* FROM serviceaccess sa, serviceproviders sp WHERE sa.serviceid=sp.id AND sp.key = ? AND sa.accountid=?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, servicekey, account);
            if(rs.next())
                return rs.getString(check.name().toLowerCase());
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return ""; //no permissions
    }

    public static int getServiceId(String servicekey)
    {
        try
        {
            String qry = "SELECT * FROM serviceproviders sp WHERE sp.key = ?";
            return DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), servicekey);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return 0;
        }
    }

    public static ResponseContainer createResponse(String status, String error, String info, Object data)
    {
        ResponseContainer rc = new ResponseContainer();
        rc.getMeta().setCode(status);
        rc.getMeta().setError(error);
        rc.getMeta().setInfo(info);
        rc.setData(data);

        return rc;
    }

    public static Response createErrorResponse(int code, String error, String message)
    {
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        return Response.status(code)
                .cacheControl(NoCache)
                .header("Access-Control-Allow-Origin", "*")
                .entity(createResponse(""+code, error, message, null))
                .build();
    }

    public static Response createSuccessResponse(int code, String error, String message, Object object)
    {
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        return Response.status(code)
                .cacheControl(NoCache)
                .header("Access-Control-Allow-Origin", "*")
                .entity(createResponse("" + code, error, message, object))
                .build();
    }

    public static Response ResponseAccessDenied()
    {
        return createErrorResponse(401, "AccessDenied", "");
    }
}

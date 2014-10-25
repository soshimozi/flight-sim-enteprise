/*
 * FS Economy
 * Copyright (C) 2005, 2006, 2007  Marty Bochane
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.fseconomy.services;

import net.fseconomy.data.DALHelper;
import net.fseconomy.util.Formatters;
import net.fseconomy.util.RestResponses;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class serviceData
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

        public static final EnumSet<PermissionSet> ALL_OPTS = EnumSet.allOf(PermissionSet.class);

        private final int id;
        PermissionSet(int id) { this.id = id; }
        public int getValue() { return id; }
        public static List<PermissionSet> parsePermissionSet(int val)
        {
            List<PermissionSet> apList = new ArrayList<>();
            for (PermissionSet ap : values())
            {
                if ((val & ap.getValue()) != 0)
                    apList.add(ap);
            }
            return apList;
        }
    }

    public static boolean hasPermission(String servicekey, int account, PermissionCategory check, PermissionSet required)
    {
        int permissions = getPermissions(servicekey, account, check);

        List<PermissionSet> list = PermissionSet.parsePermissionSet(permissions);
        for(PermissionSet val: list)
        {
            if (val.equals(required))
                return true;
        }

        return false;
    }

    private static int getPermissions(String servicekey, int account, PermissionCategory check)
    {
        try
        {
            String qry = "SELECT sa.* FROM serviceaccess sa, serviceproviders sp WHERE sa.serviceid=sp.id AND sp.key = ? AND sa.accountid=?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, servicekey, account);
            if(rs.next())
                return rs.getInt(check.name());
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return 0; //no permissions
    }

    static ResponseContainer createResponse(String status, String error, String info, Object data)
    {
        ResponseContainer rc = new ResponseContainer();
        rc.getMeta().setCode(status);
        rc.getMeta().setError(error);
        rc.getMeta().setInfo(info);
        rc.setData(data);

        return rc;
    }

    public static Response getBalance(PermissionCategory type, int account)
    {
        String qry = "";
        double balance;

        if(type == PermissionCategory.BANK)
            qry = "SELECT bank as balance FROM accounts WHERE id = ?";
        else if(type == PermissionCategory.CASH)
            qry = "SELECT money as balance FROM accounts WHERE id = ?";

        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, account);
            if(rs.next())
            {
                return Response.status(200)
                        .cacheControl(NoCache)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity(createResponse("200", null, null, Formatters.twoDecimals.format(rs.getDouble("balance"))))
                        .build();
            }
            else
            {
                return Response.status(400)
                        .cacheControl(NoCache)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity(createResponse("400", "Bad Request", "No records found.", null))
                        .build();
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();

            return Response
                    .status(500)
                    .cacheControl(NoCache)
                    .header("Access-Control-Allow-Origin", "*")
                    .entity(createResponse("400", "System Error", "Unable to fullfill the request.", null)).build();
        }
    }

    public static Response getAccountId(String name)
    {
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        try
        {
            String qry = "SELECT id FROM accounts WHERE name = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, name);
            if(rs.next())
            {
                return Response
                        .status(200)
                        .cacheControl(NoCache)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity(createResponse("200", "", "", rs.getInt("id")))
                        .build();
            }
            else
            {
                return Response.status(400)
                        .cacheControl(NoCache)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity(createResponse("400", "Bad Request", "No records found.", null))
                        .build();
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();

            return Response
                    .status(500)
                    .cacheControl(NoCache)
                    .header("Access-Control-Allow-Origin", "*")
                    .entity(createResponse("400", "System Error", "Unable to fullfill the request.", null)).build();
        }
    }

    public static Response getAccountName(int id)
    {
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        try
        {
            String qry = "SELECT name FROM accounts WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
            if(rs.next())
            {
                return Response
                        .status(200)
                        .cacheControl(NoCache)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity(createResponse("200", "", "", rs.getString("name")))
                        .build();
            }
            else
            {
                return Response.status(400)
                        .cacheControl(NoCache)
                        .header("Access-Control-Allow-Origin", "*")
                        .entity(createResponse("400", "Bad Request", "No records found.", null))
                        .build();
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();

            return Response
                    .status(500)
                    .cacheControl(NoCache)
                    .header("Access-Control-Allow-Origin", "*")
                    .entity(createResponse("400", "System Error", "Unable to fullfill the request.", null)).build();
        }
    }
}

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

import static net.fseconomy.services.common.*;

import net.fseconomy.data.*;
import net.fseconomy.util.Formatters;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import java.sql.*;


public class serviceData
{
    public static Response getBalance(PermissionCategory type, int account)
    {
        double balance;

        try
        {
            balance = getBalanceAmount(type, account);

            return createSuccessResponse(200, null, null, Formatters.twoDecimals.format(balance));
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error",  "Unable to fulfill the request.");
        }
    }

    public static Response WithdrawIntoCash(int account, double amount)
    {
        String qry;

        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        try
        {
            //is there enough bank balance?
            double balance = getBalanceAmount(PermissionCategory.BANK, account);

            //if not, return error
            if(balance < amount)
                return createErrorResponse(400, "Bad Request", "Balance less than amount. Services cannot take out loans.");

            qry = "UPDATE accounts set bank = bank - ?, money = money + ? WHERE id = ?;";
            DALHelper.getInstance().ExecuteNonQuery(qry, amount, amount, account);

            return createSuccessResponse(200, null, null, "Withdrawal successful.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error", "Unable to fulfill the request.");
        }
    }

    public static Response DepositIntoBank(int account, double amount)
    {
        String qry;

        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        try
        {
            //is there enough bank balance?
            double balance = getBalanceAmount(PermissionCategory.CASH, account);

            //if not, return error
            if(balance < amount)
                return createErrorResponse(400, "Bad Request", "Balance less than amount. Services cannot take out loans.");

            qry = "UPDATE accounts set bank = bank + ?, money = money - ? WHERE id = ?;";
            DALHelper.getInstance().ExecuteNonQuery(qry, amount, amount, account);

            return createSuccessResponse(200, null, null, "Deposit successful.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error", "Unable to fulfill the request.");
        }
    }

    public static Response TransferCashToAccount(String servicekey, int account, float amount, int transferto)
    {
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        try
        {
            //is there enough bank balance?
            double balance = getBalanceAmount(PermissionCategory.CASH, account);

            //if not, return error
            if(balance < amount)
                return createErrorResponse(400, "Bad Request", "Balance less than amount to transfer.");

            //check that transferto exists
            if(!checkAccountExists(transferto))
                return createErrorResponse(400, "Bad Request", "Transfer account does not exist.");

            int serviceid = getServiceId(servicekey);
            String qry = "{call TransferCash(?,?,?,?,?)}";
            boolean success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, account, amount, transferto, "Service: " + serviceid);

            if(success)
                return createSuccessResponse(200, null, null, "Transfer successful.");
            else
                return createErrorResponse(500, "System Error",  "Database error has occurred. Transaction terminated");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error", "Unable to fulfill the request.");
        }
    }

    public static Response PurchaseAircraft(String servicekey, int account, String reg)
    {
        try
        {
            AircraftBean[] aircraft = Data.getInstance().getAircraftByRegistration(reg);

            //if not, return error
            if(aircraft.length != 1)
                return createErrorResponse(400, "Bad Request", "Registration not found.");

            if(!aircraft[0].isForSale())
                return createErrorResponse(400, "Bad Request", "Aircraft not for sale.");

            if (!hasFundsRequired(account, aircraft[0].getSellPrice()))
                return createErrorResponse(400, "Bad Request", "Balance less than amount of purchase.");

            int serviceid = getServiceId(servicekey);
            String qry = "{call PurchaseAircraft(?,?,?,?)}";
            boolean success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, reg, account, "Service: " + serviceid);

            if(success)
                return createSuccessResponse(200, null, null, "Aircraft purchase successful.");
            else
                return createErrorResponse(500, "System Error",  "Database error has occurred. Transaction terminated");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error",  "Unable to fulfill the request.");
        }
    }

    public static Response LeaseAircraft(String servicekey, int account, String reg, int leaseto)
    {
        boolean success = false;
        String mode = "";

        try
        {
            AircraftBean[] aircraft = Data.getInstance().getAircraftByRegistration(reg);

            //if not, return error
            if(aircraft.length != 1)
                return createErrorResponse(400, "Bad Request", "Registration not found.");

            int serviceid = getServiceId(servicekey);

            if(aircraft[0].getLessor() == 0 && aircraft[0].getOwner() == account)
            {
                mode = "lease";
                String qry = "{call AircraftLease(?,?,?,?)}";
                success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, reg, leaseto, "Lease, Service: " + serviceid);
            }
            else if(aircraft[0].getLessor() == account) //return lease
            {
                mode = "unlease";
                String qry = "{call AircraftUnlease(?,?,?)}";
                success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, reg, "Lease return, Service: " + serviceid);
            }
            else
                return createErrorResponse(400, "Bad Request", "Account not lessor.");

            if (success)
                return createSuccessResponse(200, null, null, "Aircraft " + mode + " successful.");
            else
                return createErrorResponse(500, "System Error", "Database error has occurred. Transaction terminated");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error",  "Unable to fulfill the request.");
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
                return createSuccessResponse(200, null, null, rs.getInt("id"));
            else
                return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error", "Unable to fulfill the request.");
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
                return createSuccessResponse(200, null, null, rs.getString("name"));
            else
                return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error",  "Unable to fulfill the request.");
        }
    }
}

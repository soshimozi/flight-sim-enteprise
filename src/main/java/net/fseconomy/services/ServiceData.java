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

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.data.*;
import net.fseconomy.util.Formatters;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import java.sql.*;


public class ServiceData
{
    public static Response getBalance(PermissionCategory type, int account)
    {
        double balance;

        try
        {
            balance = getBalanceAmount(type, account);

            return createSuccessResponse(200, 200, null, null, Formatters.twoDecimals.format(balance));
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error",  "An error has occurred.");
        }
    }

    public static Response WithdrawIntoCash(int account, double amount)
    {
        try
        {
            //is there enough bank balance?
            double balance = getBalanceAmount(PermissionCategory.BANK, account);

            //if not, return error
            if(balance < amount)
                return createErrorResponse(200, 200, "Bad Request", "Exceeds available funds.");

            String qry = "UPDATE accounts set bank = bank - ?, money = money + ? WHERE id = ?;";
            DALHelper.getInstance().ExecuteNonQuery(qry, amount, amount, account);

            return createSuccessResponse(200, 200, null, null, "Successful.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error", "An error has occurred.");
        }
    }

    public static Response DepositIntoBank(int account, double amount)
    {
        try
        {
            //is there enough bank balance?
            double balance = getBalanceAmount(PermissionCategory.CASH, account);

            //if not, return error
            if(balance < amount)
                return createErrorResponse(200, 200, "Bad Request", "Exceeds available funds.");

            String qry = "UPDATE accounts set bank = bank + ?, money = money - ? WHERE id = ?;";
            DALHelper.getInstance().ExecuteNonQuery(qry, amount, amount, account);

            return createSuccessResponse(200, 200, null, null, "Successful.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error", "An error has occurred.");
        }
    }

    public static Response TransferCashToAccount(String serviceKey, int account, float amount, int transferTo, String note)
    {
        try
        {
            //is there enough bank balance?
            double balance = getBalanceAmount(PermissionCategory.CASH, account);

            //if not, return error
            if(balance < amount)
                return createErrorResponse(200, 200, "Bad Request", "Exceeds available funds.");

            //check that transferTo exists
            if(!checkAccountExists(transferTo))
                return createErrorResponse(200, 200, "Bad Request", "TransferTo account does not exist.");

            int serviceid = getServiceId(serviceKey);

            String qry = "{call TransferCash(?,?,?,?,?)}";
            boolean success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, account, amount, transferTo, "Service(" + serviceid + "): " + note);

            if(success)
                return createSuccessResponse(200, 200, null, null, "Successful.");
            else
                return createErrorResponse(200, 200, "System Error",  "Unable to process.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error", "An error has occurred.");
        }
    }

    public static Response PurchaseAircraft(String serviceKey, int account, int serialNumber, String note)
    {
        try
        {
            AircraftBean aircraft = Aircraft.getAircraftById(serialNumber);

            //if not, return error
            if(aircraft == null)
                return createErrorResponse(200, 200, "Bad Request", "Registration not found.");

            if(!aircraft.isForSale())
                return createErrorResponse(200, 200, "Bad Request", "Aircraft not for sale.");

            if (!hasFundsRequired(account, aircraft.getSellPrice()))
                return createErrorResponse(200, 200, "Bad Request", "Exceeds available funds.");

            int serviceid = getServiceId(serviceKey);

            String qry = "{call PurchaseAircraft(?,?,?,?)}";
            boolean success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, serialNumber, account, "Service(" + serviceid + "): " + note);

            if(success)
                return createSuccessResponse(200, 200, null, null, "Aircraft purchase successful.");
            else
                return createErrorResponse(200, 200, "System Error",  "Unable to process.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error",  "An error has occurred.");
        }
    }

    public static Response TransferAircraft(String serviceKey, int serialNumber, int account, int transferTo, String note)
    {
        try
        {
            AircraftBean aircraft = Aircraft.getAircraftById(serialNumber);

            //if not, return error
            if(aircraft == null)
                return createErrorResponse(200, 200, "Bad Request", "SerialNumber not found.");

            if(aircraft.getOwner() != account)
                return createErrorResponse(200, 200, "Bad Request", "Not the owner.");

            if(isAircraftLeased(aircraft.getId()))
                return createErrorResponse(200, 200, "Bad Request", "Aircraft is leased.");

            int serviceid = getServiceId(serviceKey);

            String qry = "{call AircraftTransfer(?,?,?,?)}";
            boolean success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, serialNumber, transferTo, "Service(" + serviceid + "): " + note);

            if (success)
                return createSuccessResponse(200, 200, null, null, "Aircraft transfer successful.");
            else
                return createErrorResponse(200, 200, "System Error", "Unable to process.");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error",  "An error has occurred.");
        }
    }

    public static Response LeaseAircraft(String serviceKey, int account, int serialNumber, int leaseTo, String note)
    {
        boolean success;
        String mode;

        try
        {
            AircraftBean aircraft = Aircraft.getAircraftById(serialNumber);

            //if not, return error
            if(aircraft == null)
                return createErrorResponse(200, 200, "Bad Request", "Registration not found.");

            int serviceid = getServiceId(serviceKey);

            if(aircraft.getLessor() == 0 && aircraft.getOwner() == account)
            {
                mode = "lease";
                String qry = "{call AircraftLease(?,?,?,?)}";
                success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, serialNumber, leaseTo, "Service(" + serviceid + "): " + note);
            }
            else if(aircraft.getLessor() == account) //return lease
            {
                mode = "unlease";
                String qry = "{call AircraftUnlease(?,?,?)}";
                success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, serialNumber, "Service(" + serviceid + "): " + note);
            }
            else
                return createErrorResponse(200, 200, "Bad Request", "Account not lessor.");

            if (success)
                return createSuccessResponse(200, 200, null, null, "Aircraft " + mode + " successful.");
            else
                return createErrorResponse(500, 500, "System Error", "Unable to process");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 200, "System Error",  "An error has occurred.");
        }
    }

    public static Response ReturnLeaseAircraft(String serviceKey, int account, int serialNumber, String note)
    {
        boolean success;

        try
        {
            AircraftBean aircraft = Aircraft.getAircraftById(serialNumber);

            //if not, return error
            if(aircraft == null)
                return createErrorResponse(200, 200, "Bad Request", "Registration not found.");

            int serviceid = getServiceId(serviceKey);

            if(aircraft.getLessor() == account) //return lease
            {
                String qry = "{call AircraftUnlease(?,?,?)}";
                success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, serialNumber, "Service(" + serviceid + "): " + note);
            }
            else
                return createErrorResponse(200, 200, "Bad Request", "Account not lessor.");

            if (success)
                return createSuccessResponse(200, 200, null, null, "Aircraft return lease successful.");
            else
                return createErrorResponse(500, 500, "System Error", "Unable to process");
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 200, "System Error",  "An error has occurred.");
        }
    }

    public static Response getAccountId(String name)
    {
        try
        {
            String qry = "SELECT id FROM accounts WHERE name = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, name);

            if(rs.next())
                return createSuccessResponse(200, 200, null, null, rs.getInt("id"));
            else
                return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error", "An error has occurred.");
        }
    }

    public static Response getAccountName(int id)
    {
        try
        {
            String qry = "SELECT name FROM accounts WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);

            if(rs.next())
                return createSuccessResponse(200, 200, null, null, rs.getString("name"));
            else
                return createErrorResponse(200, 200, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, 500, "System Error",  "An error has occurred.");
        }
    }
}

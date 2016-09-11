/*
 * FS Economy
 * Copyright (C) 2005  Marty Bochane
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

package net.fseconomy.servlets;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.SQLException;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.fseconomy.beans.*;
import net.fseconomy.data.*;

import net.fseconomy.util.Formatters;
import net.fseconomy.util.GlobalLogger;
import net.fseconomy.util.Helpers;


public class UserCtl extends HttpServlet
{
	private static final long serialVersionUID = 1L;

    final int BANK_OF_FSE_ID = 4705;

    public void init()
	{
        GlobalLogger.logApplicationLog("UserCtl init() called", UserCtl.class);

		FullFilter.updateFilter(DALHelper.getInstance());
        ScheduledTasks.getInstance().startScheduledTasks();
	}

    public void destroy()
    {
        GlobalLogger.logApplicationLog("UserCtl destroy() called", UserCtl.class);

        ScheduledTasks.getInstance().endScheduledTasks();
    }

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		doPost(req, resp);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		String action = req.getParameter("event");

        if(action == null)
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No action specified");
            return;
        }

		String returnToPage = req.getParameter("returnpage");
		String returnToPageOverride = null;
				
		try
		{
			try
			{
                switch (action)
                {
                    case "Agree & Log in":
                        if (!doLogin(req))
                            returnToPage = "/requestnewpassword.jsp";
                        else
                            returnToPageOverride = "/index.jsp";
                        break;
                    case "Log out":
                        doLogout(req);
                        returnToPageOverride = "/index.jsp";
                        break;
                    case "create":
                        newUser(req);
                        break;
                    case "updateAcct":
                        updateAcct(req);
                        break;
                    case "editUser":
                        editUser(req);
                        break;
                    case "linkAccount":
                        linkAccount(req);
                        break;
                    case "unlinkAccount":
                        unlinkAccount(req);
                        break;
                    case "addAccountNote":
                        addAccountNote(req);
                        break;
                    case "password":
                        newPassword(req);
                        break;
                    case "changePassword":
                        changePassword(req);
                        break;
                    case "lockAccount":
                        lockAccount(req);
                        break;
                    case "unlockAccount":
                        unlockAccount(req);
                        break;
                    case "Assignment":
                        processAssignment(req);
                        break;
                    case "newAssignment":
                        newAssignment(req);
                        break;
                    case "updateAssignment":
                        updateAssignment(req);
                        break;
                    case "removeAssignmentFromGroup":
                        removeAssignmentFromGroup(req);
                        break;
                    case "deleteAssignment":
                        deleteGroupAssignment(req);
                        break;
                    case "newGoodsAssignment":
                        newGoodsAssignment(req);
                        break;
                    case "updateGoodsAssignment":
                        updateGoodsAssignment(req);
                        break;
                    case "deleteGoodsAssignment":
                        deleteGoodsAssignment(req);
                        break;
                    case "unlockGoodsAssignment":
                        unlockGoodsAssignment(req);
                        break;
                    case "Aircraft":
                        addAircraft(req);
                        break;
                    case "AircraftLeaseReturn":
                        returnLeaseAircraft(req);
                        break;
                    case "Cancel":
                        cancelFlight(req);
                        break;
                    case "Market":
                        doBuyAircraft(req);
                        break;
                    case "sell":
                        doAircraftSell(req);
                        break;
                    case "payfeedebt":
                        doAircraftPayFeeDebt(req);
                        break;
                    case "bank":
                        doBank(req);
                        break;
                    case "bankDeposit":
                        doBankDeposit(req);
                        break;
                    case "bankWithdrawal":
                        doBankWithdrawal(req);
                        break;
                    case "mappings":
                        doMappings(req);
                        break;
                    case "joingroup":
                        doJoinGroup(req);
                        break;
                    case "leavegroup":
                        doLeaveGroup(req);
                        break;
                    case "groupreject":
                        doGroupRejectUser(req);
                        break;
                    case "kickgroup":
                        doKickGroup(req);
                        break;
                    case "deletegroup":
                        doDeleteGroup(req);
                        break;
                    case "transfergroup":
                        doTransferGroup(req);
                        break;
                    case "memberlevel":
                        doMemberLevel(req);
                        break;
                    case "invitation":
                        doInvitation(req);
                        break;
                    case "maintenance":
                        doMaintenance(req);
                        break;
                    case "equipment":
                        doEquipment(req);
                        break;
                    case "refuel":
                        doRefuel(req);
                        break;
                    case "flyForGroup":
                        doFlyForGroup(req);
                        break;
                    case "payGroup":
                        doPayGroup(req);
                        break;
                    case "buyGoods":
                        doBuySellGoods(req, true);
                        break;
                    case "sellGoods":
                        doBuySellGoods(req, false);
                        break;
                    case "purchaseFbo":
                        doPurchaseFbo(req);
                        break;
                    case "transferFbo":
                        doTransferFbo(req);
                        break;
                    case "deleteFbo":
                        doDeleteFbo(req);
                        break;
                    case "upgradeFbo":
                        doUpgradeFbo(req);
                        break;
                    case "rentFboFacility":
                        doRentFboFacility(req);
                        break;
                    case "deleteFboFacility":
                        doDeleteFboFacility(req);
                        break;
                    case "bankTransfer":
                        doBankTransfer(req);
                        break;
                    case "updateAircraft":
                        doUpdateAircraft(req);
                        break;
                    case "adjustGoods":
                        doAdjustGoods(req);
                        break;
                    case "resetBanList":
                        doResetBanList(req);
                        break;
                    case "confirmBulkFuel":
                        doBulkGoodsPurchase(req, Fbos.FBO_ORDER_FUEL);
                        break;
                    case "confirmBulkSupplies":
                        doBulkGoodsPurchase(req, Fbos.FBO_ORDER_SUPPLIES);
                        break;
                    case "updateXPlaneMD5":
                        doUpdateXPlaneMD5(req);
                        break;
                    case "editFboFacility":
                        doEditFboFacility(req);
                        break;
                    case "editFbo":
                        doEditFbo(req);
                        break;
                    case "buildRepair":
                        doBuildRepair(req);
                        break;
                    case "buildPassenger":
                        returnToPageOverride = doBuildPassenger(req);
                        break;
                    case "bankBalance":
                        doBankBalance(req);
                        break;
                    case "leaseAircraft":
                        doLeaseAircraft(req);
                        break;
                    case "editAircraft":
                        doEditAircraft(req);
                        break;
                    case "transferAircraft":
                        doTransferAircraft(req);
                        break;
                    case "shipAircraft":
                        doShipAircraft(req);
                        break;
                    case "editFuelPrices":
                        doEditFuelPrices(req);
                        break;
                    case "addServiceProviderAccess":
                        doAddServiceProviderAccess(req);
                        break;
                    case "deleteServiceProviderAccess":
                        doDeleteServiceProviderAccess(req);
                        break;
                    case "updateServiceProviderAccess":
                        doUpdateServiceProviderAccess(req);
                        break;
                }

				if(returnToPageOverride != null)
					resp.sendRedirect(returnToPageOverride);
				else if(returnToPage != null)
					resp.sendRedirect(returnToPage);
				else
					resp.sendRedirect("/index.jsp");
			}
			catch (NumberFormatException e)
			{
				throw new DataError("Invalid input");
			}
		} 
		catch (DataError e)
		{
			req.getSession().setAttribute("message", e.getMessage());
			req.getSession().setAttribute("back", returnToPage != null ? returnToPage : returnToPageOverride != null ? returnToPageOverride : "/index.jsp");
			req.getRequestDispatcher("error.jsp").forward(req, resp);
		}
	}

    void addAccountNote(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        if(user.getLevel() != UserBean.LEV_MODERATOR && user.getLevel() != UserBean.LEV_CSR && user.getLevel() != UserBean.LEV_ADMIN)
            throw new DataError("Permission denied.");

        String sUserId = req.getParameter("userid");

        if(sUserId == null)
            throw new DataError("Missing Parameter!");

        int accountId = Integer.parseInt(sUserId);

        Accounts.addAccountNote(accountId, user.getId(), req.getParameter("note"));
    }

    void linkAccount(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        if(user.getLevel() != UserBean.LEV_MODERATOR && user.getLevel() != UserBean.LEV_CSR && user.getLevel() != UserBean.LEV_ADMIN)
            throw new DataError("Permission denied.");

        String sUserId = req.getParameter("userid");
        String sLinkId = req.getParameter("linkid");

        if(sUserId == null || sLinkId == null)
            throw new DataError("Missing Parameter!");

        int accountId = Integer.parseInt(sUserId);
        int linkId = Integer.parseInt(sLinkId);

        Accounts.linkAccount(linkId, accountId, user.getId());
    }

    void unlinkAccount(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        if(user.getLevel() != UserBean.LEV_MODERATOR && user.getLevel() != UserBean.LEV_CSR && user.getLevel() != UserBean.LEV_ADMIN)
            throw new DataError("Permission denied.");

        String sUserId = req.getParameter("userid");

        if(sUserId == null)
            throw new DataError("Missing Parameter!");

        int accountId = Integer.parseInt(sUserId);

        Accounts.unlinkAccount(accountId, user.getId());
    }

    void doUpdateServiceProviderAccess(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        String sAccount = req.getParameter("account");
        int accountId = Integer.parseInt(sAccount);

        //check if adding to the user, or a group
        if(accountId != user.getId() && !Accounts.isGroupOwner(accountId, user.getId()))
            throw new DataError("Permission denied.");

        String sServiceId = req.getParameter("service");
        int serviceId = Integer.parseInt(sServiceId);

        String cash = req.getParameter("cash-read") != null ? "READ " : "";
        cash += req.getParameter("cash-transfer") != null ? "TRANSFER" : "";

        String bank = req.getParameter("bank-read") != null ? "READ " : "";
        bank += req.getParameter("bank-deposit") != null ? "DEPOSIT " : "";
        bank += req.getParameter("bank-withdraw") != null ? "WITHDRAW" : "";

        String aircraft = req.getParameter("aircraft-purchase") != null ? "PURCHASE " : "";
        aircraft += req.getParameter("aircraft-transfer") != null ? "TRANSFER " : "";
        aircraft += req.getParameter("aircraft-lease") != null ? "LEASE" : "";
        aircraft += req.getParameter("aircraft-edit") != null ? "EDIT" : "";
        aircraft += req.getParameter("aircraft-sale") != null ? "SALE" : "";

        HashMap<String, String> map = new HashMap<>();
        map.put("cash", cash);
        map.put("bank", bank);
        map.put("aircraft", aircraft);

        ServiceProviders.updateServiceProviderAccess(accountId, serviceId, map);
    }

    void doDeleteServiceProviderAccess(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        String sServiceId = req.getParameter("service");
        int serviceId = Integer.parseInt(sServiceId);
        String sAccount = req.getParameter("account");
        int accountId = Integer.parseInt(sAccount);

        //check if adding to the user, or a group
        if(accountId != user.getId() && !Accounts.isGroupOwner(accountId, user.getId()))
            throw new DataError("Permission denied.");

        ServiceProviders.deleteServiceProviderAccess(accountId, serviceId);
    }

    void doAddServiceProviderAccess(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        String sServiceId = req.getParameter("services");
        int serviceId = Integer.parseInt(sServiceId);
        String sAccount = req.getParameter("account");
        int accountId = Integer.parseInt(sAccount);

        //check if adding to the user, or a group
        if(accountId != user.getId() && !Accounts.isGroupOwner(accountId, user.getId()))
            throw new DataError("Permission denied.");

        ServiceProviders.addServiceProviderAccess(accountId, serviceId);
    }

    void doEditFuelPrices(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		
		String sId = req.getParameter("owner");
		int owner = Integer.parseInt(sId);
		List<FboBean> fbos = Fbos.getFboByOwner(owner, "location");

		if (!fbos.get(0).updateAllowed(user))
			throw new DataError("Permission denied.");		

		String sPrice100ll = req.getParameter("price100ll");
		String sPriceFuelJetA = req.getParameter("priceJetA");
		double price100ll = 0.0;
		double priceJetA = 0.0;
		
		if(sPrice100ll != null && !sPrice100ll.contentEquals(""))
			price100ll = Double.parseDouble(sPrice100ll);
			
		if(sPriceFuelJetA != null && !sPriceFuelJetA.contentEquals(""))
			priceJetA = Double.parseDouble(sPriceFuelJetA);

		if(priceJetA == 0.0 && price100ll == 0.0)
			return;

        for (FboBean fbo : fbos)
        {
            if (price100ll > 0)
                fbo.setFuel100LL(price100ll);

            if (priceJetA > 0)
                fbo.setFueljeta(priceJetA);

            Fbos.updateFbo(fbo, user);
        }

	}
	
	void doShipAircraft(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");

        int id = Integer.parseInt(req.getParameter("id"));
		AircraftBean aircraft = Aircraft.getAircraftById(id);

        if (aircraft == null)
            throw new DataError("Aircraft not found.");

		if (!aircraft.changeAllowed(user)) 
			throw new DataError("Permission Denied.");

		if( aircraft.getShippingState() != 0 )
			throw new DataError("You have already shipped this aircraft!!");
		
		//need to get find the list of active repair stations for departure and destination airfields
		String depart = req.getParameter("repairFrom");
		String dest = req.getParameter("repairTo");
		String shipto = req.getParameter("shipTo");
		
		int departSvc = Integer.parseInt(depart);
		int destSvc = Integer.parseInt(dest);
	
		Aircraft.processAircraftShipment(user, aircraft, shipto, departSvc, destSvc );
	}
	
	void doTransferAircraft(HttpServletRequest req) throws DataError
	{
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        String sAccountId = req.getParameter("accountid");
        String sAircraftId = req.getParameter("aircraftid");

        if (sAircraftId == null || !Helpers.isInteger(sAircraftId) || sAccountId == null || !Helpers.isInteger(sAccountId))
            throw new DataError("Missing parameters");

        AircraftBean aircraft = Aircraft.getAircraftById(Integer.parseInt(sAircraftId));
        if(aircraft == null)
            throw new DataError("Invalid aircraft!");

        UserBean account = Accounts.getAccountById(Integer.parseInt(sAccountId));
        if(account == null)
            throw new DataError("Invalid account to transfer to!");

        int owner = aircraft.getOwner();
        if(Accounts.isGroup(owner))
            owner = Accounts.accountUltimateOwner(owner);

        if(owner != user.getId())
            throw new DataError("Permission error, you are not the owner!");

		Aircraft.transferAircraft(aircraft.getId(), account.getId());

        GlobalLogger.logExploitAuditLog("Aircraft Transfer  - User: [" + user.getName() + "] Aircraft: " + sAircraftId + ", To: [" + account.getName() + "]", UserCtl.class);
    }
	
	void doEditAircraft(HttpServletRequest req) throws DataError
	{		
		UserBean user = (UserBean) req.getSession().getAttribute("user");

        String sId = req.getParameter("id");
        if(Helpers.isNullOrBlank(sId))
            throw new DataError("Invalid parameter.");

		int id = Integer.parseInt(sId);
        AircraftBean aircraft = Aircraft.getAircraftById(id);
 
        if (aircraft == null)
            throw new DataError("Aircraft not found.");

		if (!aircraft.changeAllowed(user)) 
			throw new DataError("Permission Denied.");

		String newReg = req.getParameter("newreg");
		
		if (Helpers.isNullOrBlank(newReg))
        {
            newReg = null;
        }
		else
        {
            newReg = newReg.toUpperCase();

            if(!aircraft.getRegistration().equals(newReg))
            {
                if (!newReg.matches("^[A-Z,0-9,-]*$"))
                    throw new DataError("You can only use [0-9][A-Z] and [-] in Registration Number");

                if (!Aircraft.isUniqueAircraftRegistration(newReg))
                    throw new DataError("Registration already in use!");

                if (newReg.length() > 20)
                    throw new DataError("Aircraft Registration max length is 20 characters!");
            }
        }

        String home = req.getParameter("home").toUpperCase();
		if (!Airports.isValidIcao(home))
			throw new DataError("Aircraft Home airport not found.");

		//update fields
		aircraft.setHome(home);
		
		String sBonus = req.getParameter("bonus");
		if(Helpers.isNullOrBlank(sBonus))
            aircraft.setBonus(0);
		else
            aircraft.setBonus(Integer.parseInt(sBonus));

		String sRentalPriceWet = req.getParameter("rentalPriceWet");
		if(Helpers.isNullOrBlank(sRentalPriceWet))
            aircraft.setRentalPriceWet(0);
		else
            aircraft.setRentalPriceWet(Integer.parseInt(sRentalPriceWet));

		String sRentalPriceDry = req.getParameter("rentalPriceDry");
		if(Helpers.isNullOrBlank(sRentalPriceDry))
            aircraft.setRentalPriceDry(0);
		else
            aircraft.setRentalPriceDry(Integer.parseInt(sRentalPriceDry));

		String sMaxRentalTime = req.getParameter("maxRentTime");
		if(Helpers.isNullOrBlank(sMaxRentalTime))
            aircraft.setMaxRentTime(0);
		else
            aircraft.setMaxRentTime(Integer.parseInt(sMaxRentalTime));

		String sSellPrice = req.getParameter("sellPrice");
		if(Helpers.isNullOrBlank(sSellPrice))
            aircraft.setSellPrice(0);
		else
            aircraft.setSellPrice(Integer.parseInt(sSellPrice));

        if (!Helpers.isNullOrBlank(sSellPrice))
        {
            String sSaleType = req.getParameter("saletype");
            String sSellToId = req.getParameter("selltoid");
            if("1".equals(sSaleType))
            {
                aircraft.setPrivateSale(true);
                aircraft.setSellToId(Integer.parseInt(sSellToId));
            }
            else
            {
                aircraft.setPrivateSale(false);
                aircraft.setSellToId(0);
            }

            aircraft.setSellPrice(Integer.parseInt(sSellPrice));
            GlobalLogger.logExploitAuditLog("Aircraft sellprice  - User: [" + user.getName() + "] Aircraft: " + id + ", Private: [" + Accounts.getAccountNameById(Integer.parseInt(sSellToId)) + "], Price: [" + sSellPrice + "]", UserCtl.class);
        }

        boolean advertiseFerry = Boolean.parseBoolean(req.getParameter("advertiseFerry"));
		aircraft.setAdvertiseFerry(advertiseFerry);
		
		boolean allowRepair = Boolean.parseBoolean(req.getParameter("allowRepair"));
		aircraft.setAllowRepair(allowRepair);
		
		Aircraft.updateAircraft(aircraft, newReg, user);
	}	
	
	void doLeaseAircraft(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		
		int lessee = Integer.parseInt(req.getParameter("lessee"));
		int owner = Integer.parseInt(req.getParameter("owner"));
        int id = Integer.parseInt(req.getParameter("id"));

        if(lessee == BANK_OF_FSE_ID)
        {
            GlobalLogger.logExploitAuditLog("Aircraft Lease to Bank of FSE  - User: [" + user.getName() + "] Aircraft: " + id, UserCtl.class);
            return;
        }

        AircraftBean aircraft = Aircraft.getAircraftById(id);

        if (aircraft == null)
            throw new DataError("Aircraft not found.");
		
		if (!aircraft.changeAllowed(user)) 
			throw new DataError("Permission Denied.");

		Aircraft.leaseac(aircraft.getId(), lessee, owner, aircraft.getLocation());
	}	
	
	void doBankBalance(HttpServletRequest req) throws DataError
	{
		String sUId = req.getParameter("uid");
		if(Helpers.isNullOrBlank(sUId) || !Helpers.isInteger(sUId))
			throw new DataError("Missing or Invalid User Id.");

        String sSelectedBalance = req.getParameter("selectedBalance");
        if (Helpers.isNullOrBlank(sSelectedBalance) || !Helpers.isInteger(sSelectedBalance))
            throw new DataError("Invalid parameter.");

		int uId = Integer.parseInt(sUId);
		UserBean account = Accounts.getAccountById(uId);
		UserBean user = (UserBean) req.getSession().getAttribute("user");

		if(account.isGroup() && !Accounts.isGroupOwnerStaff(account.getId(), user.getId()))
			throw new DataError("Permission Denied.");

        int index = Integer.parseInt(sSelectedBalance);

        //check that index is >= 0, if not skip
        if(index >= 0)
        {
            double money = account.getMoney();
            double bank = account.getBank();

            double balanceAmount = index * 1000;

            if( index == 999)
                balanceAmount = bank;

            if( money > balanceAmount && index != 999 )
            {
                //transfer to bank
                double amountToTransfer = money - balanceAmount;
                Banking.doBanking(account.getId(), amountToTransfer);
            }
            else
            {
                double amountToTransfer;

                //withdraw from bank
                if(index != 999)
                    amountToTransfer = balanceAmount - money;
                else
                    amountToTransfer = balanceAmount;

                if (index == 999 && balanceAmount < 0)
                {
                    throw new DataError("Sorry, the requested transaction would cause a negative cash balance.");
                }
                else
                {
                    if(bank >= amountToTransfer)
                        Banking.doBanking(account.getId(), -amountToTransfer);
                    else
                        throw new DataError("Sorry, the requested transaction would cause a negative bank balance.");
                }
            }
        }
	}
	
	void doEditFbo(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");

		int id = Integer.parseInt(sId);
		FboBean fbo = Fbos.getFbo(id);
		if (!fbo.updateAllowed(user))
			throw new DataError("Permission denied.");		

		//Check for changes
		String name = req.getParameter("name");
		String sMargin = req.getParameter("margin");
		String sEquipmentInstallMargin = req.getParameter("equipmentInstallMargin");
		String sPrice = req.getParameter("price");
		String fuel100ll = req.getParameter("fuel100ll");
		String fueljeta = req.getParameter("fueljeta");

		if (name != null)
			fbo.setName(name);
		
		if (fuel100ll != null)
			fbo.setFuel100LL(Double.parseDouble(fuel100ll));
		
		if (fueljeta != null)
			fbo.setFueljeta(Double.parseDouble(fueljeta));
		
		if (sMargin != null)
			fbo.setRepairShopMargin(Integer.parseInt(sMargin));
		
		if (sEquipmentInstallMargin != null)
			fbo.setEquipmentInstallMargin(Integer.parseInt(sEquipmentInstallMargin));
		
		if (!Helpers.isNullOrBlank(sPrice) && Helpers.isInteger(sPrice))
        {
            String sSaleType = req.getParameter("saletype");
            String sSellToId = req.getParameter("selltoid");
            if("1".equals(sSaleType))
            {
                fbo.setPrivateSale(true);
                fbo.setSellToId(Integer.parseInt(sSellToId));
            }
            else
            {
                fbo.setPrivateSale(false);
                fbo.setSellToId(0);
            }

            fbo.setPrice("".equals(sPrice) ? 0 : Integer.parseInt(sPrice));
            GlobalLogger.logExploitAuditLog("FBO sellprice  - User: [" + user.getName() + "] FBO: " + id + ", Private: [" + Integer.parseInt(sSellToId) + "], Price: [" + sPrice + "]", UserCtl.class);
        }
        else
        {
            fbo.setPrice(0);
        }

		Fbos.updateFbo(fbo, user);
		
		for (int c=0; c < Goods.commodities.length; c++)
		{
			if (Goods.commodities[c] == null)
				continue;
			
			String prefix = "g_" + c + "_";
			
			String buy = req.getParameter(prefix + "buy");
			String buyPrice = req.getParameter(prefix + "bp");
			String max = req.getParameter(prefix + "max");
			String sell = req.getParameter(prefix + "sell");
			String sellPrice = req.getParameter(prefix + "sp");
			String retain = req.getParameter(prefix + "retain");		
	
			if (buyPrice == null || max == null || sellPrice == null || retain == null)
				continue;
			
			GoodsBean good = new GoodsBean();
			good.setLocation(fbo.getLocation());
			good.setOwner(fbo.getOwner());
			good.setType(c);
			good.setBuy("true".equals(buy));
			good.setSell("true".equals(sell));
			good.setPriceBuy(Double.parseDouble(buyPrice));
			good.setPriceSell(Double.parseDouble(sellPrice));
			good.setMax(Integer.parseInt(max));
			good.setRetain(Integer.parseInt(retain));
			
			Goods.updateGoods(good, user);
		}
	}

	String doBuildPassenger(HttpServletRequest req) throws DataError
	{
		String returnToPage = null;
		
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");

		int id = Integer.parseInt(sId);
		FboBean fbo = Fbos.getFbo(id);
		if (!fbo.updateAllowed(user))
			throw new DataError("Permission denied.");

		boolean ptCreated;
		ptCreated = Fbos.buildPassengerTerminal(fbo);
			
		if (ptCreated)
		{
			int facilityId = Facilities.getDefaultFacility(fbo).getId();
			returnToPage = "editfbofacility.jsp?facilityId=" + facilityId;
		}
		
		return returnToPage;
	}
	
	void doBuildRepair(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");

		int id = Integer.parseInt(sId);
		FboBean fbo = Fbos.getFbo(id);
		if (!fbo.updateAllowed(user))
			throw new DataError("Permission denied.");

		Fbos.buildRepairShop(fbo);
	}
	
	void doEditFboFacility(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");

		int facilityId = Integer.parseInt(req.getParameter("facilityId"));		
		FboFacilityBean facility = Facilities.getFacility(facilityId);
		
		if (!facility.updateAllowed(user))
			throw new DataError("Permission denied.");

		List<FboFacilityBean> renters = null;
		FboBean fbo = Fbos.getFbo(facility.getFboId());

		if (facility.getIsDefault())
			renters = Facilities.getRenterFacilities(fbo);

		int iSessionRent=0;
		
		if (req.getSession().getAttribute(facility.getLocation() + "Rent") == null)
		{
			req.getSession().setAttribute(facility.getLocation() + "Rent", facility.getRent());
			iSessionRent=facility.getRent();
		}

		String pd_reservedSpace = req.getParameter("pd_reservedSpace");
		String pd_rent = req.getParameter("pd_rent");
		String pd_renew = req.getParameter("pd_renew");
		String pd_allowRenew = req.getParameter("pd_allowRenew");
		String pd_name = req.getParameter("pd_name");
		String pd_allowWater = req.getParameter("pd_allowWater");
		String pd_minDistance = req.getParameter("pd_minDistance");
		String pd_maxDistance = req.getParameter("pd_maxDistance");
		String pd_matchMinSize = req.getParameter("pd_matchMinSize");
		String pd_matchMaxSize = req.getParameter("pd_matchMaxSize");
		String pd_publicByDefault = req.getParameter("pd_publicByDefault");
		String pd_commodity = req.getParameter("pd_commodity");
		String pd_icaoset = req.getParameter("pd_icaoset");
		String pd_maxUnitsPerTrip = req.getParameter("pd_maxUnitsPerTrip");
		String pd_daysActive = req.getParameter("pd_daysActive");
			
		if (facility.getIsDefault())
		{
			facility.setReservedSpace(Integer.parseInt(pd_reservedSpace));
			facility.setRent("".equals(pd_rent) ? 0 : Integer.parseInt(pd_rent));
			facility.setAllowRenew("true".equals(pd_allowRenew));
		} 
		else 
		{
			facility.setRenew("true".equals(pd_renew));
		}		

		facility.setName(pd_name);
		facility.setAllowWater("true".equals(pd_allowWater));
		facility.setMinMaxDistance("".equals(pd_minDistance) ? 0 : Integer.parseInt(pd_minDistance), "".equals(pd_maxDistance) ? 0 : Integer.parseInt(pd_maxDistance));
		facility.setMatchMinSize(Integer.parseInt(pd_matchMinSize));
		facility.setMatchMaxSize(Integer.parseInt(pd_matchMaxSize));
		facility.setPublicByDefault("true".equals(pd_publicByDefault));
		facility.setDaysActive(Integer.parseInt(pd_daysActive));
		
		pd_commodity = pd_commodity.trim();						// Deal with newlines and commas...
		pd_commodity = pd_commodity.replaceAll("\r", "");		// Lose the carraige returns.
		pd_commodity = pd_commodity.replaceAll(",\n", ", ");	// Replace comma newline with comma space.
		pd_commodity = pd_commodity.replaceAll("\n", ", ");		// Replace newline with comma space (catches missed commas).
		facility.setCommodity(pd_commodity);
		facility.setIcaoSet(pd_icaoset);
		facility.setMinMaxUnitsPerTrip(0, "".equals(pd_maxUnitsPerTrip) ? 0 : Integer.parseInt(pd_maxUnitsPerTrip));
		
		int uiRent;
		boolean isDirtyRent = true;
		
		if (pd_rent != null) 
		{
			uiRent = "".equals(pd_rent) ? 0 : Integer.parseInt(pd_rent);
			if (iSessionRent == uiRent )
				isDirtyRent=false;
		}		
		
		List<Integer> rentersID = new ArrayList<>();
		
		if (renters != null)
		{			
			//adding notification to existing renter if something changes in facility settings			
            for (FboFacilityBean renter : renters)
            {
                String prefix = "pr_" + renter.getId() + "_";
                if (req.getParameter(prefix + "facilityId") != null)
                {
                    String prx_allowRenew = req.getParameter(prefix + "allowRenew");
                    renter.setAllowRenew("true".equals(prx_allowRenew));

                    if (isDirtyRent)
                        rentersID.add(renter.getOccupant());
                }
            }

			//now notify the owners that the price of the rent has changed
			if (isDirtyRent && pd_rent != null) 
			{
				if (renters.size() > 0)
				{	
					try
					{
						List<String> renterEmails = Facilities.getEmailAddressForRenterIDs(rentersID);
						Emailer emailer = Emailer.getInstance();
						
						String messageText = "Rent price change ALERT! \n\n The price of rent at airport ICAO: " + facility.getLocation() +
						" has changed. \n\n Original Rent Price: $" + req.getSession().getAttribute(facility.getLocation() + "Rent") + "\n New Rent Price: $" + pd_rent;
						
						emailer.sendEmail("no-reply@fseconomy.com", "FSEconomy Rental System",
								"FSEconomy Rent Price Change Alert", messageText, renterEmails, Emailer.ADDRESS_TO);
					}
					catch (DataError e) 
					{
						e.printStackTrace();						
					}
				 }
			}
		}

        Facilities.updateFacility(facility, renters, user);

		if (req.getParameter("doRent") != null)
		{
			int rentBlocks = Integer.parseInt(req.getParameter("rentBlocks"));
            Fbos.rentFboFacility(user, facility.getOccupant(), facility.getId(), rentBlocks);
		} 
	}
		
	void doUpdateXPlaneMD5(HttpServletRequest req) throws DataError
	{
		String MD5 = req.getParameter("MD5");
		String passcode = req.getParameter("passcode");
			try
			{				
				String qry = "SELECT svalue FROM sysvariables where variablename='XPlaneScriptMD5Passcode'";
				String code = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer());
				if(code.equals(passcode))
				{
					qry = "SELECT svalue FROM sysvariables where variablename='XPlaneScriptMD5'";
					String currMD5 = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer());
				
					if(!MD5.equals(currMD5))
					{
						qry = "UPDATE sysvariables SET svalue = ? WHERE variablename='XPlaneScriptMD5'";
                        DALHelper.getInstance().ExecuteUpdate(qry, MD5);
					}
					else
						throw new DataError("No change detected!");
				}
				else
					throw new DataError("Bzzzzt! Hacker alert, invalid passcode entered...logging");
			}
			catch(SQLException e)
			{
				e.printStackTrace();
				throw new DataError("There was a DB error, please try again, or contact Admin.");
			}
	}
	
	void cancelFlight(HttpServletRequest req)
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
        if(user == null)
            throw new InvalidParameterException("No user session");

		Flights.cancelFlight(user);
	}
	
	void doLogout(HttpServletRequest req)
	{
		req.getSession().invalidate();
	}
	
	boolean doLogin(HttpServletRequest req)
	{
		String userName = req.getParameter("user");
		String password = req.getParameter("password");
		String sOffset = req.getParameter("offset");
		if (userName == null || password == null)
		{
			return false;
		}

		UserBean user = Accounts.userExists(userName, password);
		if (user == null)
		{
			return false;
		}

		user.setTimeZone(TimeZone.getTimeZone("GMT" + sOffset));
		user.setLoggedIn(true);
        Groups.reloadMemberships(user);
		HttpSession s = req.getSession();
		s.setAttribute("user", user);

		return true;
	}
	
	void processAssignment(HttpServletRequest req) throws DataError
	{
		// id = assignment id - single record
		// select = checkbox values - multiple records		
        String sId = req.getParameter("id");
		String saId[] = req.getParameterValues("select");
		String comment = req.getParameter("comment");

        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        if (sId == null && saId == null)
			return;

        int id;
        int x = 1;

        Groups.reloadMemberships(user);

        if (saId != null)
        	x = saId.length;
        
		for (int i=0; i< x; i++)
		{
			if (saId != null)	
				id = Integer.parseInt(saId[i]);
			else
				id = Integer.parseInt(sId);
			
			String type = req.getParameter("type");
			
			if (type == null)
				return;
			
			if (type.equals("move"))
			{
				String group = req.getParameter("groupid");
                if(Helpers.isNullOrBlank(group))
                    throw new DataError("Missing groupid parameter");

				int groupId = Integer.parseInt(group);
				Assignments.moveAssignment(id, groupId, user);
			} 
			else if (type.equals("comment"))
			{
                Assignments.commentAssignment(id, comment, user);
			} 
			else if (type.equals("delete"))
			{
                Assignments.deleteAssignment(id, user);
			} 
			else if (type.equals("unlock"))
			{
                AssignmentBean ab = Assignments.getAssignmentById(id);
                if(user.getId() != ab.getUserlock())
                {
                    String group = req.getParameter("groupid");
                    if (Helpers.isNullOrBlank(group))
                        throw new DataError("Missing groupid parameter");

                    int groupId = Integer.parseInt(group);
                    if (groupId != 0 && !Accounts.isGroupOwnerStaff(groupId, user.getId()))
                        throw new DataError("Assignment is owned by another group.");
                }
                Assignments.unlockAssignment(id, user);
			} 
			else if (type.equals("hold") || type.equals("load"))
			{
                Assignments.holdAssignment(id, type.equals("hold"), user);
			} 
			else if(type.equals("add"))
			{
                Assignments.addAssignment(id, user);
			}
			else if(!type.equals("add"))
			{
                Assignments.removeAssignment(id, user);
			}
		}
	}

    void newAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        //id = assignment id - single record
        String sGroupId = req.getParameter("groupid");
        String fromIcao = req.getParameter("fromicao");
        String toIcao = req.getParameter("toicao");
        String sPilotFee = req.getParameter("pilotfee");
        String comment = req.getParameter("comment");

        if (fromIcao == null || toIcao == null || sGroupId == null || sPilotFee == null)
            throw new DataError("Invalid parameters.");

        int groupId = Integer.parseInt(sGroupId);
        double pilotFee = Double.parseDouble(sPilotFee);

        if (Groups.getRole(groupId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        Assignments.newAssignment(groupId, fromIcao.toUpperCase(), toIcao.toUpperCase(), pilotFee, comment);
    }

    void removeAssignmentFromGroup(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        //id = assignment id - single record
        String sGroupId = req.getParameter("groupid");
        String sAssignments[] = req.getParameterValues("select");

        if (sAssignments == null || sGroupId == null)
            throw new DataError("Invalid parameters.");

        int groupId = Integer.parseInt(sGroupId);
        int[] array = Arrays.stream(sAssignments).mapToInt(Integer::parseInt).toArray();

        if (Groups.getRole(groupId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        Assignments.removeAssignmentFromGroup(array);
    }

    void updateAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        //id = assignment id - single record
        String sGroupId = req.getParameter("groupid");
        String sOwnerId = req.getParameter("ownerid");
        String sAssignmentId = req.getParameter("assignmentid");
        String sPilotFee = req.getParameter("pilotfee");
        String comment = req.getParameter("comment");

        if (Helpers.isNullOrBlank(sAssignmentId) || (Helpers.isNullOrBlank(sGroupId) && Helpers.isNullOrBlank(sOwnerId)))
            throw new DataError("Invalid parameters.");

        int ownerId = 0;
        int groupId = Integer.parseInt(sGroupId);
        int assignmentId = Integer.parseInt(sAssignmentId);

        double pilotFee = -1;
        if(!Helpers.isNullOrBlank(sPilotFee))
            pilotFee = Double.parseDouble(sPilotFee);

        if ( groupId > 0 && Groups.getRole(groupId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        if(sOwnerId != null)
            ownerId = Integer.parseInt(sOwnerId);

        if ( ownerId > 0 && Groups.getRole(ownerId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        Assignments.updateAssignment(assignmentId, groupId, pilotFee, comment);
    }

    void updateGoodsAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        String sGroupId = req.getParameter("groupid");
        String sOwnerId = req.getParameter("ownerid");
        String sAssignmentId = req.getParameter("assignmentid");
        String sAmount = req.getParameter("amount");
        String sPilotFee = req.getParameter("pilotfee");
        String comment = req.getParameter("comment");

        if (Helpers.isNullOrBlank(sAssignmentId) || (Helpers.isNullOrBlank(sGroupId) && Helpers.isNullOrBlank(sOwnerId)) || Helpers.isNullOrBlank(sPilotFee) || Helpers.isNullOrBlank(sAmount))
            throw new DataError("Invalid parameters.");

        int ownerId = 0;
        int groupId = Integer.parseInt(sGroupId);
        int assignmentId = Integer.parseInt(sAssignmentId);
        int amount = Integer.parseInt(sAmount);
        int pilotFee = (int)Double.parseDouble(sPilotFee);

        if ( groupId > 0 && Groups.getRole(groupId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        if(!Helpers.isNullOrBlank(sOwnerId))
            ownerId = Integer.parseInt(sOwnerId);


        if ( groupId == 0 && ownerId != 0 && ownerId != Assignments.getAssignmentById(assignmentId).getOwner())
            throw new DataError("You do not have permission to do that.");

        Assignments.updateGoodsAssignment(assignmentId, amount, pilotFee, comment);
    }

    void newGoodsAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        String sOwner = req.getParameter("ownerid");
        String sGroup = req.getParameter("groupid");
        String fromIcao = req.getParameter("fromicao");
        String toIcao = req.getParameter("toicao");
        String sCommodity = req.getParameter("commodityid");
        String sAmount = req.getParameter("amount");
        String sPay = req.getParameter("pay");
        String sRepeat = req.getParameter("numtocreate");

        AssignmentBean assignment = new AssignmentBean();
        assignment.setId(-1);
        assignment.setCreation(new java.sql.Timestamp(System.currentTimeMillis()));
        assignment.setCreatedByUser(true);
        assignment.setUnits(AssignmentBean.UNIT_KG);

        if(Helpers.isNullOrBlank(sOwner))
            throw new DataError("Missing owner");

//        if(sOwner.equals(sGroup))
//        {
//            assignment.setGroup(true);
//            assignment.setGroupId(Integer.parseInt(sGroup));
//        }

        if (fromIcao.equalsIgnoreCase(toIcao))
            throw new DataError("Goods already at destination");

        if (!Airports.isValidIcao(fromIcao.toUpperCase()))
            throw new DataError("From airport not found.");

        if (!Airports.isValidIcao(toIcao.toUpperCase()))
            throw new DataError("To airport not found.");

        if(Helpers.isNullOrBlank(sCommodity))
            throw new DataError("Missing commodity.");

        if (Helpers.isNullOrBlank(sAmount) && !sAmount.matches("[0-9]+"))
            throw new DataError("Amount Invalid");

        if (Helpers.isNullOrBlank(sPay) && !sPay.matches("[0-9]+"))
            throw new DataError("Pay Invalid");

        if (Helpers.isNullOrBlank(sRepeat) && !sRepeat.matches("[0-9]+"))
            throw new DataError("Number of Assignments Invalid");

        int amount = Integer.parseInt(sAmount);
        if (amount <= 0)
            throw new DataError("Amount must be greater than 0");

        int numToCreate = Integer.parseInt(sRepeat);
        if (numToCreate <= 0)
            throw new DataError("Number of Assignments must be greater than 0");

        int commodity = Integer.parseInt(sCommodity);
        int pay = Integer.parseInt(sPay);
        int owner = Integer.parseInt(sOwner);
        boolean isGroup = Accounts.isGroup(owner);
        assignment.setAmount(amount);
        assignment.setCommodityId(commodity);
        assignment.setOwner(owner);
        assignment.setLocation(fromIcao);
        assignment.setFrom(fromIcao);
        assignment.setTo(toIcao);
        assignment.setCommodity(Goods.commodities[assignment.getCommodityId()].getName());

        if (!Goods.checkGoodsAvailable(fromIcao, owner, commodity, amount*numToCreate))
            throw new DataError("Not enough Goods available!");

        if (isGroup && Groups.getRole(owner, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        if (!isGroup && assignment.getOwner() != user.getId())
            throw new DataError("You do not have permission to do that.");

        assignment.updateData();
        double payscale = pay*100.0/(assignment.getDistance()*amount);
        assignment.setPay(payscale);

        //reset group id so that the assignment is unlocked
        assignment.setGroupId(0);

        for (int i = 0; i < numToCreate; i++)
            Assignments.updateAssignment(assignment, user);
    }

    void updateTransferGoods(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        //id = assignment id - single record
        String sGroupId = req.getParameter("groupid");
        String sOwnerId = req.getParameter("ownerid");
        String sAssignmentId = req.getParameter("assignmentid");
        String sGoodsAmount = req.getParameter("amount");
        String sPilotFee = req.getParameter("pilotfee");
        String comment = req.getParameter("comment");

        if (sAssignmentId == null
            || (sGroupId == null && sOwnerId == null)
            || sPilotFee == null
            || sGoodsAmount == null)
            throw new DataError("Invalid parameters.");

        int ownerId = 0;
        int groupId = Integer.parseInt(sGroupId);
        int assignmentId = Integer.parseInt(sAssignmentId);
        int goodsAmount = Integer.parseInt(sGoodsAmount);
        int pilotFee = (int)Double.parseDouble(sPilotFee);

        if ( groupId > 0 && Groups.getRole(groupId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        if(!Helpers.isNullOrBlank(sOwnerId))
            ownerId = Integer.parseInt(sOwnerId);

        if ( ownerId > 0 && Groups.getRole(ownerId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        Assignments.updateGoodsAssignment(assignmentId, goodsAmount, pilotFee, comment);
    }

    void deleteGoodsAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        String sAssignments[] = req.getParameterValues("select");
        int[] array = Arrays.stream(sAssignments).mapToInt(Integer::parseInt).toArray();

        if(array.length == 0)
            return;

        for(int assignmentId: array)
            Assignments.deleteGoodsAssignment(assignmentId, user);
    }

    void unlockGoodsAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        String sAssignments[] = req.getParameterValues("select");
        int[] array = Arrays.stream(sAssignments).mapToInt(Integer::parseInt).toArray();

        if (array.length == 0)
            return;

        for(int assignmentId: array)
            Assignments.unlockGoodsAssignment(assignmentId, user);
    }

    void deleteGroupAssignment(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        //id = assignment id - single record
        String sGroupId = req.getParameter("groupid");
        String sAssignmentId = req.getParameter("assignmentid");

        if (sAssignmentId == null || sGroupId == null)
            throw new DataError("Invalid parameters.");

        int groupId = Integer.parseInt(sGroupId);
        int assignmentId = Integer.parseInt(sAssignmentId);

        if (Groups.getRole(groupId, user.getId()) < UserBean.GROUP_STAFF)
            throw new DataError("You do not have permission to do that.");

        Assignments.deleteGroupAssignment(assignmentId, user);
    }

    void addAircraft(HttpServletRequest req) throws DataError
	{
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (user == null || !user.isLoggedIn())
            return;

        if (req.getParameter("id") == null)
            return;

        int id = Integer.parseInt(req.getParameter("id"));
		
		String type = req.getParameter("type");
		String rentalType = req.getParameter("rentalType");
		if (type == null)
			return;
		
		if (type.equals("add"))
			Aircraft.rentAircraft(id, user, "dry".equals(rentalType));
		else if (type.equals("remove"))
            Aircraft.releaseAircraft(id, user.getId());
	}

	void returnLeaseAircraft(HttpServletRequest req) throws DataError
	{
        if (req.getParameter("id") == null)
            return;

		int id = Integer.parseInt(req.getParameter("id"));

		UserBean user = (UserBean) req.getSession().getAttribute("user");
		if (user == null || !user.isLoggedIn())
			return;

        AircraftBean aircraft = Aircraft.getAircraftById(id);

        if (aircraft == null)
            throw new DataError("Aircraft not found.");

        Aircraft.leasereturnac(aircraft.getId());
	}
	
	void doRefuel(HttpServletRequest req) throws DataError
	{
        if (req.getParameter("id") == null)
            return;

		String sProvider = req.getParameter("provider");
		String sType = req.getParameter("type");
		int type = Integer.parseInt(sType);

        int id = Integer.parseInt(req.getParameter("id"));

		UserBean user = (UserBean) req.getSession().getAttribute("user");
		if (user == null || !user.isLoggedIn())
			return;

        String sFuel = req.getParameter("fuel");
		int fuel = Integer.parseInt(sFuel);
        Aircraft.refuelAircraft(id, user.getId(), fuel, Integer.parseInt(sProvider), type);
	}
	
	void doBuyAircraft(HttpServletRequest req) throws DataError
	{
        if (req.getParameter("id") == null)
            return;

        int id = Integer.parseInt(req.getParameter("id"));
		String sAccount = req.getParameter("account");
		UserBean user = (UserBean) req.getSession().getAttribute("user");

        Aircraft.buyAircraft(id, Integer.parseInt(sAccount), user);
	}
	
	void doAircraftSell(HttpServletRequest req) throws DataError
	{
        if (req.getParameter("id") == null)
            return;

        int id = Integer.parseInt(req.getParameter("id"));
		UserBean user = (UserBean) req.getSession().getAttribute("user");

        Aircraft.sellAircraft(id, user);
	}

    void doAircraftPayFeeDebt(HttpServletRequest req) throws DataError
    {
        if (!Helpers.isInteger(req.getParameter("id")))
            throw new DataError("Missing parameter");
        int id = Integer.parseInt(req.getParameter("id"));
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        Aircraft.payFeeDebt(id, user);
    }

    void newUser(HttpServletRequest req) throws DataError
	{
        UserBean user = (UserBean) req.getSession().getAttribute("user");
		String newusername = req.getParameter("user");
		String email = req.getParameter("email");
        String linkedid = req.getParameter("linkedid");

		if (user == null || email == null)
            throw new DataError("Missing parameter, name and email are required.");
		
		//Added to remove extra spaces at end of copy/pasted text (email addresses mainly) 
        newusername = newusername.trim();
		email = email.trim();

		if (!Accounts.accountNameIsUnique(newusername))
			throw new DataError("There is already an account with that name.");

		if (!Accounts.accountEmailIsUnique(email))
			throw new DataError("There is already an account with that email.");

        int linkedId = 0;
        if(linkedid != null && !linkedid.equals(""))
            linkedId = Integer.parseInt(linkedid);

        Accounts.createAccount(newusername, email, linkedId, user.getId());

		req.getSession().setAttribute("message", "An account has been created.<br /><br /><strong>User:</strong> " + newusername + "<br /><strong>Email:</strong> " + email);
	}

    void updateAcct(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        String email = req.getParameter("email");
        String showPaymentsToSelf = req.getParameter("showPaymentsToSelf");
        String selectedTimeZone = req.getParameter("selectedTimeZone");
        String banList = req.getParameter("banList");

        if (user == null || email == null)
        {
            req.getSession().setAttribute("message", "Invalid user, or missing email");
            return;
        }

        user.setEmail(email);
        user.setDateFormat(selectedTimeZone.contains("1") ? 1 : 0);
        user.setShowPaymentsToSelf(showPaymentsToSelf.contains("1"));
        user.setBanList(banList);

        if(!Accounts.updateUser(user))
            req.getSession().setAttribute("message", "User not found.");
        else
            req.getSession().setAttribute("message", "Account (" + user.getName() + ") updated successfully");
    }

    void editUser(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
            throw new DataError("You do not have permission.");

        String suser = req.getParameter("user");
        String newuser = req.getParameter("newuser");
        String email = req.getParameter("email");
        String sExposure = req.getParameter("exposure");
        String sLevel = req.getParameter("level");
        String password = req.getParameter("password");
        String linkedid = req.getParameter("linkedid");

        int exposure = Integer.parseInt(sExposure);
        if (user == null || email == null)
            return;

        if (!suser.equals(newuser))
        {
            if (!Accounts.accountNameIsUnique(newuser))
                throw new DataError("There is already an account with that name.");
        }

        int linkedId = 0;
        if(linkedid != null && !linkedid.equals(""))
            linkedId = Integer.parseInt(linkedid);

        Accounts.updateAccount(suser, newuser, email, exposure, sLevel, password, linkedId, user.getId());
        req.getSession().setAttribute("message", "Account (" + suser + ") updated successfully");
    }

	//This is used for both new accounts and resetting passwords!
	void newPassword(HttpServletRequest req) throws DataError
	{
		String user = req.getParameter("user");
		String email = req.getParameter("email");
		
		// Make sure we have our required parameters
		if (user != null && email != null)
		{		
			//See if the name and email exists
			boolean flgExists = Accounts.userEmailExists(user, email);
			
			// if not throw an error
			if (!flgExists)
				throw new DataError("There is no account with that name and email address.");
			
			//Reset the password, and email it the users email
            Accounts.resetPassword(user, email);
			
			//Set the page message
			req.getSession().setAttribute("message", "A new password has been sent to your email address.");
		}
	}
	
	void changePassword(HttpServletRequest req) throws DataError
	{
        UserBean user = (UserBean) req.getSession().getAttribute("user");

		String password = req.getParameter("password");
		String newPassword = req.getParameter("newPassword");
		String newPassword2 = req.getParameter("newPassword2");

		if (password == null || newPassword == null || newPassword2 == null || user == null)
			return;
		
		if (!newPassword.equals(newPassword2))
			throw new DataError("New passwords don't match");

        Accounts.changePassword(user, password, newPassword);
		req.getSession().setAttribute("message", "Your password was changed successfully. Please update the settings of the FSEconomy program to reflect your new password.");
	}
	
	void lockAccount(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");

        if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
            throw new DataError("You do not have permission.");

        String sAccountId = req.getParameter("userid");

        if (sAccountId == null)
            throw new DataError("Missing parameter.");

        int accountId = Integer.parseInt(sAccountId);
        Accounts.lockAccount(accountId, user.getId());
	}
	
	void unlockAccount(HttpServletRequest req) throws DataError
	{
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
            throw new DataError("You do not have permission.");

        String sAccountId = req.getParameter("userid");

        if (sAccountId == null)
            throw new DataError("Missing parameter.");

        int accountId = Integer.parseInt(sAccountId);
        Accounts.unlockAccount(accountId, user.getId());
	}

	/**
	 * 
	 * @param req - account to reset aircraft ban list
	 * @throws DataError
	 */
	void doResetBanList(HttpServletRequest req) throws DataError
	{
		String account = req.getParameter("accountname");
		//this is freaking weird.  Why would they use an array when they will always do an exact match query and only ever have 1 max result?
		UserBean user = Accounts.getAccountGroupOrUserByName(account);
		if (user == null)
			throw new DataError("Unknown user.");
		
		user.setBanList("");	//wipe out the list
        Accounts.updateUserOrGroup(user);
		
		req.setAttribute("message", "The " + account + " aircraft rental ban list has been reset to empty string.");
	}
	
	void doPayGroup(HttpServletRequest req) throws DataError
	{
		String sAmount = req.getParameter("amount");
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");
		String comment = req.getParameter("comment");		
		int id;
		float amount;
		if (sId == null || user.getId() <= 0 || sAmount == null)
			return;
			
		UserBean account;
		id = Integer.parseInt(sId);
		account = Accounts.getGroupById(id);
		if (account == null)
			throw new DataError("Group not found");
		
		amount = Float.parseFloat(sAmount);
		if (amount < 0)
			throw new DataError("Cannot pay negative amount.");
		
		if (user.getMoney() < amount)
			throw new DataError("Not enough money.");
		
		Banking.doPayGroup(user.getId(), account.getId(), amount, comment);
	}
	
	void doBankTransfer(HttpServletRequest req) throws DataError
	{
		String sAmount = req.getParameter("amount");
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");
		String dId = req.getParameter("account");
		int srcId, desId;
		float amount;
		String comment = req.getParameter("comment");
		
		if (comment == null)
			comment = "";
		
		if (sId == null || user.getId() <= 0 || sAmount == null || dId == null)
			return; //No id, or amount given ditch transaction
			
		srcId = Integer.parseInt(sId);
		desId = Integer.parseInt(dId);
		if (srcId != user.getId())
		{
			if (user.groupMemberLevel(srcId)< UserBean.GROUP_STAFF)
				throw new DataError("Must be staff member or admin!");
		}
		
		UserBean srcAccount = Accounts.getAccountById(srcId);
		UserBean dstAccount = Accounts.getAccountById(desId);
		
		if (dstAccount == null)
			throw new DataError("Destination account not found.");
		
		if (srcAccount == null)
			throw new DataError("Source account not found.");
		
		amount = Float.parseFloat(sAmount);
		if (amount < 0)
			throw new DataError("Cannot pay negative amount.");
		
		if (srcAccount.getMoney() < amount)
			throw new DataError("Not enough money.");
		
		Banking.doPayGroup(srcAccount.getId(), dstAccount.getId(), amount, comment);
	}
	
	void doBank(HttpServletRequest req) throws DataError
	{
		String deposit = req.getParameter("deposit");
		String withdraw = req.getParameter("withdraw");
		String sId = req.getParameter("id");
		double iDeposit = 0, iWithdraw = 0;
		int id;
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		UserBean account;
		if (sId != null)
		{
			id = Integer.parseInt(sId);
		} 
		else 
		{
			id = user.getId();
		}
		
		if (id != user.getId())
		{
			if (user.groupMemberLevel(id) < UserBean.GROUP_STAFF)
				throw new DataError("Permission denied");
			
			account = Accounts.getGroupById(id);
			if (account == null)
				throw new DataError("Group not found");
		}
		else
		{
			account = user;
		}
		
		if (deposit != null && !deposit.equals(""))
			iDeposit = Double.parseDouble(deposit);
		
		if (withdraw != null && !withdraw.equals(""))
			iWithdraw = Double.parseDouble(withdraw);
		
		double total = iDeposit - iWithdraw;
		if ((account.getBank() + total < -account.getLoanLimit()) && total < 0)
			throw new DataError("You can have a maximum loan of " + Formatters.currency.format(account.getLoanLimit()) + ".");
			
		if (account.getMoney() - total < 0)
			throw new DataError("You don't have enough money.");
		
		Banking.doBanking(id, total);
	}

    void doBankDeposit(HttpServletRequest req) throws DataError
    {
        UserBean account;

        String sAccountId = req.getParameter("accountid");
        String sAmount = req.getParameter("amount");

        if(sAccountId == null || sAmount == null || sAccountId.equals("") || sAmount.equals(""))
            throw new DataError("Invalid parameters");

        UserBean user = (UserBean) req.getSession().getAttribute("user");

        int accountId = Integer.parseInt(sAccountId);

        if (accountId != user.getId())
        {
            account = Accounts.getAccountById(accountId);
            if (account == null)
                throw new DataError("Account not found");

            if (account.isGroup() && user.groupMemberLevel(accountId) < UserBean.GROUP_STAFF)
                throw new DataError("Permission denied");
        }
        else
        {
            account = user;
        }

        double deposit = Double.parseDouble(sAmount);

        if ((account.getMoney() - deposit) < 0)
            throw new DataError("You don't have enough money.");

        Banking.doDeposit(account.getId(), deposit);
    }

    void doBankWithdrawal(HttpServletRequest req) throws DataError
    {
        UserBean account;

        String sAccountId = req.getParameter("accountid");
        String sAmount = req.getParameter("amount");

        if(sAccountId == null || sAmount == null || sAccountId.equals("") || sAmount.equals(""))
            throw new DataError("Invalid parameters");

        UserBean user = (UserBean) req.getSession().getAttribute("user");

        int accountId = Integer.parseInt(sAccountId);

        if (accountId != user.getId())
        {
            account = Accounts.getAccountById(accountId);
            if (account == null)
                throw new DataError("Account not found");

            if (account.isGroup() && user.groupMemberLevel(accountId) < UserBean.GROUP_STAFF)
                throw new DataError("Permission denied");
        }
        else
        {
            account = user;
        }

        double withdraw = Double.parseDouble(sAmount);

        if (account.isGroup() && (account.getBank() - withdraw) < 0)
            throw new DataError("The Group does not have enough money.");
        else if (((account.getBank() - withdraw) < -account.getLoanLimit()))
            throw new DataError("You can have a maximum loan of " + Formatters.currency.format(account.getLoanLimit()) + ".");

        Banking.doWithdrawal(account.getId(), withdraw);
    }

    void doMappings(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");

        if (user == null || user.getLevel() < UserBean.LEV_MODERATOR)
			throw new DataError("Permission denied");

        String mapevent = req.getParameter("mapevent");
        String smapId = req.getParameter("mapid");

        int mapId = 0;
        if(smapId != null)
            mapId = Integer.parseInt(smapId);

        switch (mapevent)
        {
            case "delete":
                Aircraft.deleteMapping(mapId);
                break;
            case "unmap":
                Aircraft.setMapping(mapId, 0);
                break;
            case "map":
                String smodelId = req.getParameter("modelid");
                int modelId = Integer.parseInt(smodelId);
                Aircraft.setMapping(mapId, modelId);
                break;
        }
	}
	
	void doJoinGroup(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String id = req.getParameter("id");
		if (id == null || user == null)
			return;
		
		int groupId = Integer.parseInt(id);

        Groups.joinGroupRequest(user, groupId);
	}
	
	void doLeaveGroup(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String id = req.getParameter("id");
		if (id == null || user == null)
			return;
		
		int groupId = Integer.parseInt(id);
		if (user.groupMemberLevel(groupId) == UserBean.GROUP_OWNER)
			throw new DataError("You cannot leave your own group.");
		
		Groups.leaveGroup(user, groupId);
	}

    void doGroupRejectUser(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        String sId = req.getParameter("id");
        String sGroupId = req.getParameter("groupid");
        String msg = req.getParameter("msg");

        if (user == null || !Helpers.isInteger(sId) || !Helpers.isInteger(sGroupId) || Helpers.isNullOrBlank(msg))
            throw new DataError("Missing parameters!");

        int id = Integer.parseInt(sId);
        int groupId = Integer.parseInt(sGroupId);

        if (user.groupMemberLevel(groupId) != UserBean.GROUP_STAFF && user.groupMemberLevel(groupId) != UserBean.GROUP_OWNER)
            throw new DataError("You must be staff level or above to reject members");

        UserBean member = Accounts.getAccountById(id);

        Groups.GroupRejectUser(user, groupId, member, msg);
    }

    void doKickGroup(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String id = req.getParameter("id");
		String groupId = req.getParameter("groupid");
		
		if (id == null || user == null || groupId == null)
			return;
		
		int intId = Integer.parseInt(id);
		int intgroupId = Integer.parseInt(groupId);
		
		if (user.groupMemberLevel(intgroupId) != UserBean.GROUP_STAFF && user.groupMemberLevel(intgroupId) != UserBean.GROUP_OWNER)
			throw new DataError("You must be staff level or above to delete members");
		
		UserBean member = Accounts.getAccountById(intId);

        Groups.leaveGroup(member, intgroupId);
	}
	
	void doDeleteGroup(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String id = req.getParameter("id");

		if (id == null || user == null)
            throw new DataError("Invalid parameters.");
		
		int groupId = Integer.parseInt(id);
		if (user.groupMemberLevel(groupId) != UserBean.GROUP_OWNER)
			throw new DataError("Permission denied.");

        Groups.deleteGroup(user, groupId);
	}

    void doTransferGroup(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");

        String sTransferTo = req.getParameter("transferto");
        String sGroupId = req.getParameter("groupid");

        if (sTransferTo == null || sGroupId == null || user == null)
            throw new DataError("Invalid parameters.");

        int transferTo = Integer.parseInt(sTransferTo);
        int groupId = Integer.parseInt(sGroupId);
        if (user.groupMemberLevel(groupId) != UserBean.GROUP_OWNER)
            throw new DataError("Permission denied.");

        Groups.transferGroup(user.getId(), transferTo, groupId);
    }

    void doInvitation(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String id = req.getParameter("id");
		String action = req.getParameter("action");

		if (id == null || user == null || action == null)
			return;
			
		boolean accept = "accept".equals(action);
		int groupId = Integer.parseInt(id	);

        Groups.doInvitation(user, groupId, accept);
	}
	
	void doMemberLevel(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String id = req.getParameter("id");
		String level = req.getParameter("level");
		String group = req.getParameter("groupid");
		String invitation = req.getParameter("membername");
		String money = req.getParameter("money");
		String message = req.getParameter("email");
		String comment = req.getParameter("comment");
		
		if (comment == null)
			comment="";
		
		if (group == null)
			return;
			
		int groupId = Integer.parseInt(group);
		
		if (invitation != null && !invitation.equals(""))
		{
			if (user.groupMemberLevel(groupId) != UserBean.GROUP_STAFF && user.groupMemberLevel(groupId) != UserBean.GROUP_OWNER)
				throw new DataError("Permission denied.");

			UserBean account = Accounts.getAccountByName(invitation);
			if (account == null)
				throw new DataError("Unknown user.");

            Groups.joinGroup(account, groupId, "invited");

			return;
		}
		
		if (message != null && !message.trim().equals(""))
		{
			String[] toWho = req.getParameterValues("selected");
			
			if (toWho == null || toWho.length < 1)
				throw new DataError("No members selected!");

            Groups.mailMembers(user, groupId, toWho, message, false);
			return;
		}
		
		if (money != null && !money.equals(""))
		{
			double amount = Double.parseDouble(money);
			if (amount < 0)
				throw new DataError("Cannot pay negative amount.");
			
			String[] toWho = req.getParameterValues("selected");
			if (toWho == null || toWho.length == 0)
				throw new DataError("No one selected to pay!");

            Groups.payMembers(user, groupId, amount, toWho, comment);
			return;
		}
		
		if (id == null || level == null)
			return;
		
		int userId = Integer.parseInt(id);
		if (user.groupMemberLevel(groupId) != UserBean.GROUP_OWNER)
			throw new DataError("Permission denied.");
		
		if (!level.equals("staff") && !level.equals("member"))
			throw new DataError("Invalid level.");

        UserBean member = Accounts.getAccountById(userId);
        String groupName = Accounts.getAccountNameById(groupId);

        GlobalLogger.logGroupAuditLog("doMemberLevel() for Group [" + groupName + "] User: " + user.getName() + " changed " + member.getName() + " to " + level, UserCtl.class);
        Groups.changeMembership(userId, groupId, level);
        Groups.mailMember(groupId, member, groupName + " has changed your access level to: " + level);
	}
	
	void doMaintenance(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String reg = req.getParameter("reg");
		String maintenanceType = req.getParameter("maintenanceType");
		String sFbo = req.getParameter("fbo");
		
		if (reg == null || maintenanceType == null || sFbo == null)
			return;

		int type = Integer.parseInt(maintenanceType);
		int fbo = Integer.parseInt(sFbo);

        int aircraftId = Aircraft.getAircraftIdByRegistration(reg);
        AircraftBean aircraft = Aircraft.getAircraftById(aircraftId);

		if (aircraft == null)
			throw new DataError("Aircraft not found");
		
		if (type != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT)
		{
			if (!aircraft.changeAllowed(user))
				throw new DataError("Permission denied");
		}
		FboBean selectedFbo = null;
		
		if (fbo > 0)
		{
			selectedFbo = Fbos.getFbo(fbo);
			if (selectedFbo == null)
				throw new DataError("Fbo not found");
		}
		
		Aircraft.doMaintenance(aircraft, type, selectedFbo);
	}
	
	void doEquipment(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String reg = req.getParameter("reg");
		String equipmentType = req.getParameter("equipmentType");
        String action = req.getParameter("action");
		String sFbo = req.getParameter("fbo");
		
		if (reg == null || equipmentType == null || action == null || sFbo == null)
			return;
		
		int type = Integer.parseInt(equipmentType);
		int fbo = Integer.parseInt(sFbo);

        int aircraftId = Aircraft.getAircraftIdByRegistration(reg);
        AircraftBean aircraft = Aircraft.getAircraftById(aircraftId);
		
        if (aircraft == null)
			throw new DataError("Aircraft not found");
		
		if (!aircraft.changeAllowed(user))
			throw new DataError("Permission denied");	
		
		FboBean selectedFbo = null;
		
		if (fbo > 0)
		{
			selectedFbo = Fbos.getFbo(fbo);
			if (selectedFbo == null)
				throw new DataError("Fbo not found");
		}
		
		Aircraft.doEquipment(aircraft, type, action, selectedFbo);
	}
	
	void doFlyForGroup(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");
		
		if (sId == null)
			return;
		
		int id = Integer.parseInt(sId);	
		
		Groups.flyForGroup(user, id);
	}	
	
	void doBuySellGoods(HttpServletRequest req, boolean isBuy) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sOwner = req.getParameter("owner");
		String sType = req.getParameter("type");
		String sAmount = req.getParameter("amount");
		String sAccount = req.getParameter("account");
		if (sOwner == null || sType == null || sAmount == null || sAccount == null)
			return;

        String location = req.getParameter("icao");
        if(!Airports.isValidIcao(location))
            throw new DataError("Invalid location.");

		int type = Integer.parseInt(sType);
		if(type < GoodsBean.GOODS_LOWEST || type > GoodsBean.GOODS_HIGHEST)
			throw new DataError("Invalid Goods Type!");
		
		int amount = Integer.parseInt(sAmount);
		int owner = Integer.parseInt(sOwner);
		int account = Integer.parseInt(sAccount);
		int from = isBuy ? owner : account;
		int to = isBuy ? account : owner;
		
		if (account != user.getId() && user.groupMemberLevel(account) < UserBean.GROUP_STAFF)
			throw new DataError("Permission denied");
		
        CachedAirportBean airportInfo = Airports.cachedAirports.get(location);
		List<GoodsBean> goods = Goods.getGoodsAtAirport(airportInfo.getIcao(), airportInfo.getSize(), 0, 0);
		
		int checkid = isBuy ? from : to;
        boolean found = false;

        for (GoodsBean good : goods)
        {
            if (good.getOwner() == checkid && good.getType() == type)
            {
                boolean checkamount = isBuy ? good.getAmountForSale() == -1 || good.getAmountForSale() > amount : good.getAmountAccepted() == -1 || good.getAmountAccepted() >= amount;
                if (isBuy && !checkamount)
                    throw new DataError("Error, not enough goods.");
                else if (!isBuy && !checkamount)
                    throw new DataError("Error, Amount is greater then the max goods the FBO will purchase.");

                found = true;
                break;
            }
        }
		if(!found)
			throw new DataError("No Goods Found!");
		
		Goods.transferGoods(from, to, account, location, type, amount);
	}

    void doPurchaseFbo(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        String sAccountId = req.getParameter("accountid");
        String sFboId = req.getParameter("fboid");

        if (sFboId == null || sAccountId == null)
            throw new DataError("Missing parameters");

        Fbos.buyFbo(Integer.parseInt(sFboId), Integer.parseInt(sAccountId), user);
    }

    void doTransferFbo(HttpServletRequest req) throws DataError
    {
        UserBean user = (UserBean) req.getSession().getAttribute("user");
        String sAccountId = req.getParameter("accountid");
        String sFboId = req.getParameter("fboid");

        if (sFboId == null || !Helpers.isInteger(sFboId) || sAccountId == null || !Helpers.isInteger(sAccountId))
            throw new DataError("Missing parameters");

        FboBean fbo = Fbos.getFbo(Integer.parseInt(sFboId));
        if(fbo == null)
            throw new DataError("Invalid FBO!");

        UserBean account = Accounts.getAccountById(Integer.parseInt(sAccountId));
        if(account == null)
            throw new DataError("Invalid FBO!");

        int owner = fbo.getOwner();
        if(Accounts.isGroup(owner))
            owner = Accounts.accountUltimateOwner(owner);

        if(owner != user.getId())
            throw new DataError("Permission error, you are not the owner!");

        Fbos.transferFbo(fbo, account.getId());
    }

    void doDeleteFbo(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");
		int fboId = Integer.parseInt(sId);
		FboBean fbo = Fbos.getFbo(fboId);
		if (fbo == null)
			return;
		
		if (!fbo.deleteAllowed(user))
			throw new DataError("Permission denied.");
		
		Fbos.deleteFbo(fboId, user);
	}
	
	void doUpgradeFbo(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		String sId = req.getParameter("id");
		int fboId = Integer.parseInt(sId);
		FboBean fbo = Fbos.getFbo(fboId);
		if (fbo == null)
			return;
		
		if (!fbo.updateAllowed(user))
			throw new DataError("Permission denied.");
		
		Fbos.upgradeFbo(fboId, user);
	}
	
	void doRentFboFacility(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		int facilityId = Integer.parseInt(req.getParameter("facilityId"));
		int blocks = Integer.parseInt(req.getParameter("blocks"));
		int occupantId = Integer.parseInt(req.getParameter("occupantId"));

		Fbos.rentFboFacility(user, occupantId, facilityId, blocks);
	}

	void doDeleteFboFacility(HttpServletRequest req) throws DataError
	{
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		int facilityId = Integer.parseInt(req.getParameter("facilityId"));

        Facilities.deleteFacility(user, facilityId);
	}
	
	void doUpdateAircraft(HttpServletRequest req) throws DataError
	{
        int id = Integer.parseInt(req.getParameter("id"));
		String newreg = req.getParameter("newreg");
		String home = req.getParameter("home");
		String location = req.getParameter("location").toUpperCase();
		int owner = 0;
		if (req.getParameter("owner") != null && !req.getParameter("owner").equals(""))
			owner = Integer.parseInt(req.getParameter("owner"));
		
		int userlock = Integer.parseInt(req.getParameter("userlock"));
		int bonus = Integer.parseInt(req.getParameter("bonus"));
		int accounting = Integer.parseInt(req.getParameter("accounting"));
		int rentalDry = Integer.parseInt(req.getParameter("rentalPriceDry"));
		int rentalWet = Integer.parseInt(req.getParameter("rentalPriceWet"));
		int maxRentTime = Integer.parseInt(req.getParameter("maxRentTime"));
		int sellPrice = 0;
		
		if (req.getParameter("sellPrice") != null && !req.getParameter("sellPrice").equals(""))
			sellPrice = Integer.parseInt(req.getParameter("sellPrice"));
		
		String advertiseFerry = req.getParameter("advertiseFerry");
		int advertise;
		if (advertiseFerry == null)
			advertise = 0;
		else
			advertise = 1;
		
		String allowRepair = req.getParameter("allowRepair");
		int repair;
		if (allowRepair == null)
			repair = 0;
		else
			repair = 1;
		
		if (newreg != null && newreg.equals(""))
			newreg = null;
		
		if (newreg != null)
			newreg = newreg.toUpperCase();

        String sEquip = req.getParameter("equip-ifr");
        int equip = 0;
        if (sEquip != null)
            equip |= ModelBean.EQUIPMENT_IFR_MASK;

        sEquip = req.getParameter("equip-gps");
        if (sEquip != null)
            equip |= ModelBean.EQUIPMENT_GPS_MASK;

        sEquip = req.getParameter("equip-ap");
        if (sEquip != null)
            equip |= ModelBean.EQUIPMENT_AP_MASK;

        AircraftBean aircraft = Aircraft.getAircraftById(id);

        if (aircraft == null)
            throw new DataError("Aircraft not found.");

		aircraft.setHome(home);
		aircraft.setOwner(owner);
		aircraft.setLocation(location);
		aircraft.setUserLock(userlock);
		aircraft.setBonus(bonus);
		aircraft.setAccounting(accounting);
		aircraft.setRentalPriceDry(rentalDry);
		aircraft.setRentalPriceWet(rentalWet);
		aircraft.setMaxRentTime(maxRentTime);
        aircraft.setEquipment(equip);
		aircraft.setSellPrice(sellPrice);
		
		if (advertise == 0)
			aircraft.setAdvertiseFerry(false);
		else
			aircraft.setAdvertiseFerry(true);
		
		if (repair == 0)
			aircraft.setAllowRepair(false);
		else
			aircraft.setAllowRepair(true);
		
		Aircraft.updateAircraft4Admins(aircraft, newreg);
	}
	
	void doAdjustGoods(HttpServletRequest req) throws DataError
	{
		int owner = Integer.parseInt(req.getParameter("owner"));
		UserBean account = Accounts.getAccountById(owner);
		String ownername = account.getName();

		String location = req.getParameter("location").toUpperCase();
		if (!Airports.isValidIcao(location))
			throw new DataError("Invalid Location ICAO");
		
		int commodity = Integer.parseInt(req.getParameter("commodity"));
		String goods = Goods.commodities[commodity].getName();
		int amount = Integer.parseInt(req.getParameter("amount"));

		Goods.updateGoods4Admins(owner, location, commodity, amount);
		
		req.setAttribute("message", "Completed - Adjusted " + goods + " by " + amount + "kg to " + ownername + "'s inventory @ " + location );
	}
	
	void doBulkGoodsPurchase(HttpServletRequest req, int order) throws DataError
	{
        int amount100ll = 0;
        int amountJetA = 0;
        int amount = 0;

        if(order == Fbos.FBO_ORDER_FUEL)
        {
            amount100ll = Integer.parseInt(req.getParameter("amount100ll"));
            amountJetA = Integer.parseInt(req.getParameter("amountJetA"));
        }
        else //(order == Fbos.FBO_ORDER_SUPPLIES)
        {
            amount = Integer.parseInt(req.getParameter("amount"));
        }

		int fboID = Integer.parseInt(req.getParameter("fboID"));			
		int daysOut = Integer.parseInt(req.getParameter("daysOut"));
		int accountToPay = Integer.parseInt(req.getParameter("accountToPay"));
		int location = Integer.parseInt(req.getParameter("location"));
		String icao = req.getParameter("icao");
		
		UserBean user = (UserBean) req.getSession().getAttribute("user");
		Fbos.registerBulkGoodsOrder(user, fboID, order, amount100ll, amountJetA, amount, daysOut, accountToPay, location, icao);
	}
}

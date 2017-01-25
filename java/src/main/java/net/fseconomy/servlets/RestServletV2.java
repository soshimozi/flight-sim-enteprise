package net.fseconomy.servlets;

import net.fseconomy.beans.GoodsBean;
import net.fseconomy.data.Data;
import net.fseconomy.data.Goods;
import net.fseconomy.data.Stats;
import net.fseconomy.dto.LatLonCount;
import net.fseconomy.dto.PilotStatus;
import net.fseconomy.services.ServiceData;
import net.fseconomy.util.Formatters;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static net.fseconomy.services.common.*;

@Path("/api/v2/")
@Produces({ "application/json;charset=UTF-8" })
public class RestServletV2
{
    @GET
    @Path("/account/bank/{account}")
    public Response getBalanceBank(@HeaderParam("servicekey") String servicekey,
                                   @PathParam("account") final int account)
    {
        PermissionCategory category = PermissionCategory.BANK;

        if(!hasPermission(servicekey, account, category, PermissionSet.READ))
            return ResponseAccessDenied();

        return ServiceData.getBalance(category, account);
    }

    @GET
    @Path("/account/cash/{account}")
    public Response getBalanceCash(@HeaderParam("servicekey") String servicekey,
                                   @PathParam("account") final int account)
    {
        PermissionCategory category = PermissionCategory.CASH;

        if(!hasPermission(servicekey, account, category, PermissionSet.READ))
            return ResponseAccessDenied();

        return ServiceData.getBalance(category, account);
    }

    @POST
    @Path("/account/withdraw/{account}")
    public Response getWithdrawIntoCash(@HeaderParam("servicekey") String servicekey,
                                        @PathParam("account") final int account,
                                        @FormParam("amount") final String amount)
    {
        PermissionCategory category = PermissionCategory.BANK;

        if(!hasPermission(servicekey, account, category, PermissionSet.WITHDRAW))
            return ResponseAccessDenied();

        return ServiceData.WithdrawIntoCash(account, new Double(amount));
    }

    @POST
    @Path("/account/deposit/{account}")
    public Response getDepositIntoBank(@HeaderParam("servicekey") String servicekey,
                                       @PathParam("account") final int account,
                                       @FormParam("amount") final String amount)
    {
        PermissionCategory category = PermissionCategory.BANK;

        if(!hasPermission(servicekey, account, category, PermissionSet.DEPOSIT))
            return ResponseAccessDenied();

        return ServiceData.DepositIntoBank(account, new Double(amount));
    }

    @POST
    @Path("/account/transfer/{account}/{transferto}")
    public Response getTransferFromCash(@HeaderParam("servicekey") String servicekey,
                                        @PathParam("account") final int account,
                                        @PathParam("transferto") final int transferto,
                                        @FormParam("amount") final String amount,
                                        @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.CASH;

        if(!hasPermission(servicekey, account, category, PermissionSet.TRANSFER))
            return ResponseAccessDenied();

        return ServiceData.TransferCashToAccount(servicekey, account, new Float(amount), transferto, note);
    }

    @POST
    @Path("/aircraft/purchase/{account}/{serialnumber}")
    public Response PurchaseAircraft(@HeaderParam("servicekey") String servicekey,
                                     @PathParam("account") final int account,
                                     @PathParam("serialnumber")int serialNumber,
                                     @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.PURCHASE))
            return ResponseAccessDenied();

        return ServiceData.PurchaseAircraft(servicekey, account, serialNumber, note);
    }

    @POST
    @Path("/aircraft/transfer/{account}/{transferto}/{serialnumber}")
    public Response TransferAircraft(@HeaderParam("servicekey") String servicekey,
                                     @PathParam("account") final int account,
                                     @PathParam("transferto") final int transferto,
                                     @PathParam("serialnumber")int serialNumber,
                                     @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.TRANSFER))
            return ResponseAccessDenied();

        if(transferto == 0) //no transfers to bank
            return createErrorResponse(200, 200, "Bad Request", "No transfer account specified.");

        return ServiceData.TransferAircraft(servicekey, serialNumber, account, transferto, note);
    }

    @POST
    @Path("/aircraft/lease/{account}/{leaseto}/{serialnumber}")
    public Response LeaseAircraft(@HeaderParam("servicekey") String servicekey,
                                  @PathParam("account") final int account,
                                  @PathParam("leaseto") final int leaseto,
                                  @PathParam("serialnumber")int serialNumber,
                                  @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.LEASE))
            return ResponseAccessDenied();

        return ServiceData.LeaseAircraft(servicekey, account, serialNumber, leaseto, note);
    }

    @POST
    @Path("/aircraft/returnlease/{account}/{serialnumber}")
    public Response ReturnLeaseAircraft(@HeaderParam("servicekey") String servicekey,
                                        @PathParam("account") final int account,
                                        @PathParam("serialnumber")int serialNumber,
                                        @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.LEASE))
            return ResponseAccessDenied();

        return ServiceData.ReturnLeaseAircraft(servicekey, account, serialNumber, note);
    }

    @GET
    @Path("/aircraft/checkreg")
    public Response checkAircraftRegistration(@HeaderParam("servicekey") String servicekey,
                                              @QueryParam("registration") final String registration)
    {
        return ServiceData.isValidRegistration(registration);
    }

    @POST
    @Path("/aircraft/changereg/{account}/{serialnumber}")
    public Response changeAircraftRegistration(@HeaderParam("servicekey") String servicekey,
                                               @PathParam("account") final int account,
                                               @PathParam("serialnumber")int serialNumber,
                                               @FormParam("registration") final String registration,
                                               @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.EDIT))
            return ResponseAccessDenied();

        return ServiceData.changeRegistration(servicekey, serialNumber, registration, note);
    }

    @POST
    @Path("/aircraft/update/{account}/{serialnumber}")
    public Response updateAircraft(@HeaderParam("servicekey") String servicekey,
                                   @PathParam("account") final int account,
                                   @PathParam("serialnumber")int serialnumber,
                                   @FormParam("home") final String home,            //optional defaults to: null and will not change current
                                   @FormParam("bonus") final int bonus,             //optional defaults to: 0
                                   @FormParam("rentalwet") final int rentalwet,     //optional defaults to: 0
                                   @FormParam("rentaldry") final int rentaldry,     //optional defaults to: 0
                                   @FormParam("maxrenthrs") final int maxrenthrs,   //optional defaults to: 1 range: 1-10 step 1
                                   @FormParam("ferry") final boolean ferry,         //optional defaults to: false
                                   @FormParam("repair") final boolean repair)       //optional defaults to: false
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.EDIT))
            return ResponseAccessDenied();


        return ServiceData.updateAircraft(servicekey, account, serialnumber, home.toUpperCase(), bonus, rentalwet, rentaldry, maxrenthrs, ferry, repair);
    }


    @POST
    @Path("/aircraft/sale/{account}/{serialnumber}")
    public Response saleAircraft(@HeaderParam("servicekey") String servicekey,
                                 @PathParam("account") final int account,
                                 @PathParam("serialnumber")int serialnumber,
                                 @FormParam("privatesale") final boolean privatesale,
                                 @FormParam("buyer") final int buyerid,
                                 @FormParam("price") final int price,
                                 @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.SALE))
            return ResponseAccessDenied();


        return ServiceData.updateAircraftSalePrice(servicekey, account, serialnumber, privatesale, buyerid, price, note);
    }


    @POST
    @Path("/account/search/name")
    public Response getAccountId(@HeaderParam("servicekey") String servicekey,
                                 @FormParam("accountname")String name)
    {
        return ServiceData.getAccountId(name);
    }

    @POST
    @Path("/account/search/id/{account}")
    public Response getAccountId(@HeaderParam("servicekey") String servicekey,
                                 @PathParam("account") final int id)
    {
        return ServiceData.getAccountName(id);
    }

    @PermitAll
    @GET
    @Path("/pilotstatus")
    public Response getPilotStatus()
    {
        //This is called A LOT.
        //The PilotStat class uses single character labels to reduce traffic.
        List<PilotStatus> list = Data.getPilotStatus();

        return createSuccessResponse(200, 200, null, null, list);
    }

    @PermitAll
    @GET
    @Path("/flightsummary")
    public Response getFlightSummary()
    {
        //This is called A LOT.
        //The PilotStat class uses single character labels to reduce traffic.
        List<LatLonCount> list = Stats.FlightSummaryList;

        return createSuccessResponse(200, 200, null, null, list);
    }

    @PermitAll
    @GET
    @Path("/fuelquote/{fueltype}/{amount}/{icao}")
    public Response getGoodsQuote(@PathParam("fueltype") final int fueltype,
                                  @PathParam("amount") final int amount,
                                  @PathParam("icao") final String icao)
    {
        String price;
        if(fueltype == GoodsBean.GOODS_SUPPLIES)
            price = Formatters.currency.format(Goods.quoteOrder(icao, fueltype, amount, false));
        else
            price = Formatters.currency.format(Goods.quoteOrder(icao, fueltype, amount, true));

        return createSuccessResponse(200, 200, null, null, price);
    }

    @PermitAll
    @GET
    @Path("/goodsquote/{goodstype}/{amount}/{icao}/{src}")
    public Response getGoodsQuote(@PathParam("goodstype") final int goodstype,
                                  @PathParam("amount") final int amount,
                                  @PathParam("icao") final String icao,
                                  @PathParam("src") final int src)
    {
        boolean BUY = true;

        String price = Formatters.currency.format(Goods.quoteGoods(icao, goodstype, amount, src, BUY));

        return createSuccessResponse(200, 200, null, null, price);
    }
}
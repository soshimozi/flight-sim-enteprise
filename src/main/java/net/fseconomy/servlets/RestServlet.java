package net.fseconomy.servlets;

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

@Path("/api")
@Produces({ "application/json;charset=UTF-8" })
public class RestServlet
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
    @Path("/account/transfer/{account}")
    public Response getTransferFromCash(@HeaderParam("servicekey") String servicekey,
                                        @PathParam("account") final int account,
                                        @FormParam("transferto") final int transferto,
                                        @FormParam("amount") final String amount,
                                        @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.CASH;

        if(!hasPermission(servicekey, account, category, PermissionSet.TRANSFER))
            return ResponseAccessDenied();

        return ServiceData.TransferCashToAccount(servicekey, account, new Float(amount), transferto, note);
    }

    @POST
    @Path("/aircraft/purchase/{account}")
    public Response PurchaseAircraft(@HeaderParam("servicekey") String servicekey,
                                     @PathParam("account") final int account,
                                     @FormParam("serialnumber")int serialNumber,
                                     @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.PURCHASE))
            return ResponseAccessDenied();

        return ServiceData.PurchaseAircraft(servicekey, account, serialNumber, note);
    }

    @POST
    @Path("/aircraft/transfer/{account}")
    public Response TransferAircraft(@HeaderParam("servicekey") String servicekey,
                                     @PathParam("account") final int account,
                                     @FormParam("serialnumber")int serialNumber,
                                     @FormParam("transferto") final int transferto,
                                     @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.TRANSFER))
            return ResponseAccessDenied();

        if(transferto == 0) //no transfers to bank
            return createErrorResponse(400, "Bad Request", "No transfer account specified.");

        return ServiceData.TransferAircraft(servicekey, serialNumber, account, transferto, note);
    }

    @POST
    @Path("/aircraft/lease/{account}")
    public Response LeaseAircraft(@HeaderParam("servicekey") String servicekey,
                                  @PathParam("account") final int account,
                                  @FormParam("serialnumber")int serialNumber,
                                  @FormParam("leaseto") final int leaseto,
                                  @FormParam("note") final String note)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.LEASE))
            return ResponseAccessDenied();

        return ServiceData.LeaseAircraft(servicekey, account, serialNumber, leaseto, note);
    }

    @PermitAll
    @POST
    @Path("/account/search/name")
    public Response getAccountId(@HeaderParam("servicekey") String servicekey,
                                 @FormParam("accountname")String name)
    {
        return ServiceData.getAccountId(name);
    }

    @PermitAll
    @POST
    @Path("/account/search/id")
    public Response getAccountId(@HeaderParam("servicekey") String servicekey,
                                 @FormParam("id") final int id)
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

        return createSuccessResponse(200, null, null, list);
    }

    @PermitAll
    @GET
    @Path("/flightsummary")
    public Response getFlightSummary()
    {
        //This is called A LOT.
        //The PilotStat class uses single character labels to reduce traffic.
        List<LatLonCount> list = Stats.FlightSummaryList;

        return createSuccessResponse(200, null, null, list);
    }

    @PermitAll
    @GET
    @Path("/fuelquote/{fueltype}/{amount}/{icao}")
    public Response getGoodsQuote(@PathParam("fueltype") final int fueltype,
                                  @PathParam("amount") final int amount,
                                  @PathParam("icao") final String icao)
    {
        String price = Formatters.currency.format(Goods.quoteFuel(icao, fueltype, amount));

        return createSuccessResponse(200, null, null, price);
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

        return createSuccessResponse(200, null, null, price);
    }
}
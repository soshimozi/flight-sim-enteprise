package net.fseconomy.servlets;

import net.fseconomy.data.Data;
import net.fseconomy.services.serviceData;
import net.fseconomy.util.Formatters;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.util.List;

import static net.fseconomy.services.serviceData.*;
import static net.fseconomy.services.common.*;

@Path("/")
@PreMatching
public class RestServlet
{
    @GET
    @Path("/account/bank/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getBalanceBank(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account)
    {
        PermissionCategory category = PermissionCategory.BANK;

        if(!hasPermission(servicekey, account, category, PermissionSet.READ))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.getBalance(category, account);
    }

    @GET
    @Path("/account/cash/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getBalanceCash(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account)
    {
        PermissionCategory category = PermissionCategory.CASH;

        if(!hasPermission(servicekey, account, category, PermissionSet.READ))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.getBalance(category, account);
    }

    @POST
    @Path("/account/withdraw/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getWithdrawIntoCash(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account, @FormParam("amount") final String amount)
    {
        PermissionCategory category = PermissionCategory.BANK;

        if(!hasPermission(servicekey, account, category, PermissionSet.WITHDRAW))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.WithdrawIntoCash(account, new Double(amount));
    }

    @POST
    @Path("/account/deposit/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getDepositIntoBank(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account, @FormParam("amount") final String amount)
    {
        PermissionCategory category = PermissionCategory.BANK;

        if(!hasPermission(servicekey, account, category, PermissionSet.DEPOSIT))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.DepositIntoBank(account, new Double(amount));
    }

    @POST
    @Path("/account/transfer/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getTransferFromCash(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account, @FormParam("transferto") final int transferto, @FormParam("amount") final String amount)
    {
        PermissionCategory category = PermissionCategory.CASH;

        if(!hasPermission(servicekey, account, category, PermissionSet.TRANSFER))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.TransferCashToAccount(servicekey, account, new Float(amount), transferto);
    }

    @POST
    @Path("/aircraft/purchase/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response PurchaseAircraft(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account, @FormParam("reg")String reg)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.PURCHASE))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.PurchaseAircraft(servicekey, account, reg);
    }

    @POST
    @Path("/aircraft/lease/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response LeaseAircraft(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account, @FormParam("reg")String reg, @FormParam("leaseto") final int leaseto)
    {
        PermissionCategory category = PermissionCategory.AIRCRAFT;

        if(!hasPermission(servicekey, account, category, PermissionSet.LEASE))
            return ResponseAccessDenied();

        //Get the selected balance response
        return serviceData.LeaseAircraft(servicekey, account, reg, leaseto);
    }

    @PermitAll
    @POST
    @Path("/account/search/name")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getAccountId(@HeaderParam("servicekey") String servicekey, @FormParam("accountname")String name)
    {
        return serviceData.getAccountId(name);
    }

    @PermitAll
    @POST
    @Path("/account/search/id")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getAccountId(@HeaderParam("servicekey") String servicekey, @FormParam("id") final int id)
    {
        return serviceData.getAccountName(id);
    }

    @PermitAll
    @GET
    @Path("/pilotstatus")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getPilotStatus()
    {
        //This is called A LOT.
        //The PilotStat class uses single character labels to reduce traffic.
        List<Data.PilotStatus> list = Data.getInstance().getPilotStatus();
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        return Response
                .status(200)
                .cacheControl(NoCache)
                .header("Access-Control-Allow-Origin","*")
                .entity(list).build();
    }

    @PermitAll
    @GET
    @Path("/flightsummary")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getFlightSummary()
    {
        //This is called A LOT.
        //The PilotStat class uses single character labels to reduce traffic.
        List<Data.LatLonCount> list = Data.getInstance().FlightSummaryList;
        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        return Response
                .status(200)
                .cacheControl(NoCache)
                .header("Access-Control-Allow-Origin","*")
                .entity(list).build();
    }

    @PermitAll
    @GET
    @Path("/fuelquote/{fueltype}/{amount}/{icao}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getGoodsQuote(@PathParam("fueltype") final int fueltype,
                                  @PathParam("amount") final int amount,
                                  @PathParam("icao") final String icao)
    {
        String price = Formatters.twoDecimals.format(Data.getInstance().quoteFuel(icao, fueltype, amount));

        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        return Response
                .status(200)
                .cacheControl(NoCache)
                .header("Access-Control-Allow-Origin","*")
                .entity(price).build();
    }

    @PermitAll
    @GET
    @Path("/goodsquote/{goodstype}/{amount}/{icao}/{src}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getGoodsQuote(@PathParam("goodstype") final int goodstype,
                                  @PathParam("amount") final int amount,
                                  @PathParam("icao") final String icao,
                                  @PathParam("src") final int src)
    {
        boolean BUY = true;

        String price = Formatters.twoDecimals.format(Data.getInstance().quoteGoods(icao, goodstype, amount, src, BUY));

        CacheControl NoCache = new CacheControl();
        NoCache.setNoCache(true);

        return Response
                .status(200)
                .cacheControl(NoCache)
                .header("Access-Control-Allow-Origin","*")
                .entity(price).build();
    }

}
package net.fseconomy.servlets;

import net.fseconomy.data.Data;
import net.fseconomy.services.serviceData;
import net.fseconomy.util.Formatters;
import net.fseconomy.util.RestResponses;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
@PreMatching
public class resteasyServlet
{
    @GET
    @Path("/account/bank/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getBalanceBank(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account)
    {
        serviceData.PermissionCategory category = serviceData.PermissionCategory.BANK;

        if(!serviceData.hasPermission(servicekey, account, category, serviceData.PermissionSet.READ))
            return RestResponses.ACCESS_DENIED;

        //Get the selected balance response
        return serviceData.getBalance(category, account);
    }

    @GET
    @Path("/account/cash/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getBalanceCash(@HeaderParam("servicekey") String servicekey, @PathParam("account") final int account)
    {
        serviceData.PermissionCategory category = serviceData.PermissionCategory.CASH;

        if(!serviceData.hasPermission(servicekey, account, category, serviceData.PermissionSet.READ))
            return RestResponses.ACCESS_DENIED;

        //Get the selected balance response
        return serviceData.getBalance(category, account);
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
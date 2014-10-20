package net.fseconomy.servlets;

import net.fseconomy.data.Data;
import net.fseconomy.util.Formatters;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * A simple REST service which is able to say hello to someone using HelloService Please take a look at the web.xml where JAX-RS
 * is enabled
 *
 * @author gbrey@redhat.com
 *
 */
@Path("/")
@PreMatching
public class resteasyServlet
{

    public void init()
    {
    }

    @RolesAllowed({"Read"})
    @GET
    @Path("/balance/{type}/{account}")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getBalance(@PathParam("type") final String type, @PathParam("account") final int account)
    {
        //Get the selected balance
        double amount = Data.getInstance().getPilotStatus();
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
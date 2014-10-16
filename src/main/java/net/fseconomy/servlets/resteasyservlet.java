package net.fseconomy.servlets;

import net.fseconomy.data.Data;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * A simple REST service which is able to say hello to someone using HelloService Please take a look at the web.xml where JAX-RS
 * is enabled
 *
 * @author gbrey@redhat.com
 *
 */

@Path("/")
public class resteasyServlet
{

    public void init()
    {
    }

    @GET
    @Path("/pilotstatus")
    @Produces({ "application/json;charset=UTF-8" })
    public Response getHelloWorldJSON()
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
}
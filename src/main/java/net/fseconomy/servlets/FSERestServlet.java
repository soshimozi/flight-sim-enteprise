package net.fseconomy.servlets;

import com.google.gson.Gson;
import net.fseconomy.services.AdminServiceData;
import net.fseconomy.services.Authenticator;
import net.fseconomy.services.ClientServices;
import net.fseconomy.util.Crypto;
import net.fseconomy.util.Helpers;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static net.fseconomy.services.common.ResponseAccessDenied;
import static net.fseconomy.services.common.createErrorResponse;
import static net.fseconomy.services.common.createSuccessResponse;

//Status codes: http://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml

@DeclareRoles({"admin", "moderator", "csr", "aca"})

@Path("/fse/api")
@Produces({ "application/json;charset=UTF-8" })
public class FSERestServlet
{
    @PermitAll
    @GET
    @Path("/accountcheck/{account}")
    public Response accountCheck(@PathParam("account") final String encrypted)
    {
        String params = Crypto.decrypt(encrypted);
        if(Helpers.isNullOrBlank(params))
            return createErrorResponse(400, 400, "Bad Request", "Invalid username and password.");

        String[] sp = params.trim().split("\\|\\^\\|");
        if(sp.length != 2)
            return createErrorResponse(400, 400, "Bad Request", "Invalid username and password.");

        String token = Authenticator.getInstance().login(sp[0], sp[1]);
        if(token == null)
            return createErrorResponse(400, 400, "Bad Request", "Invalid credentials.");
        else
            return createSuccessResponse(200, 200, null, null, token);
    }

    @PermitAll
    @POST
    @Path("/login")
    public Response login(@FormParam("username") final String username,@FormParam("password") final String password)
    {
        String token = Authenticator.getInstance().login(username, password);
        if(token == null)
            return createErrorResponse(400, 400, "Bad Request", "Invalid username and password.");
        else
            return createSuccessResponse(200, 200, null, null, token);
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("authtoken") String authToken, @FormParam("username") final String username)
    {
        if (Authenticator.getInstance().logout(authToken))
            return createSuccessResponse(200, 200, null, null, "Logged out.");
        else
            return createErrorResponse(400, 400, "Bad Request", "Invalid username and authtoken.");
    }

    @RolesAllowed({"admin", "moderator"})
    @GET
    @Path("/templateitems/{id}")
    public Response getPilotStatus(@PathParam("id") final int id)
    {
        return AdminServiceData.getTemplateLatLonCountById(id);
    }

    @GET
    @Path("/rentalaircraftconfig")
    public Response getRentedAircraftConfig(@HeaderParam("authtoken") String authToken)
    {
        int userId = Authenticator.getInstance().getUserIdFromToken(authToken);
        Response response = ClientServices.getRentedAircraftConfig(userId);

        return response;
    }

    @POST
    @Path("/checkalias")
    public Response checkAlias(@HeaderParam("authtoken") String authToken, @FormParam("alias") final String alias, @FormParam("tanksJson") final String tanksJson)
    {
        int userId = Authenticator.getInstance().getUserIdFromToken(authToken);
        int[] tanks = new Gson().fromJson(tanksJson, int[].class);

        Response response = ClientServices.checkAlias(userId, alias, tanks);

        return response;
    }
    @POST
    @Path("/addaircraft")
    public Response addAlias(@HeaderParam("authtoken") String authToken, @FormParam("alias") final String alias, @FormParam("fuel") final int[] tanks)
    {
        int userId = Authenticator.getInstance().getUserIdFromToken(authToken);
        Response response = ClientServices.addAlias(userId, alias, tanks);

        return response;
    }
}
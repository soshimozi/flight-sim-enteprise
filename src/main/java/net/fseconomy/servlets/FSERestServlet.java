package net.fseconomy.servlets;

import net.fseconomy.services.AdminServiceData;
import net.fseconomy.services.Authenticator;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static net.fseconomy.services.common.ResponseAccessDenied;
import static net.fseconomy.services.common.createErrorResponse;
import static net.fseconomy.services.common.createSuccessResponse;

@DeclareRoles({"admin", "moderator", "csr", "aca"})

@Path("/fse/api")
@Produces({ "application/json;charset=UTF-8" })
public class FSERestServlet
{
    @PermitAll
    @POST
    @Path("/login")
    public Response login(@FormParam("username") final String username,@FormParam("password") final String password)
    {
        String token = Authenticator.getInstance().login(username, password);
        if(token == null)
            return createErrorResponse(400, "Bad Request", "Invalid username and password.");
        else
            return createSuccessResponse(200, null, null, token);
    }

    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("authtoken") String authToken, @FormParam("username") final String username)
    {
        if (Authenticator.getInstance().logout(authToken))
            return createSuccessResponse(200, null, null, "Logged out.");
        else
            return createErrorResponse(400, "Bad Request", "Invalid username and authtoken.");
    }

    @POST
    @Path("/pilotinfo")
    public Response pilotInfo(@HeaderParam("authtoken") String authToken)
    {
//        if (Authenticator.getInstance().isAuthTokenValid(authToken))
//            return FSEServiceData.getPilotInfo();
//        else
            return ResponseAccessDenied();
    }

    @RolesAllowed({"admin", "moderator"})
    @GET
    @Path("/templateitems/{id}")
    public Response getPilotStatus(@PathParam("id") final int id)
    {
        return AdminServiceData.getTemplateLatLonCountById(id);
    }
}
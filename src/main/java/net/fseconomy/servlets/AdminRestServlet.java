package net.fseconomy.servlets;

import net.fseconomy.data.Data;
import net.fseconomy.dto.PilotStatus;
import net.fseconomy.dto.TemplateHeatmapItem;
import net.fseconomy.services.AdminServiceData;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/sst")
@Produces({ "application/json;charset=UTF-8" })
public class AdminRestServlet
{
    @GET
    @Path("/templateitems/{id}")
    public Response getPilotStatus(@PathParam("id") final int id)
    {
        return AdminServiceData.getTemplateLatLonCountById(id);
    }
}
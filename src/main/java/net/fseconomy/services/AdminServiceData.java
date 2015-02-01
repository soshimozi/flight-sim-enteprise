package net.fseconomy.services;

import static net.fseconomy.services.common.*;

import net.fseconomy.beans.CachedAirportBean;
import net.fseconomy.data.*;
import net.fseconomy.dto.LatLonCount;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class AdminServiceData
{
    public static Response getTemplateLatLonCountById(int templateId)
    {
        List<LatLonCount> toList = new ArrayList<>();

        try
        {
            String qry = "SELECT fromicao, count(id) as count from assignments WHERE assignments.fromTemplate = ? group by fromicao order by fromicao";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, templateId);

            while(rs.next())
            {
                CachedAirportBean cab = Airports.cachedAirports.get(rs.getString("fromicao"));

                LatLonCount llc = new LatLonCount(cab.getLatLon().lat, cab.getLatLon().lon, rs.getInt("count"));
                toList.add(llc);
            }

            return createSuccessResponse(200, null, null, toList);
        }
        catch(BadRequestException e)
        {
            return createErrorResponse(400, "Bad Request", "No records found.");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return createErrorResponse(500, "System Error",  "Unable to fulfill the request.");
        }
    }
}

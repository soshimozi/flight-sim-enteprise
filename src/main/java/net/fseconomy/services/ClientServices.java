package net.fseconomy.services;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.data.Aircraft;
import net.fseconomy.dto.AircraftConfig;
import net.fseconomy.dto.AircraftConfig2;

import javax.ws.rs.core.Response;

public class ClientServices
{
    public static Response getRentedAircraftConfig(int userId)
    {
        AircraftBean ab = Aircraft.getAircraftForUser(userId);
        if(ab != null)
        {
            AircraftConfig2 ac = Aircraft.getAircraftConfigs2(ab.getModelId());
            return common.createSuccessResponse(200, null, null, ac);
        }

        return common.createErrorResponse(428, "Warning", "No aircraft rented.");
    }
}

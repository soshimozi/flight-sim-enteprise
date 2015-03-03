package net.fseconomy.services;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.beans.ModelBean;
import net.fseconomy.data.Aircraft;
import net.fseconomy.data.Models;
import net.fseconomy.dto.AircraftAlias;
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
            return common.createSuccessResponse(200, 200, null, null, ac);
        }

        return common.createErrorResponse(200, 428,  "Warning", "No aircraft rented.");
    }

    public static Response checkAlias(int userId, String alias, int[] fuel)
    {
        boolean modelFound = false;
        boolean tanksMatch = false;

        int modelId = Aircraft.getAircraftModelIdForUser(userId);


        if(modelId == 0)
        {
            //return no rented aircraft
            return common.createSuccessResponse(200, 404, "Error", "No rented aircraft found.", false);
        }

        modelFound = Aircraft.aircraftMappingFound(modelId, alias);
        if(!modelFound)
        {
            //return no match found
            return common.createSuccessResponse(200, 404, "Error", "No match found.", false);
        }

        ModelBean mb = Models.getModelById(modelId);
        tanksMatch = mb.compareFuelTanks(fuel);

        if(!tanksMatch)
        {
            //return fuel tank mismatch
            return common.createSuccessResponse(200, 404, "Error", "Fuel tank mismatch.", false);
        }

        return common.createSuccessResponse(200, 200, null, null, mb.getMakeModel());
    }

    public static Response addAlias(int userId, String alias, int[] fuel)
    {
        int modelId = Aircraft.getAircraftModelIdForUser(userId);
        boolean found = Aircraft.aircraftMappingFound(modelId, alias);
        ModelBean mb = Models.getModelById(modelId);
        boolean tanksMatch = mb.compareFuelTanks(fuel);

        return common.createSuccessResponse(200, 200, null, null, found);
    }
}

package net.fseconomy.data;

import net.fseconomy.beans.*;
import net.fseconomy.dto.AircraftAlias;
import net.fseconomy.dto.AircraftConfigs;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.LatLonSize;
import net.fseconomy.util.Constants;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;

import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class Aircraft implements Serializable
{
    public static void transferac(String reg, int buyer, int owner, String location) throws DataError
    {
        try
        {
            String qry = "UPDATE aircraft SET owner = ? WHERE owner = ? AND registration = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, buyer, owner, reg);

            Banking.doPayment(buyer, owner, 0, PaymentBean.AIRCRAFT_SALE, 0, -1, location, reg, "AC Transfer", false);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void leaseac(String reg, int lessee, int owner, String location) throws DataError
    {
        try
        {
            String qry = "UPDATE aircraft SET sellprice=null, owner = ?, lessor = ? WHERE owner = ? AND registration = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, lessee, owner, owner, reg);

            Banking.doPayment(lessee, owner, 0, PaymentBean.AIRCRAFT_LEASE, 0, -1, location, reg, "Aircraft Lease", false);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void leasereturnac(String reg, int lessee, int owner, String location) throws DataError
    {
        try
        {
            String qry = "UPDATE aircraft SET owner = ?, lessor = null WHERE lessor = ? AND registration = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, owner, owner, reg);

            Banking.doPayment(owner, lessee, 0, PaymentBean.AIRCRAFT_LEASE, 0, -1, location, reg, "Aircraft Lease Return", false);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<AircraftBean> getAircraftForSale()
    {
        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND sellPrice is not null ORDER BY models.make, models.model, sellPrice");
    }

    public static AircraftBean getAircraftByRegistration(String reg)
    {
        List<AircraftBean> result = getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND registration='" + Converters.escapeSQL(reg) + "'");
        return result.size() == 0 ? null : result.get(0);
    }

    public static Boolean isAircraftRegistrationUnique(String reg)
    {
        Boolean exists = true; //default is not to allow the registration on error

        try
        {
            String qry = "SELECT (Count(registration) = 0) as notfound FROM aircraft where registration = ?";
            exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), reg);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return exists;
    }

    /**
     * Gets the aircraft by registration and then fills in the shipping config values for passed in aircraft
     *
     * @param reg Airboss 12/21/10
     */
    public static AircraftBean getAircraftShippingInfoByRegistration(String reg)
    {
        List<AircraftBean> aircraftList = getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND registration='" + Converters.escapeSQL(reg) + "'");
        AircraftBean aircraft = aircraftList.get(0);

        //Currently not used
        //if no shipping size available then return without trying to set.
        //if( aircraft[0].getShippingSize() < 1)
        //	return aircraft;

        try
        {
            String qry = "SELECT * FROM shippingConfigsAircraft WHERE minSize <= ? AND maxSize >= ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, aircraft.getEmptyWeight(), aircraft.getEmptyWeight());
            rs.next();
            aircraft.setShippingConfigAircraft(rs.getInt("shippingStateDelay"), rs.getDouble("costPerKg"), rs.getInt("costPerCrate"), rs.getInt("costDisposal"));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return aircraft;
    }

    /**
     * Gets the aircraft that are in a shipped state
     *
     * @return AircraftBean List
     */
    public static List<AircraftBean> getShippedAircraft()
    {
        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND ShippingState != 0");
    }

    /**
     * This updates aircraft into initial shipping state of disassembly, making payments, etc...
     *
     * @param user      - player that initiated the shipping
     * @param aircraft  - aircraft being shipped
     * @param shipto    - ICAO aircraft is being shipped to
     * @param departSvc - FBOID for departure service fees
     * @param destSvc   - FBOID for destination service fees
     */
    public static void processAircraftShipment(UserBean user, AircraftBean aircraft, String shipto, int departSvc, int destSvc) throws DataError
    {
        int departMargin;
        int destMargin;

        //make sure that its the owner or grop staff member
        if (user.getId() != aircraft.getOwner() && user.groupMemberLevel(aircraft.getOwner()) < UserBean.GROUP_STAFF)
        {
            throw new DataError("Permission denied");
        }

        //calculate departure and destination shipping costs
        FboBean fromfbo = Fbos.getFbo(departSvc);
        FboBean tofbo = Fbos.getFbo(destSvc);

        //system fbo
        if (fromfbo == null)
        {
            AirportBean airport = Airports.getAirport(aircraft.getLocation());
            List<FboBean> fbos = Fbos.getFboForRepair(airport, Fbos.FBO_REPAIR_MARGIN);

            if (fbos.size() == 0)
            {
                throw new DataError("Unable to find a repair shop!");
            }

            fromfbo = fbos.get(0); //return the cheapest
        }

        if (tofbo == null)
        {
            // Get a Default FBO if none is specified
            AirportBean airport = Airports.getAirport(shipto);
            List<FboBean> fbos = Fbos.getFboForRepair(airport, Fbos.FBO_REPAIR_MARGIN);

            if (fbos.size() == 0)
            {
                throw new DataError("Unable to find a repair shop!");
            }

            tofbo = fbos.get(0); //return the cheapest
        }

        //get the margins
        departMargin = departSvc == 0 ? departMargin = 25 : fromfbo.getRepairShopMargin();
        destMargin = destSvc == 0 ? destMargin = 25 : tofbo.getRepairShopMargin();

        AircraftBean acShippingInfo = getAircraftShippingInfoByRegistration(aircraft.getRegistration());

        //Compute total shipping costs
        double[] shippingcost = acShippingInfo.getShippingCosts(1);

        double totaldepartcost = shippingcost[0] * (1.0 + (departMargin / 100.0));
        double totaldestcost = shippingcost[1] * (1.0 + (destMargin / 100.0));

        //Total amount for bank check
        double totalshippingcost = totaldepartcost + totaldestcost;

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            double money = Banking.getAccountFundsById(aircraft.getOwner());
            if (money < totalshippingcost)
            {
                throw new DataError("Not enough money to ship aircraft");
            }

            //Payments and log entries are all handled here
            doMaintenanceAircraftShipment(aircraft, AircraftMaintenanceBean.MAINT_SHIPMENTDISASSEMBLY, user, false, fromfbo, totaldepartcost);
            doMaintenanceAircraftShipment(aircraft, AircraftMaintenanceBean.MAINT_SHIPMENTREASSEMBLY, user, true, tofbo, totaldestcost);

            //remove aircraft from use and set shipping details
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration = '" + aircraft.getRegistration() + "'");

            rs.next();

            //set state to disassembly
            rs.updateInt("shippingState", 1);

            //compute the time for disassembly
            Date date = new Date();
            Date shippingNext = new Date(date.getTime() + (acShippingInfo.getShippingStateDelay() * 1000));
            Timestamp ts = new Timestamp(shippingNext.getTime());
            rs.updateTimestamp("shippingStateNext", ts);

            //set who shipped the aircraft
            rs.updateInt("ShippedBy", user.getId());

            //set where its being shipped
            rs.updateString("shippingTo", shipto);

            //Remove fuel
            aircraft.emptyAllFuel();
            aircraft.writeFuel(rs);

            rs.updateRow();
            rs.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static AircraftBean getAircraftForUser(int userId)
    {
        List<AircraftBean> result = getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND userlock=" + userId);
        return result.size() == 0 ? null : result.get(0);
    }//Modified to add Lessor by Airboss 5/8/11

    public static List<AircraftBean> getAircraftOwnedByUser(int userId)
    {
        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND (owner=" + userId + " OR lessor=" + userId + ") ORDER BY make,models.model");
    }

    public static List<AircraftBean> getAircraftInArea(String location, List<CloseAirport> locations)
    {
        StringBuilder where = new StringBuilder("'" + location + "'");

        for (CloseAirport location1 : locations)
        {
            where.append(", '" + location1.icao + "'");
        }

        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND location in (" + where.toString() + ")");
    }

    public static List<AircraftBean> getAircraftOfTypeInArea(String location, List<CloseAirport> locations, int type)
    {
        StringBuilder where = new StringBuilder("'" + location + "'");
        for (CloseAirport location1 : locations)
        {
            where.append(", '" + location1.icao + "'");
        }

        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND models.id = " + type + " AND location in (" + where.toString() + ")");
    }

    public static List<AircraftBean> getAircraft(String location)
    {
        return getAircraftSQL("SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND location='" + location + "'" + "ORDER BY make,models.model");
    }

    /**
     * Finds all aircraft for sale that match user supplied parameters
     *
     * @param modelId
     * @param lowPrice
     * @param highPrice
     * @param lowTime
     * @param highTime
     * @param lowPax
     * @param highPax
     * @param lowLoad
     * @param highLoad
     * @param distance
     * @param fromParam
     * @param hasIfr
     * @param hasAp
     * @param hasGps
     * @param isSystemOwned
     * @param isPlayerOwned
     * @param equipment
     * @return AircraftBean List
     */
    public static List<AircraftBean> findAircraftForSale(int modelId, int lowPrice, int highPrice, int lowTime, int highTime, int lowPax, int highPax, int lowLoad, int highLoad, int distance, String fromParam, boolean hasVfr, boolean hasIfr, boolean hasAp, boolean hasGps, boolean isSystemOwned, boolean isPlayerOwned, String equipment) throws DataError
    {
        ArrayList<AircraftBean> result = new ArrayList<AircraftBean>();

        try
        {
            StringBuilder tables = new StringBuilder("aircraft");
            StringBuilder where = new StringBuilder(" WHERE aircraft.model = models.id AND sellPrice is not null ");
            StringBuilder query = new StringBuilder("SELECT * FROM ");
            StringBuilder query2 = new StringBuilder("SELECT DISTINCT location, lat, lon FROM aircraft, models, airports ");

            tables.append(", models");

            // Construct equipment code to reflect installed equipment
            if (!equipment.equals("all"))
            {
                int equipmentCode = 0;

                if (equipment.equals("vfrOnly"))
                {
                    where.append("AND aircraft.equipment = ");
                    where.append(equipmentCode);
                    where.append(" ");
                }
                else
                { // equipment.equals("equipmentList")
                    if (hasIfr)
                    {
                        equipmentCode += ModelBean.EQUIPMENT_IFR_MASK;
                    }
                    if (hasAp)
                    {
                        equipmentCode += ModelBean.EQUIPMENT_AP_MASK;
                    }
                    if (hasGps)
                    {
                        equipmentCode += ModelBean.EQUIPMENT_GPS_MASK;
                    }

                    if (hasVfr)
                    {
                        where.append("AND (aircraft.equipment & ");
                        where.append(equipmentCode);
                        where.append(" ) ='");
                        where.append(equipmentCode);
                        where.append("' AND (aircraft.equipment & ");
                        where.append(ModelBean.EQUIPMENT_IFR_MASK);
                        where.append(" ) = 0 ");
                    }
                    else
                    {
                        where.append("AND (aircraft.equipment & ");
                        where.append(equipmentCode);
                        where.append(" ) ='");
                        where.append(equipmentCode);
                        where.append("' ");
                    }
                }
            }

            if (modelId > 0)
            {
                where.append("AND models.id = ");
                where.append(modelId);
                where.append(" ");
            }

            if (lowPrice != -1)
            {
                where.append("AND sellPrice >= ");
                where.append(lowPrice);
                where.append(" ");
            }

            if (highPrice != -1)
            {
                where.append("AND sellPrice <= ");
                where.append(highPrice);
                where.append(" ");
            }

            if (lowPax != -1)
            {
                where.append("AND (seats-IF(crew>2,2,crew)) >= ");
                where.append(lowPax);
                where.append(" ");
            }

            if (highPax != -1)
            {
                where.append("AND (seats-IF(crew>2,2,crew)) <= ");
                where.append(highPax);
                where.append(" ");
            }

            if (lowLoad != -1)
            {
                where.append("AND (maxWeight-emptyWeight) >= ");
                where.append(lowLoad);
                where.append(" ");
            }

            if (highLoad != -1)
            {
                where.append("AND (maxWeight-emptyWeight) <= ");
                where.append(highLoad);
                where.append(" ");
            }

            if ((lowTime == -1 || lowTime == 0) && highTime != -1)
            {
                where.append("AND (airframe <= ");
                where.append(highTime * 3600);
                where.append(" OR airframe IS NULL) ");
            }
            else
            {
                if (lowTime != -1)
                {
                    where.append("AND airframe >= ");
                    where.append(lowTime * 3600);
                    where.append(" ");
                }

                if (highTime != -1)
                {
                    where.append("AND airframe <= ");
                    where.append(highTime * 3600);
                    where.append(" ");
                }
            }

            if (isSystemOwned != isPlayerOwned)
            {
                if (isSystemOwned)
                {
                    where.append("AND owner = 0 ");
                }

                if (isPlayerOwned)
                {
                    where.append("AND owner != 0 ");
                }
            }

            query.append(tables);
            query.append(where);
            query.append("ORDER BY models.make, models.model, aircraft.sellPrice");
            query2.append(where);
            query2.append(" AND icao = location");

            double lat = 0, lon = 0;

            if (fromParam != null)
            {
                //String qry = "SELECT lat, lon FROM airports WHERE icao = ?";
                //ResultSet rsAp = dalHelper.ExecuteReadOnlyQuery(qry, fromParam);
                //if (!rsAp.next())
                //	throw new DataError("Airport " + fromParam + " not found.");
                LatLonSize lls = Airports.cachedAPs.get(fromParam.toUpperCase());
                if (lls == null)
                {
                    throw new DataError("Airport " + fromParam.toUpperCase() + " not found.");
                }

                lat = lls.lat;
                lon = lls.lon;
            }

            Map<String, Double> distanceMap = new HashMap<String, Double>();
            double lat1, lon1;

            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(query2.toString());
            while (rs.next())
            {
                lat1 = rs.getDouble(2);
                lon1 = rs.getDouble(3);

                distanceMap.put(rs.getString(1), Airports.getDistance(lat1, lon1, lat, lon));
            }

            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(query.toString());
            while (rs.next())
            {
                AircraftBean aircraft = new AircraftBean(rs);

                // not searching with a distance parameter, just add aircraft to result.
                if (fromParam == null)
                {
                    result.add(aircraft);
                }
                else // searching with a distance parameter
                {
                    if (aircraft.getLocation() != null && distanceMap.get(aircraft.getLocation()) < distance)
                    {
                        result.add(aircraft);
                    }
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * @param modelId aircraft model type
     * @return count of aircraft available for sale in system now for given model type
     */
    public static int FindAircraftForSaleByModelCount(int modelId)
    {
        int count = 0;

        try
        {
            String qry = "SELECT count(*) FROM aircraft, models WHERE aircraft.model = models.id AND sellPrice is not null AND (aircraft.equipment & 0 ) ='0' AND models.id = ?";
            count = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), modelId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return count;
    }

    public static List<AircraftBean> getAircraftSQL(String qry)
    {
        ArrayList<AircraftBean> result = new ArrayList<AircraftBean>();
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                AircraftBean aircraft = new AircraftBean(rs);
                result.add(aircraft);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void rentAircraft(String reg, int user, boolean rentedDry) throws DataError
    {
        try
        {
            //get aircraft info
            String qry = "SELECT * FROM aircraft, models WHERE aircraft.model = models.id AND registration = ?";
            ResultSet aircraftRS = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, reg);

            if (!aircraftRS.next())
            {
                throw new DataError("No aircraft found!");
            }

            AircraftBean aircraft = new AircraftBean(aircraftRS);

            //get renter info
            qry = "SELECT * FROM accounts WHERE id = ?";
            ResultSet renterRS = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user);

            if (!renterRS.next())
            {
                throw new DataError("user not found!");
            }

            UserBean renter = new UserBean(renterRS);
            Accounts.reloadMemberships(renter);

            //get owner of aircraft info
            qry = "SELECT * FROM accounts WHERE id = ?";
            ResultSet ownerRS = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, aircraft.getOwner());

            if (!ownerRS.next())
            {
                throw new DataError("owner not found!");
            }

            UserBean owner = new UserBean(ownerRS);

            //compare the renter to the values in the owner's BanList
            if (owner.isInBanList(renter.getName()))
            {
                throw new DataError("The owner [" + owner.getName() + "] has indicated that you are not permitted to rent aircraft from them. " +
                        "If you wish to contact them about this issue, you must do so privately. " +
                        "You may use the forum PM (Private Message) system at <a href='http://www.fseconomy.net/inbox'> http://www.fseconomy.net/inbox</a> if you have no other contact means. " +
                        "<b>DO NOT</b> post any public static message about this issue in the forums.");
            }

            //The following check allows ALLIN only aircraft to be rented, bypassing the exploit canceling code
            if ((aircraft.getRentalPriceDry() + aircraft.getRentalPriceWet() == 0) && !(aircraft.canAlwaysRent(renter)))
            {
                ModelBean mb = Models.getModelById(aircraft.getModelId());
                if (mb.getFuelSystemOnly() == 0) // 0 == can be fueled, so not an ALLIN limited aircraft
                {
                    throw new DataError("Rental not authorized");
                }
            }

            //normal flow for renting a plane begins
            qry = "SELECT (count(*) > 0) AS Found FROM aircraft WHERE userlock = ?";
            boolean found = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
            if (found)
            {
                throw new DataError("There is already an aircraft selected.");
            }

            qry = "SELECT (count(*) = 0) AS rented FROM aircraft WHERE userlock is null AND registration = ?";
            boolean rented = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), reg);
            if (rented)
            {
                throw new DataError("Aircraft is already locked.");
            }

            qry = "SELECT * FROM aircraft, models WHERE userlock is null AND location is not null AND aircraft.model = models.id AND registration = ?";
            ResultSet fuelRS = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, reg);
            fuelRS.next();
            AircraftBean thisCraft = new AircraftBean(fuelRS);

            Float initialFuel = rentedDry ? (float) thisCraft.getTotalFuel() : null;

            qry = "UPDATE aircraft SET userlock = ?, lockedSince = ?, initialFuel = ? where registration = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, user, new Timestamp(GregorianCalendar.getInstance().getTime().getTime()), initialFuel, reg);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void releaseAircraft(String reg, int user) throws DataError
    {
        try
        {
            String qry = "SELECT (count(registration) > 0) AS found FROM aircraft WHERE location is not null AND registration = ? AND userlock = ?";
            boolean found = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), reg, user);
            if (!found)
            {
                return;
            }

            qry = "SELECT location FROM aircraft WHERE location is not null AND registration = ? AND userlock = ?";
            String location = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), reg, user);

            if (location == null)
            {
                throw new DataError("No aircraft to cancel.");
            }

            qry = "UPDATE aircraft SET holdRental=0, userlock = ?, lockedSince = ?, initialFuel = ? WHERE registration = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, null, null, null, reg);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean setHoldRental(String reg, int userId, boolean hold)
    {
        boolean result = false;
        try
        {
            String qry = "UPDATE aircraft SET holdRental = ? WHERE registration = ? and userlock = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, hold, reg, userId);
            result = true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean aircraftOk(AircraftBean bean, String aircraft)
    {
        boolean result = false;
        try
        {
            Timestamp now = new Timestamp(GregorianCalendar.getInstance().getTime().getTime());

            String qry = "UPDATE fsmappings SET lastused = ? WHERE fsaircraft=? AND model = ?";
            int numrecs = DALHelper.getInstance().ExecuteUpdate(qry, now, aircraft, bean.getModelId());

            if (numrecs == 1)
            {
                result = true;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public static void defuelAircraft(AircraftBean aircraft, int userid, int amount) throws DataError
    {
        UserBean user = Accounts.getAccountById(userid);
        Accounts.reloadMemberships(user);
        ModelBean mb = Models.getModelById(aircraft.getModelId());
        if (!aircraft.changeAllowed(user) && mb.getFuelSystemOnly() != 1)
        {
            throw new DataError("Only the owner or group staff may defuel.");
        }

        if (amount < 0)
        {
            amount = 0;
        }

        if (amount >= aircraft.getTotalFuel())
        {
            return;
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            aircraft.emptyAllFuel();
            aircraft.addFuel(amount);
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration='" + aircraft.getRegistration() + "' AND userlock=" + user.getId());
            if (rs.next())
            {
                aircraft.writeFuel(rs);
                rs.updateRow();
            }

            rs.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void refuelAircraft(String reg, int user, int amount, int provider, int type) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        AircraftBean aircraft = getAircraftByRegistration(reg);
        UserBean pilot = Accounts.getAccountById(user);
        String location;

        if (aircraft == null)
        {
            throw new DataError("Aircraft not found.");
        }

        location = aircraft.getLocation();
        if (location == null)
        {
            throw new DataError("Cannot refuel while aircraft is in the air.");
        }

        if (aircraft.getUserLock() != user)
        {
            throw new DataError("Permission denied.");
        }

        if (provider == -2)
        {
            defuelAircraft(aircraft, user, amount);
            return;
        }

        if (aircraft.getTotalCapacity() <= aircraft.getTotalFuel())
        {
            throw new DataError("Aircraft is already filled up.");
        }

        double fuelBefore = aircraft.getTotalFuel();
        if (fuelBefore >= amount)
        {
            return;
        }

        int capacity = aircraft.getTotalCapacity();
        if (amount > capacity)
        {
            amount = capacity;
        }

        aircraft.addFuel(amount);

        FboBean fbo = null;
        double added = amount - fuelBefore;
        int kg = (int) Math.floor(Constants.GALLONS_TO_KG * added);
        int fboId = -1;
        if (provider > 0)                            // Refuel from FBO
        {
            fbo = Fbos.getFbo(provider);
            fboId = fbo.getId();
            GoodsBean fuel = Goods.getGoods(location, fbo.getOwner(), GoodsBean.GOODS_FUEL100LL);
            if (type > 0)
            {
                fuel = Goods.getGoods(location, fbo.getOwner(), GoodsBean.GOODS_FUELJETA);
            }

            if (fuel == null || fuel.getAmount() < kg)
            {
                throw new DataError("Not enough fuel available.");
            }
        }
        else if (provider == -1)                    // Refuel from private drums
        {
            GoodsBean fuel = Goods.getGoods(location, user, GoodsBean.GOODS_FUEL100LL);
            if (type > 0)
            {
                fuel = Goods.getGoods(location, user, GoodsBean.GOODS_FUELJETA);
            }

            if (fuel == null || fuel.getAmount() < kg)
            {
                throw new DataError("Not enough fuel available.");
            }
        }

        try
        {
            conn = DALHelper.getInstance().getConnection();

            double fuelPrice;
            switch (provider)
            {
                case -1:
                    fuelPrice = 0.0;
                    break;
                case 0:
                    fuelPrice = Goods.getFuelPrice(location);
                    if (type > 0)
                    {
                        fuelPrice = Goods.getFuelPrice(location) * Goods.getJetaMultiplier();
                    }
                    break;
                default:
                    if (aircraft.getOwner() > 0) // System aircraft always pay bucket rate
                    {
                        fuelPrice = fbo.getFuelByType(type);
                    }
                    else
                    {
                        fuelPrice = Goods.getFuelPrice(location);
                        if (type > 0)
                        {
                            fuelPrice = Goods.getFuelPrice(location) * Goods.getJetaMultiplier();
                        }
                    }
                    break;
            }

            float cost = (float) (added * fuelPrice);

            //Owner have enough cash to pay for fuel?
            UserBean account = Accounts.getAccountById(aircraft.getOwner());
            if (cost > account.getMoney())
            {
                throw new DataError("Aircraft owner does not have enough cash money to purchase fuel amount requested");
            }

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration='" + reg + "' AND userlock=" + user);
            if (rs.next())
            {
                aircraft.writeFuel(rs);
                rs.updateRow();
            }
            rs.close();
            rs = null;

            if (provider > 0)
            {
                if (type < 1)
                {
                    Goods.changeGoodsRecord(location, GoodsBean.GOODS_FUEL100LL, fbo.getOwner(), -kg, false);
                }
                else
                {
                    Goods.changeGoodsRecord(location, GoodsBean.GOODS_FUELJETA, fbo.getOwner(), -kg, false);
                }
            }
            else if (provider == -1)
            {
                if (type < 1)
                {
                    Goods.changeGoodsRecord(location, GoodsBean.GOODS_FUEL100LL, user, -kg, false);
                }
                else
                {
                    Goods.changeGoodsRecord(location, GoodsBean.GOODS_FUELJETA, user, -kg, false);
                }
            }

            if (cost > 0)
            {
                rs = stmt.executeQuery("SELECT * from log where 1=2");
                rs.moveToInsertRow();
                rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
                rs.updateString("aircraft", reg);
                rs.updateString("user", pilot.getName());
                rs.updateString("type", "refuel");
                rs.updateFloat("fuelCost", cost);
                rs.updateInt("fbo", provider);
                rs.insertRow();
                rs.last();
                int logId = rs.getInt("id");
                rs.close();
                rs = null;

                String comment = "User ID: " + user + " Amount (gals): " + Formatters.oneDecimal.format(added) + ", $ per Gal: " + Formatters.currency.format(fuelPrice);
                if (type < 1)
                {
                    Banking.doPayment(aircraft.getOwner(), fbo == null ? 0 : fbo.getOwner(), cost, PaymentBean.REASON_REFUEL, logId, fboId, location, reg, comment, false);
                }
                else
                {
                    Banking.doPayment(aircraft.getOwner(), fbo == null ? 0 : fbo.getOwner(), cost, PaymentBean.REASON_REFUEL_JETA, logId, fboId, location, reg, comment, false);
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void buyAircraft(String aircraft, int account, UserBean user) throws DataError
    {
        if (user.getId() != account && user.groupMemberLevel(account) < UserBean.GROUP_STAFF)
        {
            throw new DataError("Permission denied");
        }

        try
        {
            String qry = "SELECT * from aircraft WHERE sellPrice > 0 AND registration = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, aircraft);
            if (rs.next())
            {
                int sellPrice = rs.getInt("sellPrice");
                int oldOwner = rs.getInt("owner");
                String location = rs.getString("location");

                if (!Banking.checkFunds(account, sellPrice))
                {
                    throw new DataError("Not enough money to buy aircraft");
                }

                qry = "UPDATE aircraft SET owner = ?, sellPrice = null, marketTimeout = null where registration = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, account, aircraft);

                Banking.doPayment(account, oldOwner, sellPrice, PaymentBean.AIRCRAFT_SALE, 0, -1, location, aircraft, "", false);
            }
            else
            {
                throw new DataError("Aircraft not found");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void sellAircraft(String reg, UserBean user) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DALHelper.getInstance().getConnection();

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from aircraft WHERE registration = '" + reg + "'");
            if (rs.next())
            {
                AircraftBean aircraft = getAircraftByRegistration(reg);

                if (aircraft == null)
                {
                    throw new DataError("Aircraft not found.");
                }

                if (!aircraft.changeAllowed(user))
                {
                    throw new DataError("Not your aircraft.");
                }

                if (aircraft.isBroken())
                {
                    throw new DataError("The Bank of FSE does not buy broken aircraft.");
                }

                int sellPrice = aircraft.getMinimumPrice();
                int oldOwner = rs.getInt("owner");
                String location = rs.getString("location");
                rs.updateInt("owner", 0);
                rs.updateNull("lessor");
                rs.updateInt("advertise", 0);
                rs.updateNull("sellPrice");
                rs.updateNull("bonus");
                rs.updateNull("maxRentTime");
                rs.updateNull("accounting");

                // Randomize rent
                ModelBean model = Models.getModelById(rs.getInt("model"));
                int equipment = rs.getInt("equipment");
                int rent = model.getTotalRentalTarget(equipment);
                rent *= 1 + (Math.random() * 0.40) - 0.2;
                rs.updateInt("RentalDry", rent);
                String home = rs.getString("home");
                int fuelCost = (int) Math.round(model.getGph() * Goods.getFuelPrice(home));
                rs.updateInt("RentalWet", fuelCost + rent);
                rs.updateRow();
                rs.close();
                rs = null;

                Banking.doPayment(0, oldOwner, sellPrice, PaymentBean.AIRCRAFT_SALE, 0, -1, location, reg, "", false);
            }
            else
            {
                throw new DataError("Aircraft not found.");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void addAircraftDamage(String aircraft, int engine, int parameter, int value)
    {
        if (value == 0)
        {
            return;
        }

        try
        {
            String qry = "SELECT (count(aircraft) > 0) AS found from damage WHERE aircraft = ? AND engine = ? AND parameter = ?";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft, engine, parameter);
            if (exists)
            {
                qry = "UPDATE damage SET value = value + ? WHERE aircraft = ? AND engine = ? and parameter = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, value, aircraft, engine, parameter);
            }
            else
            {
                qry = "INSERT INTO damage (aircraft, engine, parameter, value) VALUES(?,?,?,?)";
                DALHelper.getInstance().ExecuteUpdate(qry, aircraft, engine, parameter, value);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateAircraft(AircraftBean aircraft, String newRegistration, UserBean user) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DALHelper.getInstance().getConnection();

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from aircraft WHERE registration = '" + aircraft.getRegistration() + "'");
            if (!rs.next())
            {
                throw new DataError("No aircraft found.");
            }

            if (rs.getInt("userlock") > 0)
            {
                throw new DataError("Aircraft is rented.");
            }

            aircraft.setLocation(rs.getString("location"));
            aircraft.setOwner(rs.getInt("owner"));
            aircraft.setLessor(rs.getInt("lessor"));    //Added by Airboss 5/8/11
            aircraft.setEquipment(rs.getInt("equipment"));

            if (!aircraft.changeAllowed(user))
            {
                throw new DataError("Permission denied");
            }

            if (newRegistration != null && getAircraftByRegistration(newRegistration) == null)
            {
                throw new DataError("Registration already in use.");
            }

            double[] distanceFromHome = Airports.getDistanceBearing(aircraft.getLocation(), aircraft.getHome());
            aircraft.setDistance((int) Math.round(distanceFromHome[0]));
            aircraft.setBearing((int) Math.round(distanceFromHome[1]));
            aircraft.writeBean(rs);
            if (newRegistration != null)
            {
                newRegistration = newRegistration.trim();
                PreparedStatement logUpdate = conn.prepareStatement("UPDATE log SET aircraft = ? WHERE aircraft = ?");
                logUpdate.setString(1, newRegistration);
                logUpdate.setString(2, aircraft.getRegistration());
                logUpdate.execute();
                logUpdate.close();
                PreparedStatement paymentsUpdate = conn.prepareStatement("UPDATE payments SET aircraft = ? WHERE aircraft = ?");
                paymentsUpdate.setString(1, newRegistration);
                paymentsUpdate.setString(2, aircraft.getRegistration());
                paymentsUpdate.execute();
                paymentsUpdate.close();
                PreparedStatement damageUpdate = conn.prepareStatement("UPDATE damage SET aircraft = ? WHERE aircraft = ?");
                damageUpdate.setString(1, newRegistration);
                damageUpdate.setString(2, aircraft.getRegistration());
                damageUpdate.execute();
                damageUpdate.close();

                rs.updateString("registration", newRegistration);
            }
            rs.updateRow();
            rs.close();
            rs = null;

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    /**
     * Aircraft shipping
     *
     * @param reg              - Aircraft registration to finalize shipment
     * @param resetdepart      - indicates if the aircraft should returned to its departure location
     * @param deleteassignment - Indicates if we should also make sure the assignment is removed
     *                         Airboss 12/26/10
     */
    public static void finalizeAircraftShipment(String reg, boolean resetdepart, boolean deleteassignment)
    {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            //***********
            //Remember, aircraft is BEFORE any changes that follow, as we are in a transaction!!
            //***********
            AircraftBean aircraft = getAircraftShippingInfoByRegistration(reg);

            rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration='" + reg + "'");

            if (rs.next())
            {
                Date shippingNext = new Date(new Date().getTime() + (aircraft.getShippingStateDelay() * 1000));
                Timestamp ts = new Timestamp(shippingNext.getTime());
                rs.updateTimestamp("shippingStateNext", ts);

                rs.updateInt("shippingState", 3);

                //if not admin reset to departure, set the aircraft to its shipped to location
                if (!resetdepart)
                {
                    rs.updateString("location", aircraft.getShippingTo());
                }

                rs.updateRow();
                rs.close();
            }
            rs = stmt.executeQuery("SELECT * FROM log WHERE type='maintenance' and subtype=" + AircraftMaintenanceBean.MAINT_SHIPMENTREASSEMBLY + " and aircraft='" + reg + "' order by id desc");

            if (rs.next())
            {
                rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
                rs.updateRow();
                rs.close();
            }

            stmt.close();

            //if we are calling from admin page, check if we need to remove any assigments
            if (deleteassignment)
            {
                //For completed Assignments, remove them
                stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                stmt.executeUpdate("DELETE from assignments WHERE commodityid=99 AND commodity like '%" + aircraft.getRegistration() + "%'");
                stmt.close();
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void doEquipment(AircraftBean aircraft, int equipmentType, FboBean fbo) throws DataError
    {
        // Get a default FBO if none is specified.
        if (fbo == null)
        {
            String location = aircraft.getLocation();
            if (location == null)
            {
                return;
            }

            AirportBean airport = Airports.getAirport(aircraft.getLocation());
            List<FboBean> fbos = Fbos.getFboForRepair(airport, Fbos.FBO_EQUIPMENT_MARGIN);

            if (fbos.size() == 0)
            {
                return;
            }

            fbo = fbos.get(0);
        }

        try
        {
            int price = aircraft.getEquipmentPriceFBO(equipmentType, fbo);
            UserBean owner = Accounts.getAccountById(aircraft.getOwner());

            if (owner == null)
            {
                throw new DataError("Owner not found.");
            }

            if (owner.getMoney() < price)
            {
                throw new DataError("Not enough money.");
            }

            String qry = "update aircraft set equipment = equipment|? where registration = ?";
            if (DALHelper.getInstance().ExecuteUpdate(qry, equipmentType, aircraft.getRegistration()) != 1)
            {
                throw new DataError("Aircraft not found.");
            }

            double factor = 1 + fbo.getEquipmentInstallMargin() / 100.0;

            Banking.doPayment(aircraft.getOwner(), fbo.getOwner(), price, PaymentBean.EQUIPMENT, 0, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);
            Banking.doPayment(fbo.getOwner(), 0, (float) (price / factor), PaymentBean.EQUIPMENT_FBO_COST, 0, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static AircraftAlias[] getAircraftAliasesOld()
    {
        ArrayList<AircraftAlias> result = new ArrayList<>();
        String qry;
        ResultSet rs;

        try
        {
            qry = "SELECT fsaircraft, models.make, models.model FROM fsmappings, models WHERE fsmappings.model = models.id ORDER BY models.make, models.model, fsaircraft";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);

            while (rs.next())
            {
                AircraftAlias aircraft = new AircraftAlias(rs.getString(1), rs.getString(2) + " " + rs.getString(3));
                result.add(aircraft);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result.toArray(new AircraftAlias[result.size()]);
    }

    public static Map<String, Set<String>> getAircraftAliases()
    {
        Map<String, Set<String>> result = new TreeMap<String, Set<String>>();
        String qry;
        ResultSet rs = null;

        try
        {
            qry = "SELECT models.make, models.model, models.id FROM models ORDER BY models.make, models.model";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                Set<String> aliases = new HashSet<String>();
                result.put(rs.getString(1) + " " + rs.getString(2), aliases);

                qry = "SELECT fsaircraft FROM fsmappings WHERE  model=? ORDER BY fsaircraft";
                ResultSet rsAlias = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, rs.getInt(3));
                while (rsAlias.next())
                {
                    aliases.add(rsAlias.getString(1));
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static AircraftConfigs getAircraftConfigs(int modelid)
    {
        ResultSet rs;
        AircraftConfigs aircraft = null;
        try
        {
            String qry = "SELECT make, model, crew, fueltype, seats, cruisespeed, " +
                    "fcapExt1, fcapLeftTip, fcapLeftAux, fcapLeftMain, " +
                    "fcapCenter, fcapCenter2, fcapCenter3, fcapRightMain, " +
                    "fcapRightAux, fcapRightTip, fcapExt2, " +
                    "gph, maxWeight, emptyWeight, price, engines, engineprice, canShip, fcaptotal " +
                    "FROM models WHERE id=? ORDER BY make, model";

            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, modelid);
            if (rs.next())
            {
                aircraft = new AircraftConfigs(rs.getString(1) + " " + rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13), rs.getInt(14), rs.getInt(15), rs.getInt(16), rs.getInt(17), rs.getInt(18), rs.getInt(19), rs.getInt(20), rs.getInt(21), rs.getInt(22), rs.getInt(23), rs.getBoolean(24), (int) rs.getDouble(25));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return aircraft;
    }

    public static List<AircraftConfigs> getAircraftConfigs()
    {
        ArrayList<AircraftConfigs> result = new ArrayList<AircraftConfigs>();
        ResultSet rs;

        try
        {
            String qry = "SELECT make, model, crew, fueltype, seats, cruisespeed, " +
                    "fcapExt1, fcapLeftTip, fcapLeftAux, fcapLeftMain, " +
                    "fcapCenter, fcapCenter2, fcapCenter3, fcapRightMain, " +
                    "fcapRightAux, fcapRightTip, fcapExt2, " +
                    "gph, maxWeight, emptyWeight, price, engines, engineprice, canShip, fcaptotal " +
                    "FROM models ORDER BY make, model";

            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                AircraftConfigs aircraft = new AircraftConfigs(rs.getString(1) + " " + rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13), rs.getInt(14), rs.getInt(15), rs.getInt(16), rs.getInt(17), rs.getInt(18), rs.getInt(19), rs.getInt(20), rs.getInt(21), rs.getInt(22), rs.getInt(23), rs.getBoolean(24), (int) rs.getDouble(25));
                result.add(aircraft);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Probe the database for an aircraft with a specific title. If found, return the nearest aircraft of this type
     *
     * @param aircraft            The title of the aircraft
     * @param airport             closeAirport object
     * @param airportList         The list to fill with aircraft
     * @param currentAirport      A list that will be filled with the airport found at the specified location
     * @param alternativeAircraft A list that will be filled with the aircraft found at the specified location
     * @return The name of the model, Null if aircraft title is unknown.
     */
    public static String probeAircraft(String aircraft, CloseAirport airport, List<CloseAirport> airportList, List<AirportBean> currentAirport, List<AircraftBean> alternativeAircraft)
    {
        String aircraftModelName = null;
        int modelId = -1;

        try
        {
            String qry = "SELECT models.id, models.make, models.model FROM fsmappings, models where models.id = fsmappings.model AND fsaircraft= ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, aircraft);
            if (rs.next())
            {
                modelId = rs.getInt(1);
                aircraftModelName = rs.getString(2) + " " + rs.getString(3);
            }

            AirportBean thisAirport = Airports.getAirport(airport.icao);
            thisAirport.setFuelPrice(Goods.getFuelPrice(thisAirport.getIcao()));
            currentAirport.add(thisAirport);
            if (modelId != -1)
            {
                HashMap<String, CloseAirport> airportMap = new HashMap<String, CloseAirport>();
                List<CloseAirport> closeAirports = Airports.fillCloseAirports(airport.icao, 0, 100);
                for (CloseAirport closeAirport : closeAirports)
                {
                    airportMap.put(closeAirport.icao.toLowerCase(), closeAirport);
                }

                List<AircraftBean> areaAircraft = getAircraftOfTypeInArea(airport.icao, closeAirports, modelId);
                Set<CloseAirport> airportSet = new HashSet<CloseAirport>();
                for (AircraftBean anAreaAircraft : areaAircraft)
                {
                    if (anAreaAircraft.getLocation().toLowerCase().equals(thisAirport.getIcao().toLowerCase()))
                    {
                        airportList.add(new CloseAirport(thisAirport.getIcao(), 0, 0));
                    }
                    else
                    {
                        CloseAirport thisAp = airportMap.get((anAreaAircraft.getLocation().toLowerCase()));

                        if (thisAp != null)
                        {
                            airportList.add(thisAp);
                        }
                    }
                }
                airportList.addAll(airportSet);
            }

            List<AircraftBean> otherAircraft = getAircraft(thisAirport.getIcao());
            for (AircraftBean anOtherAircraft : otherAircraft)
            {
                if (anOtherAircraft.getUserLock() == 0)
                {
                    alternativeAircraft.add(anOtherAircraft);
                }
            }

            return aircraftModelName;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void updateAircraft4Admins(AircraftBean aircraft, String newRegistration) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from aircraft WHERE registration = '" + aircraft.getRegistration() + "'");
            if (!rs.next())
            {
                throw new DataError("No aircraft found.");
            }

            aircraft.setEquipment(rs.getInt("equipment"));
            if (newRegistration != null && getAircraftByRegistration(newRegistration) != null)
            {
                throw new DataError("Registration already in use.");
            }

            double[] distanceFromHome = Airports.getDistanceBearing(aircraft.getLocation(), aircraft.getHome());
            aircraft.setDistance((int) Math.round(distanceFromHome[0]));
            aircraft.setBearing((int) Math.round(distanceFromHome[1]));
            rs.updateString("home", aircraft.getHome());
            rs.updateString("location", aircraft.getLocation());
            rs.updateInt("owner", aircraft.getOwner());

            if (aircraft.getUserLock() == 0)
            {
                rs.updateNull("userlock");
                rs.updateNull("lockedSince");
            }
            else
            {
                rs.updateInt("userlock", aircraft.getUserLock());
            }
            rs.updateInt("bonus", aircraft.getBonus());
            rs.updateInt("accounting", aircraft.getAccounting());
            rs.updateInt("rentalDry", aircraft.getRentalPriceDry());
            rs.updateInt("rentalWet", aircraft.getRentalPriceWet());
            rs.updateInt("maxRentTime", aircraft.getMaxRentTime());
            rs.updateInt("equipment", aircraft.getEquipment());
            rs.updateInt("advertise", aircraft.getAdvertise());
            rs.updateInt("allowFix", aircraft.getAllowFix());
            if (aircraft.getHome().equals(aircraft.getLocation()))
            {
                rs.updateNull("bearingToHome");
                rs.updateInt("distanceFromHome", 0);
            }
            else
            {
                rs.updateInt("bearingToHome", aircraft.getBearing());
                rs.updateInt("distanceFromHome", aircraft.getDistance());
            }

            if (aircraft.getSellPrice() != 0)
            {
                rs.updateInt("sellPrice", aircraft.getSellPrice());
            }
            else
            {
                rs.updateNull("sellPrice");
            }

            if (newRegistration != null)
            {
                newRegistration = newRegistration.trim();
                PreparedStatement logUpdate = conn.prepareStatement("UPDATE log SET aircraft = ? WHERE aircraft = ?");
                logUpdate.setString(1, newRegistration);
                logUpdate.setString(2, aircraft.getRegistration());
                logUpdate.execute();
                logUpdate.close();
                PreparedStatement paymentsUpdate = conn.prepareStatement("UPDATE payments SET aircraft = ? WHERE aircraft = ?");
                paymentsUpdate.setString(1, newRegistration);
                paymentsUpdate.setString(2, aircraft.getRegistration());
                paymentsUpdate.execute();
                paymentsUpdate.close();
                PreparedStatement damageUpdate = conn.prepareStatement("UPDATE damage SET aircraft = ? WHERE aircraft = ?");
                damageUpdate.setString(1, newRegistration);
                damageUpdate.setString(2, aircraft.getRegistration());
                damageUpdate.execute();
                damageUpdate.close();

                rs.updateString("registration", newRegistration);
            }

            rs.updateRow();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static boolean checkAllInAircraftWithOutAssigment(AircraftBean aircraft) throws DataError
    {
        return Assignments.checkAllInAircraftWithOutAssigment(aircraft);
    }

    public static boolean checkAllInFlightWithAssignment(AircraftBean aircraft) throws DataError
    {
        return Assignments.checkAllInFlightWithAssignment(aircraft);
    }

    public static String getAircraftMakeModel(String reg)
    {
        String result = null;

        try
        {
            String qry = "SELECT CONCAT(m.make, ' ', m.model) FROM aircraft a, models m  WHERE a.model=m.id AND registration = ?;";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), reg);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void doMaintenance(AircraftBean aircraft, int maintenanceType, UserBean user, FboBean fbo) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Statement damageStmt = null;
        Statement maintenanceStmt = null;
        ResultSet damage = null;
        ResultSet maintenance = null;

        // Get a Default FBO if none is specified
        if (fbo == null)
        {
            String location = aircraft.getLocation();
            if (location == null)
                return;

            AirportBean airport = Airports.getAirport(aircraft.getLocation());
            List<FboBean> fbos = Fbos.getFboForRepair(airport, Fbos.FBO_ID);

            if (fbos.size() == 0)
                return;

            fbo = fbos.get(0);
        }

        try
        {
            int logId;

            conn = DALHelper.getInstance().getConnection();

            // First call for the maintenance price
            int price = aircraft.getMaintenancePrice(maintenanceType, fbo);
            int repairmargin = fbo.getRepairShopMargin();

            // Prevent system planes from being repaired when FBO Repair Margin  > system default FBO Repair Margin.
            if (aircraft.getOwner() == 0 && repairmargin > FboBean.FBO_DEFAULT_REPAIRSHOPMARGIN)
                return;

            if (aircraft.getOwner() > 0)
            {
                UserBean owner = Accounts.getAccountById(aircraft.getOwner());

                if (owner != null && owner.getMoney() < price)
                    throw new DataError("Not enough money.");
            }

            //To close possible maintenace exploit
            if (!aircraft.getLocation().equals(fbo.getLocation()))
                throw new DataError("The Aircraft Location and the FBO Location are not the same.");

            damageStmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            damage = damageStmt.executeQuery("SELECT * FROM damage WHERE aircraft = '" + aircraft.getRegistration() + "'");

            maintenanceStmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            maintenance = maintenanceStmt.executeQuery("SELECT * FROM maintenance");

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            rs = stmt.executeQuery("SELECT * FROM log where 1=2");
            rs.moveToInsertRow();
            rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
            rs.updateString("type", "maintenance");
            rs.updateString("aircraft", aircraft.getRegistration());
            rs.updateInt("totalEngineTime", aircraft.getTotalEngineTime());
            rs.updateInt("subType", maintenanceType);
            rs.updateInt("fbo", fbo.getId());
            rs.insertRow();
            rs.last();
            logId = rs.getInt("id");
            rs.close();

            rs = stmt.executeQuery("SELECT * FROM aircraft WHERE registration = '" + aircraft.getRegistration() + "'");

            if (!rs.next())
                throw new DataError("Aircraft not found.");

            // Second call for the maintenance price - what was actually done
            price = aircraft.performMaintenance(maintenanceType, logId, fbo, rs, damage, maintenance);
            double factor = 1 + fbo.getRepairShopMargin() / 100.0;

            // First call for additional cost during 100-hour
            int[] conditionPrice = aircraft.getConditionPrice(aircraft, maintenanceType);
            int addedPrice=0;
            int condition =0;
            if (maintenanceType != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT)
            {
                for (int i = 0; i < conditionPrice.length; i++)
                {

                    addedPrice += conditionPrice[i];
                }
                price += (addedPrice * factor);
            }
            else  //Emergency Aircraft Repair between $1000-5000, reset condition
            {
                do //reset condition so its not in the 'broken' range
                {
                    condition = (int) (50000 + (59999 - 50000)* Math.random());
                }while(condition > AircraftBean.REPAIR_RANGE_LOW-1 && condition < AircraftBean.REPAIR_RANGE_HIGH+1);
                rs.updateInt("condition", condition);
                rs.updateInt("lastFix", rs.getInt("airframe"));
            }
            rs.updateRow();

            stmt.executeUpdate("UPDATE log SET maintenanceCost = " + price + ", ageECost = " + conditionPrice[0] + ", ageAvCost = " + conditionPrice[1] + ", ageAfCost = " + conditionPrice[2] + ", ageAdwCost = " + conditionPrice[3] + " WHERE id = " + logId);
            Banking.doPayment(aircraft.getOwner(), fbo.getOwner(), price, PaymentBean.MAINTENANCE, logId, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);

            if (maintenanceType != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT)
                Banking.doPayment(fbo.getOwner(), 0, (float) (price / factor), PaymentBean.MAINTENANCE_FBO_COST, logId, fbo.getId(), aircraft.getLocation(), aircraft.getRegistration(), "", false);

            rs.close();

            // clear engine damage and hours if new engine
            if(maintenanceType == AircraftMaintenanceBean.MAINT_REPLACEENGINE)
            {
                String sQuery = "UPDATE damage SET value = 0 WHERE aircraft = '" + aircraft.getRegistration() + "' and 'engine' < 3";
                stmt.executeUpdate(sQuery);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(damage);
            DALHelper.getInstance().tryClose(damageStmt);
            DALHelper.getInstance().tryClose(maintenance);
            DALHelper.getInstance().tryClose(maintenanceStmt);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static void doMaintenanceAircraftShipment(AircraftBean aircraft, int maintenanceType, UserBean user, boolean istofbo, FboBean fbo, double shippingcost) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Statement maintenanceStmt = null;

        try
        {
            int logId;

            conn = DALHelper.getInstance().getConnection();

            // First call for the maintenance price
            maintenanceStmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            rs = stmt.executeQuery("SELECT * FROM log where 1=2");
            rs.moveToInsertRow();
            rs.updateTimestamp("time", new Timestamp(System.currentTimeMillis()));
            rs.updateString("user", user.getName());
            rs.updateString("type", "maintenance");
            rs.updateString("aircraft", aircraft.getRegistration());
            rs.updateInt("subType", maintenanceType);
            rs.updateInt("totalEngineTime", aircraft.getTotalEngineTime());
            rs.updateInt("fbo", fbo.getId());
            rs.insertRow();
            rs.last();
            logId = rs.getInt("id");
            rs.close();

            double factor = 1 + fbo.getRepairShopMargin() / 100.0;

            stmt.executeUpdate("UPDATE log SET maintenanceCost = " + shippingcost + ", ageECost = " + 0 + ", ageAvCost = " + 0 + ", ageAfCost = " + 0 + ", ageAdwCost = " + 0 + " WHERE id = " + logId);

            String comment;
            if(maintenanceType == AircraftMaintenanceBean.MAINT_SHIPMENTDISASSEMBLY)
                comment = "Aircraft shipment disassembly.";
            else
                comment = "Aircraft shipment reassembly.";

            String PayLocation;
            if(istofbo)
                PayLocation = fbo.getLocation();
            else
                PayLocation = aircraft.getLocation();

            Banking.doPayment(aircraft.getOwner(), fbo.getOwner(), shippingcost, PaymentBean.MAINTENANCE, logId, fbo.getId(), PayLocation, aircraft.getRegistration(), comment, false);

            if (maintenanceType != AircraftMaintenanceBean.MAINT_FIXAIRCRAFT)
                Banking.doPayment(fbo.getOwner(), 0, (float) (shippingcost / factor), PaymentBean.MAINTENANCE_FBO_COST, logId, fbo.getId(), PayLocation, aircraft.getRegistration(), comment, false);

            rs.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            DALHelper.getInstance().tryClose(rs);
            DALHelper.getInstance().tryClose(maintenanceStmt);
            DALHelper.getInstance().tryClose(stmt);
            DALHelper.getInstance().tryClose(conn);
        }
    }

    public static AircraftMaintenanceBean getMaintenance(int logId) throws DataError
    {
        try
        {
            LogBean log = Logging.getLogById(logId);

            if (log == null)
                throw new DataError("Maintenance record not found.");

            AircraftBean aircraft = getAircraftByRegistration(log.getAircraft());
            if (aircraft == null)
                throw new DataError("Aircraft not found.");

            FboBean fbo = log.getFbo() == 0 ? FboBean.getInstance() : Fbos.getFbo(log.getFbo());

            if (fbo == null)
                throw new DataError("Repairshop not found.");

            String qry = "SELECT * FROM maintenance WHERE log = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, logId);

            return new AircraftMaintenanceBean(rs, aircraft, log.getMaintenanceCost(), fbo, log.getAgeECost(), log.getAgeAvCost(), log.getAgeAfCost(), log.getAgeAdwCost());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static List<String> getMappingsFilterList()
    {
        List<String> result = new ArrayList<>();

        try
        {
            String startchar = null, endchar = null;
            int groupcount = 0;
            int thiscount;

            String qry = "select substr(fsaircraft, 1, 1) as firstchar, count(*) from fsmappings group by firstchar order by firstchar";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                thiscount = rs.getInt(2);
                if (startchar != null && thiscount + groupcount > 200)
                {
                    if (startchar.equals(endchar))
                        result.add(startchar);
                    else
                        result.add(startchar + ".." + endchar);

                    startchar = null;
                    groupcount = 0;
                }

                groupcount += thiscount;
                endchar = rs.getString(1).toUpperCase();

                if (startchar == null)
                    startchar = endchar;
            }

            if (startchar.equals(endchar))
                result.add(startchar);
            else
                result.add(startchar + ".." + endchar);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<FSMappingBean> getFilteredMappings(String filter)
    {
        if (filter.length() != 1 && filter.length() != 4)
            return null;

        char[] chars = filter.toCharArray();
        String infilter = "";

        if (filter.length() == 4)
        {
            for (int c = (int)chars[0]; c <= (int)chars[3]; c++)
            {
                if (infilter.length() > 0)
                    infilter = infilter + ", ";

                infilter = infilter + "'" + (char)c + "'";
            }
        }
        else
        {
            infilter = "'" + filter + "'";
        }

        return getMappingSQL("SELECT * FROM fsmappings where substr(fsaircraft, 1, 1) in (" + infilter + ") ORDER By fsaircraft");
    }

    public static List<FSMappingBean> getRequestedMappings()
    {
        return getMappingSQL("SELECT * FROM fsmappings WHERE model = 0 ORDER By fsaircraft");
    }

    public static List<FSMappingBean> getMappingById(int id)
    {
        return getMappingSQL("SELECT * FROM fsmappings WHERE id = " + id);
    }

    public static List<FSMappingBean> getMappingByFSAircraft(String target)
    {
        return getMappingSQL("SELECT * FROM fsmappings WHERE fsaircraft LIKE '%" + target + "%' order by fsaircraft");
    }

    public static List<FSMappingBean> getMappingSQL(String qry)
    {
        ArrayList<FSMappingBean> result = new ArrayList<>();

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                FSMappingBean mapping = new FSMappingBean(rs);
                result.add(mapping);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void setMapping(int id, int modelId)
    {
        if(modelId < 0)
            deleteMapping(id);
        else
            updateMapping(id, modelId);
    }

    private static void updateMapping(int id, int modelId)
    {
        try
        {
            String qry = "UPDATE fsmappings SET model = ? WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, modelId, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private static void deleteMapping(int id)
    {
        try
        {
            String qry = "DELETE from fsmappings WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    //	/*
    //	 * Gets the aircraft shipping size from the shipping config table using the aircraft empty weight
    //	 * param emptyweight
    //	 */
    //	public int getAircraftShippingSize(int emptyweight)
    //	{
    //		int shippingSize = 0;
    //
    //		try
    //		{
    //			String qry = "SELECT shippingSize FROM shippingConfigsAircraft WHERE minSize <= ? AND maxSize >= ?";
    //			Object o = dalHelper.ExecuteScalar(qry, emptyweight, emptyweight);
    //			if(o != null && o instanceof Integer)
    //				shippingSize = (Integer)o;
    //		}
    //		catch (SQLException e)
    //		{
    //			e.printStackTrace();
    //		}
    //
    //		return shippingSize;
    //	}
}
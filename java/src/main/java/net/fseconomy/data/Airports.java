package net.fseconomy.data;

import com.google.gson.Gson;
import net.fseconomy.beans.*;
import net.fseconomy.dto.*;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Helpers;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Airports implements Serializable
{

    /**
     * Used to hold an ICAO and lat/lon instance in Hashtable
     * This is initialized in the Data() constructor on startup 
     */
    public static HashMap<String, CachedAirportBean> cachedAirports = new HashMap<>();

    static
    {
        initializeAirportCache();

        //updateBuckets();
    }

    //Moved this here, not even sure it needs to be done at all.
    //    static void updateBuckets()
    //    {
    //        try
    //        {
    //            String qry = "SELECT * FROM airports WHERE bucket is null";
    //            ResultSet rs = dalHelper.ExecuteReadOnlyQuery(qry);
    //            while (rs.next())
    //            {
    //                int newbucket = AirportBean.bucket(rs.getDouble("lat"), rs.getDouble("lon"));
    //                qry = "UPDATE airports set bucket = ? WHERE icao = ?";
    //                dalHelper.ExecuteUpdate(qry, newbucket, rs.getString("icao"));
    //            }
    //        }
    //        catch (SQLException e)
    //        {
    //            e.printStackTrace();
    //        }
    //    }

    public static void initializeAirportCache()
    {
        if (cachedAirports.size() == 0)
        {
            //pull the airports
            try
            {
                String qry = "SELECT * FROM airports";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
                while (rs.next())
                {
                    CachedAirportBean cab = new CachedAirportBean(rs);

                    double price100ll = Goods.getFuelPrice(cab.getIcao());
                    double priceJetA = price100ll * Goods.getJetaMultiplier();
                    cab.setFuelPrice(price100ll, priceJetA);

                    cachedAirports.put(cab.getIcao(), cab);
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static boolean isValidIcao(String icao)
    {
        if(icao.contains("$SINGLE")
            || icao.contains("$NOREVERSE")
            || icao.contains("$FBO")
            || icao.contains("$MILITARY")
            || icao.contains("$WATER"))
            return true;

        return cachedAirports.containsKey(icao);
    }

    public static List<String> CheckValidIcaos(List<String> icaos)
    {
        List<String> badIcaos = new ArrayList<>();

        for(String icao: icaos)
        {
            if(!isValidIcao(icao))
                badIcaos.add(icao);
        }

        return badIcaos;
    }

    public static double getDistance(String from, String to)
    {
        LatLonRadians llTo = cachedAirports.get(from.toUpperCase()).getLatLonRadians();
        LatLonRadians llFrom = cachedAirports.get(to.toUpperCase()).getLatLonRadians();

        DistanceBearing result = getDistanceBearing(llTo, llFrom, true, false);

        return result.distance;
    }

    public static double getDistance(LatLon llFrom, LatLon llTo)
    {
        DistanceBearing result = getDistanceBearing(llFrom, llTo, true, false);

        return result.distance;
    }

    public static DistanceBearing getDistanceBearing(String from, String to)
    {
        LatLonRadians llrTo = cachedAirports.get(from.toUpperCase()).getLatLonRadians();
        LatLonRadians llrFrom = cachedAirports.get(to.toUpperCase()).getLatLonRadians();

        return getDistanceBearing(llrTo, llrFrom, true, true);
    }

    public static DistanceBearing getDistanceBearing(LatLon from, LatLon to)
    {
        return getDistanceBearing(from, to, true, true);
    }

    public static DistanceBearing getDistanceBearing(LatLonRadians from, LatLonRadians to)
    {
        return getDistanceBearing(from, to, true, true);
    }

    /**
     * This returns the computed distance for the passed in from/to latlons
     *
     * @param from LatLon
     * @param to LatLon
     * @param returnDistance - return distance if true, or 0 if false
     * @param returnBearing  - return bearing if true, or 0 if false
     * @return double[] - 0 = distance, 1 = bearing
     */
    public static DistanceBearing getDistanceBearing(LatLon from, LatLon to, boolean returnDistance, boolean returnBearing)
    {
        if ((!returnDistance && !returnBearing) || (from.lat == to.lat && from.lon == to.lon))
        {
            return new DistanceBearing(0, 0);
        }

        double lat1 = Math.toRadians(from.lat);
        double lon1 = Math.toRadians(from.lon);
        double lat2 = Math.toRadians(to.lat);
        double lon2 = Math.toRadians(to.lon);

        double sinLat1 = Math.sin(lat1);
        double sinLat2 = Math.sin(lat2);
        double cosLat1 = Math.cos(lat1);
        double cosLat2 = Math.cos(lat2);

        double distanceRadians = Math.acos(sinLat1 * sinLat2 + cosLat1 * cosLat2 * Math.cos(lon2 - lon1));

        double distance = 0;
        if (returnDistance)
        {
            distance = 3443.9 * distanceRadians;
        }

        double bearing = 0;
        if (returnBearing)
        {
            bearing = Math.acos((sinLat2 - sinLat1 * Math.cos(distanceRadians)) / (cosLat1 * Math.sin(distanceRadians)));
            bearing = Math.toDegrees(bearing);

            if (Math.sin(lon2 - lon1) < 0.0)
            {
                bearing = 360 - bearing;
            }
        }

        return new DistanceBearing(distance, bearing);
    }

    public static DistanceBearing getDistanceBearing(LatLonRadians from, LatLonRadians to, boolean returnDistance, boolean returnBearing)
    {
        if ((!returnDistance && !returnBearing) || (from.rlat == to.rlat && from.rlon == to.rlon))
        {
            return new DistanceBearing(0, 0);
        }

        double sinLat1 = Math.sin(from.rlat);
        double sinLat2 = Math.sin(to.rlat);
        double cosLat1 = Math.cos(from.rlat);
        double cosLat2 = Math.cos(to.rlat);

        double distanceRadians = Math.acos(sinLat1 * sinLat2 + cosLat1 * cosLat2 * Math.cos(to.rlon - from.rlon));

        double distance = 0;
        if (returnDistance)
        {
            distance = 3443.9 * distanceRadians;
        }

        double bearing = 0;
        if (returnBearing)
        {
            bearing = Math.acos((sinLat2 - sinLat1 * Math.cos(distanceRadians)) / (cosLat1 * Math.sin(distanceRadians)));
            bearing = Math.toDegrees(bearing);

            if (Math.sin(to.rlon - from.rlon) < 0.0)
            {
                bearing = 360 - bearing;
            }
        }

        return new DistanceBearing(distance, bearing);
    }

    /**
     * This returns a hashtable of airports found with the passed in parameters
     *
     * @param icao - center airport
     * @param clipLat - window in degrees latitude to search
     * @param clipLon - window in degrees longitude to search adjusted for latitude
     * @param minSize - minimum airport size to search for
     * @param maxSize - maximum airport size to search for
     * @param civilOk - airport types to include in the search
     * @param waterOk - airport types to include in the search
     * @param militaryOk - airport types to include in the search
     */
    public static Hashtable<String, CachedAirportBean> getAirportsInRange(String icao, double clipLat, double clipLon, int minSize, int maxSize, boolean civilOk, boolean waterOk, boolean militaryOk, int surfType)
    {
        //Get the lat/lon to pass in
        CachedAirportBean cab = cachedAirports.get(icao.toUpperCase());

        Hashtable<String, CachedAirportBean> results = getAirportsInRange(cab.getLatLon(), clipLat, clipLon, minSize, maxSize, civilOk, waterOk, militaryOk, surfType);

        //removed center airport
        if (results.size() > 0)
        {
            results.remove(icao);
        }

        return results;
    }

    /**
     * This returns a hashtable of airports found with the passed in parameters
     *
     * @param latlon - Center LatLon
     * @param clipLat - window in degrees latitude to search
     * @param clipLon - window in degrees longitude to search adjusted for latitude
     * @param minSize - minimum airport size to search for
     * @param maxSize - maximum airport size to search for
     * @param civilOk - airport types to include in the search
     * @param waterOk - airport types to include in the search
     * @param militaryOk - airport types to include in the search
     * @param surfType - surface types to include in the search
     */
    public static Hashtable<String, CachedAirportBean> getAirportsInRange(LatLon latlon, double clipLat, double clipLon, int minSize, int maxSize, boolean civilOk, boolean waterOk, boolean militaryOk, int surfType)
    {
        Hashtable<String, CachedAirportBean> results = new Hashtable<>();
        CachedAirportBean cab;
        BitSet bitSet = BitSet.valueOf(new long[]{(long) surfType});

        for (Map.Entry<String, CachedAirportBean> entry : cachedAirports.entrySet())
        {
            //get our current loop key and value
            cab = entry.getValue();

            double clat = Math.abs(cab.getLatLon().lat - latlon.lat);
            double clon = Math.abs(cab.getLatLon().lon - latlon.lon);

            if(clat > clipLat || clon > clipLon)
                continue;

            if(minSize != 0 && cab.getLongestRunway() < minSize)
                continue;

            if(maxSize != 0 && cab.getLongestRunway() > maxSize)
                continue;

            if(surfType != 0)
                if(!bitSet.get(cab.getSurfaceType()))
                    continue;

            //compare against current size and radius
            if ((civilOk && cab.getType() == 1) ||
                    (waterOk && cab.getType() == 2) ||
                    (militaryOk && cab.getType() == 3))
            {
                results.put(entry.getKey(), cab);
            }
        }

        return results;
    }

    public static CloseAirport closestAirport(double lat, double lon)
    {
        return closestAirport(lat, lon, 0, true);
    }

    /**
     * This returns closest airport found with the passed in parameters
     *
     * @param lat     - window in degrees latitude to search
     * @param lon     - window in degrees longitude to search adjusted for latitude
     * @param minSize - minimum airport size to search for
     * @param waterOk - water airports allowed
     */
    public static CloseAirport closestAirport(double lat, double lon, int minSize, boolean waterOk)
    {
        String bestIcao = null;
        double bestDistance = 0;

        String key;
        CachedAirportBean cab;

        boolean found = false;
        double degrees = 0.2;

        do
        {
            //convert the distance to degrees (60nm at the equator = 1 degree)
            double degreeClipLat = degrees;

            //adjust for compression toward poles
            double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(lat)));

            //get the airports in range
            Hashtable<String, CachedAirportBean> results = getAirportsInRange(new LatLon(lat, lon), degreeClipLat, degreeClipLon, minSize, 0, true, waterOk, true, 0);

            //loop through the results to see if any are closest
            Enumeration<String> keys = results.keys();
            while (keys.hasMoreElements())
            {
                //get our current loop key and value
                key = keys.nextElement();
                cab = results.get(key);

                //get the distance
                double distance = getDistance(new LatLon(lat, lon), cab.getLatLon());

                //check if we found a new match that is better then the previous
                if (bestIcao == null || distance < bestDistance)
                {
                    found = true;
                    bestIcao = key;
                    bestDistance = distance;
                }
            }
            //if we haven't found one, increase the radius
            if (!found)
            {
                degrees *= 2;
            }
        } while (!found);

        return new CloseAirport(bestIcao, bestDistance);
    }

    /**
     * This returns a randomly selected airport found with the passed in parameters
     *
     * @param icao          - airport to center on
     * @param minDistance - minimum distance to search for
     * @param maxDistance - maximum distance to search for
     * @param minsize     - minimum airport size to search for
     * @param maxsize     - maximum airport size to search for
     * @param lat         - used to compute correct value for longitudinal degree values for distance
     * @param icaoSet     - preselected icaos to search through
     * @param waterOk     - ok, to include water airports
     */
    public static CloseAirport getRandomCloseAirport(String icao, double minDistance, double maxDistance, int minsize, int maxsize, double lat, Set<String> icaoSet, boolean waterOk, int surfType)
    {
        CloseAirport returnValue = null;

        //if the template has defined ICAOs, search them first for one meeting min/max distance criteria
        if (icaoSet != null && !icaoSet.isEmpty())
        {
            String airports[] = icaoSet.toArray(new String[icaoSet.size()]);

            //new code that filters the list of airports down to the ones that meet
            //the min/max distance criteria
            Set<String> inRange = new HashSet<>();
            for (String airport : airports)
            {
                DistanceBearing distanceBearing = getDistanceBearing(icao, airport);
                if (distanceBearing != null &&
                    distanceBearing.distance != 0 &&
                   (distanceBearing.distance >= minDistance && distanceBearing.distance <= maxDistance))
                {
                    inRange.add(airport);
                }
            }

            //if 1 or more airports met the criteria, randomly select one and return
            if (inRange.size() > 0)
            {
                String aps[] = inRange.toArray(new String[inRange.size()]);
                int index = (int) (aps.length * Math.random());
                DistanceBearing distanceBearing = getDistanceBearing(icao, aps[index]);

                return new CloseAirport(aps[index], distanceBearing.distance, distanceBearing.bearing);
            }
        }

        //If no ICAO was found in the passed in ICAOset then
        //query the DB to see if any airport meets the min/max distance criteria

        //convert the distance to degrees (60nm at the equator = 1 degree)
        double degreeClipLat = Math.abs(maxDistance / 60.0);
        //adjust for compression toward poles
        double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(lat)));

        //get the airports that met the criteria passed
        CachedAirportBean cab = Airports.cachedAirports.get(icao);
        Hashtable<String, CachedAirportBean> results = getAirportsInRange(cab.getLatLon(), degreeClipLat, degreeClipLon, minsize, maxsize, true, waterOk, true, surfType);

        //Don't bother if none found
        if (results != null && results.size() != 0)
        {
            String key;
            CachedAirportBean value;
            DistanceBearing distbearing;

            //Iterate through the returned set
            Enumeration<String> keys = results.keys();
            while (keys.hasMoreElements())
            {
                //get our current loop key and value
                key = keys.nextElement();
                value = results.get(key);

                distbearing = getDistanceBearing(icao, value.getIcao());

                //filter out the ones that don't meet the minimum distance, or is the center airport
                if (icao.contains(key)
                    || distbearing.distance < minDistance
                    || distbearing.distance > maxDistance)
                {
                    results.remove(key);
                }
            }

            //Anything left?
            if (results.size() != 0)
            {
                keys = results.keys();
                key = "";

                //if there is only one, just return it
                if (results.size() == 1)
                {
                    key = keys.nextElement();
                    value = results.get(key);

                    distbearing = getDistanceBearing(icao, value.getIcao());
                    returnValue = new CloseAirport(key, distbearing.distance, distbearing.bearing);
                }
                else
                {
                    //if there is more then one, then randomize the selection out of those available
                    int index = (int) (results.size() * Math.random());
                    for (int i = 0; i <= index; i++)
                    {
                        key = keys.nextElement();
                    }

                    value = results.get(key);
                    distbearing = getDistanceBearing(icao, value.getIcao());
                    returnValue = new CloseAirport(key, distbearing.distance, distbearing.bearing);
                }
            }
        }

        return returnValue;
    }

    public static List<CloseAirport> closeAirportsWithAssignments(String id, boolean outbound)
    {
        return closeAirportsWithAssignments(id, 0, 50, outbound);
    }

    public static List<CloseAirport> closeAirportsWithAssignments(String id, double minDistance, double maxDistance, boolean outbound)
    {
        ArrayList<CloseAirport> result = new ArrayList<>();
        try
        {
            String join;
            if (outbound)
            {
                join = "JOIN assignments j ON j.location = b.icao AND j.userlock IS NULL and j.groupId IS NULL";
            }
            else
            {
                join = "JOIN assignments j ON j.toIcao = b.icao AND j.userlock IS NULL AND j.groupId IS NULL";
            }

            String qry = "SELECT DISTINCT a.icao, b.icao FROM airports a, airports b " + join + " WHERE a.icao <> b.icao AND ABS(a.lat-b.lat) < 2 AND ABS(a.lon-b.lon)<2 AND a.icao = ? order by (ABS(a.lat-b.lat)+ABS(a.lon-b.lon)) limit 50";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
            while (rs.next())
            {
                DistanceBearing distanceBearing = getDistanceBearing(rs.getString(1), rs.getString(2));

                if (distanceBearing.distance >= minDistance && distanceBearing.distance < maxDistance)
                {
                    result.add(new CloseAirport(rs.getString(2), distanceBearing.distance, distanceBearing.bearing));
                }
            }

            Collections.sort(result);
            if (result.size() > 10)
            {
                result = new ArrayList<>(result.subList(0, 10));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * This returns an array of closeAirport found with the passed in parameters
     *
     * @param icao          - airport to center on
     * @param minDistance - minimum distance to search for
     * @param maxDistance - maximum distance to search for
     */
    public static List<CloseAirport> fillCloseAirports(String icao, double minDistance, double maxDistance)
    {
        String key;
        CachedAirportBean value;

        if (icao == null)
            return null;

        List<CloseAirport> result = new ArrayList<>();

        CachedAirportBean cab = cachedAirports.get(icao);

        //convert the distance to degrees (60nm at the equator = 1 degree)
        double degreeClipLat = Math.abs(maxDistance / 60.0);

        //adjust for compression toward poles
        double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(cab.getLatLon().lat)));

        Hashtable<String, CachedAirportBean> results = getAirportsInRange(cab.getLatLon(), degreeClipLat, degreeClipLon, 0, 0, true, true, true, 0);

        //loop through the results to see if any are closest
        Enumeration<String> keys = results.keys();
        while (keys.hasMoreElements())
        {
            //get our current loop key and value
            key = keys.nextElement();
            value = results.get(key);

            //get the distance / bearing
            DistanceBearing distanceBearing = getDistanceBearing(icao, value.getIcao());

            boolean skipself = icao.contains(key);

            //check if minimum distance met, we already know it meets the max distance (2 degrees or 120nm)
            if (!skipself && distanceBearing.distance >= minDistance)
            {
                result.add(new CloseAirport(key, distanceBearing.distance, distanceBearing.bearing));
            }
        }

        //limit to return the closest 12 only
        Collections.sort(result);

        if (result.size() > 12)
        {
            result = new ArrayList<>(result.subList(0, 12));
        }

        return result;
    }

    //TODO this needs to change to return a list of ICAOs that then get the cached airport
    public static List<AirportBean> getAirportsForFboConstruction(int owner)
    {
        return getAirportSQL("SELECT a.* FROM airports a, goods g" +
                " WHERE a.icao = g.location" +
                " AND g.amount >= " + GoodsBean.CONSTRUCT_FBO +
                " AND g.type = " + GoodsBean.GOODS_BUILDING_MATERIALS +
                " AND g.owner = " + owner +
                " AND NOT EXISTS (SELECT * FROM fbo WHERE fbo.location = a.icao AND fbo.owner = g.owner)" +
                " AND (select" +
                "       case" +
                "        when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1" +
                "        when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2" +
                "        else 3" +
                "       end - case when ISNULL(fbo.location) then 0 else sum(fbosize) end" +
                "       as fboslotsremain" +
                "      from airports" +
                "      left outer join fbo on fbo.location = airports.icao" +
                "      where airports.icao = a.icao" +
                "      group by airports.icao) > 0");
    }

    public static HashMap<String, CachedAirportBean> getAirportsFromFboList(List<FboBean> fbos)
    {
        HashMap<String, CachedAirportBean> result = new HashMap<>();

        for(FboBean fbo: fbos)
            result.put(fbo.getLocation(), Airports.cachedAirports.get(fbo.getLocation()));

        return result;
    }

    public static List<AirportBean> getAirportSQL(String sql)
    {
        ArrayList<AirportBean> result = new ArrayList<>();
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(sql);
            while (rs.next())
            {
                result.add(new AirportBean(rs));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void FillZeroOps(ArrayList<FlightOp> ops, String icao, int startmon, int startyear, int endmon, int endyear)
    {
        ArrayList<FlightOp> results = new ArrayList<>();

        int curryear = startyear;
        int currmonth = startmon;

        //fill in the first year
        if (endyear > curryear)
        {
            for (int i = startmon; i <= 12; i++)
            {
                results.add(new FlightOp(endyear, i, icao, 0));
            }

            curryear++;
            currmonth = 1;
        }
        //see if we still have a year to go
        if (endyear > curryear)
        {
            for (int i = 1; i <= 12; i++)
            {
                results.add(new FlightOp(curryear, i, icao, 0));
            }

            curryear++;
        }
        for (int i = currmonth; i <= endmon; i++)
        {
            results.add(new FlightOp(curryear, i, icao, 0));
        }

        for (int i = results.size() - 1; i >= 0; i--)
        {
            ops.add(results.get(i));
        }
    }

    public static String getAirportOperationDataJSON(String icao)
    {
        List<FlightOp> ops = new ArrayList<>();
        if (isValidIcao(icao))
        {
            ops = getAirportOperationData(icao);
        }

        Gson gson = new Gson();
        return gson.toJson(ops);
    }

    /**
     * Gets an array of FlightOps for the approximately the last 24 months
     *
     * @param icao - ICAO of interest
     */
    public static List<FlightOp> getAirportOperationData(String icao)
    {
        ArrayList<FlightOp> results = new ArrayList<>();

        //exit if icao does not exist
        if (!isValidIcao(icao))
            return results;

        //get the current year and month
        Calendar cal = Calendar.getInstance();
        int currYear = cal.get(Calendar.YEAR);
        int currMonth = cal.get(Calendar.MONTH) + 1; //0 based instead of 1

        //setup our loop variables
        int loopYear = currYear;
        int loopMonth = currMonth;

        try
        {
            //get our records, using op year in (?,?,?) allows index to be used
            String qry = "SELECT * FROM flightops WHERE opyear in (?,?,?) AND icao= ? ORDER BY opyear DESC, opmonth DESC";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, currYear - 2, currYear - 1, currYear, icao);

            int opYear;
            int opMonth;
            int ops;

            //loop though results, remember this is a spare array and might be missing 1 or more
            while (rs.next())
            {
                //get our data
                opYear = rs.getInt("opyear");
                opMonth = rs.getInt("opmonth");
                ops = rs.getInt("ops");

                //if everything matches just add it
                if (opYear == loopYear && opMonth == loopMonth)
                {
                    results.add(new FlightOp(opYear, opMonth, icao, ops));
                }
                else //oops no match
                {
                    //Fill in months with no ops
                    FillZeroOps(results, icao, opMonth + 1, opYear, loopMonth, loopYear);

                    //ok, we are ready to add in our current record now after back filling the data
                    results.add(new FlightOp(opYear, opMonth, icao, ops));

                    loopMonth = opMonth;
                    loopYear = opYear;
                }

                //if we have 24 (or more) we are done
                if (results.size() >= 25)
                {
                    break;
                }

                //decrement to our next anticipated month
                loopMonth--;

                //if the month is 0, then we need to loop back around to 12
                if (loopMonth <= 0)
                {
                    loopMonth = 12;
                    //Since we moved to december, we also need to decrement the year
                    loopYear--;

                    //If we are past the two year mark something bad has happened so exit the loop
                    if (loopYear < currYear - 2)
                    {
                        break;
                    }
                }
            }
            if (results.size() < 25)
            {
                int amtLeft = 24 - results.size();
                if (loopMonth >= amtLeft)
                {
                    FillZeroOps(results, icao, loopMonth - amtLeft, loopYear, loopMonth, loopYear);
                }
                else
                {
                    int amt = amtLeft - loopMonth;
                    FillZeroOps(results, icao, 12 - amt, loopYear - 1, loopMonth, loopYear);
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return results;
    }

    /**
     * returns an int of the average number of ops for the last 12 months
     *
     * @param icao - ICAO of interest
     */
    public static int[] getAirportOperationsPerMonth(String icao)
    {
        int[] result = new int[2];
        int average = 0;

        //get the data
        List<FlightOp> ao = getAirportOperationData(icao);

        //if no data found, return 0
        if (ao.size() == 0)
        {
            result[0] = 0;
            result[1] = 0;
            return result;
        }
        //only add up the last 12 months, skip current month
        for (int i = 1; i <= 12; i++)
        {
            average += ao.get(i).ops;
        }

        //return the current, and the average
        result[0] = ao.get(0).ops;
        result[1] = average / 12;
        return result;
    }

    public static List<CachedAirportBean> findAirports(boolean assignments, int modelId, String name, int distance, String from, boolean ferry, boolean buy, int commodity, int minAmount, boolean fuel, boolean jeta, boolean repair, boolean acForSale, boolean fbo, boolean isRentable) throws DataError
    {
        ArrayList<CachedAirportBean> result = new ArrayList<>();
        String qry;
        try
        {
            if(!Helpers.isNullOrBlank(from) && !cachedAirports.containsKey(from.toUpperCase()))
                throw new DataError("Error: Bad ICAO");

            StringBuilder tables = new StringBuilder("airports");
            StringBuilder query = new StringBuilder();
            String and = " WHERE ";

            if (assignments)
            {
                tables.append(" LEFT JOIN assignments ON airports.icao = assignments.location ");
                query.append(and);
                query.append(" assignments.groupid is null and assignments.userlock is null ");
                and = "AND ";
            }

            if (modelId >= 0 || ferry || acForSale)
            {
                //Code correction to prevent Sql error when fuel and ferry selected in aiport search parameters
                tables.append(" LEFT JOIN aircraft on aircraft.location = airports.icao ");

                query.append(and);
                query.append(" aircraft.userlock is null ");

                if (modelId >= 0)
                {
                    query.append("AND aircraft.model = ").append(modelId).append(" ");
                    //added support for rentable flag
                    if (isRentable)
                    {
                        query.append("AND (aircraft.rentalDry > 0 OR aircraft.rentalWet > 0) ");
                    }
                }

                if (ferry)
                {
                    query.append("AND (aircraft.advertise & " + AircraftBean.ADV_FERRY + ") > 0 ");
                }

                if (acForSale)
                {
                    query.append("AND NOT (aircraft.sellPrice is null) ");
                }

                and = "AND ";
            }

            if (fuel || repair || fbo || jeta)
            {
                tables.append(" LEFT JOIN fbo on fbo.location = airports.icao ");
                if (fuel)
                {
                    query.append(and);
                    query.append(" ((airports.services & " + AirportBean.SERVICES_AVGAS + ") > 0 or (fbo.owner IN (SELECT owner FROM goods WHERE type='3' AND location= airports.icao AND amount > 130) AND fbo.active='1'))");
                    and = "AND ";
                }
                if (jeta)
                {
                    query.append(and);
                    query.append(" ((airports.services & " + AirportBean.SERVICES_AVGAS + ") > 0 or (fbo.owner IN (SELECT owner FROM goods WHERE type='4' AND location= airports.icao AND amount > 130) AND fbo.active='1'))");
                    and = "AND ";
                }
                if (repair)
                {
                    query.append(and);
                    query.append(" (airports.size > " + AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE + " OR (fbo.active > 0 AND (fbo.services & " + FboBean.FBO_REPAIRSHOP + ") > 0))");
                    and = "AND ";
                }
                if (fbo)
                {
                    query.append(and);
                    query.append(" (fbo.active > 0)");
                    and = "AND ";
                }
            }

            if (commodity > 0)
            {
                if (minAmount < 0)
                {
                    minAmount = 0;
                }

                tables.append(" LEFT JOIN goods ON goods.type = ").append(commodity).append(" AND goods.location = airports.icao AND ((goods.amount - cast(goods.retain as signed int) > ").append(minAmount).append(") AND (goods.saleFlag &").append(buy ? GoodsBean.SALEFLAG_BUY : GoodsBean.SALEFLAG_SELL).append(") > 0)");

                query.append(and);
                query.append(" (goods.type = ").append(commodity).append(" OR size > ").append(Goods.commodities[commodity].getMinAirportSize()).append(")");
                and = " AND ";
            }

            if (!Helpers.isNullOrBlank(name))
            {
                query.append(and);
                query.append("(airports.name LIKE '%").append(Converters.escapeSQL(name)).append("%' OR city LIKE '%").append(Converters.escapeSQL(name)).append("%')");
                and = "AND ";
            }

            double lat = 0, lon = 0;
            if (!Helpers.isNullOrBlank(from) && isValidIcao(from.toUpperCase()))
            {
                CachedAirportBean cab = Airports.cachedAirports.get(from.toUpperCase());

                lat = cab.getLatLon().lat;
                lon = cab.getLatLon().lon;
                double degreeClipLat = distance / 60.0;
                double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(lat)));

                query.append(and);
                query.append("(abs(lat - ").append(lat).append(") < ").append(degreeClipLat).append(" AND abs(lon - ").append(lon).append(") < ").append(degreeClipLon).append(")");
            }

            query.append(" ORDER BY icao LIMIT 100");

            qry = "SELECT DISTINCT airports.icao FROM " + tables.toString() + query.toString();
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                CachedAirportBean airport = Airports.cachedAirports.get(rs.getString("icao"));

                if (!Helpers.isNullOrBlank(from) && getDistance(rs.getString("icao"), from) > distance)
                    continue;

                result.add(airport);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<AirportBean> findCertainAirports(String region, String state, String country, String icao, String name, boolean fuel, boolean repair, boolean fbo, boolean fboInactive, boolean facilityPTRent, boolean facilityCTRent, int fboOwner) throws DataError
    {
        ArrayList<AirportBean> result = new ArrayList<>();
        try
        {
            StringBuilder tables = new StringBuilder("airports");
            StringBuilder query = new StringBuilder();
            String and = " WHERE ";

            if (fuel || repair || fbo || fboInactive || facilityPTRent || facilityCTRent || (fboOwner > 0))
            {
                tables.append(" LEFT JOIN fbo ON fbo.location = airports.icao ");
                if (fboOwner > 0)
                {
                    query.append(and);
                    query.append(" (fbo.owner = ").append(fboOwner).append(")");
                    and = "AND ";
                }
                if (fuel)
                {
                    query.append(and);
                    query.append(" ((airports.services & " + AirportBean.SERVICES_AVGAS + ") > 0 or (fbo.owner IN (SELECT owner FROM goods WHERE type='3' AND location= airports.icao AND amount > 130) AND fbo.active='1'))");
                    and = "AND ";
                }
                if (repair)
                {
                    query.append(and);
                    query.append(" (airports.size > " + AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE + " OR (fbo.active > 0 AND (fbo.services & " + FboBean.FBO_REPAIRSHOP + ") > 0))");
                    and = "AND ";
                }
                if (fbo)
                {
                    query.append(and);
                    query.append(" (fbo.active > 0)");
                    and = "AND ";
                }
                if (fboInactive)
                {
                    tables.append(" LEFT JOIN accounts ON accounts.id = fbo.owner ");
                    query.append(and);
                    query.append(" (fbo.active = 0 AND accounts.id >0)");
                    and = "AND ";
                }
                if (facilityPTRent)
                {
                    tables.append(" LEFT JOIN fbofacilities pt ON pt.fboId = fbo.id and pt.reservedSpace >= 0 and units = 'passengers' ");
                    tables.append(" LEFT JOIN (select fboId, sum(size) as spaceInUse from fbofacilities where reservedSpace < 0 and units = 'passengers' group by fboId) pts ON pts.fboId = fbo.id ");
                    query.append(and);
                    query.append(" (fbo.fboSize * (case when airports.size < 1000 then 1 when airports.size < 3500 then 2 else 3 end) - pt.reservedSpace - (IF (pts.spaceInUse IS NULL,0,pts.spaceInUse)) > 0) ");
                    and = "AND ";
                }
            }

            String regionSQL = getSearchRegionSQL(region);
            if (regionSQL != null)
            {
                query.append(and);
                query.append(regionSQL);
                and = "AND ";
            }

            if (state != null)
            {
                query.append(and);
                query.append("airports.state = '").append(Converters.escapeSQL(state)).append("' ");
                and = "AND ";
            }

            if (country != null)
            {
                query.append(and);
                query.append("airports.country = '").append(Converters.escapeSQL(country)).append("' ");
                and = "AND ";
            }

            if (icao != null)
            {
                query.append(and);
                query.append("airports.icao = '").append(Converters.escapeSQL(icao)).append("' ");
                and = "AND ";
            }

            if (name != null)
            {
                query.append(and);
                query.append("airports.name LIKE '%").append(Converters.escapeSQL(name)).append("%' ");
            }

            query.append(" ORDER BY icao");
            String qry = "SELECT DISTINCT airports.* FROM " + tables.toString() + query.toString();
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                AirportBean airport = new AirportBean(rs);

                result.add(airport);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    /* airportLink with a gmap pop require in the jsp page:
     * <head>
	 * <script type="text/javascript" src="scripts/PopupWindow.js"></script>
	 * <script type="text/javascript"> var gmap = new PopupWindow(); </script>
	 * </head>	  
	 */

    public static String airportLink(String icao, HttpServletResponse response)
    {
        return airportLink(icao, null, null, null, response);
    }

    public static String airportLink(String icao, String gmapIcao, HttpServletResponse response)
    {
        return airportLink(icao, null, gmapIcao, null, response);
    }

    public static String airportLink(String icao, String gmapIcao, String gmapIcaoTo, HttpServletResponse response)
    {
        return airportLink(icao, null, gmapIcao, gmapIcaoTo, response);
    }

    public static String airportLink(String icao, String bulletCodeLocation, String gmapIcao, String gmapIcaoTo, HttpServletResponse response)
    {
        if(!isValidIcao(icao) || (!Helpers.isNullOrBlank(gmapIcao) && !isValidIcao(gmapIcao)) || (!Helpers.isNullOrBlank(gmapIcaoTo) && !isValidIcao(gmapIcaoTo)))
            return "";

        CachedAirportBean cabIcao = Airports.cachedAirports.get(icao);
        String image = "";
        if (!Helpers.isNullOrBlank(gmapIcao))
        {
            CachedAirportBean cabGMapIcao = Airports.cachedAirports.get(gmapIcao);
            String icaodPart = "";
            if (!Helpers.isNullOrBlank(gmapIcaoTo) && !gmapIcaoTo.equals(gmapIcao))
            {
                icaodPart = "&icaod=" + gmapIcaoTo;
            }

            image = "<A HREF=\"#\" onClick=\"gmap.setSize(620,530);gmap.setUrl('gmap.jsp?icao=" +
                    gmapIcao +
                    icaodPart +
                    "');gmap.showPopup('gmap');return false;\" NAME=\"gmap\" ID=\"gmap\">" +
                    "<img src=\"" +
                    getDescriptiveImage(cabGMapIcao, Fbos.getAirportFboSlotsInUse(icao) > 0) +
                    "\" align=\"absmiddle\" border=\"0\" /></a>";
        }

        String bulletPart = "";
        if ((bulletCodeLocation != null) && (!bulletCodeLocation.equals("")))
        {
            bulletPart = bulletCodeLocation + " onMouseOut=\"hideBullet()\" ";
        }

        String href = response.encodeRedirectURL("/airport.jsp?icao=" + icao);
        String textLink = "<a title=\"" + cabIcao.getTitle() + "\" " +
                bulletPart +
                " class=\"normal\" href=\"" + href + "\">" +
                icao +
                "</a>";

        return image + textLink;
    }

    public static String getBearingImageURL(double bearing)
    {
        int id = (int) Math.round(bearing / 45.0) % 8;
        return "img/set2_" + id + ".gif";
    }

    public static String getSearchRegionSQL(String region)
    {
        if (region != null)
        {
            if (region.equals("Caribbean"))
                return " airports.country IN ('Antigua and Barbuda', 'Aruba', 'Bahamas, The', 'Cayman Islands', 'Cuba', 'Dominica', 'Dominican Republic', " +
                        "'Grenada', 'Guadeloupe', 'Haiti', 'Jamaica', 'Martinique', 'Puerto Rico', 'St. Kitts and Nevis', 'St. Lucia', " +
                        "'St. Vincent and the Grenadines', 'Trinidad and Tobago', 'Turks and Caicos Islands', 'Virgin Islands', 'Virgin Islands, British') ";

            if (region.equals("Central America"))
                return " airports.country IN ('Belize', 'Costa Rica', 'El Salvador', 'Guatemala', 'Honduras', 'Mexico', 'Nicaragua', 'Panama') ";

            if (region.equals("South America"))
                return " airports.country IN ('Argentina', 'Bolivia', 'Brazil', 'Chile', 'Colombia', 'Ecuador', 'French Guiana', 'Guyana', " +
                        "'Paraguay', 'Peru', 'Suriname', 'Uruguay', 'Venezuela', 'Falkland Islands (Islas Malvinas)') ";

            if (region.equals("Pacific Islands"))
                return " (airports.country IN ('American Samoa', 'Cook Islands', 'Fiji Islands', 'New Caledonia', 'Niue', 'Samoa', 'Solomon Islands', " +
                        "'French Polynesia', 'Tonga', 'Tuvalu', 'Vanuatu', 'Wallis and Futuna') OR " +
                        " airports.state IN ('Hawaii', 'Johnston Atoll', 'Midway Islands', 'Wake Island')) ";
        }
        return null;
    }

    public static List<String> getSearchRegions()
    {
        ArrayList<String> result = new ArrayList<>();

        result.add("Caribbean");
        result.add("Central America");
        result.add("Pacific Islands");
        result.add("South America");

        return result;
    }

    /**
     * Gets an the total FlightOps for the selected ICAO, Month, and Year
     * @param groupId - Group of interest
     * @param month - Month of interest
     * @param year - Year of interest
     **/
    public static int getGroupOperationsByMonthYear(int groupId, int month, int year)
    {
        int result = 0;

        try
        {
            //First we need to get all the FBO locations for the groupId passed in.
            List<FboBean> fbos = Fbos.getFboByOwner(groupId);

            StringBuilder sb = new StringBuilder(4096);
            if(fbos.size() > 0)
            {
                boolean firstLoop = true;
                for(FboBean fbo : fbos)
                {
                    if(!firstLoop)
                        sb.append(", ");
                    else
                        firstLoop = false;

                    sb.append("'").append(fbo.getLocation()).append("'");
                }

                //get our records
                String qry = "SELECT * FROM flightops WHERE opmonth=? AND opyear=? AND icao in (" + sb.toString() + ")";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, month, year);
                while(rs.next())
                {
                    //get our data
                    result += rs.getInt("ops");
                }
            }
            else
            {
                result = -1;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getTotalFboSlots(String icao)
    {
        int size = cachedAirports.get(icao).getSize();

        if (size < AirportBean.MIN_SIZE_MED)
            return 1;
        else if (size < AirportBean.MIN_SIZE_BIG)
            return 2;

        return 3;
    }

    public boolean hasSystemGoodsForSale(int size)
    {
        if(size > CachedAirportBean.MIN_SIZE_BIG)
            return true;

        for (int c = 0; c < Goods.commodities.length; c++)
        {
            if (Goods.commodities[c] != null && size > Goods.commodities[c].getMinAirportSize())
                return true;
        }

        return false;
    }

    public static String getDescriptiveImage(CachedAirportBean cab, boolean hasFbo)
    {
        String base;
        String ext="";
        boolean hasFuel = cab.has100ll() || cab.hasJetA();

        switch (cab.getType())
        {
            case CachedAirportBean.TYPE_WATER:
                base = "seaplane";
                break;
            case CachedAirportBean.TYPE_MILITARY:
                base = "military";
                break;
            default:
                if (cab.getSize() < CachedAirportBean.MIN_SIZE_MED)
                    base = "airstrip";
                else if (cab.getSize() < CachedAirportBean.MIN_SIZE_BIG)
                    base = "small-airport";
                else
                    base = "large-airport";
        }

        if (hasFbo)
            ext = "-fbo";
        else if (hasFuel)
            ext = "-fuel";

        return "/img/" + base + ext + ".gif";
    }

    public static boolean hasSystemRepairShop(int size)
    {
        return size >= AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE;
    }

}
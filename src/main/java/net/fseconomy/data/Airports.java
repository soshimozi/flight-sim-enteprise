package net.fseconomy.data;

import com.google.gson.Gson;
import net.fseconomy.beans.*;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.FlightOp;
import net.fseconomy.dto.LatLonSize;
import net.fseconomy.util.Converters;

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
    public static Hashtable<String,LatLonSize> cachedAPs = new Hashtable<>();

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
        if (cachedAPs.size() == 0)
        {
            //pull the airports
            try
            {
                String qry = "SELECT icao, lat, lon, size, type FROM airports";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
                while (rs.next())
                {
                    int itype;

                    String icao = rs.getString("icao");
                    double lat = rs.getDouble("lat");
                    double lon = rs.getDouble("lon");
                    int size = rs.getInt("size");
                    String type = rs.getString("type");

                    if (type.contains("military"))
                    {
                        itype = AirportBean.TYPE_MILITARY;
                    }
                    else if (type.contains("water"))
                    {
                        itype = AirportBean.TYPE_WATER;
                    }
                    else
                    {
                        itype = AirportBean.TYPE_CIVIL;
                    }

                    LatLonSize lls = new LatLonSize(lat, lon, size, itype);
                    cachedAPs.put(icao, lls);
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static int findDistance(String from, String to)
    {
        int returnval = 0;

        if (from != null && to != null)
        {
            double distanceBearing[] = getDistanceBearing(from, to);
            returnval = (int)Math.round(distanceBearing[0]);
        }

        return returnval;
    }

    public static double getDistance(double lat1, double lon1, double lat2, double lon2)
    {
        LatLonSize lls1 = new LatLonSize(lat1, lon1, 0, 0);
        LatLonSize lls2 = new LatLonSize(lat2, lon2, 0, 0);

        double[] result = getDistanceBearing(lls1, lls2, true, false);

        return result[0];
    }

    public static double[] getDistanceBearing(AirportBean from, AirportBean to)
    {
        return getDistanceBearing(from.getIcao(), to.getIcao());
    }

    public static double getDistance(String from, String to)
    {
        LatLonSize lls1 = cachedAPs.get(from.toUpperCase()); //Added .toUpperCase to make sure comparison will work
        LatLonSize lls2 = cachedAPs.get(to.toUpperCase());

        if (lls1 == null || lls2 == null)
        {
            //			if(lls1 == null)
            //				System.err.println("-->distanceBearing Error, bad From ICAO: " + from);
            //			if(lls2 == null)
            //				System.err.println("-->distanceBearing Error, bad To ICAO: " + to);

            return 0;
        }

        double[] result = getDistanceBearing(lls1, lls2, true, false);

        return result[0]; //distance only
    }

    public static double[] getDistanceBearing(String from, String to)
    {
        LatLonSize lls1 = cachedAPs.get(from.toUpperCase()); //Added .toUpperCase to make sure comparison will work
        LatLonSize lls2 = cachedAPs.get(to.toUpperCase());

        if (lls1 == null || lls2 == null)
        {
            //			if(lls1 == null)
            //				System.err.println("-->distanceBearing Error, bad From ICAO: " + from);
            //			if(lls2 == null)
            //				System.err.println("-->distanceBearing Error, bad To ICAO: " + to);

            return null;
        }

        return getDistanceBearing(lls1, lls2);
    }

    public static double[] getDistanceBearing(LatLonSize from, LatLonSize to)
    {
        return getDistanceBearing(from, to, true, true);
    }

    /**
     * This returns the computed distance for the passed in from/to latlons
     *
     * @param from LatLonSize
     * @param to LatLonSize
     * @param returnDistance - return distance if true, or 0 if false
     * @param returnBearing  - return beaing if true, or 0 if false
     * @return double[] - 0 = distance, 1 = bearing
     */
    public static double[] getDistanceBearing(LatLonSize from, LatLonSize to, boolean returnDistance, boolean returnBearing)
    {
        if ((!returnDistance && !returnBearing) || (from.lat == to.lat && from.lon == to.lon))
        {
            return new double[]{0, 0};
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

        return new double[]{distance, bearing};
    }

    /**
     * This returns a hashtable of airports found with the passed in parameters
     *
     * @param icao - center airport
     * @param clipLat - window in degrees latitude to search
     * @param clipLon - window in degrees longitude to search adjusted for latitude
     * @param minSize - minimum airport size to search for
     * @param maxSize - maximum airport size to search for
     * @param aptypes - airport types to include in the search
     *                Airboss 5/30/11
     */
    public static Hashtable<String, LatLonSize> getAirportsInRange(String icao, double clipLat, double clipLon, int minSize, int maxSize, boolean aptypes[])
    {
        //Get the lat/lon to pass in
        LatLonSize lls = cachedAPs.get(icao.toUpperCase());

        if (lls == null)
        {
            //System.err.println("-->getAirportsInRange Error, bad ICAO: " + icao);
            return null;
        }

        Hashtable<String, LatLonSize> results = getAirportsInRange(lls.lat, lls.lon, clipLat, clipLon, minSize, maxSize, aptypes);

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
     * @param lat - Center Lat
     * @param lon - Center Lon
     * @param clipLat - window in degrees latitude to search
     * @param clipLon - window in degrees longitude to search adjusted for latitude
     * @param minSize - minimum airport size to search for
     * @param maxSize - maximum airport size to search for
     * @param aptypes - airport types to include in the search
     */
    public static Hashtable<String, LatLonSize> getAirportsInRange(double lat, double lon, double clipLat, double clipLon, int minSize, int maxSize, boolean aptypes[])
    {
        Hashtable<String, LatLonSize> results = new Hashtable<>();
        String key;
        LatLonSize value;
        int minSz;
        int maxSz;
        boolean military;
        boolean civil;
        boolean water;

        civil = aptypes[0];
        water = aptypes[1];
        military = aptypes[2];

        minSz = minSize;
        maxSz = maxSize == 0 ? Integer.MAX_VALUE : maxSize;

        Enumeration<String> keys = cachedAPs.keys();
        while (keys.hasMoreElements())
        {
            //get our current loop key and value
            key = keys.nextElement();
            value = cachedAPs.get(key);

            double clat = Math.abs(value.lat - lat);
            double clon = Math.abs(value.lon - lon);

            //compare against current size and radius
            if (value.size >= minSz && value.size <= maxSz &&
                    clat <= clipLat && clon <= clipLon &&
                    ((civil && value.type == 1) ||
                            (water && value.type == 2) ||
                            (military && value.type == 3)))
            {
                results.put(key, value);
            }
        }

        return results;
    }

    /**
     * This returns closest airport found with the passed in parameters
     *
     * @param lat     - window in degrees latitude to search
     * @param lon     - window in degrees longitude to search adjusted for latitude
     * @param minSize - minimum airport size to search for
     */
    public static CloseAirport closestAirport(double lat, double lon, int minSize)
    {
        return closestAirport(lat, lon, minSize, true);
    }

    public static CloseAirport closestAirport(double lat, double lon, int minSize, boolean waterOk)
    {
        String bestIcao = null;
        double bestDistance = 0;

        String key;
        LatLonSize value;

        boolean found = false;
        double degrees = 0.2;
        boolean[] aptypes = {true, waterOk, true};

        do
        {
            //convert the distance to degrees (60nm at the equator = 1 degree)
            double degreeClipLat = degrees;

            //adjust for compression toward poles
            double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(lat)));

            //get the airports in range
            Hashtable<String, LatLonSize> results = getAirportsInRange(lat, lon, degreeClipLat, degreeClipLon, minSize, 0, aptypes);

            //loop through the results to see if any are closest
            Enumeration<String> keys = results.keys();
            while (keys.hasMoreElements())
            {
                //get our current loop key and value
                key = keys.nextElement();
                value = results.get(key);

                //get the distance
                double distance = getDistance(lat, lon, value.lat, value.lon);

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

        if (bestIcao == null)
        {
            return null;
        }

        return new CloseAirport(bestIcao, bestDistance);
    }

    /**
     * This returns a randomly selected airport found with the passed in parameters
     *
     * @param id          - airport to center on
     * @param minDistance - minimum distance to search for
     * @param maxDistance - maximum distance to search for
     * @param minsize     - minimum airport size to search for
     * @param maxsize     - maximum airport size to search for
     * @param lat         - used to compute correct value for longitudinal degree values for distance
     * @param icaoSet     - preselected icaos to search through
     * @param waterOk     - ok, to include water airports
     */
    public static CloseAirport getRandomCloseAirport(String id, double minDistance, double maxDistance, int minsize, int maxsize, double lat, Set<String> icaoSet, boolean waterOk)
    {
        CloseAirport returnValue = null;

        //if the template has defined ICAOs, search them first for one meeting min/max distance criteria
        if (icaoSet != null && !icaoSet.isEmpty())
        {
            String airports[] = icaoSet.toArray(new String[icaoSet.size()]);

            //new code that filters the list of airports down to the ones that meet
            //the min/max distance criteria
            Set<String> inrange = new HashSet<>();
            for (String airport : airports)
            {
                double[] distanceBearing = getDistanceBearing(id, airport);
                if (distanceBearing != null &&
                        distanceBearing[0] != 0 &&
                        (distanceBearing[0] >= minDistance && distanceBearing[0] <= maxDistance))
                {
                    inrange.add(airport);
                }
            }

            //if 1 or more airports met the criteria, randomly select one and return
            if (inrange.size() > 0)
            {
                String aps[] = inrange.toArray(new String[inrange.size()]);
                int index = (int) (aps.length * Math.random());
                double[] distanceBearing = getDistanceBearing(id, aps[index]);

                return new CloseAirport(aps[index], distanceBearing[0], distanceBearing[1]);
            }
        }

        //If no ICAO was found in the passed in ICAOset then
        //query the DB to see if any airport meets the min/max distance criteria

        //convert the distance to degrees (60nm at the equator = 1 degree)
        double degreeClipLat = Math.abs(maxDistance / 60.0);
        //adjust for compression toward poles
        double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(lat)));

        //Prepare for what types of aiports we are looking for
        // 0 = civil, 1 = water, 2 = military
        boolean[] aptypes = {false, false, false};

        aptypes[0] = true;
        if (waterOk)
        {
            aptypes[1] = true;
        }

        //get the airports that met the criteria passed
        Hashtable<String, LatLonSize> results = getAirportsInRange(id, degreeClipLat, degreeClipLon, minsize, maxsize, aptypes);

        //Don't bother if none found
        if (results != null && results.size() != 0)
        {
            LatLonSize icao = cachedAPs.get(id);
            String key;
            LatLonSize value;
            double[] distbearing;

            //Iterate through the returned set
            Enumeration<String> keys = results.keys();
            while (keys.hasMoreElements())
            {
                //get our current loop key and value
                key = keys.nextElement();
                value = results.get(key);

                distbearing = getDistanceBearing(icao, value);

                //filter out the ones that don't meet the minimum distance, or is the center airport
                if (id.contains(key) || distbearing[0] < minDistance || distbearing[0] > maxDistance)
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

                    distbearing = getDistanceBearing(icao, value);
                    returnValue = new CloseAirport(key, distbearing[0], distbearing[1]);
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
                    distbearing = getDistanceBearing(icao, value);
                    returnValue = new CloseAirport(key, distbearing[0], distbearing[1]);
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
                LatLonSize lls1 = cachedAPs.get(rs.getString(1));
                LatLonSize lls2 = cachedAPs.get(rs.getString(2));
                double[] distanceBearing = getDistanceBearing(lls1, lls2);

                double distance = distanceBearing[0];
                double bearing = distanceBearing[1];

                if (distance >= minDistance && distance < maxDistance)
                {
                    result.add(new CloseAirport(rs.getString(2), distance, bearing));
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
     * @param id          - airport to center on
     * @param minDistance - minimum distance to search for
     * @param maxDistance - maximum distance to search for
     *                    Airboss 5/30/11
     */
    public static List<CloseAirport> fillCloseAirports(String id, double minDistance, double maxDistance)
    {
        if (id == null)
        {
            return null;
        }

        List<CloseAirport> result = new ArrayList<>();

        String key;
        LatLonSize value;
        LatLonSize icao;

        icao = cachedAPs.get(id);

        if (icao == null)
        {
            return null;
        }

        //Prepare for what types of aiports we are looking for
        // 0 = civil, 1 = water, 2 = military
        boolean[] aptypes = {true, true, true};

        //convert the distance to degrees (60nm at the equator = 1 degree)
        double degreeClipLat = Math.abs(maxDistance / 60.0);

        //adjust for compression toward poles
        double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(icao.lat)));

        Hashtable<String, LatLonSize> results = getAirportsInRange(id, degreeClipLat, degreeClipLon, 0, 0, aptypes);

        //loop through the results to see if any are closest
        Enumeration<String> keys = results.keys();
        while (keys.hasMoreElements())
        {
            //get our current loop key and value
            key = keys.nextElement();
            value = results.get(key);

            //get the distance / bearing
            double[] distance = getDistanceBearing(icao, value);

            boolean skipself = id.contains(key);

            //check if minimum distance met, we already know it meets the max distance (2 degrees or 120nm)
            if (!skipself && distance[0] >= minDistance)
            {
                result.add(new CloseAirport(key, distance[0], distance[1]));
            }
        }

        //limit to return the closest 15 only
        Collections.sort(result);

        if (result.size() > 12)
        {
            result = new ArrayList<>(result.subList(0, 12));
        }

        return result;
    }

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

    public static List<AirportBean> getAirportsByBucket(String icao)
    {
        return getAirportSQL("SELECT * FROM airports WHERE bucket = (SELECT bucket from airports WHERE icao = '" + icao + "') order by name");
    }

    public static AirportBean getAirport(String icao)
    {
        List<AirportBean> result = getAirportSQL("SELECT * FROM airports WHERE icao='" + Converters.escapeSQL(icao) + "'");
        return result.size() == 0 ? null : result.get(0);
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
            for (int i = 1; i > 12; i++)
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
        if (icao != null && cachedAPs.get(icao.toUpperCase()) != null)
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
        if (!cachedAPs.containsKey(icao.toUpperCase()))
        {
            return results;
        }

        //get the current year and month
        Calendar cal = Calendar.getInstance();
        int curryear = cal.get(Calendar.YEAR);
        int currmonth = cal.get(Calendar.MONTH) + 1; //0 based instead of 1

        //setup our loop variables
        int loopyear = curryear;
        int loopmonth = currmonth;

        try
        {
            //get our records, using opyear in (?,?,?) allows index to be used
            String qry = "SELECT * FROM flightops WHERE opyear in (?,?,?) AND icao= ? ORDER BY opyear DESC, opmonth DESC";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, curryear - 2, curryear - 1, curryear, icao);

            int opyear;
            int opmonth;
            int ops;

            //loop though results, remember this is a spare array and might be missing 1 or more
            while (rs.next())
            {
                //get our data
                opyear = rs.getInt("opyear");
                opmonth = rs.getInt("opmonth");
                ops = rs.getInt("ops");

                //if everything matches just add it
                if (opyear == loopyear && opmonth == loopmonth)
                {
                    results.add(new FlightOp(opyear, opmonth, icao, ops));
                }
                else //oops no match
                {
                    //Fill in months with no ops
                    FillZeroOps(results, icao, opmonth + 1, opyear, loopmonth, loopyear);

                    //ok, we are ready to add in our current record now after backfilling the data
                    results.add(new FlightOp(opyear, opmonth, icao, ops));

                    loopmonth = opmonth;
                    loopyear = opyear;
                }

                //if we have 24 (or more) we are done
                if (results.size() >= 25)
                {
                    break;
                }

                //decrement to our next anticipated month
                loopmonth--;

                //if the month is 0, then we need to loop back around to 12
                if (loopmonth <= 0)
                {
                    loopmonth = 12;
                    //Since we moved to december, we also need to decrement the year
                    loopyear--;

                    //If we are past the two year mark something bad has happend so exit the loop
                    if (loopyear < curryear - 2)
                    {
                        break;
                    }
                }
            }
            if (results.size() < 25)
            {
                int amtleft = 24 - results.size();
                if (loopmonth >= amtleft)
                {
                    FillZeroOps(results, icao, loopmonth - amtleft, loopyear, loopmonth, loopyear);
                }
                else
                {
                    int amt = amtleft - loopmonth;
                    FillZeroOps(results, icao, 12 - amt, loopyear - 1, loopmonth, loopyear);
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
        for (int i = 1; i <= 12; i++) //correction Airboss 8/1/2011
        {
            average += ao.get(i).ops;
        }

        //return the current, and the average
        result[0] = ao.get(0).ops;
        result[1] = average / 12;
        return result;
    }

    public static void fillAirport(AirportBean input)
    {
        try
        {
            String qry = "SELECT * FROM airports WHERE icao = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, input.getIcao());
            if (rs.next())
            {
                input.fill(rs);
                input.setLandingFee(0);
                input.setFuelPrice(Goods.getFuelPrice(input.getIcao()));
                input.setJetaPrice(Goods.getJetaMultiplier());
            }
            else
            {
                input.available = false;
            }

            input.closestAirports = fillCloseAirports(input.getIcao(), 0, 50);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<AirportBean> findAirports(boolean assignments, int modelId, String name, int distance, String from, boolean ferry, boolean buy, int commodity, int minAmount, boolean fuel, boolean jeta, boolean repair, boolean acForSale, boolean fbo, boolean isRentable) throws DataError
    {
        ArrayList<AirportBean> result = new ArrayList<>();
        String qry;
        try
        {
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
                //Code correction to prevent Sql error when fuel and ferry selected in aiport search parameters - Airboss 6/6/11
                tables.append(" LEFT JOIN aircraft on aircraft.location = airports.icao ");

                query.append(and);
                query.append(" aircraft.userlock is null ");

                if (modelId >= 0)
                {
                    query.append("AND aircraft.model = ").append(modelId).append(" ");
                    //gurka - added suport for rentable flag
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
                query.append(" (goods.type = ").append(commodity).append(" OR size >= ").append(Goods.commodities[commodity].getMinAirportSize()).append(")");
                and = " AND ";
            }

            if (name != null)
            {
                query.append(and);
                query.append("(airports.name LIKE '%").append(Converters.escapeSQL(name)).append("%' OR city LIKE '%").append(Converters.escapeSQL(name)).append("%')");
                and = "AND ";
            }

            double lat = 0, lon = 0;
            if (from != null)
            {
                qry = "SELECT lat, lon FROM airports WHERE icao = ?";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, from);
                if (!rs.next())
                {
                    throw new DataError("Airport " + from + " not found.");
                }

                lat = rs.getDouble(1);
                lon = rs.getDouble(2);
                double degreeClipLat = distance / 60.0;
                double degreeClipLon = Math.abs(degreeClipLat / Math.cos(Math.toRadians(lat)));

                query.append(and);
                query.append("(abs(lat - ").append(lat).append(") < ").append(degreeClipLat).append(" AND abs(lon - ").append(lon).append(") < ").append(degreeClipLon).append(")");
            }

            query.append(" ORDER BY icao LIMIT 100");

            qry = "SELECT DISTINCT airports.* FROM " + tables.toString() + query.toString();
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                AirportBean airport = new AirportBean(rs);
                if (from != null && getDistance(airport.getLat(), airport.getLon(), lat, lon) > distance)
                {
                    continue;
                }

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
                    tables.append(" LEFT JOIN accounts ON Accounts.id = fbo.owner ");
                    query.append(and);
                    query.append(" (fbo.active = 0 AND Accounts.id >0)");
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
    }/* airportLink with a gmap pop require in the jsp page:
     * <head>
	 * <script type="text/javascript" src="scripts/PopupWindow.js"></script>
	 * <script type="text/javascript"> var gmap = new PopupWindow(); </script>
	 * </head>	  
	 */

    public static String airportLink(AirportBean airport, HttpServletResponse response)
    {
        return airportLink(airport, null, null, null, response);
    }

    public static String airportLink(AirportBean airport, AirportBean gmapAirport, HttpServletResponse response)
    {
        return airportLink(airport, null, gmapAirport, null, response);
    }

    public static String airportLink(AirportBean airport, AirportBean gmapAirport, AirportBean gmapAirportTo, HttpServletResponse response)
    {
        return airportLink(airport, null, gmapAirport, gmapAirportTo, response);
    }

    public static String airportLink(AirportBean airport, String bulletCodeLocation, AirportBean gmapAirport, AirportBean gmapAirportTo, HttpServletResponse response)
    {
        if (airport == null)
        {
            return "";
        }

        String sorthelp = "";
        String image = "";
        if (gmapAirport != null)
        {
            sorthelp = Data.sortHelper(airport.getIcao());

            String icaodPart = "";
            if ((gmapAirportTo != null) && !gmapAirportTo.getIcao().equals(gmapAirport.getIcao()))
            {
                icaodPart = "&icaod=" + gmapAirportTo.getIcao();
            }

            image = "<A HREF=\"#\" onClick=\"gmap.setSize(620,530);gmap.setUrl('gmap.jsp?icao=" +
                    gmapAirport.getIcao() +
                    icaodPart +
                    "');gmap.showPopup('gmap');return false;\" NAME=\"gmap\" ID=\"gmap\">" +
                    "<img src=\"" +
                    airport.getDescriptiveImage(Fbos.getFboByLocation(airport.getIcao())) +
                    "\" align=\"absmiddle\" border=\"0\" /></a>";
        }

        String bulletPart = "";
        if ((bulletCodeLocation != null) && (!bulletCodeLocation.equals("")))
        {
            bulletPart = bulletCodeLocation + " onMouseOut=\"hideBullet()\" ";
        }

        String href = response.encodeRedirectURL("/airport.jsp?icao=" + airport.getIcao());
        String textLink = "<a title=\"" + airport.getTitle() + "\" " +
                bulletPart +
                " class=\"normal\" href=\"" + href + "\">" +
                airport.getIcao() +
                "</a>";

        return sorthelp + image + textLink;
    }

    public static String getBearingImageURL(double bearing)
    {
        int id = (int) Math.round(bearing / 45.0) % 8;
        return "img/set2_" + id + ".gif";
    }

    public static String getAirportName(String icao)
    {
        String result = null;

        try
        {
            String qry = "SELECT name FROM airports WHERE icao = ?;";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), icao);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
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
     * Airboss 7/1/11
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
}
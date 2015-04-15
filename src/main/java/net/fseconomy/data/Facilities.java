package net.fseconomy.data;

import net.fseconomy.beans.*;
import net.fseconomy.util.Helpers;

import java.io.Serializable;
import java.sql.*;
import java.util.*;

public class Facilities implements Serializable
{
    public static void rentFacility(UserBean user, int occupantId, int facilityId, int blocks) throws DataError
    {
        if (blocks < 1)
        {
            throw new DataError("No gates selected to rent.");
        }

        try
        {
            if ((user.getId() != occupantId) && (user.groupMemberLevel(occupantId) < UserBean.GROUP_STAFF))
            {
                throw new DataError("Permission denied.");
            }

            int existingFacilityId = -1;

            FboFacilityBean landlord = getFacility(facilityId);
            if (!landlord.getIsDefault())
            {
                existingFacilityId = landlord.getId();
                landlord = getDefaultFacility(landlord.getFboId());
            }

            FboBean fbo = Fbos.getFbo(landlord.getFboId());

            if (blocks > calcFacilitySpaceAvailable(landlord, fbo))
            {
                throw new DataError("Not enough space available.");
            }

            Calendar paymentDate = GregorianCalendar.getInstance();
            int daysInMonth = paymentDate.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
            int daysLeftInMonth = daysInMonth - paymentDate.get(GregorianCalendar.DAY_OF_MONTH) + 1;
            int rent = Math.round(landlord.getRent() * ((float) daysLeftInMonth / (float) daysInMonth)) * blocks;

            if ((occupantId != landlord.getOccupant()) && (!Banking.checkFunds(occupantId, (double) rent)))
            {
                throw new DataError("Not enough money to pay first month rent. $" + rent + ".00 needed.");
            }

            if (existingFacilityId != -1)
            {
                String qry = "UPDATE fbofacilities SET size = size + ?, lastRentPayment = ? WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, blocks, new Timestamp(paymentDate.getTime().getTime()), existingFacilityId);
            }
            else
            {
                String qry = "INSERT INTO fbofacilities (location, fboId, occupant, size, name, units, commodity, maxDistance, matchMaxSize, publicByDefault, lastRentPayment) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
                DALHelper.getInstance().ExecuteUpdate(qry, fbo.getLocation(), fbo.getId(), occupantId, blocks, "Rented Facility", landlord.getUnits(), FboFacilityBean.DEFAULT_COMMODITYNAME_PASSENGERS, 300, 99999, 1, new Timestamp(paymentDate.getTime().getTime()));
            }

            Banking.doPayment(occupantId, landlord.getOccupant(), (double) rent, PaymentBean.FBO_FACILITY_RENT, 0, fbo.getId(), fbo.getLocation(), 0, "", false);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<FboFacilityBean> getFacilitiesByOccupant(int account)
    {
        return getFacilitiesSql("select t.* from fbofacilities t where t.occupant = " + account + " order by location, id");
    }

    public static List<FboFacilityBean> getDefaultFacilitiesForAirport(AirportBean airport)
    {
        return getFacilitiesForAirport(airport.getIcao());
    }

    public static List<FboFacilityBean> getDefaultFacilitiesForAirport(String icao)
    {
        return getFacilitiesSql("SELECT * FROM fbofacilities WHERE reservedSpace >= 0 AND location ='" + icao + "' order by id");
    }

    public static List<FboFacilityBean> getFacilitiesForAirport(String icao)
    {
        return getFacilitiesSql("select t.* from fbofacilities t, fbo f where t.fboId = f.id and f.active = 1 and f.location = '" + icao + "' order by id");
    }

    public static FboFacilityBean getDefaultFacility(FboBean fbo)
    {
        return getDefaultFacility(fbo.getId());
    }

    public static FboFacilityBean getDefaultFacility(int fboId)
    {
        List<FboFacilityBean> result = getFacilitiesSql("SELECT * FROM fbofacilities WHERE reservedSpace >= 0 AND fboId=" + fboId);
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<FboFacilityBean> getRenterFacilities(FboBean fbo)
    {
        return getFacilitiesSql("select * from fbofacilities where reservedSpace < 0 and fboId = " + fbo.getId() + " order by id");
    }

    public static FboFacilityBean getFacility(int id)
    {
        List<FboFacilityBean> result = getFacilitiesSql("SELECT * FROM fbofacilities WHERE id=" + id);
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<FboFacilityBean> getFacilitiesSql(String qry)
    {
        ArrayList<FboFacilityBean> result = new ArrayList<>();

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                result.add(new FboFacilityBean(rs));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int calcFacilitySpaceAvailable(FboFacilityBean facility, FboBean fbo)
    {
        int spaceInUse = getFacilityBlocksInUse(fbo.getId());
        int fboSlots = Airports.getTotalFboSlots(fbo.getLocation());
        int totalSpace = fbo.getFboSize() * fboSlots;

        return Math.max(0, totalSpace - spaceInUse - facility.getReservedSpace());
    }

    public static int getFacilityBlocksInUse(int fboId)
    {
        int result = 0;
        try
        {
            String qry = "select sum(size) from fbofacilities where reservedSpace < 0 and fboId = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), fboId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }// Note: deleteFbo() changed to reduce size by one, and only delete when size reaches 0

    public static void deleteFacility(UserBean user, int facilityId) throws DataError
    {
        try
        {
            FboFacilityBean facility = getFacility(facilityId);
            if (facility == null)
            {
                throw new DataError("Facility not found.");
            }

            if (!facility.deleteAllowed(user))
            {
                throw new DataError("Permission denied.");
            }

            if (facility.getIsDefault())
            {
                throw new DataError("The default facility can not be removed.");
            }

            String qry = "UPDATE fbofacilities SET size = size - 1 WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, facilityId);

            qry = "DELETE FROM fbofacilities WHERE reservedspace < 0 and size < 1";
            DALHelper.getInstance().ExecuteUpdate(qry);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateFacility(FboFacilityBean facility, List<FboFacilityBean> renters, UserBean user) throws DataError
    {
        if (facility == null)
        {
            return;
        }

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try
        {
            if (!facility.updateAllowed(user))
            {
                throw new DataError("Permission denied.");
            }

            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            String icaos = facility.getIcaoSet();
            if (!Helpers.isNullOrBlank(icaos))
            {
                String items[] = icaos.toUpperCase().trim().split(", *");
                icaos = "";
                if (items.length == 0)
                {
                    throw new DataError("ICAO set returned zero items.");  // should not happen
                }

                for (String item1 : items)
                {
                    String icao = item1.trim();
                    if (!Airports.isValidIcao(icao))
                    {
                        throw new DataError("ICAO '" + icao + "' not found.");
                    }

                    if (Airports.getDistance(facility.getLocation(), icao) > FboFacilityBean.MAX_ASSIGNMENT_DISTANCE)
                    {
                        throw new DataError("ICAO '" + icao + "' is too far. " + FboFacilityBean.MAX_ASSIGNMENT_DISTANCE + " NM limit in place.");
                    }

                    if (icaos.length() == 0)
                    {
                        icaos = icao;
                    }
                    else
                    {
                        icaos = icaos + ", " + icao;
                    }
                }

                facility.setIcaoSet(icaos);
            }

            rs = stmt.executeQuery("SELECT * FROM fbofacilities WHERE fboId = " + facility.getFboId());
            while (rs.next())
            {
                if (rs.getInt("id") == facility.getId())
                {
                    facility.writeBean(rs);
                    rs.updateRow();
                }
                else if (renters != null)
                {
                    for (FboFacilityBean renter : renters)
                    {
                        if (rs.getInt("id") == renter.getId())
                        {
                            rs.updateBoolean("allowRenew", renter.getAllowRenew());
                            rs.updateRow();
                            break;
                        }
                    }
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

    public static int getFacilityJobCount(int facilityId)
    {
        int cnt = 0;
        try
        {
            String qry = "select t.* from fbofacilities t where t.id = ? order by location, id";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, facilityId);

            while (rs.next())
            {
                int facid = rs.getInt("id");
                qry = "SELECT sum(amount) FROM assignments where fromfbotemplate = ?";
                cnt += DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), facid);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return cnt;
    }

    /**
     * return a collection of email addresses associated with renter ID's (Account ID's in the Accounts table or GrouID's in the GroupMembership table)
     *
     * @ param renters - ArrayList of renter ID's
     * @ return ArrayList - collection of email addresses
     * @ author - Gurka
     */
    public static List<String> getEmailAddressForRenterIDs(List<Integer> renters)
    {
        String qry;
        String result;
        List<String> emails = new ArrayList<>();

        try
        {
            for (int id : renters)
            {
                String type = Accounts.getAccountTypeById(id);
                if ("group".contains(type))
                {
                    //get email ID's for the staff belonging to the group which rented this facility
                    qry = "SELECT email FROM accounts, groupmembership AS gm WHERE id = userID and (gm.level = 'staff' or gm.level = 'owner') and groupID = ?";
                    ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
                    while (rs.next())
                        emails.add(rs.getString(1));
                }
                else
                {
                    //get email ID for the user
                    qry = "SELECT email FROM accounts WHERE id = ?";
                    result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);
                    if (result != null) //this should never be null, but check anyways
                        emails.add(result);
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return emails;
    }
}
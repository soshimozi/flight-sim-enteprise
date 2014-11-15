package net.fseconomy.data;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.beans.AssignmentBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.util.Converters;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Assignments implements Serializable
{
    public static final int ASSIGNMENT_ACTIVE = 0;
    public static final int ASSIGNMENT_ENROUTE = 1;
    public static final int ASSIGNMENT_HOLD = 2;

    static final int MAX_FLIGHT_ASSIGNMENTS = 60;


    public static String BuildAssignmentCargoFilter(int minPax, int maxPax, int minKG, int maxKG)
    {
        String paxFilter = "";
        if (minPax > -1 || maxPax > -1)
        {
            paxFilter = "units = " + AssignmentBean.UNIT_PASSENGERS + " AND ";
            if (minPax > -1)
            {
                paxFilter = paxFilter + "amount >= " + minPax + " ";
                if (maxPax > -1)
                {
                    paxFilter = paxFilter + "AND ";
                }
            }
            if (maxPax > -1)
            {
                paxFilter = paxFilter + "amount <= " + maxPax;
            }

            paxFilter = "(" + paxFilter + ")";
        }

        String kgFilter = "";
        if (minKG > -1 || maxKG > -1)
        {
            kgFilter = "units = " + AssignmentBean.UNIT_KG + " AND ";
            if (minKG > -1)
            {
                kgFilter = kgFilter + "amount >= " + minKG + " ";
                if (maxKG > -1)
                {
                    kgFilter = kgFilter + "AND ";
                }
            }
            if (maxKG > -1)
            {
                kgFilter = kgFilter + "amount <= " + maxKG;
            }

            kgFilter = "(" + kgFilter + ")";
        }

        if (paxFilter.length() > 0 && kgFilter.length() > 0)
        {
            return "(" + paxFilter + " OR " + kgFilter + ") AND ";
        }
        else if (paxFilter.length() > 0)
        {
            return paxFilter + " AND ";
        }
        else if (kgFilter.length() > 0)
        {
            return kgFilter + " AND ";
        }
        else
        {
            return "";
        }
    }

    public static List<AssignmentBean> getAssignmentsToArea(String location, List<CloseAirport> locations, int minPax, int maxPax, int minKG, int maxKG)
    {
        StringBuilder where = new StringBuilder("'" + Converters.escapeSQL(location) + "'");
        for (CloseAirport location1 : locations)
        {
            where.append(", '").append(location1.icao).append("'");
        }

        String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);

        return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " toicao in (" + where.toString() + ") ORDER BY bearing");
    }

    public static List<AssignmentBean> getAssignmentsInArea(String location, List<CloseAirport> locations, int minPax, int maxPax, int minKG, int maxKG)
    {
        StringBuilder where = new StringBuilder("'" + Converters.escapeSQL(location) + "'");
        for (CloseAirport location1 : locations)
        {
            where.append(", '" + location1.icao + "'");
        }

        String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);

        return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " location in (" + where.toString() + ") ORDER BY bearing");
    }

    public static AssignmentBean getAssignmentById(int id)
    {
        return getAssignmentSQL("SELECT * FROM assignments WHERE id=" + id);
    }

    public static List<AssignmentBean> getAssignmentsForGroup(int groupId, boolean includelocked)
    {
        if (includelocked)
        {
            return getAssignmentsSQL("SELECT * FROM assignments WHERE groupId=" + groupId + " ORDER BY location");
        }
        else
        {
            return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId=" + groupId + " ORDER BY location");
        }
    }

    public static List<AssignmentBean> getAssignmentsForUser(int userId)
    {
        return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock=" + userId);
    }

    public static List<AssignmentBean> getAssignmentsForTransfer(int userId)
    {
        return getAssignmentsSQL("SELECT * FROM assignments WHERE owner=" + userId);
    }

    public static List<AssignmentBean> getAssignments(String location, int minPax, int maxPax, int minKG, int maxKG)
    {
        String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);
        return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " location='" + location + "' AND (aircraft is null OR location = (SELECT location FROM aircraft WHERE aircraft.registration = assignments.aircraft)) ORDER BY bearing");
    }

    public static List<AssignmentBean> getAssignmentsToAirport(String location, int minPax, int maxPax, int minKG, int maxKG)
    {
        String cargoFilter = BuildAssignmentCargoFilter(minPax, maxPax, minKG, maxKG);
        return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId is null AND " + cargoFilter + " toicao='" + location + "' AND (aircraft is null OR location = (SELECT location FROM aircraft WHERE aircraft.registration = assignments.aircraft)) ORDER BY bearing");
    }

    public static List<AssignmentBean> getAssignmentsFromAirport(String location)
    {
        String sql = "SELECT * FROM assignments WHERE userlock is null AND groupId is null AND location IN(" + location + ") ORDER BY location, distance";

        return getAssignmentsSQL(sql);
    }

    public static List<AssignmentBean> getAssignmentsToAirport(String location)
    {
        String sql = "SELECT * FROM assignments WHERE userlock is null AND groupId is null AND toicao IN(" + location + ") ORDER BY location, distance";

        return getAssignmentsSQL(sql);
    }

    public static AssignmentBean getAssignmentSQL(String qry)
    {
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            if (rs.next())
            {
                return new AssignmentBean(rs);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static List<AssignmentBean> getAssignmentsSQL(String qry)
    {
        ArrayList<AssignmentBean> result = new ArrayList<AssignmentBean>();
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                AssignmentBean assignment = new AssignmentBean(rs);
                result.add(assignment);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void unlockAssignment(int id, boolean unlockAll)
    {
        try
        {
            String qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE (active = 0 or active = 2) AND id = ?";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (exists)
            {
                if (unlockAll)
                {
                    qry = "UPDATE assignments SET userlock = NULL, active = 0, groupId = null, pilotFee = 0, comment = null WHERE id = ?";
                }
                else
                {
                    qry = "UPDATE assignments SET userlock = NULL, active = 0 WHERE id = ?";
                }

                DALHelper.getInstance().ExecuteUpdate(qry, id);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void holdAssignment(int id, boolean hold)
    {
        try
        {
            String qry;
            int activeflag = hold ? ASSIGNMENT_HOLD : ASSIGNMENT_ACTIVE;

            if (hold)
            {
                qry = "UPDATE assignments SET active = ? WHERE active = 0 AND id = ? AND aircraft IS NULL";
            }
            else
            {
                qry = "UPDATE assignments SET active = ? WHERE active = 2 and id = ?";
            }

            DALHelper.getInstance().ExecuteUpdate(qry, activeflag, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void moveAssignment(UserBean user, int id, int groupid) throws DataError
    {
        UserBean group = Accounts.getGroupById(groupid);
        if (group == null)
        {
            throw new DataError("Group not found.");
        }

        try
        {
            String qry = "SELECT (count(*) > 0) from assignments where id = ?";
            boolean idExists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (!idExists) //just return if not found
            {
                return;
            }

            //All-In check - can't add All-In flight to a group
            qry = "SELECT (count(*) > 0) from assignments where id = ? and aircraft is not null";
            boolean isAllIn = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (isAllIn)
            {
                throw new DataError("All-In assignments cannot be added to a group queue.");
            }

            //See if already locked
            qry = "SELECT userlock, groupId from assignments where id = ?";
            int userlock = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), id);
            if (userlock != 0 && userlock != user.getId())
            {
                throw new DataError("Assignment is already selected by a pilot.");
            }

            //Move it to group assignments
            qry = "UPDATE assignments SET pilotFee=pay*amount*distance*0.01*?, groupId = ? WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, group.getDefaultPilotFee() / 100.0, groupid, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void commentAssignment(int id, String comment) throws DataError
    {
        try
        {
            String qry = "SELECT (count(*) > 0) from assignments where id = ?";
            boolean idExists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (!idExists) //just return if not found
            {
                return;
            }

            //Move it to group assignments
            qry = "UPDATE assignments SET comment=? WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, comment, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void addAssignment(int id, int user, boolean add) throws DataError
    {
        try
        {
            //Myflight assignment limit rule
            String qry = "SELECT (count(*) >= ?) FROM assignments WHERE userlock= ?";
            boolean toManyJobs = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), MAX_FLIGHT_ASSIGNMENTS, user);
            if (toManyJobs)
            {
                throw new DataError("You have reached the limit of allowed assignments. Limit: " + MAX_FLIGHT_ASSIGNMENTS);
            }

            //No assignment found
            qry = "SELECT (count(*) = 0) AS notFound FROM assignments WHERE id = ?";
            boolean noRecord = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (noRecord)
            {
                throw new DataError("No assignment found for id: " + id);
            }

            //Get aircraft registration for assignment
            qry = "SELECT aircraft FROM assignments WHERE id = ?";
            String aircraft = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);

            //This is an All-In job if it has an assigned airplane to the job already
            //Do our checks and rent the aircraft
            if (aircraft != null)
            {
                //No jobs in the loading area
                qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE userlock = ? AND active <> 2";
                boolean noLoadAreaJobs = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
                if (noLoadAreaJobs)
                {
                    throw new DataError("Cannot have any jobs in the Loading Area when trying to select an All-In job.");
                }

                //existing all-in jobs in queue for user
                qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE userLock = ? AND aircraft IS NOT NULL";
                boolean foundAllIn = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
                if (foundAllIn)
                {
                    throw new DataError("FSE Validation Error: cannot have more than 1 All-In job in My Flight queue.");
                }

                Aircraft.rentAircraft(aircraft, user, false);
            }
            else
            {
                //All-In validation - cannot add regular assignments when there is an active all-in job
                if (hasAllInJobInQueue(user))
                {
                    throw new DataError("FSE Validation Error: cannot mix All-In jobs with regular jobs.");
                }
            }

            qry = "UPDATE assignments SET userlock = ? where id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, user, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void removeAssignment(int id, int user, boolean add) throws DataError
    {
        try
        {
            //No assignment found
            String qry = "SELECT (count(*) = 0) AS notFound FROM assignments WHERE id = ?";
            boolean noRecord = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (noRecord)
            {
                throw new DataError("No assignment found for id: " + id);
            }

            //Get aircraft registration for assignment
            qry = "SELECT aircraft FROM assignments WHERE id = ?";
            String aircraft = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);

            //if All-In job, remove lock on aircraft now that job is canceled
            if (aircraft != null)
            {
                Aircraft.releaseAircraft(aircraft, user);
            }

            qry = "UPDATE assignments SET userlock = null, active = 0 where id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateAssignment(AssignmentBean assignment, UserBean user) throws DataError
    {
        Boolean localconn = false;
        Connection conn = null;
        Statement stmt = null, check = null;
        ResultSet rs = null, checkrs = null;

        if (assignment.getOwner() != user.getId() &&
                user.groupMemberLevel(assignment.getOwner()) < UserBean.GROUP_STAFF &&
                user.groupMemberLevel(assignment.getGroupId()) < UserBean.GROUP_STAFF)
        {
            throw new DataError("Permission denied");
        }

        try
        {
            boolean newEntry;

            if (conn == null)
            {
                localconn = true;
                conn = DALHelper.getInstance().getConnection();
            }

            assignment.updateData();

            if (assignment.getActive() == 1)
            {
                throw new DataError("The assignment is in flight");
            }

            if (assignment.isGoods() && assignment.getAmount() < 1)
            {
                throw new DataError("Transfer assignments must have a quantity greater than zero");
            }

            if (assignment.isCreatedByUser() && assignment.calcPay() < 0)
            {
                throw new DataError("Assignment pay may not be less than zero");
            }

            if (assignment.getPilotFee() < 0)
            {
                throw new DataError("Pilot fee may not be less than zero");
            }

            if (assignment.isGroup() && !Banking.checkAnyFunds(assignment.getGroupId(), assignment.getPilotFee()))
            {
                throw new DataError("Not enough money for paying this pilot fee.");
            }

            if (assignment.calcPay() != 0 && assignment.isCreatedByUser() && !Banking.checkAnyFunds(assignment.getOwner(), assignment.calcPay()))
            {
                throw new DataError("Not enough money for paying this assignment. ");
            }

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from assignments WHERE id = " + assignment.getId());
            int oldAmount = 0;
            if (!rs.next())
            {
                newEntry = true;
                rs.moveToInsertRow();
                oldAmount = 0;
            }
            else
            {
                newEntry = false;
                oldAmount = rs.getInt("amount");
            }
            int diffAmount = assignment.getAmount() - oldAmount;

            //added for aircraft shipping - Airboss 1/10/11
            if (diffAmount != 0 && assignment.getCommodityId() > 0 && assignment.getCommodityId() < 99) //ignore aircraft crate
            {
                Goods.changeGoodsRecord(assignment.getLocation(), assignment.getCommodityId(), assignment.getOwner(), -diffAmount, false);
            }

            assignment.writeBean(rs);

            if (newEntry)
            {
                rs.insertRow();
            }
            else
            {
                rs.updateRow();
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
            DALHelper.getInstance().tryClose(checkrs);
            DALHelper.getInstance().tryClose(check);

            if (localconn)
            {
                DALHelper.getInstance().tryClose(conn);
            }
        }
    }

    public static int getPTAssignmentCount(int user)
    {
        int ptAssignmentCount = 0;

        try
        {
            String qry = "SELECT count(*) FROM assignments WHERE fromTemplate is null AND fromFboTemplate is not null AND userlock = ? AND active = 1";
            ptAssignmentCount = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), user);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return ptAssignmentCount;
    }

    public static boolean checkAllInAircraftWithOutAssigment(AircraftBean aircraft) throws DataError
    {
        try
        {
            String qry = "SELECT (fuelSystemOnly=1) as found FROM aircraft, models where aircraft.model=models.id AND fuelSystemOnly=1 AND (rentalPrice is null OR rentalPrice=0) AND registration = ? AND location = ?";
            boolean isAllInAircraft = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getRegistration(), aircraft.getLocation());

            if (isAllInAircraft && !hasAllInJobInQueue(aircraft.getUserLock()))
            {
                return true;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkAllInFlightWithAssignment(AircraftBean aircraft) throws DataError
    {
        try
        {
            String qry = "SELECT (fuelSystemOnly=1) as found FROM aircraft, models where aircraft.model=models.id AND fuelSystemOnly=1 AND (rentalPrice is null OR rentalPrice=0) AND registration = ? AND location = ?";
            boolean isAllInAircraft = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getRegistration(), aircraft.getLocation());

            if (isAllInAircraft && !hasAllInJobInQueue(aircraft.getUserLock()))
            {
                return true;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasAllInJobInQueue(int user)
    {
        boolean hasAllInJob=false;

        try
        {
            String qry = "SELECT (count(id) > 0) AS found FROM assignments WHERE userlock = ? AND aircraft IS NOT NULL";
            hasAllInJob = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return hasAllInJob;
    }

    public static void deleteAssignment(int id, UserBean user) throws DataError
    {
        try
        {
            String qry = "SELECT * FROM assignments WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);
            if (!rs.next())
                throw new DataError("Assignment not found.");

            AssignmentBean assignment = new AssignmentBean(rs);

            if (assignment.getUserlock() > 0 && assignment.getUserlock() != user.getId())
                throw new DataError("Assignment is currently locked by a pilot.");

            if (!assignment.deleteAllowed(user))
                throw new DataError("Permission denied.");

            if (assignment.isGoods() && assignment.authorityOverGoods(user) && assignment.getCommodityId() < 99)
            {
                Goods.changeGoodsRecord(assignment.getLocation(), assignment.getCommodityId(), assignment.getOwner(), assignment.getAmount(), false);

                qry = "DELETE FROM assignments WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, id);
            }
            else if (assignment.isFerry())
            {
                qry = "DELETE FROM assignments WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, id);
            }
            else if (assignment.isGroup())
            {
                qry = "UPDATE assignments SET groupId = NULL, pilotFee = NULL, comment = NULL WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, id);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }


}
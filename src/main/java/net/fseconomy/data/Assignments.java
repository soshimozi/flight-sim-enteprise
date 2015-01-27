package net.fseconomy.data;

import net.fseconomy.beans.AircraftBean;
import net.fseconomy.beans.AssignmentBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.dto.CloseAirport;
import net.fseconomy.dto.DistanceBearing;
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
            where.append(", '").append(location1.icao).append("'");
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
            return getAssignmentsSQL("SELECT * FROM assignments WHERE groupId=" + groupId + " ORDER BY location, toicao");
        }
        else
        {
            return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock is null AND groupId=" + groupId + " ORDER BY location, toicao");
        }
    }

    public static List<AssignmentBean> getAssignmentsForUser(int userId)
    {
        return getAssignmentsSQL("SELECT * FROM assignments WHERE userlock=" + userId + " order by location, toicao");
    }

    public static List<AssignmentBean> getAssignmentsForTransfer(int ownerId)
    {
        return getAssignmentsSQL("SELECT * FROM assignments WHERE owner=" + ownerId);
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
        ArrayList<AssignmentBean> result = new ArrayList<>();
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

    public static void unlockAssignment(int id)
    {
        try
        {
            String qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE (active = 0 or active = 2) AND id = ?";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (exists)
            {
                qry = "UPDATE assignments SET userlock = NULL, active = 0 WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, id);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void unlockGoodsAssignment(int id) throws DataError
    {
        try
        {
            String qry = "SELECT (count(*) > 0) AS found FROM assignments WHERE (active = 0 or active = 2) AND id = ?";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (exists)
            {
                qry = "UPDATE assignments SET userlock = NULL, groupId = NULL, active = 0 WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, id);
            }
            else
                throw new DataError("Goods assignment not found, or enroute.");
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
            qry = "SELECT (userlock is not null) as found from assignments where id = ?";
            boolean isUserlock = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
            if (isUserlock)
            {
                throw new DataError("Assignment is locked by a pilot.");
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

    public static void newAssignment(int groupId, String fromIcao, String toIcao, double pilotFee, String comment) throws DataError
    {
        try
        {
            DistanceBearing db = Airports.getDistanceBearing(fromIcao,toIcao);
            String qry = "INSERT INTO assignments (creation, commodityid, owner, active, units, groupId, fromicao, location, toicao, distance, bearing, pilotfee, comment) VALUES(NOW(),0,0,0,'kg',?,?,?,?,?,?,?,?)";
            DALHelper.getInstance().ExecuteUpdate(qry, groupId, fromIcao, fromIcao, toIcao, db.distance, db.bearing, pilotFee, comment);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void addAssignment(int id, int user, boolean add, boolean isAirport) throws DataError
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

            if(isAirport)
            {
                qry = "SELECT (userlock is not null OR groupId is not null) as owned FROM assignments WHERE id = ?";
                boolean isOwned = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);

                if (isOwned)
                {
                    throw new DataError("Assignment already selected by a pilot or group.");
                }
            }

            //Get aircraft for assignment
            qry = "SELECT aircraftid FROM assignments WHERE id = ?";
            int aircraftId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), id);

            //This is an All-In job if it has an assigned airplane to the job already
            //Do our checks and rent the aircraft
            if (aircraftId > 0)
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

                Aircraft.rentAircraft(aircraftId, user, false);
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
            qry = "SELECT aircraftid FROM assignments WHERE id = ?";
            int aircraftId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), id);

            //if All-In job, remove lock on aircraft now that job is canceled
            if (aircraftId > 0)
            {
                Aircraft.releaseAircraft(aircraftId, user);
            }

            qry = "UPDATE assignments SET userlock = null, active = 0 where id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void removeAssignmentFromGroup(int[] assignments) throws DataError
    {
        try
        {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for( int i: assignments)
            {
                if(!first)
                    sb.append(", ");

                sb.append(i);

                first = false;
            }
            String qry = "UPDATE assignments SET groupId = null, active = 0, pilotfee = 0, comment = null WHERE userlock IS NULL AND id IN (";
            qry += sb.toString();
            qry += ")";
            DALHelper.getInstance().ExecuteUpdate(qry);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateAssignment(int assignmentId, int groupId, double pilotFee, String comment) throws DataError
    {
        try
        {
            //No assignment found
            String qry = "SELECT (userlock is not null) AS locked  FROM assignments WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, assignmentId);
            if (!rs.next())
            {
                throw new DataError("No assignment found for id: " + assignmentId);
            }

            //Assignment is locked!
            if (rs.getBoolean("locked"))
            {
                throw new DataError("Assignment is locked!");
            }

            if(groupId > 0 && pilotFee <= 0)
            {
                int defaultPay = Groups.getDefaultPay(groupId);
                qry = "UPDATE assignments SET pilotFee = pay*amount*distance*0.01*?, comment = ? WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, defaultPay/100.0, comment, assignmentId);
            }
            else
            {
                qry = "UPDATE assignments SET pilotFee = ?, comment = ? WHERE id = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, pilotFee, comment, assignmentId);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateGoodsAssignment(int assignmentId, int goodsAmount, int pilotFee, String comment) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try
        {
            conn = DALHelper.getInstance().getConnection();

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from assignments WHERE id = " + assignmentId);

            if (!rs.next())
            {
                throw new DataError("No assignment found for id: " + assignmentId);
            }

            AssignmentBean assignment = new AssignmentBean(rs);

            if (assignment.getActive() == 1)
            {
                throw new DataError("The assignment is in flight");
            }

            if (assignment.isGoods() && goodsAmount < 1)
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

            if (assignment.isGroup() && !Banking.checkAnyFunds(assignment.getGroupId(), pilotFee))
            {
                throw new DataError("Not enough money for paying this pilot fee.");
            }

            if (assignment.calcPay() != 0 && assignment.isCreatedByUser() && !Banking.checkAnyFunds(assignment.getOwner(), assignment.calcPay()))
            {
                throw new DataError("Not enough money for paying this assignment. ");
            }

            int diffAmount = assignment.getAmount() - goodsAmount;

            //added for aircraft shipping - Airboss 1/10/11
            if (diffAmount != 0 && assignment.getCommodityId() > 0 && assignment.getCommodityId() < 99) //ignore aircraft crate
            {
                Goods.changeGoodsRecord(assignment.getLocation(), assignment.getCommodityId(), assignment.getOwner(), diffAmount, false);
            }

            assignment.setAmount(goodsAmount);
            assignment.setPilotFee(pilotFee);
            assignment.setComment(comment);
            assignment.writeBean(rs);

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

    public static void updateAssignment(AssignmentBean assignment, UserBean user) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        if (assignment.getOwner() != user.getId() &&
                user.groupMemberLevel(assignment.getOwner()) < UserBean.GROUP_STAFF &&
                user.groupMemberLevel(assignment.getGroupId()) < UserBean.GROUP_STAFF)
        {
            throw new DataError("Permission denied");
        }

        try
        {
            boolean newEntry;

            conn = DALHelper.getInstance().getConnection();

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
            int oldAmount;
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
            DALHelper.getInstance().tryClose(conn);
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
            String qry = "SELECT (fuelSystemOnly=1) as found FROM aircraft, models where aircraft.model=models.id AND fuelSystemOnly=1 AND (rentalPrice is null OR rentalPrice=0) AND aircraft.id = ? AND location = ?";
            boolean isAllInAircraft = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), aircraft.getId(), aircraft.getLocation());

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
            String qry = "SELECT (count(id) > 0) AS found FROM assignments WHERE userlock = ? AND aircraftid IS NOT NULL";
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

    public static void deleteGoodsAssignment(int assignmentId, UserBean user) throws DataError
    {
        try
        {
            String qry = "SELECT * FROM assignments WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, assignmentId);
            if (!rs.next())
                throw new DataError("Assignment not found.");

            AssignmentBean assignment = new AssignmentBean(rs);

            if (assignment.getUserlock() > 0 && assignment.getUserlock() != user.getId())
                throw new DataError("Assignment is currently locked by a pilot.");

            if (!assignment.deleteAllowed(user))
                throw new DataError("Permission denied.");

            if (!assignment.isGoods() || !(assignment.getCommodityId() < 99))
                throw new DataError("Assignment is not a goods assignment.");

            Goods.changeGoodsRecord(assignment.getLocation(), assignment.getCommodityId(), assignment.getOwner(), assignment.getAmount(), false);

            qry = "DELETE FROM assignments WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, assignmentId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void deleteGroupAssignment(int assignmentId) throws DataError
    {
        try
        {
            //No assignment found
            String qry = "SELECT (userlock is not null) AS locked  FROM assignments WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, assignmentId);
            if (!rs.next())
            {
                throw new DataError("No assignment found for id: " + assignmentId);
            }

            //Assignment is locked!
            if (rs.getBoolean("locked"))
            {
                throw new DataError("Assignment is locked!");
            }

            qry = "DELETE FROM assignments WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, assignmentId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

}
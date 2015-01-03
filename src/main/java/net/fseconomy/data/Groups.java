package net.fseconomy.data;

import net.fseconomy.beans.GoodsBean;
import net.fseconomy.beans.PaymentBean;
import net.fseconomy.beans.UserBean;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Groups implements Serializable
{
    public static class groupMemberData implements Serializable
    {
        private static final long serialVersionUID = 1L;
        public int groupId;
        public int memberLevel;
        public String groupName;

        public groupMemberData(int groupId, int memberLevel, String name)
        {
            this.groupId = groupId;
            this.memberLevel = memberLevel;
            this.groupName = name;
        }
    }

    public static synchronized void updateGroup(UserBean group, UserBean user) throws DataError
    {
        Accounts.mustBeLoggedIn(user);

        if (group.getName().trim().length() < 4)
        {
            throw new DataError("Group name must be at least 4 characters.");
        }

        if (!(group.getDefaultPilotFee() >= 0 && group.getDefaultPilotFee() <= 100))
        {
            throw new DataError("Pilot Fee must be in the range of 0 to 100%");
        }

        try
        {
            String qry = "SELECT (count(id) > 0) as found FROM accounts WHERE type = 'group' AND id = ?";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), group.getId());
            if (!exists)
            {
                System.err.println("id=" + group.getId());
                throw new DataError("Group not found!");
            }

            qry = "UPDATE accounts SET name=?, comment=?, url=?, defaultPilotFee=?, banList=?, exposure=?, readAccessKey=? WHERE id=?";
            DALHelper.getInstance().ExecuteUpdate(qry, group.getName(), group.getComment(), group.getUrl(), group.getDefaultPilotFee(), group.getBanList(), group.getExposure(), group.getReadAccessKey(), group.getId());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static synchronized void CreateGroup(UserBean group, UserBean user) throws DataError
    {
        Accounts.mustBeLoggedIn(user);

        if (group.getName().trim().length() < 4)
        {
            throw new DataError("Group name must be at least 4 characters.");
        }

        if (group.getDefaultPilotFee() > 100)
        {
            throw new DataError("Pilot Fee cannot exceed 100%");
        }

        try
        {
            String qry = "select (count(*) > 0) from accounts where upper(name) = upper(?)";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), group.getName());
            if (exists)
            {
                throw new DataError("Group name already exists.");
            }

            qry = "INSERT INTO accounts (created, type, name, comment, url, defaultPilotFee, banList, exposure, readAccessKey) VALUES(?,?,?,?,?,?,?,?,?)";
            DALHelper.getInstance().ExecuteUpdate(qry, new Timestamp(System.currentTimeMillis()), "group", group.getName(), group.getComment(), group.getUrl(), group.getDefaultPilotFee(), group.getBanList(), group.getExposure(), group.getReadAccessKey());

            qry = "select id from accounts where upper(name) = upper(?)";
            int id = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), group.getName());
            joinGroup(user, id, "owner");

            Accounts.addAccountNote(user.getId(), user.getId(), "Created group: " + group.getName() + "[" + group.getId() + "]");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void joinGroup(UserBean user, int group, String level) throws DataError
    {
        Accounts.mustBeLoggedIn(user);

        try
        {
            String qry = "SELECT (count(userId) > 0) as found FROM groupmembership WHERE userId = ? AND groupId = ?";
            boolean found = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user.getId(), group);
            if (!found)
            {
                qry = "INSERT INTO groupmembership (userId, groupId, level) VALUES (?, ?, ?)";
                DALHelper.getInstance().ExecuteUpdate(qry, user.getId(), group, level);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        reloadMemberships(user);
    }

    public static void reloadMemberships(UserBean user)
    {
        LinkedHashMap<Integer, groupMemberData> memberships = new LinkedHashMap<>();
        try
        {
            boolean hasItems = false;

            String qry = "SELECT * FROM groupmembership, accounts WHERE groupId = accounts.id AND userId = ? order by accounts.name";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user.getId());
            while (rs.next())
            {
                int groupId = rs.getInt("groupId");
                int groupLevel =  UserBean.getGroupLevel(rs.getString("level"));

                memberships.put(groupId, new groupMemberData(groupId, groupLevel, rs.getString("name")));

                hasItems = true;
            }

            if (!hasItems)
                memberships = null;

            user.setMemberships(memberships);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void doInvitation(UserBean user, int group, boolean accept) throws DataError
    {
        Accounts.mustBeLoggedIn(user);
        try
        {
            String qry;

            if (accept)
            {
                qry = "UPDATE groupmembership SET level = 'member' where level = 'invited' AND userId = ? AND groupId = ?";
            }
            else
            {
                qry = "DELETE from groupmembership WHERE level = 'invited' AND userId = ? AND groupId = ?";
            }

            DALHelper.getInstance().ExecuteUpdate(qry, user.getId(), group);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        reloadMemberships(user);
    }

    public static void changeMembership(int user, int group, String level) throws DataError
    {
        try
        {
            String qry = "UPDATE groupmembership SET level = ? WHERE userId = ? AND groupId = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, level, user, group);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void leaveGroup(UserBean user, int group) throws DataError
    {
        try
        {
            String qry = "DELETE FROM groupmembership WHERE userId = ? AND groupId = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, user.getId(), group);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        reloadMemberships(user);
    }

    public static void deleteGroup(UserBean user, int groupId) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Accounts.mustBeLoggedIn(user);
        try
        {
            conn = DALHelper.getInstance().getConnection();

            UserBean group = Accounts.getGroupById(groupId);

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT money + bank, bank, name FROM accounts WHERE type = 'group' AND id = " + groupId);
            if (!rs.next())
            {
                throw new DataError("Group not found.");
            }

            float groupMoney = rs.getFloat(1);
            String groupName = rs.getString(3);
            if (groupMoney < 0)
            {
                throw new DataError("This group has a negative bank balance.");
            }

            // Remove system assignments from group assignments list
            stmt.executeUpdate("UPDATE assignments SET groupId = null, pilotFee = 0, comment = null WHERE fromTemplate is not null and groupId = " + groupId);

            // Remove assignments owned by others from group assignments list
            stmt.executeUpdate("UPDATE assignments SET groupId = null, pilotFee = 0, comment = null WHERE owner <> groupid and groupId = " + groupId);

            // Delete Ferry assignments. They can not be transfered to an individual.
            stmt.executeUpdate("DELETE FROM assignments WHERE fromTemplate is null and (commodityId is null or commodityId = 0) and groupId = " + groupId);

            // Change ownership of transfer assignments to group owner
            stmt.executeUpdate("UPDATE assignments SET owner = " + user.getId() + " WHERE owner = " + groupId);

            // Change ownership of leases to group owner - 8-26-12 Airboss
            stmt.executeUpdate("UPDATE aircraft SET lessor = " + user.getId() + " WHERE lessor = " + groupId);

            // No assignments should remain now where owner or groupid equals group


            // Transfer ownership of group owned aircraft
            stmt.executeUpdate("UPDATE aircraft SET owner = " + user.getId() + " WHERE owner = " + groupId);

            // Transfer ownership of group owned FBOs
            stmt.executeUpdate("UPDATE fbo SET owner = " + user.getId() + " WHERE owner = " + groupId);

            // Transfer ownership of FBO facilities
            stmt.executeUpdate("update fbofacilities set occupant = " + user.getId() + " where occupant = " + groupId);

            // Transfer ownership of group owned goods
            List<GoodsBean> goods = Goods.getGoodsForAccountAvailable(groupId);
            for (GoodsBean good : goods)
            {
                String location = good.getLocation();
                int type = good.getType();
                int amount = good.getAmount();
                Goods.changeGoodsRecord(location, type, groupId, -amount, false);
                Goods.changeGoodsRecord(location, type, user.getId(), amount, false);
            }

            // Delete group owned goods records (all zero amount now)
            stmt.executeUpdate("DELETE FROM goods WHERE owner = " + groupId);

            // The group should have no property now

            // Transfer funds
            Banking.doPayment(groupId, user.getId(), groupMoney, PaymentBean.GROUP_DELETION, 0, -1, "", 0, groupName, false);

            // Delete group membership
            stmt.executeUpdate("DELETE FROM groupmembership WHERE groupId = " + groupId);

            // Delete the group account
            stmt.executeUpdate("UPDATE accounts SET exposure = 0, comment = '" + user.getName() + "'  WHERE type='group' AND id = " + groupId);

            Accounts.addAccountNote(user.getId(), user.getId(), "Deleted group: " + group.getName() + "[" + groupId + "]");
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

        reloadMemberships(user);
    }

    public static void transferGroup(int userId, int transferTo, int groupId)
    {
        try
        {
            String qry = "{call groupTransfer(?,?,?,?)}";
            boolean success = DALHelper.getInstance().ExecuteStoredProcedureWithStatus(qry, groupId, userId, transferTo);
            if(success)
                Banking.addPaymentRecord(transferTo, userId, new Money(0), PaymentBean.TRANSFER_GROUP, -1, -1, "", -1, "Group Transfer");
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void flyForGroup(UserBean user, int groupid) throws DataError
    {
        UserBean group = Accounts.getGroupById(groupid);
        if (group == null)
        {
            throw new DataError("Group not found.");
        }

        try
        {
            if (user.groupMemberLevel(groupid) < UserBean.GROUP_MEMBER)
            {
                throw new DataError("Permission denied.");
            }

            String qry = "UPDATE assignments SET pilotFee=pay*amount*distance*0.01*?, groupId = ? WHERE userlock = ? and active <> 2";
            DALHelper.getInstance().ExecuteUpdate(qry, group.getDefaultPilotFee() / 100.0, groupid, user.getId());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void payMembers(UserBean user, int groupid, double money, String[] members, String comment) throws DataError
    {
        UserBean group = Accounts.getGroupById(groupid);

        if (group == null)
        {
            throw new DataError("Group not found.");
        }

        if (user.groupMemberLevel(groupid) < UserBean.GROUP_OWNER)
        {
            throw new DataError("Permission denied.");
        }

        if (group.getMoney() < money)
        {
            throw new DataError("Group does not have enough money.");
        }

        Money topay = new Money(money);
        topay.divide(members.length);

        for (String member : members)
        {
            Banking.doPayment(groupid, Integer.parseInt(member), topay, PaymentBean.GROUP_PAYMENT, 0, -1, "", 0, comment, false);
        }
    }

    public static void mailMembers(UserBean user, int groupId, String[] members, String text) throws DataError
    {
        UserBean group = Accounts.getGroupById(groupId);

        if (group == null)
        {
            throw new DataError("Group not found.");
        }

        if (user.groupMemberLevel(groupId) < UserBean.GROUP_OWNER)
        {
            throw new DataError("Permission denied.");
        }

        try
        {
            StringBuilder list = new StringBuilder("(");
            for (int c = 0; c < members.length; c++)
            {
                if (c > 0)
                {
                    list.append(", ");
                }
                list.append(members[c]);
            }
            list.append(")");

            List<String> toList = new ArrayList<>();
            String qry = "SELECT email FROM accounts, groupmembership WHERE accounts.id = groupmembership.userId AND groupId = ? AND accounts.id IN " + list.toString();
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, groupId);
            while (rs.next()) //add recipients to receive this message
            {
                toList.add(rs.getString(1));
            }

            String messageText = "This message is sent to you by the administrator of the FSEconomy flight group \"" + group.getName() + "\".\n----------\n" + text;

            Emailer emailer = Emailer.getInstance();
            emailer.sendEmail("no-reply@fseconomy.net", "FS Economy flight group " + group.getName(), "FSEconomy Group Message", messageText, toList, Emailer.ADDRESS_BCC);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static int getRole(int groupId, int userId)
    {
        int result = -1;

        try
        {
            String qry = "SELECT level FROM groupmembership WHERE groupId = ? AND userId = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, groupId, userId);
            if(rs.next())
                return UserBean.getGroupLevel(rs.getString("level"));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
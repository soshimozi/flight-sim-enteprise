package net.fseconomy.data;

import net.fseconomy.beans.GoodsBean;
import net.fseconomy.beans.PaymentBean;
import net.fseconomy.beans.UserBean;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Groups implements Serializable
{
    public static void doInvitation(UserBean user, int group, boolean accept) throws DataError
    {
        Accounts.mustBeLoggedIn(user);
        try
        {
            String qry = "";

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

        Accounts.reloadMemberships(user);
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

    public static void cancelGroup(UserBean user, int group) throws DataError
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

        Accounts.reloadMemberships(user);
    }

    public static void deleteGroup(UserBean user, int group) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Accounts.mustBeLoggedIn(user);
        try
        {
            conn = DALHelper.getInstance().getConnection();

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT money + bank, bank, name FROM accounts WHERE type = 'group' AND id = " + group);
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
            stmt.executeUpdate("UPDATE assignments SET groupId = null, pilotFee = 0, comment = null WHERE fromTemplate is not null and groupId = " + group);

            // Remove assignments owned by others from group assignments list
            stmt.executeUpdate("UPDATE assignments SET groupId = null, pilotFee = 0, comment = null WHERE owner <> groupid and groupId = " + group);

            // Delete Ferry assignments. They can not be transfered to an individual.
            stmt.executeUpdate("DELETE FROM assignments WHERE fromTemplate is null and (commodityId is null or commodityId = 0) and groupId = " + group);

            // Change ownership of transfer assignments to group owner
            stmt.executeUpdate("UPDATE assignments SET owner = " + user.getId() + " WHERE owner = " + group);

            // Change ownership of leases to group owner - 8-26-12 Airboss
            stmt.executeUpdate("UPDATE aircraft SET lessor = " + user.getId() + " WHERE lessor = " + group);

            // No assignments should remain now where owner or groupid equals group


            // Transfer ownership of group owned aircraft
            stmt.executeUpdate("UPDATE aircraft SET owner = " + user.getId() + " WHERE owner = " + group);

            // Transfer ownership of group owned FBOs
            stmt.executeUpdate("UPDATE fbo SET owner = " + user.getId() + " WHERE owner = " + group);

            // Transfer ownership of FBO facilities
            stmt.executeUpdate("update fbofacilities set occupant = " + user.getId() + " where occupant = " + group);

            // Transfer ownership of group owned goods
            List<GoodsBean> goods = Goods.getGoodsForAccountAvailable(group);
            for (GoodsBean good : goods)
            {
                String location = good.getLocation();
                int type = good.getType();
                int amount = good.getAmount();
                Goods.changeGoodsRecord(location, type, group, -amount, false);
                Goods.changeGoodsRecord(location, type, user.getId(), amount, false);
            }

            // Delete group owned goods records (all zero amount now)
            stmt.executeUpdate("DELETE FROM goods WHERE owner = " + group);

            // The group should have no property now

            // Transfer funds
            Banking.doPayment(group, user.getId(), groupMoney, PaymentBean.GROUP_DELETION, 0, -1, "", 0, groupName, false);

            // Delete group membership
            stmt.executeUpdate("DELETE FROM groupmembership WHERE groupId = " + group);

            // Delete the group account
            stmt.executeUpdate("UPDATE accounts SET exposure = 0, comment = '" + user.getName() + "'  WHERE type='group' AND id = " + group);
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

        Accounts.reloadMemberships(user);
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

    public static void mailMembers(UserBean user, int groupid, String[] members, String text) throws DataError
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

        try
        {
            StringBuffer list = new StringBuffer("(");
            for (int c = 0; c < members.length; c++)
            {
                if (c > 0)
                {
                    list.append(", ");
                }
                list.append(members[c]);
            }
            list.append(")");

            List<String> toList = new ArrayList<String>();
            String qry = "SELECT email FROM accounts, groupmembership WHERE accounts.id = groupmembership.userId AND groupId = ? AND accounts.id IN " + list.toString();
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, group);
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
}
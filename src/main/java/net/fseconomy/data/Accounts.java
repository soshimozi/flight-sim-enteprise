package net.fseconomy.data;

import net.fseconomy.beans.UserBean;
import net.fseconomy.dto.AccountNote;
import net.fseconomy.dto.LinkedAccount;
import net.fseconomy.util.Constants;
import net.fseconomy.util.Converters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class Accounts implements Serializable
{
    public static final int ACCT_TYPE_ALL = 1;
    public static final int ACCT_TYPE_PERSON = 2;
    public static final int ACCT_TYPE_GROUP = 3;

    public static final int LINK_INACTIVE = 0;
    public static final int LINK_ACTIVE = 1;

    public static Logger logger = LoggerFactory.getLogger(Accounts.class);

    private static final Object lock = new Object();

    public Accounts()
    {
    }

    static String createPassword()
    {
        return ServiceProviders.createAccessKey(8);
    }

    public static boolean userEmailExists(String user, String email)
    {
        boolean result = false;
        try
        {
            String qry = "select (count(name) > 0) as found from accounts where name = ? and email=?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), user, email);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean needLevel(UserBean user, int level)
    {
        return (user.getLevel() == level);
    }

    public static UserBean userExists(String user, String password)
    {
        UserBean result = null;
        try
        {
            String qry = "select * from accounts where name = ? and  password=password(?)";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user, password);

            if (rs.next())
            {
                result = new UserBean(rs);
                UpdateLogonTime(result.getId());
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    static void UpdateLogonTime(int id) throws SQLException
    {
        String qry = "UPDATE accounts set logon=? WHERE id = ?";
        DALHelper.getInstance().ExecuteUpdate(qry, new Timestamp(GregorianCalendar.getInstance().getTime().getTime()), id);
    }

    static void mustBeLoggedIn(UserBean user) throws DataError
    {
        if (user == null || user.getId() == -1)
            throw new DataError("Not logged in.");
    }

    public static boolean updateUser(UserBean user) throws DataError
    {
        boolean success = false;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        mustBeLoggedIn(user);
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM accounts WHERE type = 'person' AND id = '" + user.getId() + "'");
            if (!rs.next())
            {
                return false;
            }
            else
            {
                user.writeBean(rs);
                rs.updateRow();

                success = true;
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
        return success;
    }

    public static void updateUserOrGroup(UserBean user) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        mustBeLoggedIn(user);
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM accounts WHERE (type = 'person' or type = 'group') AND id = '" + user.getId() + "'");
            if (!rs.next())
            {
                throw new DataError("User not found.");
            }

            user.writeBean(rs);
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

    public static void changePassword(UserBean user, String password, String newPassword) throws DataError
    {
        try
        {
            newPassword = Converters.escapeSQL(newPassword);
            password = Converters.escapeSQL(password);

            String qry = "UPDATE accounts SET password=password(?) WHERE id = ? AND password = password(?)";
            int count = DALHelper.getInstance().ExecuteUpdate(qry, newPassword, user.getId(), password);

            if (count == 0)
            {
                throw new DataError("Invalid password specified.");
            }

            addAccountNote(user.getId(), user.getId(), "Password changed.");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void lockAccount(int accountId, int userId) throws DataError
    {
        try
        {
            String qry = "UPDATE accounts SET email = concat('LockedAccount-', email), password = '*B2C37C48A693188842BC5F24929A4C99209652A5' where id = ?";
            int count = DALHelper.getInstance().ExecuteUpdate(qry, accountId);

            if (count == 0)
            {
                throw new DataError("Account Lock Operation Failed.");
            }

            addAccountNote(accountId, userId, "Locked account.");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void unlockAccount(int accountId, int userId) throws DataError
    {
        try
        {
            String qry = "UPDATE accounts SET email = trim(leading 'LockedAccount-' from email) where id = ?";
            int count = DALHelper.getInstance().ExecuteUpdate(qry, accountId);

            if (count == 0)
            {
                throw new DataError("Account Unlock Operation Failed.");
            }

            addAccountNote(accountId, userId, "Unlocked account.");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateAccount(String currUserName, String editedUserName, String email, int exposure, String level, String newpassword, int linkToId, int userId) throws DataError
    {
        String qry;
        int count;
        try
        {
            UserBean account = getAccountByName(currUserName);
            if (currUserName.equals(editedUserName)) //not a name change
            {
                if (newpassword.length() != 0)
                {
                    newpassword = Converters.escapeSQL(newpassword);
                    qry = "UPDATE accounts SET email = ?, exposure = ?, level = ?, password = password(?) where name = ?";
                    count = DALHelper.getInstance().ExecuteUpdate(qry, email, exposure, level, newpassword, currUserName);

                    addAccountNote(account.getId(), userId, "Password changed.");

                    if(account.getExposure() != exposure)
                        addAccountNote(account.getId(), userId, "Exposure changed: [" + account.getExposure() + "] to [" + exposure + "]");

                    if(!account.getEmail().equals(email))
                        addAccountNote(account.getId(), userId, "Email changed: [" + account.getEmail() + "] to [" + email + "]");
                }
                else
                {
                    qry = "UPDATE accounts SET email = ?, exposure = ?, level = ? where name = ?";
                    count = DALHelper.getInstance().ExecuteUpdate(qry, email, exposure, level, currUserName);

                    if(account.getExposure() != exposure)
                        addAccountNote(account.getId(), userId, "Exposure changed: [" + account.getExposure() + "] to [" + exposure + "]");

                    if(!account.getEmail().equals(email))
                        addAccountNote(account.getId(), userId, "Email changed: [" + account.getEmail() + "] to [" + email + "]");

                    if(!account.getLevelString(account.getLevel()).equals(level))
                        addAccountNote(account.getId(), userId, "Level changed: [" + account.getLevelString(account.getLevel()) + "] to [" + level + "]");
                }
            }
            else //name has changed
            {
                qry = "UPDATE accounts SET name = ?, email = ?, exposure = ? where name = ?";
                count = DALHelper.getInstance().ExecuteUpdate(qry, editedUserName, email, exposure, currUserName);

                qry = "UPDATE log SET user = ? WHERE user = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, editedUserName, currUserName);

                addAccountNote(account.getId(), userId, "Name changed: [" + account.getName() + "] to [" + editedUserName + "]");
            }

            int accountId = getAccountIdByName(editedUserName);

            if(linkToId != 0)
            {
                linkAccount(linkToId, accountId, userId);
            }

            if (count == 0)
            {
                throw new DataError("Account Update Failed.");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean accountNameIsUnique(String accountName)
    {
        boolean doesNotExist = false;
        try
        {
            String qry = "SELECT (count(name) = 0) as notfound FROM accounts WHERE upper(name) = upper(?)";
            doesNotExist = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountName);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return doesNotExist;
    }

    public static boolean accountEmailIsUnique(String accountEmail)
    {
        boolean exists = false;
        try
        {
            String qry = "SELECT (count(email) > 0) as found FROM accounts WHERE upper(email) = upper(?)";
            exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountEmail);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return !exists;
    }

    public static void createAccount(String user, String email, int linkedId, int userId) throws DataError
    {
        String password = createPassword();

        try
        {
            if (email.indexOf('@') < 0)
                throw new AddressException();

            if (user.length() < 3 || user.indexOf(' ') >= 0)
                throw new DataError("Invalid user name.");

            if (userEmailExists(user, email))
                throw new DataError("User already exists!");

            if (!accountNameIsUnique(user))
                throw new DataError("User name already exists!");

            String qry = "INSERT INTO accounts (name, password, email, exposure) VALUES(?, password(?), ?, ?)";
            DALHelper.getInstance().ExecuteUpdate(qry, user, password, email, UserBean.EXPOSURE_SCORE);

            int accountId = getAccountIdByName(user);

            addAccountNote(accountId, userId, "Created Account");

            if(linkedId != 0)
                linkAccount(linkedId, accountId, userId);


                List<String> toList = new ArrayList<>();
            toList.add(email);

            String messageText = "Welcome to FSEconomy.\nYour account has been created. ";

            messageText += "You can login at " + Constants.systemLocation +
                    " with the following account:\n\nUser: " +
                    user + "\nPassword: " + password;

            sendAccountEmailMessage(toList, messageText);
        }
        catch (AddressException e)
        {
            throw new DataError("Invalid email address.");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void addAccountNote(int accountId, int userId, String note)
    {
        try
        {
            //Check if accountId exists in the table
            String qry = "INSERT INTO accountnotes (accountid, createdby, note) VALUES(?,?,?)";
            DALHelper.getInstance().ExecuteNonQuery(qry, accountId, userId, note);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void linkAccount(int linkToId, int accountId, int userId) throws DataError
    {
        //if it does get the set id and add the new account
        try
        {
            //check if account already linked
            if (checkAccountLinked(accountId))
                throw new DataError("Account already linked");

            //Check if linkTo already belongs to a set
            String qry = "SELECT linkid FROM linkedaccounts WHERE accountid = ?";
            int setId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), linkToId);

            //if it doesn't create a new set and add the two accounts
            if (setId == 0)
            {
                synchronized (lock)
                {
                    qry = "SELECT max(linkid) FROM linkedaccounts";
                    int newSetId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());

                    //have to have for brand new table, no records returns 0
                    if(newSetId == 0)
                        newSetId = 1;

                    qry = "INSERT INTO linkedaccounts (linkid, accountid, status) VALUES(?,?,?);";
                    DALHelper.getInstance().ExecuteNonQuery(qry, newSetId, linkToId, LINK_ACTIVE);
                    DALHelper.getInstance().ExecuteNonQuery(qry, newSetId, accountId, LINK_ACTIVE);

                    String note = "Account linked to: " + getAccountNameById(linkToId);
                    addAccountNote(accountId, userId, note);

                    note = "Account linked to: " + getAccountNameById(accountId);
                    addAccountNote(linkToId, userId, note);
                }
            }
            else
            {
                qry = "INSERT INTO linkedaccounts (linkid, accountid, status) VALUES(?,?,?);";
                DALHelper.getInstance().ExecuteNonQuery(qry, setId, accountId, LINK_ACTIVE);

                String note = "Account linked to: " + getAccountNameById(linkToId);
                addAccountNote(accountId, userId, note);

                note = "Account linked to: " + getAccountNameById(accountId);
                addAccountNote(linkToId, userId, note);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    private static boolean checkAccountLinked(int accountId)
    {
        boolean result = false;

        try
        {
            //Check if accountId exists in the table
            String qry = "SELECT (sum(accountid) > 0) as found FROM linkedaccounts WHERE accountid = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void unlinkAccount(int accountId, int userId)
    {
        try
        {
            //Check if accountId exists in the table
            String qry = "DELETE FROM linkedaccounts WHERE accountid = ?";
            DALHelper.getInstance().ExecuteNonQuery(qry, accountId);

            String note = "Account unlinked";
            addAccountNote(accountId, userId, note);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static List<LinkedAccount> getLinkedAccountList()
    {
        List<LinkedAccount> result = new ArrayList<>();

        try
        {
            //Check if accountId exists in the table
            String qry = "SELECT la.*, a.name as accountname FROM linkedaccounts la, accounts a WHERE a.id=la.accountid";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                LinkedAccount la = new LinkedAccount();
                la.linkId = rs.getInt("linkid");
                la.accountId = rs.getInt("accountid");
                la.status = rs.getInt("status");
                la.accountName = rs.getString("accountname");

                result.add(la);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<LinkedAccount> getLinkedAccountList(int accountId)
    {
        List<LinkedAccount> result = new ArrayList<>();

        try
        {
            //Check if linkTo already belongs to a set
            String qry = "SELECT linkid FROM linkedaccounts WHERE accountid = ?";
            int setId = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), accountId);

            if(setId != 0)//no sets found
            {
                //Check if accountId exists in the table
                qry = "SELECT la.*, a.name as accountname FROM linkedaccounts la, accounts a WHERE a.id=la.accountid AND la.linkid=?";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, setId);
                while (rs.next())
                {
                    LinkedAccount la = new LinkedAccount();
                    la.linkId = rs.getInt("linkid");
                    la.accountId = rs.getInt("accountid");
                    la.status = rs.getInt("status");
                    la.accountName = rs.getString("accountname");

                    result.add(la);
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<AccountNote> getAccountNoteList(int accountId)
    {
        List<AccountNote> result = new ArrayList<>();

        try
        {
            //Check if accountId exists in the table
            String qry = "SELECT an.*, a1.name as accountname, a2.name as createdbyname FROM accountnotes an, accounts a1, accounts a2 WHERE a1.id=an.accountid AND a2.id=an.createdby AND an.accountid = ? order by created desc";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, accountId);
            while (rs.next())
            {
                AccountNote an = new AccountNote();
                an.accountId = rs.getInt("accountid");
                an.created = rs.getTimestamp("created");
                an.createdBy = rs.getInt("createdby");
                an.note = rs.getString("note");

                an.accountName = rs.getString("accountName");
                an.createdByName = rs.getString("createdbyname");

                result.add(an);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void resetPassword(String user, String email) throws DataError
    {
        String password = createPassword();

        try
        {
            if (email.indexOf('@') < 0)
            {
                throw new AddressException();
            }

            if (user.length() < 3 || user.indexOf(' ') >= 0)
            {
                throw new DataError("Invalid user name.");
            }

            if (!userEmailExists(user, email))
            {
                throw new DataError("User not found!");
            }

            String qry = "UPDATE accounts SET password = password(?) WHERE name = ? AND email = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, password, user, email);

            List<String> toList = new ArrayList<>();
            toList.add(email);

            String messageText = "A new password has been generated for you. ";

            messageText += "You can login at " + Constants.systemLocation +
                    " with the following account:\n\nUser: " +
                    user + "\nPassword: " + password;


            sendAccountEmailMessage(toList, messageText);
        }
        catch (AddressException e)
        {
            throw new DataError("Invalid email address.");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void sendAccountEmailMessage(List<String> emailTo, String message) throws DataError
    {
        Emailer emailer = Emailer.getInstance();

        emailer.sendEmail("no-reply@fseconomy.net", "FSEconomy Account Management System", "FSEconomy account details", message, emailTo, Emailer.ADDRESS_TO);
    }

    public static List<String> getUsers(String usertype) throws DataError
    {
        ArrayList<String> result = new ArrayList<>();
        String qry;

        try
        {
            if (usertype == null)
            {
                usertype = "";
            }

            switch (usertype)
            {
                case "flying":
                {
                    qry = "SELECT accounts.name FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is null ORDER BY accounts.name";
                    ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
                    while (rs.next())
                    {
                        result.add(rs.getString(1));
                    }
                    break;
                }
                case "parked":
                {
                    qry = "SELECT accounts.name FROM aircraft LEFT JOIN accounts on aircraft.userlock = accounts.id WHERE aircraft.location is not null and aircraft.userlock is not null ORDER BY accounts.name";
                    ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
                    while (rs.next())
                    {
                        result.add(rs.getString(1));
                    }
                    break;
                }
                default:
                    throw new DataError(usertype + " not implemented!");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static UserBean getGroupById(int id)
    {
        return getAccountSQL("SELECT * FROM accounts WHERE type = 'group' AND id = " + id);
    }

    public static String getAccountTypeById(int id)
    {
        String result = "person";
        try
        {
            String qry = "SELECT `type` FROM accounts WHERE id = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static BigDecimal getAccountInterest(int id)
    {
        BigDecimal retval = new BigDecimal("0.00");
        try
        {
            String qry = "SELECT interest FROM accounts WHERE id = ?";
            retval = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BigDecimalResultTransformer(), Integer.valueOf(id));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return retval;
    }

    public static UserBean getAccountById(int id)
    {
        return getAccountSQL("SELECT * FROM accounts WHERE id = " + id);
    }

    public static String getAccountNameById(int id)
    {
        String retval = null;

        try
        {
            String qry = "SELECT name FROM accounts WHERE id = ?";
            retval = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return retval;
    }

    public static UserBean getAccountByName(String name)
    {
        return getAccountSQL("SELECT * FROM accounts WHERE type = 'person' AND name = '" + name + "'");
    }

    public static int getAccountIdByName(String name)
    {
        int retval = 0;

        try
        {
            String qry = "SELECT id FROM accounts WHERE name = ?";
            retval = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), name);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return retval;
    }

    public static UserBean getAccountGroupOrUserByName(String name)
    {
        return getAccountSQL("SELECT * FROM accounts WHERE (type = 'person' OR type = 'group') AND name = '" + name + "'");
    }

    public static List<UserBean> getGroupsThatInviteUser(int userId)
    {
        return getAccountsSQL("SELECT * FROM accounts WHERE type = 'group' AND EXISTS (SELECT * FROM groupmembership WHERE groupId = accounts.id AND userId = " + userId + " AND level = 'invited') ORDER BY name");
    }

    public static List<UserBean> getGroupsForUser(int userId)
    {
        return getAccountsSQL("SELECT * FROM accounts WHERE type = 'group' AND EXISTS (SELECT * FROM groupmembership WHERE groupId = accounts.id AND userId = " + userId + " AND level <> 'invited' ) ORDER BY name");
    }

    public static List<UserBean> getAllExposedGroups()
    {
        return getAccountsSQL("SELECT * FROM accounts WHERE type = 'group' AND (exposure & " + UserBean.EXPOSURE_GROUPS + ") > 0  ORDER BY name");
    }

    public static List<UserBean> getUsersForGroup(int groupId)
    {
        return getAccountsSQL("SELECT accounts.* FROM accounts, groupmembership WHERE type = 'person' AND userId = accounts.id AND groupId = " + groupId + " ORDER BY FIND_IN_SET(groupmembership.level, 'owner,staff,member'), name ");
    }

    public static List<UserBean> getAccounts()
    {
        return getAccounts(false);
    }

    public static List<UserBean> getAccounts(boolean usersonly)
    {
        if (usersonly)
        {
            return getAccountsSQL("SELECT * from accounts WHERE type = 'person' ORDER BY name");
        }
        else
        {
            return getAccountsSQL("SELECT * from accounts ORDER BY name");
        }
    }

    public static boolean isGroupOwnerStaff(int groupid, int userid)
    {
        boolean result = false;
        try
        {
            String qry = "SELECT (count(groupid) > 0) AS found FROM groupmembership WHERE groupid = ? AND userid = ? AND (level = 'owner' OR level = 'staff')";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), groupid, userid);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean isGroupOwner(int groupId, int userId)
    {
        boolean result = false;
        try
        {
            String qry = "SELECT (count(groupid) > 0) AS found FROM groupmembership WHERE groupid = ? AND userid = ? AND level = 'owner'";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), groupId, userId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getUserGroupIdByReadAccessKey(String key)
    {
        int result = -1;
        try
        {
            String qry = "SELECT id FROM accounts WHERE ReadAccessKey = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, key);
            if (rs.next())
            {
                result = rs.getInt("id");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<UserBean> getAccountNames(String partialName, int acctType, int limit, boolean displayHidden)
    {
        ArrayList<UserBean> result = new ArrayList<>();
        try
        {
            String accttype = ""; // ACCT_TYPE_ALL
            if (acctType == ACCT_TYPE_PERSON)
            {
                accttype = " AND type = 'person' ";
            }
            else if (acctType == ACCT_TYPE_GROUP)
            {
                accttype = " AND type = 'group' ";
            }

            String qry;
            if (displayHidden)
            {
                qry = "SELECT * FROM accounts WHERE name like ? " + accttype + " ORDER BY name LIMIT " + limit;
            }
            else
            {
                qry = "SELECT * FROM accounts WHERE exposure <> 0 AND name like ? " + accttype + " ORDER BY name LIMIT " + limit;
            }

            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, partialName + "%");
            while (rs.next())
            {
                UserBean template = new UserBean(rs);
                result.add(template);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<UserBean> getAccountEmails(String partialName, int acctType, int limit, boolean displayHidden)
    {
        ArrayList<UserBean> result = new ArrayList<>();
        try
        {
            String accttype = ""; // ACCT_TYPE_ALL
            if (acctType == ACCT_TYPE_PERSON)
            {
                accttype = " AND type = 'person' ";
            }
            else if (acctType == ACCT_TYPE_GROUP)
            {
                accttype = " AND type = 'group' ";
            }

            String qry;
            if (displayHidden)
            {
                qry = "SELECT * FROM accounts WHERE email like ? " + accttype + " ORDER BY name LIMIT " + limit;
            }
            else
            {
                qry = "SELECT * FROM accounts WHERE exposure <> 0 AND email like ? " + accttype + " ORDER BY email LIMIT " + limit;
        }

            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, partialName + "%");
            while (rs.next())
            {
                UserBean template = new UserBean(rs);
                result.add(template);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private static UserBean getAccountSQL(String qry)
    {
        UserBean result = null;

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            if (rs.next())
            {
                result = new UserBean(rs);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private static List<UserBean> getAccountsSQL(String qry)
    {
        ArrayList<UserBean> result = new ArrayList<>();
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                UserBean template = new UserBean(rs);
                result.add(template);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int accountUltimateOwner(int accountId)
    {
        int result = accountId;
        try
        {
            String qry = "SELECT userId FROM groupmembership WHERE level = 'owner' AND groupId = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, accountId);

            if (rs.next())
            {
                result = rs.getInt("userId");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int accountUltimateGroupOwner(int accountId)
    {
        int result = accountId;
        try
        {
            String qry = "SELECT userId FROM groupmembership WHERE level = 'owner' AND groupId = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, accountId);

            if (rs.next())
            {
                result = rs.getInt("userId");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean isGroup(int accountid)
    {
        boolean result = false;

        try
        {
            String qry = "SELECT (type='group') FROM accounts WHERE id = ?;";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountid);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
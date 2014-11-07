package net.fseconomy.data;

import net.fseconomy.beans.PaymentBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.util.Converters;
import net.fseconomy.util.Formatters;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Banking implements Serializable
{
    public static void reloadMoney(UserBean bean)
    {
        try
        {
            String qry = "SELECT money, bank FROM accounts WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, bean.getId());
            if (rs.next())
            {
                bean.setMoney(rs.getDouble("money"));
                bean.setBank(rs.getDouble("bank"));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static double getAccountFundsById(int id) throws DataError
    {
        double money = 0;
        try
        {
            //check if funds available
            String qry = "SELECT money FROM accounts WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);

            if (!rs.next())
            {
                throw new DataError("Account not found");
            }

            money = rs.getDouble(1);

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return money;
    }

    public static int getAmountPaymentsForUser(int user, int fboId, String aircraft, boolean paymentsToSelf)
    {
        String where = "";
        if (fboId > 0)
        {
            where = where + " AND fbo = " + fboId;
        }

        if ((aircraft != null) && !aircraft.equals(""))
        {
            where = where + " AND aircraft = '" + Converters.escapeSQL(aircraft) + "'";
        }

        if (!paymentsToSelf)
        {
            where = where + " AND user <> otherparty";
        }

        int result = -1;
        try
        {
            String qry = "SELECT count(*) FROM payments WHERE user = " + user + where;
            int result1 = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());

            qry = "SELECT count(*) FROM payments WHERE otherparty = " + user + where;
            int result2 = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer());

            result = result1 + result2;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<PaymentBean> getPaymentsForUser(int user, int from, int amount, int fboId, String aircraft, boolean paymentsToSelf)
    {
        String where = "";
        if (fboId > 0)
        {
            where = where + " AND fbo = " + fboId;
        }

        if ((aircraft != null) && !aircraft.equals(""))
        {
            where = where + " AND aircraft = '" + Converters.escapeSQL(aircraft) + "'";
        }

        if (!paymentsToSelf)
        {
            where = where + " AND user <> otherparty ";
        }

        if (from < 0)
        {
            from = 0;
        }

        if (amount <= 0)
        {
            amount = 10;
        }

        //Airboss 7/11/13
        //See: http://explainextended.com/2011/02/11/late-row-lookups-innodb/
        //See: http://explainextended.com/2009/10/23/mysql-order-by-limit-performance-late-row-lookups/
        return getPaymentLogSQL("Select m.* from (Select id from payments where (user = " + user + " OR otherparty = " + user + ") " + where + " order by id desc Limit " + from + "," + amount + ") q join payments m on q.id=m.id order by id desc");
    }

    public static List<PaymentBean> getPaymentsForIdByMonth(int id, int month, int year)
    {
        String sql = "Select m.* from (Select id, year(CONVERT_TZ(`time`, @@session.time_zone, '+00:00')) year, month(CONVERT_TZ(`time`, @@session.time_zone, '+00:00')) month from payments where (user = " + id + " OR otherparty = " + id + ") order by id desc) q join payments m on q.id=m.id WHERE (q.year= " + year + " AND q.month=" + month + ")  order by id desc";
        return getPaymentLogSQL(sql);
    }

    public static List<PaymentBean> getPaymentLogSQL(String qry)
    {
        ArrayList<PaymentBean> result = new ArrayList<PaymentBean>();

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                PaymentBean log = new PaymentBean(rs);
                result.add(log);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void doBanking(int user, double value)
    {
        try
        {
            String sValue = Formatters.twoDecimals.format(value);

            //I want to use these but can't because of how the rounding is being done in a string
            //I don't want to introduce a difference in how the banking is done yet.
            //String qry = "UPDATE accounts SET bank = ROUND(bank + ?, 2) WHERE id = ?";
            //qry = "UPDATE accounts SET money = ROUND(money - ?, 2) WHERE id = ?";

            String qry = "UPDATE accounts SET bank = ROUND(bank + " + sValue + ", 2) WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, user);

            qry = "UPDATE accounts SET money = ROUND(money - " + sValue + ", 2) WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, user);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void doPayGroup(int user, int group, float value)
    {
        doPayment(user, group, value, PaymentBean.GROUP_PAYMENT, 0, -1, "", "", "", false);
    }

    public static void doPayGroup(int user, int group, float value, String comment)
    {
        doPayment(user, group, value, PaymentBean.GROUP_PAYMENT, 0, -1, "", "", comment, false);
    }

    public static void doPayBulkFuel(int user, int group, float value, int fbo, String comment, String icao, int type)
    {
        doPayment(user, group, value, (type == 3) ? PaymentBean.SALE_GOODS_FUEL : PaymentBean.SALE_GOODS_JETA, 0, fbo, icao, "", comment, false);
    }

    public static void doPayBulkFuelDelivered(int user, int group, float value, int fbo, String comment, String icao)
    {
        doPayment(user, group, value, PaymentBean.BULK_FUEL, 0, fbo, icao, "", comment, false);
    }

    /**
     * Add a payment record to the payments table
     *
     * @param user       The originating party
     * @param otherParty The other party
     * @param amount     The amount
     * @param reason     The reason for payment
     * @param logEntry   The optional log entry this payment is associated with
     */
    public static void addPaymentRecord(int user, int otherParty, Money amount, short reason, long logEntry, int fbo, String location, String aircraft, String comment)
    {
        StringBuilder fields = new StringBuilder();
        StringBuilder values = new StringBuilder();
        try
        {
            fields.append("time, user, otherParty, amount, reason, logEntry");
            values.append("'").append(new Timestamp(System.currentTimeMillis())).append("'");
            values.append(", ").append(user);
            values.append(", ").append(otherParty);
            values.append(", ").append(amount.getAsFloat());
            values.append(", ").append(reason);
            values.append(", ").append(logEntry);

            if (fbo >= 0)
            {
                fields.append(", fbo");
                values.append(", ").append(fbo);
            }

            if (!"".equals(location))
            {
                fields.append(", location");
                values.append(", '").append(location).append("'");
            }

            if (!"".equals(aircraft))
            {
                fields.append(", aircraft");
                values.append(", '").append(Converters.escapeSQL(aircraft)).append("'");
            }

            if (!"".equals(comment))
            {
                fields.append(", comment");
                values.append(", '").append(Converters.escapeSQL(comment)).append("'");
            }

            String qry = "INSERT INTO payments (" + fields.toString() + ") VALUES(" + values.toString() + ")";
            DALHelper.getInstance().ExecuteUpdate(qry);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Perform and log a payment
     *
     * @param user       The originating party
     * @param otherParty The other party
     * @param amount     The amount
     * @param reason     The reason for payment
     * @param logEntry   The optional log entry this payment is associated with
     */
    public static boolean doPayment(int user, int otherParty, double amount, short reason, long logEntry, int fbo, String location, String aircraft, String comment, boolean blockOnDept)
    {
        return doPayment(user, otherParty, new Money(amount), reason, logEntry, fbo, location, aircraft, comment, blockOnDept);
    }

    public static boolean doPayment(int user, int otherParty, Money amount, short reason, long logEntry, int fbo, String location, String aircraft, String comment, boolean blockOnDept)
    {
        // if any of the following are true, then let the zero payment through, otherwise exit
        if (amount.getAsDouble() == 0 &&
                reason != PaymentBean.FBO_SALE &&
                reason != PaymentBean.AIRCRAFT_SALE &&
                reason != PaymentBean.AIRCRAFT_LEASE &&    //Added by Airboss 5/8/11
                reason != PaymentBean.TRANSFER_GOODS_BUILDING_MATERIALS &&
                reason != PaymentBean.TRANSFER_GOODS_FUEL &&
                reason != PaymentBean.TRANSFER_GOODS_JETA &&
                reason != PaymentBean.TRANSFER_GOODS_SUPPLIES &&
                reason != PaymentBean.BULK_FUEL)    //added by gurka bulk fuel delivery transfers
        {
            return true;
        }

        // Check if amount is negative, reverse payee/payer
        if (amount.getAsDouble() < 0)
        {
            return doPayment(otherParty, user, amount.times(-1), reason, logEntry, fbo, location, aircraft, comment, blockOnDept);
        }

        try
        {
            boolean blocked = false;

            // Trying to pay themselves?
            if (user != otherParty)
            {
                if (blockOnDept && user != 0)
                {
                    // Credit check required first.
                    blocked = !checkAnyFunds(user, amount.getAsDouble());
                }

                // If they have funds continue
                if (!blocked)
                {
                    String qry = "SELECT * FROM accounts WHERE id in (" + user + ", " + otherParty + ")";
                    ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
                    while (rs.next())
                    {
                        Money balance = new Money(rs.getDouble("Money"));
                        if (rs.getInt("id") == 0)
                        {
                            continue;
                        }
                        else if (rs.getInt("id") == user)
                        {
                            balance = balance.minus(amount);
                        }
                        else if (rs.getInt("id") == otherParty)
                        {
                            balance = balance.plus(amount);
                        }

                        qry = "UPDATE accounts SET money = ? where id = ?";
                        DALHelper.getInstance().ExecuteUpdate(qry, balance.getAsDouble(), rs.getInt("id"));
                    }
                }
            }

            // No funds
            if (blocked)
            {
                comment = "[BLOCKED] " + comment;
            }

            // Log payment attempt
            addPaymentRecord(otherParty, user, amount, reason, logEntry, fbo, location, aircraft, comment);

            return !blocked;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public void checkMoneyAvailable( UserBean user, double cost) throws DataError
    {
        if (user != null)
        {
            try
            {
                if (cost != 0 && !Banking.checkAnyFunds(user.getId(), cost))
                    throw new DataError("Not enough money for paying this assignment. ");
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            throw new DataError("No user provided");
        }
    }

    public static boolean checkFunds(int accountId, double requiredAmount) throws SQLException
    {
        boolean result = false;
        try
        {
            String qry = "SELECT money FROM accounts WHERE id = ?";
            double cash = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), accountId);

            result = requiredAmount <= cash;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean checkAnyFunds(int accountId, double requiredAmount) throws SQLException
    {
        boolean result = false;
        try
        {
            String qry = "SELECT money + bank FROM accounts WHERE id = ?";
            double cash = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), accountId);

            result = requiredAmount <= cash;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static double[][] getStatement(Calendar month, int user, int fboId, String aircraft, boolean paymentsToSelf)
    {
        double[][] result = new double[PaymentBean.MAX_REASON + 1][2];

        try
        {
            //TODO does this need to change to UTC returned values or is server time ok?
            String sql = "SELECT reason, sum(amount) FROM payments WHERE MONTH(time) = " +
                    (month.get(Calendar.MONTH) + 1) +
                    " AND YEAR(time) = " + month.get(Calendar.YEAR) + " AND ";

            if (!paymentsToSelf)
            {
                sql = sql + "user <> otherparty AND ";
            }

            if (fboId > 0)
            {
                sql = sql + " fbo = " + fboId + " AND ";
            }

            if ((aircraft != null) && !aircraft.equals(""))
            {
                sql = sql + " aircraft = '" + aircraft + "' AND ";
            }

            double total = 0.0f;

            String qry = sql + "user = ? AND amount > 0 GROUP BY reason";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user);
            while (rs.next())
            {
                result[rs.getInt(1)][0] += rs.getDouble(2);
                total += rs.getDouble(2);
            }
            rs.close();

            qry = sql + "user = ? AND amount < 0 GROUP BY reason";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user);
            while (rs.next())
            {
                result[rs.getInt(1)][1] += rs.getDouble(2);
                total += rs.getDouble(2);
            }
            rs.close();

            qry = sql + "otherParty = ? AND amount > 0 GROUP BY reason";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user);
            while (rs.next())
            {
                result[rs.getInt(1)][1] += -rs.getDouble(2);
                total -= rs.getDouble(2);
            }
            rs.close();

            qry = sql + "otherParty = ? AND amount < 0 GROUP BY reason";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, user);
            while (rs.next())
            {
                result[rs.getInt(1)][0] += -rs.getDouble(2);
                total -= rs.getDouble(2);
            }

            result[0][0] = total;
            rs.close();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }
}
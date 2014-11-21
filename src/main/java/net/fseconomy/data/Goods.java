package net.fseconomy.data;

import net.fseconomy.beans.*;
import net.fseconomy.dto.LatLonSize;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Goods implements Serializable
{
    public static  CommodityBean[] commodities = null;
    static int maxCommodityId = 0;


    public static double currFuelPrice = 3.0;
    public static double currJetAMultiplier = 1.0;

    static
    {
        initializeCommodities();
        initializeFuelValues();
    }

    static void initializeCommodities()
    {
        ArrayList<CommodityBean> result = new ArrayList<>();
        ResultSet rs;

        try
        {
            String qry = "SELECT * FROM commodities ORDER BY id";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            int max = 0;

            while (rs.next())
            {
                CommodityBean c = new CommodityBean(rs);
                result.add(c);
                if (c.getId() < 99 && c.getId() > max)
                    max = c.getId();
            }

            commodities = new CommodityBean[max + 1];
            for (int c = 0; c < max; c++)
            {
                CommodityBean b = result.get(c);
                commodities[b.getId()] = b;
            }

            maxCommodityId = max;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static int getMaxCommodityId()
    {
        return maxCommodityId;
    }


    public static void initializeFuelValues()
    {
        try
        {
            String qry = "Select value from sysvariables where variablename='100LLFuelPrice'";
            currFuelPrice = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer());

            qry = "Select value from sysvariables where variablename='JetAMultiplier'";
            currJetAMultiplier = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer());
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void resetAllGoodsSellBuyFlag(int owner, String icao) throws SQLException
    {
        String qry = "UPDATE goods SET saleFlag=0 WHERE owner=? AND location=?";
        DALHelper.getInstance().ExecuteUpdate(qry, owner, icao);
    }

    public static boolean checkGoodsAvailable(String icao, int userId, int commodityId, int amount) throws DataError
    {
        boolean result = false;

        if (icao != null)
        {
            try
            {
                int currentAmount = 0;
                String qry = "SELECT * FROM goods WHERE location = ? AND owner = ? AND type = ?";
                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, icao, userId, commodityId);
                if (rs.next())
                {
                    currentAmount = rs.getInt("amount");
                    if (currentAmount >= amount)
                    {
                        result = true;
                    }
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static List<GoodsBean> getGoodsAtAirportToSell(String icao, int type, int size, double fuelPrice, double JetAPrice)
    {
        return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " +
                "goods, commodities, accounts WHERE ( goods.owner = 0 or exists (select * from fbo WHERE fbo.location = goods.location AND fbo.owner = goods.owner AND " +
                " fbo.active = 1 AND (saleFlag&" + GoodsBean.SALEFLAG_BUY + ") > 0)) AND goods.owner = accounts.id AND goods.type = commodities.id AND goods.type = " + type +
                " AND (max = 0 OR max > amount) AND goods.location = '" + icao + "'", icao, type, size, fuelPrice, JetAPrice);
    }

    public static List<GoodsBean> getGoodsForFbo(String icao, int owner)
    {
        return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " +
                "goods, commodities, accounts WHERE goods.owner = accounts.id AND goods.type = commodities.id AND owner = " +
                owner + " AND location = '" + icao + "'", icao, 0, -1, 0, 0);
    }

    public static List<GoodsBean> getGoodsAtAirportGMap(String icao, int size, double fuelPrice, double JetAPrice)
    {
        return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " +
                "goods, commodities, accounts WHERE (goods.owner = 0 OR exists (select * from fbo WHERE fbo.location = goods.location AND fbo.owner = goods.owner AND " +
                "fbo.active = 1  AND saleFlag > 0)) AND commodities.id between 1 and 2 AND " +  //only showing BMs and Supplies after fuel everywhere update - changed by airboss - 8/15/12
                "goods.location = '" + icao + "' AND goods.owner = accounts.id", icao, 0, size, fuelPrice, JetAPrice);
    }

    public static List<GoodsBean> getGoodsAtAirport(String icao, int size, double fuelPrice, double JetAPrice)
    {
        return getGoodsAtAirportSQL("SELECT goods.*, commodities.name, accounts.name FROM " +
                "goods, commodities, accounts WHERE (goods.owner = 0 OR exists (select * from fbo WHERE fbo.location = goods.location AND fbo.owner = goods.owner AND " +
                "fbo.active = 1  AND saleFlag > 0)) AND goods.type = commodities.id AND " +
                "goods.location = '" + icao + "' AND goods.owner = accounts.id", icao, 0, size, fuelPrice, JetAPrice);
    }

    public static List<GoodsBean> getGoodsAtAirportSQL(String SQL, String icao, int type, int size, double fuelPrice, double JetAPrice)
    {
        List<GoodsBean> returnValue = getGoodsSQL(SQL);
        if (commodities != null && size > 0)
        {
            List<GoodsBean> result = new ArrayList<GoodsBean>();
            int amount[] = new int[commodities.length + 1];
            for (GoodsBean item : returnValue)
            {
                if (item.getOwner() == 0)
                {
                    amount[item.getType()] += item.getAmount();
                }
                else
                {
                    result.add(item);
                }
            }

            for (int c = 0; c < commodities.length; c++)
            {
                if (commodities[c] != null && size > commodities[c].getMinAirportSize() && (type == 0 || c == type))
                {
                    result.add(new GoodsBean(commodities[c], icao, size, fuelPrice, amount[commodities[c].getId()], JetAPrice));
                }
            }

            returnValue = result;
        }

        return returnValue;
    }

    public static List<GoodsBean> getGoodsForAccountAvailable(int id)
    {
        return getGoodsSQL("SELECT goods.*, commodities.name, accounts.name FROM goods, commodities, accounts WHERE goods.owner = accounts.id AND goods.type = commodities.id AND amount > 0 AND owner=" + id);
    }

    public static GoodsBean getGoods(String location, int owner, int type)
    {
        List<GoodsBean> returnValue = getGoodsSQL("SELECT goods.*, commodities.name, accounts.name FROM goods, commodities, accounts WHERE goods.owner = accounts.id AND goods.type = commodities.id AND goods.type = " + type + " AND goods.owner = " + owner + " AND goods.location = '" + location + "'");

        return returnValue.size() == 0 ? null : returnValue.get(0);
    }

    static List<GoodsBean> getGoodsSQL(String qry)
    {
        ArrayList<GoodsBean> result = new ArrayList<GoodsBean>();
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                GoodsBean goods = new GoodsBean(rs);
                result.add(goods);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getGoodsQty(FboBean fbo, int type)
    {
        GoodsBean goods = getGoods(fbo.getLocation(), fbo.getOwner(), type);

        return goods != null ? goods.getAmount() : 0;
    }

    public static int getGoodsQty(String location, int owner, int type)
    {
        GoodsBean goods = getGoods(location, owner, type);

        return goods != null ? goods.getAmount() : 0;
    }

    public static synchronized void changeGoodsRecord(String location, int type, int owner, int amount, boolean allowNegativeGoods) throws DataError
    {
        try
        {
            boolean recordExists = true;
            int currentAmount = 0;

            String qry = "SELECT amount FROM goods WHERE location = ? AND owner = ? AND type = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, location, owner, type);

            if (!rs.next())
            {
                recordExists = false;
            }
            else
            {
                currentAmount = rs.getInt(1);
            }

            if (owner > 0 && amount < 0 && (currentAmount + amount) < 0 && !allowNegativeGoods)
            {
                throw new DataError("Not enough goods available.");
            }

            int newAmount = currentAmount + amount;

            if (recordExists)
            {
                qry = "UPDATE goods set amount = ? WHERE location = ? AND owner = ? AND `type` = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, newAmount, location, owner, type);
            }
            else
            {
                qry = "INSERT INTO goods (location, owner, `type`, amount) VALUES(?,?,?,?)";
                DALHelper.getInstance().ExecuteUpdate(qry, location, owner, type, newAmount);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static double quoteGoods(String location, int type, int amount, int src, boolean buying)
    {
        try
        {
            if (Airports.cachedAPs.get(location.toUpperCase()) == null)
            {
                throw new Exception("Unknown airport.");
            }

            // Step 1: If otherParty = 0, price is not fixed, Calculate the price
            double kgPrice;
            if (src == 0)
            {
                int overstock = 0;
                int airportSize = Airports.cachedAPs.get(location.toUpperCase()).size;
                double fuelPrice = getFuelPrice(location);
                double mult = getJetaMultiplier();
                double JetAPrice = fuelPrice * mult;

                kgPrice = commodities[type].getKgSalePrice(amount, airportSize, fuelPrice, overstock, JetAPrice);
            }
            else
            {
                String qry;
                if (buying)
                {
                    qry = "SELECT sellprice FROM goods WHERE owner=? and type=? and location=?";
                }
                else
                {
                    qry = "SELECT buyprice FROM goods WHERE owner=? and type=? and location=?";
                }

                ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, src, type, location);
                if (!rs.next())
                {
                    throw new Exception("No price found.");
                }

                kgPrice = rs.getDouble(1);
            }

            return kgPrice * amount;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return 0.0;
    }

    public static double quoteFuel(String location, int type, int amount)
    {
        try
        {
            int overstock = 0;
            double fuelPrice = getFuelPrice(location);
            int airportSize = Airports.cachedAPs.get(location.toUpperCase()).size;
            double mult = getJetaMultiplier();
            double JetAPrice = fuelPrice * mult;

            double kgPrice = commodities[type].getKgSalePrice(amount, airportSize, fuelPrice, overstock, JetAPrice);

            return kgPrice * amount;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static synchronized void transferGoods(int from, int to, int initiator, String location, int type, int amount) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        //System.out.println("Transfer: from=" + from + ", to=" + to + ", initiator=" + initiator + ", location=" + location + ", type=" + type + ", amount=" + amount);
        if (amount < 1)
        {
            throw new DataError("Not enough goods available.");
        }

        try
        {
            int otherParty = initiator == from ? to : from;
            String typeLocation = " AND type=" + type + " AND location='" + location + "'";
            String fromWhere = "owner=" + from + typeLocation;
            String toWhere = "owner=" + to + typeLocation;
            String otherPartyWhere = otherParty == from ? fromWhere : toWhere;

            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            // Step 1: Check if <from> has enough of <type> at <location>
            // If from == 0, there's always enough
            rs = stmt.executeQuery("SELECT amount, saleFlag, retain FROM goods WHERE " + fromWhere);
            int available = 0;
            if (rs.next())
            {
                if (from == 0 || from == initiator)
                {
                    available = rs.getInt(1);
                }
                else if ((rs.getInt(2) & GoodsBean.SALEFLAG_SELL) > 0)
                {
                    available = rs.getInt(1) - rs.getInt(3);
                }
            }
            if (available < amount && from > 0)
            {
                throw new DataError("Not enough goods available.");
            }
            rs.close();

            // Step 2: In case of sale, check if other party wants to receive
            if (to != initiator && to != 0)
            {
                rs = stmt.executeQuery("SELECT saleFlag, max FROM goods WHERE " + toWhere);
                boolean saleOk = false;
                if (rs.next())
                {
                    saleOk = (rs.getInt(1) & GoodsBean.SALEFLAG_BUY) > 0;
                    if (saleOk && rs.getInt(2) > 0 && amount > (rs.getInt(2) - amount)) // Airboss 8/22/13 - moved subtract amount here to prevent unsigned bigint error in db
                    {
                        throw new DataError("Other party does not want to accept this quantity.");
                    }
                }
                if (!saleOk)
                {
                    throw new DataError("Other party does not accept the goods.");
                }

                rs.close();
            }

            // Step 3: If otherParty = 0, price is not fixed, Calculate the price
            double kgPrice;
            if (otherParty == 0)
            {
                int overstock = 0;
                if (from == 0)
                {
                    overstock = available;
                }
                else
                {
                    rs = stmt.executeQuery("SELECT amount FROM goods WHERE " + toWhere);
                    if (rs.next())
                    {
                        overstock = rs.getInt(1);
                    }
                    rs.close();
                }

                LatLonSize lls = Airports.cachedAPs.get(location);
                if (lls == null)
                {
                    throw new DataError("Unknown airport.");
                }

                int airportSize = lls.size;

                double fuelPrice = getFuelPrice(location);
                double mult = getJetaMultiplier();
                double JetAPrice = fuelPrice * mult;
                if (from == 0)
                {
                    kgPrice = commodities[type].getKgSalePrice(amount, airportSize, fuelPrice, overstock, JetAPrice);
                }
                else
                {
                    kgPrice = commodities[type].getKgBuyPrice(airportSize, fuelPrice, overstock, JetAPrice);
                }
            }
            else
            {
                rs = stmt.executeQuery("SELECT buyPrice, sellPrice FROM goods WHERE " + otherPartyWhere);
                if (!rs.next())
                {
                    throw new DataError("No price found.");
                }
                kgPrice = rs.getDouble(from == initiator ? 1 : 2);
                rs.close();
            }


            // Step 4: Check if <to> has enough money
            double price = kgPrice * amount;

            if (to > 0)
            {
                rs = stmt.executeQuery("SELECT money FROM accounts WHERE id=" + to);
                if (!rs.next())
                {
                    throw new DataError("Unknown user.");
                }
                double money = rs.getDouble(1);
                if (money < price)
                {
                    throw new DataError("Not enough money to pay for the goods.");
                }

                rs.close();
            }

            int fboId = -1;

            rs = null;
            // Step 5: perform transfer
            stmt.close();
            stmt = null;
            short logType = type == GoodsBean.GOODS_BUILDING_MATERIALS ? PaymentBean.SALE_GOODS_BUILDING_MATERIALS : type == GoodsBean.GOODS_FUEL100LL ? PaymentBean.SALE_GOODS_FUEL : type == GoodsBean.GOODS_FUELJETA ? PaymentBean.SALE_GOODS_JETA : PaymentBean.SALE_GOODS_SUPPLIES;
            Banking.doPayment(to, from, price, logType, 0, fboId, location, "", "", false);

            changeGoodsRecord(location, type, from, -amount, false);
            changeGoodsRecord(location, type, to, amount, false);
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

    public static void updateGoods(GoodsBean goods, UserBean user) throws DataError
    {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM goods WHERE owner=" + goods.getOwner() + " AND location='" + goods.getLocation() + "' AND type=" + goods.getType());
            boolean exists;
            if (rs.next())
            {
                exists = true;

            }
            else
            {
                rs.moveToInsertRow();
                rs.updateString("location", goods.getLocation());
                rs.updateInt("owner", goods.getOwner());
                rs.updateInt("type", goods.getType());
                exists = false;
            }

            goods.writeBean(rs);

            if (exists)
            {
                rs.updateRow();
            }
            else
            {
                rs.insertRow();
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
    public static void updateFuelPrice(int bucket, double price)
    {
        try
        {
            String qry = "SELECT (count(*) > 0) AS found FROM fuel WHERE bucket = ?";
            boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), bucket);
            if(exists)
            {
                qry = "UPDATE fuel SET price = ? WHERE bucket = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, price, bucket);
            }
            else
            {
                qry = "INSERT INTO fuel (price) VALUES(?) WHERE bucket = ?";
                DALHelper.getInstance().ExecuteUpdate(qry, price, bucket);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static double getFuelPrice(String icao)
    {
        double result = currFuelPrice;

        try
        {
            String qry = "SELECT price FROM fuel, airports WHERE airports.bucket = fuel.bucket AND icao=?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.DoubleResultTransformer(), icao);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * for getting the jeta price multiplier
     * @return double
     */
    public static double getJetaMultiplier()
    {
        return currJetAMultiplier;
    }

    public static float getLandingFee(String icao, Connection conn)
    {
        float result = 0.0f;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery("SELECT price, fixed FROM landingfees WHERE icao='" + icao + "' OR icao='" + icao.substring(0,3) + "' OR icao='" + icao.substring(0,2) + "' OR icao='" + icao.substring(0,1) + "' ORDER BY char_length(icao) DESC LIMIT 1" );
            if (rs.next())
            {
                result = rs.getFloat(1);
                if (rs.getInt(2) != 1)
                {
                    rs.close();
                    rs = stmt.executeQuery("SELECT size FROM airports WHERE icao='" + icao + "'");
                    if (rs.next())
                    {
                        int size = rs.getInt(1);
                        result *= (size/3000.0);
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
        }

        return result;
    }

    public static void updateGoods4Admins(int owner, String icao, int type, int amount) throws DataError
    {
        changeGoodsRecord(icao, type, owner, amount, false);
    }
}
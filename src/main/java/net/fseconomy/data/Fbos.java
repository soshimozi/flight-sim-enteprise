package net.fseconomy.data;

import net.fseconomy.beans.*;
import net.fseconomy.util.Constants;
import net.fseconomy.util.Formatters;
import net.fseconomy.util.Helpers;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.*;
import java.util.*;

public class Fbos implements Serializable
{
    public static final int FBO_ID = 0;
    public static final int FBO_REPAIR_MARGIN = 1;
    public static final int FBO_EQUIPMENT_MARGIN = 2;


    public static void doTransferFbo(FboBean fbo, int buyer, int owner, String icao, boolean goods) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sUpdate;
        try
        {
            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            if(!Airports.isValidIcao(icao))
                throw new DataError("Invalid ICAO.");

            int mergeWithId = 0;
            int mergeWithServices = 0;
            rs = stmt.executeQuery("select f.id, f.fbosize, f.services from fbo f where f.location = '" + icao + "' and f.owner = " + buyer);
            if (rs.next())
            {
                mergeWithId = rs.getInt(1);
                mergeWithServices = rs.getInt(3);
                if (mergeWithId == fbo.getId())
                {
                    throw new DataError("Buyer already owns this FBO.");
                }
            }
            rs.close();

            if (goods)
            {
                rs = stmt.executeQuery("SELECT type, amount FROM goods where owner = " + owner + " and location = '" + icao + "' and amount <> 0");
                while (rs.next())
                {
                    int type = rs.getInt("type");
                    int amount = rs.getInt("amount");
                    Goods.changeGoodsRecord(icao, type, owner, -amount, true);
                    Goods.changeGoodsRecord(icao, type, buyer, amount, true);
                }
                rs.close();
            }
            else // Keeping goods remove for sell/buy flags
            {
                String qry = "UPDATE goods SET saleFlag=0 WHERE owner=? AND location=?";
                DALHelper.getInstance().ExecuteUpdate(qry, owner, icao);
            }

            if (mergeWithId == 0)
            {
                // Buyer does not have an existing FBO. Just transfer ownership.
                sUpdate = "UPDATE fbo SET fbo.owner = " + buyer + ", fbo.saleprice = 0 WHERE fbo.owner = " + owner + " AND fbo.location ='" + icao + "'";
                stmt.executeUpdate(sUpdate);
                sUpdate = "UPDATE fbofacilities set occupant = " + buyer + " WHERE occupant = " + owner + " and fboId = " + fbo.getId();
                stmt.executeUpdate(sUpdate);
            }
            else
            {
                // Buyer has an FBO. Merge the FBOs.
                mergeWithServices = mergeWithServices | fbo.getServices();

                // Increase size of existing FBO by the size of the purchased FBO.
                sUpdate = "UPDATE fbo set fbosize = fbosize + " + fbo.getFboSize() + ", services = " + mergeWithServices + " where id = " + mergeWithId;
                stmt.executeUpdate(sUpdate);

                // If the existing FBO has a facility, delete the facility of the purchased FBO.
                rs = stmt.executeQuery("select id from fbofacilities where fboId = " + mergeWithId);
                if (rs.next())
                {
                    stmt.executeUpdate("delete from fbofacilities where reservedSpace >= 0 and fboId = " + fbo.getId());
                }

                rs.close();

                // For the purchased FBOs facilities, where the occupant is the seller, change to the buyer.
                stmt.executeUpdate("update fbofacilities set occupant = " + buyer + " where occupant = " + owner + " and fboId = " + fbo.getId());
                // Link the purchased FBOs facilities with the existing FBO.
                stmt.executeUpdate("update fbofacilities set fboId = " + mergeWithId + " where fboId = " + fbo.getId());

                // Delete the purchased FBO and update logs.
                stmt.executeUpdate("delete from fbo where id = " + fbo.getId());
                stmt.executeUpdate("update log set fbo = " + mergeWithId + " where fbo = " + fbo.getId());
                stmt.executeUpdate("update payments set fbo = " + mergeWithId + " where fbo = " + fbo.getId());
            }

            Banking.doPayment(owner, buyer, 0, PaymentBean.FBO_SALE, 0, fbo.getId(), icao, 0, "FBO Transfer", false);
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

    public static void buyFbo(int fboId, int accountId, UserBean user) throws DataError
    {
        if (user.getId() != accountId && user.groupMemberLevel(accountId) < UserBean.GROUP_STAFF)
        {
            throw new DataError("Permission denied");
        }

        try
        {
            FboBean fbo = getFbo(fboId);
            if ((fbo != null) && fbo.isForSale())
            {
                int sellPrice = fbo.getPrice();
                int oldOwner = fbo.getOwner();
                String icao = fbo.getLocation();
                boolean includesGoods = fbo.getPriceIncludesGoods();

                String qry = "SELECT (count(id) > 0) AS found FROM accounts WHERE id = ?";
                boolean exists = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), accountId);
                if (!exists)
                {
                    throw new DataError("Account not found");
                }

                qry = "SELECT (money >= ?) as enough FROM accounts WHERE id = ?";
                boolean enough = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), sellPrice, accountId);
                if (!enough)
                {
                    throw new DataError("Not enough money to buy FBO");
                }

                doTransferFbo(fbo, accountId, oldOwner, icao, includesGoods);
                Goods.resetAllGoodsSellBuyFlag(oldOwner, icao);

                Banking.doPayment(accountId, oldOwner, sellPrice, PaymentBean.FBO_SALE, 0, fboId, icao, 0, "", false);
            }
            else
            {
                throw new DataError("FBO not found");
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void transferFbo(FboBean fbo, UserBean user, int buyer, int owner, String icao, boolean goods) throws DataError
    {
        if (!fbo.updateAllowed(user) && (!Accounts.needLevel(user, UserBean.LEV_MODERATOR)))
        {
            throw new DataError("Permission denied.");
        }

        doTransferFbo(fbo, buyer, owner, icao, goods);
    }

    public static void buildRepairShop(FboBean fbo) throws DataError
    {
        if ((fbo.getServices() & FboBean.FBO_REPAIRSHOP) > 0)
        {
            throw new DataError("Repairshop already built.");
        }

        GoodsBean goods = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
        if (goods == null || goods.getAmount() < GoodsBean.CONSTRUCT_REPAIRSHOP)
        {
            throw new DataError("Not enough building materials available.");
        }

        try
        {
            Goods.changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_REPAIRSHOP, false);

            String qry = "UPDATE fbo SET services = services | ?, margin = 20, equipmentmargin = 50 where id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, FboBean.FBO_REPAIRSHOP, fbo.getId());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean hasSuppliesForSale(String icao)
    {
        boolean result = false;

        try
        {
            String qry = "SELECT COUNT(goods.owner) > 0 as supplies FROM goods LEFT JOIN airports ON goods.location = airports.icao WHERE goods.location = ?  AND goods.type between 1 AND 4 AND goods.saleFlag > 0 AND (goods.amount - cast(goods.retain as signed int) > 0)";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), icao);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean hasRepairShop(String icao)
    {
        boolean result = false;

        try
        {
            String qry = "SELECT count(id) > 0 AS repairshop FROM fbo WHERE (services & 1)  > 0 AND location = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), icao);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean buildPassengerTerminal(FboBean fbo) throws DataError
    {
        if (fbo.getName().length() > 45)
        {
            throw new DataError("FBO Name cannot exceed 45 characters.");
        }

        if ((fbo.getServices() & FboBean.FBO_PASSENGERTERMINAL) > 0)
        {
            throw new DataError("Passenger terminal already built.");
        }

        GoodsBean goods = Goods.getGoods(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS);
        if (goods == null || goods.getAmount() < GoodsBean.CONSTRUCT_PASSENGERTERMINAL)
        {
            throw new DataError("Not enough building materials available.");
        }

        try
        {
            Goods.changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_PASSENGERTERMINAL, false);

            String qry = "UPDATE fbo SET services = services | ? WHERE owner = ? AND location = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, FboBean.FBO_PASSENGERTERMINAL, fbo.getOwner(), fbo.getLocation());

            int fboSlots = Airports.getTotalFboSlots(fbo.getLocation());

            qry = "INSERT INTO fbofacilities (location, fboId, occupant, reservedSpace, size, rent, name, units, commodity, maxDistance, matchMaxSize, publicByDefault) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

            DALHelper.getInstance().ExecuteUpdate(qry, fbo.getLocation(), fbo.getId(), fbo.getOwner(), fbo.getFboSize() * fboSlots, 0, FboFacilityBean.DEFAULT_RENT, fbo.getName(), AssignmentBean.UNIT_PASSENGERS, FboFacilityBean.DEFAULT_COMMODITYNAME_PASSENGERS, 300, 99999, 1);

            return true;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return false;
    }

    public static void rentFboFacility(UserBean user, int occupantId, int facilityId, int blocks) throws DataError
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

            FboFacilityBean landlord = getFboFacility(facilityId);
            if (!landlord.getIsDefault())
            {
                existingFacilityId = landlord.getId();
                landlord = getFboDefaultFacility(landlord.getFboId());
            }

            FboBean fbo = getFbo(landlord.getFboId());
            //AirportBean airport = Airports.getAirport(landlord.getLocation());

            if (blocks > calcFboFacilitySpaceAvailable(landlord, fbo))
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

    public static synchronized void transferFBOGoods(int buyer, int seller, String location, int type, int amount) throws DataError
    {
        // The amount to transfer is validated in transfergoods.jsp and does not have to be repeated here
        Goods.changeGoodsRecord(location, type, seller, -amount, false);
        Goods.changeGoodsRecord(location, type, buyer, amount, false);

        int fboId = -1;
        short cnvCommodityToTransfer[] = {0, PaymentBean.TRANSFER_GOODS_BUILDING_MATERIALS, PaymentBean.TRANSFER_GOODS_SUPPLIES, PaymentBean.TRANSFER_GOODS_FUEL, PaymentBean.TRANSFER_GOODS_JETA};

        Banking.doPayment(buyer, seller, 0, cnvCommodityToTransfer[type], 0, fboId, location, 0, amount + " Units", false);
    }

    public static List<FboBean> getFbo()
    {
        return getFboSql("SELECT * from fbo ORDER BY id");
    }

    public static List<FboBean> getFboByOwner(int owner)
    {
        return getFboSql("SELECT * FROM fbo WHERE owner=" + owner + " ORDER BY id");
    }

    public static FboBean getFboByID(int fboID)
    {
        return getSingleFboSql("SELECT * FROM fbo WHERE id=" + fboID);
    }

    public static String getFboNameById(int fboID)
    {
        String retval = null;

        try
        {
            String qry = "SELECT name FROM fbo WHERE id = ?";
            retval = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), fboID);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return retval;
    }

    public static List<FboBean> getFboByOwner(int owner, String sortFieldName)
    {
        return getFboSql("SELECT * FROM fbo WHERE owner=" + owner + " ORDER BY " + sortFieldName);
    }

    public static List<FboBean> getFboByLocation(String location)
    {
        return getFboSql("SELECT * FROM fbo WHERE active = 1 AND location='" + location + "' ORDER BY id");
    }

    public static List<FboBean> getInactiveFboByLocation(String location)
    {
        return getFboSql("SELECT * FROM fbo WHERE active = 0 AND location='" + location + "' ORDER BY id");
    }

    public static List<FboBean> getFboForRepair(String icao)
    {
        return getFboForRepair(icao, 0);
    }

    public static List<FboBean> getFboForRepair(String icao, int orderby)
    {
        //0 = id, 1 = repair margin, 2 = equipment margin
        String order;
        switch (orderby)
        {
            case Fbos.FBO_REPAIR_MARGIN:
                order = "margin";
            case Fbos.FBO_EQUIPMENT_MARGIN:
                order = "equipmentmargin";
            default:
                order = "id";
        }
        
        CachedAirportBean cab = Airports.cachedAirports.get(icao);
        List<FboBean> returnValue = getFboSql("SELECT * from fbo WHERE active = 1 AND (services & " + FboBean.FBO_REPAIRSHOP + ") > 0 AND location='" + icao + "' ORDER BY " + order);

        if (cab.getSize() >= AircraftMaintenanceBean.REPAIR_AVAILABLE_AIRPORT_SIZE)
        {
            FboBean fb = FboBean.getInstance();
            fb.setLocation(icao);

            returnValue.add(fb);
        }

        return returnValue;
    }

    public static List<FboBean> getFboForSale()
    {
        //id, name, price, location
        ArrayList<FboBean> result = new ArrayList<>();

        try
        {
            String qry = "SELECT id, owner, name, active, fbosize, services, saleprice, location FROM fbo f WHERE f.saleprice > 0 ORDER BY f.saleprice";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                FboBean fbo = new FboBean();

                fbo.setId(rs.getInt("id"));
                fbo.setOwner(rs.getInt("owner"));
                fbo.setName(rs.getString("name"));
                fbo.setActive(rs.getInt("active"));
                fbo.setLocation(rs.getString("location"));
                fbo.setPrice(rs.getInt("saleprice"));
                fbo.setFboSize(rs.getInt("fbosize"));
                fbo.setServices(rs.getInt("services"));

                result.add(fbo);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static FboBean getFbo(int id)
    {
        if (id == 0)
        {
            return null;
        }

        List<FboBean> result = getFboSql("SELECT * FROM fbo WHERE id=" + id);
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<FboBean> getFboSql(String qry)
    {
        ArrayList<FboBean> result = new ArrayList<>();

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                result.add(new FboBean(rs));
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static FboBean getSingleFboSql(String qry)
    {
        FboBean fbo = null;

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            if (rs.next())
            {
                fbo = new FboBean(rs);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return fbo;
    }

    public static List<FboFacilityBean> getFboFacilitiesByOccupant(int account)
    {
        return getFboFacilitiesSql("select t.* from fbofacilities t where t.occupant = " + account + " order by location, id");
    }

    public static List<FboFacilityBean> getFboDefaultFacilitiesForAirport(AirportBean airport)
    {
        return getFboFacilitiesForAirport(airport.getIcao());
    }

    public static List<FboFacilityBean> getFboDefaultFacilitiesForAirport(String icao)
    {
        return getFboFacilitiesSql("SELECT * FROM fbofacilities WHERE reservedSpace >= 0 AND location ='" + icao + "' order by id");
    }

    public static List<FboFacilityBean> getFboFacilitiesForAirport(String icao)
    {
        return getFboFacilitiesSql("select t.* from fbofacilities t, fbo f where t.fboId = f.id and f.active = 1 and f.location = '" + icao + "' order by id");
    }

    public static FboFacilityBean getFboDefaultFacility(FboBean fbo)
    {
        return getFboDefaultFacility(fbo.getId());
    }

    public static FboFacilityBean getFboDefaultFacility(int fboId)
    {
        List<FboFacilityBean> result = getFboFacilitiesSql("SELECT * FROM fbofacilities WHERE reservedSpace >= 0 AND fboId=" + fboId);
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<FboFacilityBean> getFboRenterFacilities(FboBean fbo)
    {
        return getFboFacilitiesSql("select * from fbofacilities where reservedSpace < 0 and fboId = " + fbo.getId() + " order by id");
    }

    public static FboFacilityBean getFboFacility(int id)
    {
        List<FboFacilityBean> result = getFboFacilitiesSql("SELECT * FROM fbofacilities WHERE id=" + id);
        return result.size() == 0 ? null : result.get(0);
    }

    public static List<FboFacilityBean> getFboFacilitiesSql(String qry)
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

    public static int calcFboFacilitySpaceAvailable(FboFacilityBean facility, FboBean fbo)
    {
        int spaceInUse = getFboFacilityBlocksInUse(fbo.getId());
        int fboSlots = Airports.getTotalFboSlots(fbo.getLocation());
        int totalSpace = fbo.getFboSize() * fboSlots;

        return Math.max(0, totalSpace - spaceInUse - facility.getReservedSpace());
    }

    public static int getFboFacilityBlocksInUse(int fboId)
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

    public static void deleteFbo(int fboId, UserBean user) throws DataError
    {
        try
        {
            FboBean fbo = getFbo(fboId);
            if (fbo == null)
            {
                throw new DataError("FBO not found.");
            }

            if (!fbo.deleteAllowed(user))
            {
                throw new DataError("Permission denied.");
            }

            if (doesBulkFuelRequestExist(fbo.getId()))
            {
                throw new DataError("Cannot teardown an FBO that has an active pending bulk fuel delivery.");
            }

            int inUse = getFboFacilityBlocksInUse(fbo.getId());
            int newSpace = (fbo.getFboSize() - 1) * Airports.getTotalFboSlots(fbo.getLocation());
            if (inUse > newSpace)
            {
                throw new DataError("An FBO with tennants can not be torn down.");
            }

            int recover = fbo.recoverableBuildingMaterials();
            Goods.changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), recover, false);

            String qry = "UPDATE fbo SET fbosize = fbosize - 1 WHERE id = " + fbo.getId() + ";";
            qry += "UPDATE fbofacilities set reservedSpace = " + newSpace + " WHERE reservedSpace > " + newSpace + " and fboId = " + fboId + ";";
            qry += "DELETE FROM fbofacilities where fboId IN (select id from fbo where fboSize < 1);";
            qry += "DELETE FROM fbo WHERE fbosize < 1;";
            DALHelper.getInstance().ExecuteBatchUpdate(qry);

            //if fbo deleted remove sell/buy flag from any goods
            qry = "SELECT id FROM fbo where id=?";
            int id = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), fbo.getId());
            if (id == 0)
            {
                Goods.resetAllGoodsSellBuyFlag(fbo.getOwner(), fbo.getLocation());
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void upgradeFbo(int fboId, UserBean user) throws DataError
    {
        try
        {
            FboBean fbo = getFbo(fboId);
            if (fbo == null)
            {
                throw new DataError("FBO not found.");
            }

            if (!fbo.updateAllowed(user))
            {
                throw new DataError("Permission denied.");
            }

            if (getAirportFboSlotsAvailable(fbo.getLocation()) < 1)
            {
                throw new DataError("There is no room for construction at this airport.");
            }

            if (!Goods.checkGoodsAvailable(fbo.getLocation(), fbo.getOwner(), GoodsBean.GOODS_BUILDING_MATERIALS, GoodsBean.CONSTRUCT_FBO))
            {
                throw new DataError("Not enough building materials available.");
            }

            Goods.changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_FBO, false);

            String qry = "UPDATE fbo SET fbosize = fbosize + 1 WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, fbo.getId());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void updateFbo(FboBean fbo, UserBean user) throws DataError
    {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try
        {
            if (!fbo.updateAllowed(user))
            {
                throw new DataError("Permission denied.");
            }

            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

            rs = stmt.executeQuery("SELECT * FROM fbo WHERE id = " + fbo.getId());
            if (!rs.next())
            {
                throw new DataError("Could not find FBO!");
            }

            fbo.writeBean(rs);
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

    public static void createFbo(FboBean fbo, UserBean user) throws DataError
    {
        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try
        {
            if (!fbo.updateAllowed(user))
            {
                throw new DataError("Permission denied.");
            }

            String qry = "select" +
                    "       case" +
                    "        when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1" +
                    "        when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2" +
                    "        else 3" +
                    "       end - case when ISNULL(fbo.location) then 0 else sum(fbosize) end" +
                    "        > 0 as found" +
                    "      from airports" +
                    "      left outer join fbo on fbo.location = airports.icao" +
                    "      where airports.icao='" + fbo.getLocation() + "'" +
                    "      group by airports.icao";
            boolean result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer());
            if (!result)
            {
                throw new DataError("No lots open!");
            }

            qry = "SELECT count(*) > 0 as found FROM fbo WHERE owner = ? AND location = ? AND id <> ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), fbo.getOwner(), fbo.getLocation(), fbo.getId());
            if (result)
            {
                throw new DataError("You already own an FBO at this location.");
            }

            int availableBM;
            int availableSupplies;
            qry = "SELECT amount FROM goods WHERE type=? AND location=? AND owner=?";
            availableBM = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getLocation(), fbo.getOwner());

            if (availableBM < GoodsBean.CONSTRUCT_FBO)
            {
                throw new DataError("Not enough building materials available.");
            }

            qry = "SELECT amount FROM goods WHERE type=? AND location=? AND owner=?";
            availableSupplies = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), GoodsBean.GOODS_SUPPLIES, fbo.getLocation(), fbo.getOwner());

            if (availableSupplies < 10)
            {
                throw new DataError("Unable to build. The required 1 day of supplies(10kg) are not available.");
            }

            conn = DALHelper.getInstance().getConnection();
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * FROM fbo WHERE id = " + fbo.getId());

            rs.moveToInsertRow();

            rs.updateString("location", fbo.getLocation());
            rs.updateInt("owner", fbo.getOwner());
            rs.updateInt("active", 1);
            fbo.writeBean(rs);

            rs.insertRow();
            Goods.changeGoodsRecord(fbo.getLocation(), GoodsBean.GOODS_BUILDING_MATERIALS, fbo.getOwner(), -GoodsBean.CONSTRUCT_FBO, false);
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

    public static void deleteFboFacility(UserBean user, int facilityId) throws DataError
    {
        try
        {
            FboFacilityBean facility = getFboFacility(facilityId);
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

    public static void updateFboFacility(FboFacilityBean facility, List<FboFacilityBean> renters, UserBean user) throws DataError
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

    public static void logBulkFuelRequest(int fboId)
    {
        //log the current date/time for this request so another one cannot be requested for 24hrs.
        Timestamp timestamp = new Timestamp(new java.util.Date().getTime());

        try
        {
            String qry = "UPDATE fbo SET bulkFuelOrderTimeStamp = ? WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, timestamp, fboId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }//reset the timestamp for a given FBO so they can process a request for bulk fuel without waiting for the 24 hour rule

    public static void resetBulkFuelOrder(int fboId)
    {
        try
        {
            String qry = "UPDATE fbo SET bulkFuelOrderTimeStamp = null, bulk100llOrdered = null, bulkJetAOrdered = null, bulkFuelDeliveryDateTime = null WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, fboId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }//check to see if a request has been made in the past 24 hrs for an FBO

    public static boolean doesBulkFuelRequestExist(int id)
    {
        Calendar calNow = Calendar.getInstance();
        try
        {
            String qry = "SELECT bulkFuelOrderTimeStamp, bulkFuelDeliveryDateTime FROM fbo WHERE id = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id);

            if (!rs.next())
            {
                return false;
            }

            Timestamp orderTS = rs.getTimestamp(1);
            Timestamp deliveryTS = rs.getTimestamp(2);

            if (orderTS == null)
            {
                return false;
            }

            if (deliveryTS != null)
            {
                return true;
            }

            //check for a request in the past 24 hours
            Calendar calTS = Calendar.getInstance();
            calTS.setTimeInMillis(orderTS.getTime());

            long diff = calNow.getTimeInMillis() - calTS.getTimeInMillis();
            long diffHours = diff / Constants.MILLISECS_PER_HOUR;

            if (diffHours < 24)
            {
                return true;
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return false;
    }//check to see if a order in progress

    public static boolean doesBulkFuelOrderExist(int id)
    {
        try
        {
            String qry = "SELECT (bulkFuelDeliveryDateTime is not null) as found FROM fbo WHERE id = ?";

            return DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.BooleanResultTransformer(), id);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return false;
    }//calculate a shipping day for bulk fuel

    public static int calculateShippingDay()
    {
        Random randomGenerator = new Random();
        int randomInt;
        int base = 3;    //from 0 to 3 days out
        randomInt = randomGenerator.nextInt(base);

        return randomInt;
    }

    public static String deliveryDateFormatted(int daysOut)
    {
        Calendar deliveryDate = Calendar.getInstance();
        if (daysOut == 0)
        {
            deliveryDate.add(Calendar.HOUR_OF_DAY, 4); //add 4 hours if same day
        }
        else
        {
            deliveryDate.add(Calendar.DATE, daysOut);
        }

        Timestamp deliveryDateSql = new Timestamp(deliveryDate.getTimeInMillis());

        return Formatters.dateyyyymmddhhmmzzz.format(deliveryDateSql);
    }//record the transaction request for bulk fuel - to be delivered later by the maintenance code

    public static void registerBulkFuelOrder(UserBean user, int fboID, int amount100ll, int amountJetA, int daysOut, int accountToPay, int location, String icao) throws DataError
    {
        UserBean account = Accounts.getAccountById(accountToPay);

        if (account.getId() != user.getId() && user.groupMemberLevel(account.getId()) < UserBean.GROUP_STAFF)
        {
            throw new DataError("Permission denied");
        }

        if (doesBulkFuelOrderExist(fboID))
        {
            throw new DataError("Fuel order already exists.");
        }

        double price100ll = Goods.quoteFuel(icao, GoodsBean.GOODS_FUEL100LL, amount100ll);
        double priceJetA = Goods.quoteFuel(icao, GoodsBean.GOODS_FUELJETA, amountJetA);

        double total = price100ll + priceJetA;
        if (account.getMoney() < total)
        {
            throw new DataError(account.getName() + " has insufficent funds for this purchase!");
        }

        Calendar calDeliveryDate = Calendar.getInstance();
        if (daysOut == 0)
        {
            calDeliveryDate.add(Calendar.HOUR_OF_DAY, 4); //add 4 hours if same day
        }
        else
        {
            calDeliveryDate.add(Calendar.DATE, daysOut);
        }

        Timestamp deliveryTS = new Timestamp(calDeliveryDate.getTimeInMillis());

        try
        {
            String daysMsg = " -- delivery ETA: " + deliveryDateFormatted(daysOut);
            String comment1 = "", comment2 = "";

            if (amount100ll > 0)
            {
                comment1 = "100LL:" + amount100ll + " Kg";
            }

            if (amountJetA > 0)
            {
                comment2 = " JetA:" + amountJetA + " Kg";
            }

            String qry = "UPDATE fbo SET bulk100llOrdered = ?, bulkJetAOrdered = ?, bulkFuelDeliveryDateTime = ? WHERE id = ?";
            DALHelper.getInstance().ExecuteUpdate(qry, amount100ll, amountJetA, deliveryTS, fboID);

            //Now deduct the $ from the account paying for the order - transfer amount to Bank of FSE, log each payment seperately
            Banking.doPayBulkFuel(accountToPay, 0, (int) price100ll, location, comment1 + daysMsg, icao, GoodsBean.GOODS_FUEL100LL);
            Banking.doPayBulkFuel(accountToPay, 0, (int) priceJetA, location, comment2 + daysMsg, icao, GoodsBean.GOODS_FUELJETA);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static int getFacilityJobCount(int id, String location)
    {
        int cnt = 0;
        try
        {
            String qry = "select t.* from fbofacilities t where t.occupant = ? AND t.location = ? order by location, id";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, id, location);

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

    public static int getAirportFboSlotsAvailable(String icao)
    {
        int result = 0;
        try
        {
            String qry = "select" +
                    "       case" +
                    "        when airports.size < " + AirportBean.MIN_SIZE_MED + " then 1" +
                    "        when airports.size < " + AirportBean.MIN_SIZE_BIG + " then 2" +
                    "        else 3" +
                    "       end - case when ISNULL(fbo.location) then 0 else sum(fbosize) end" +
                    "       as SlotsAvailable" +
                    "      from airports" +
                    "      left outer join fbo on fbo.location = airports.icao" +
                    "      where airports.icao = ? " +
                    "      group by airports.icao";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), icao);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static int getAirportFboSlotsInUse(String icao)
    {
        int result = 0;
        try
        {
            String qry = "select sum(fbosize) as SlotsUsed from fbo where location = ?";
            result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.IntegerResultTransformer(), icao);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }


    //Added ability to just add up total value, and not do any DB updates - Airboss 3/5/11
    public static double payFboGroundCrewFees(String fboIcao, AssignmentBean assignment, int payAssignmentToAccount, String location, int aircraftId, boolean checkonly)
    {
        if (assignment.isFerry())
            return 0.0;

        double fboAssignmentFee = 0.0;
        double fbofee = assignment.calcPay() * 0.05;
        List<FboBean> fbos = Fbos.getFboByLocation(fboIcao);
        if (fbos.size() > 0)
        {
            FboBean ownerFbo = null;
            if (assignment.getFromFboTemplate() > 0)
            {
                FboFacilityBean facility = Fbos.getFboFacility(assignment.getFromFboTemplate());
                if (facility != null)
                {
                    FboBean facilityFbo = Fbos.getFbo(facility.getFboId());
                    if (facilityFbo != null)
                    {
                        int facilityFboUltimateOwner = Accounts.accountUltimateOwner(facilityFbo.getOwner());
                        for (FboBean fbo : fbos)
                        {
                            if (Accounts.accountUltimateOwner(fbo.getOwner()) == facilityFboUltimateOwner)
                            {
                                ownerFbo = fbo;
                                break;
                            }
                        }
                    }
                }
            }

            if (ownerFbo == null)
            {
                int flightUltimateOwner = Accounts.accountUltimateOwner(payAssignmentToAccount);
                for (FboBean fbo : fbos)
                {
                    if (Accounts.accountUltimateOwner(fbo.getOwner()) == flightUltimateOwner)
                    {
                        ownerFbo = fbo;
                        break;
                    }
                }
            }

            if (ownerFbo != null)
            {
                fbos = new ArrayList<>();
                fbos.add(ownerFbo);
            }

            fboAssignmentFee = fbofee;

            if (!checkonly)
            {
                // Divide fee equally between originating FBOs
                int lotsTotal = 0;
                for (FboBean fbo : fbos)
                    lotsTotal += fbo.getFboSize();

                double thisFboFee;
                for (FboBean fbo : fbos)
                {
                    thisFboFee = fbofee * ((double) fbo.getFboSize() / lotsTotal);
                    Banking.doPayment(payAssignmentToAccount, fbo.getOwner(), thisFboFee, PaymentBean.FBO_ASSIGNMENT_FEE, 0, fbo.getId(), location, aircraftId, "", false);
                }
            }
        }

        return fboAssignmentFee;
    }

    public static InputStream getInvoiceBackground(int fbo)
    {
        InputStream returnValue = null;
        try
        {
            String qry = "SELECT invoice FROM fbo WHERE id = ?";
            Blob image;
            image = DALHelper.getInstance().ExecuteScalarBlob(qry, fbo);
            if (image != null)
                returnValue = image.getBinaryStream();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return returnValue;
    }

    public static void updateInvoiceBackground(FboBean fbo, InputStream data, int length, UserBean user) throws DataError
    {
        if (!fbo.updateAllowed(user))
            throw new DataError("Permission denied.");

        try
        {
            String qry = "SELECT invoice, id FROM fbo WHERE id = ?";
            if (!DALHelper.getInstance().ExecuteUpdateBlob(qry, "invoice", data, length, fbo.getId()))
                throw new DataError("Update to invoice failed!");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
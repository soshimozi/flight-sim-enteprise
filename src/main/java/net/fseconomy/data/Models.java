package net.fseconomy.data;

import net.fseconomy.beans.ModelBean;
import net.fseconomy.beans.UserBean;
import net.fseconomy.dto.MakeModel;
import net.fseconomy.dto.Model;
import net.fseconomy.dto.ModelAliases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Models implements Serializable
{
    static final int MAX_MODEL_TITLE_LENGTH = 128;

    public final static Logger logger = LoggerFactory.getLogger(Data.class);

    public static String addModel(String aircraft, int[] fuelCapacities)
    {
        String result = null;

        try
        {
            String qry = "SELECT * FROM fsmappings WHERE fsaircraft = ?";
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, aircraft);
            if (!rs.next())
            {
                String title = aircraft.length() > MAX_MODEL_TITLE_LENGTH ? aircraft.substring(0, MAX_MODEL_TITLE_LENGTH - 1) : aircraft;

                qry = "INSERT INTO fsmappings (fsaircraft, fcapCenter, fcapLeftMain, fcapLeftAux, fcapLeftTip, fcapRightMain, fcapRightAux, fcapRightTip, fcapCenter2, fcapCenter3, fcapExt1, fcapExt2) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
                DALHelper.getInstance().ExecuteUpdate(qry, title, fuelCapacities[ModelBean.fuelTank.Center], fuelCapacities[ModelBean.fuelTank.LeftMain], fuelCapacities[ModelBean.fuelTank.LeftAux], fuelCapacities[ModelBean.fuelTank.LeftTip], fuelCapacities[ModelBean.fuelTank.RightMain], fuelCapacities[ModelBean.fuelTank.RightAux], fuelCapacities[ModelBean.fuelTank.RightTip], fuelCapacities[ModelBean.fuelTank.Center2], fuelCapacities[ModelBean.fuelTank.Center3], fuelCapacities[ModelBean.fuelTank.Ext1], fuelCapacities[ModelBean.fuelTank.Ext2]);
            }
            else
            {
                int model = rs.getInt("model");
                if (model > 0)
                {
                    qry = "SELECT CONCAT_WS(' ', make, model) FROM models WHERE id = ?";
                    result = DALHelper.getInstance().ExecuteScalar(qry, new DALHelper.StringResultTransformer(), model);

                    ModelBean mb = getModelById(model);
                    int[] mcap = mb.getCapacity();
                    boolean mismatch = false;
                    for (int i = 0; i < ModelBean.fuelTank.NumTanks; i++)
                    {
                        if (fuelCapacities[i] != mcap[i])
                        {
                            mismatch = true;
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append(result);

                    if (mismatch)
                    {
                        sb.append("|");
                        sb.append("|");
                        sb.append("Fuel Tank Mismatch!|");
                        sb.append("Center - FSE ");
                        sb.append(mcap[0]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.Center]);
                        sb.append("|");
                        sb.append("LeftMain - FSE ");
                        sb.append(mcap[1]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.LeftMain]);
                        sb.append("|");
                        sb.append("LeftAux - FSE ");
                        sb.append(mcap[2]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.LeftAux]);
                        sb.append("|");
                        sb.append("LeftTip - FSE ");
                        sb.append(mcap[3]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.LeftTip]);
                        sb.append("|");
                        sb.append("RightMain - FSE ");
                        sb.append(mcap[4]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.RightMain]);
                        sb.append("|");
                        sb.append("RightAux - FSE ");
                        sb.append(mcap[5]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.RightAux]);
                        sb.append("|");
                        sb.append("RightTip - FSE ");
                        sb.append(mcap[6]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.RightTip]);
                        sb.append("|");
                        sb.append("Center2 - FSE ");
                        sb.append(mcap[7]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.Center2]);
                        sb.append("|");
                        sb.append("Center3 - FSE ");
                        sb.append(mcap[8]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.Center3]);
                        sb.append("|");
                        sb.append("External1 - FSE ");
                        sb.append(mcap[9]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.Ext1]);
                        sb.append("|");
                        sb.append("External2 - FSE ");
                        sb.append(mcap[10]);
                        sb.append(", Aircraft ");
                        sb.append(fuelCapacities[ModelBean.fuelTank.Ext2]);
                        sb.append("|");
                    }
                    result = sb.toString();
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<ModelBean> getAllModels()
    {
        return getModelsSQL("SELECT * FROM models ORDER By make,model");
    }

    public static ModelBean getModelById(int id)
    {
        return getModelSQL("SELECT * FROM models WHERE id = " + id);
    }

    public static ModelBean getModelSQL(String qry)
    {
        ModelBean result = null;

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            if (rs.next())
            {
                result = new ModelBean(rs);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static List<ModelBean> getModelsSQL(String qry)
    {
        ArrayList<ModelBean> result = new ArrayList<ModelBean>();

        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                ModelBean model = new ModelBean(rs);
                result.add(model);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    public static void updateModel(ModelBean model, UserBean user) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            boolean newEntry;
            conn = DALHelper.getInstance().getConnection();

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from models WHERE id = " + model.getId());
            if (!rs.next())
            {
                newEntry = true;
                rs.moveToInsertRow();
            }
            else
            {
                newEntry = false;
            }

            model.writeBean(rs);
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

    public static List<MakeModel> getMakeModels()
    {
        String qry;
        ResultSet rs;
        ArrayList<MakeModel> makemodels = new ArrayList<>();

        try
        {
            qry = "SELECT DISTINCT models.make FROM models ORDER BY models.make";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                MakeModel mm = new MakeModel();
                mm.MakeName = rs.getString(1);

                makemodels.add(mm);
            }

            for(MakeModel item : makemodels)
            {
                ArrayList<Model> models = new ArrayList<>();
                qry = "SELECT models.id, models.model FROM models where models.make = ? ORDER BY models.make, models.model";
                rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, item.MakeName);
                while (rs.next())
                {
                    Model m = new Model();
                    m.Id = rs.getInt(1);
                    m.ModelName = rs.getString(2);

                    models.add(m);
                }
                item.Models = models.toArray(new Model[models.size()]);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return makemodels;
    }

    public static ModelAliases getModelAliases(int modelId)
    {
        String qry;
        ResultSet rs = null;
        ModelAliases modelaliases = new ModelAliases();

        try
        {
            qry = "SELECT models.make, models.model FROM models where id=? ORDER BY models.make";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, modelId);

            if(!rs.next())
            {
                logger.error("getModelAliases() unable to find modelId: " + modelId);
                return new ModelAliases();
            }

            modelaliases.MakeModel = rs.getString(1) + " " + rs.getString(2);

            ArrayList<String> aliases = new ArrayList<>();
            qry = "SELECT fsaircraft FROM fsmappings where model = ? ORDER BY fsaircraft;";
            rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry, modelId);
            while (rs.next())
            {
                aliases.add(rs.getString(1));
            }
            modelaliases.Aliases = aliases.toArray(new String[aliases.size()]);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return modelaliases;
    }
}
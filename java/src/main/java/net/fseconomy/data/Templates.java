package net.fseconomy.data;

import net.fseconomy.beans.TemplateBean;
import net.fseconomy.beans.UserBean;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Templates
{
    public static List<TemplateBean> getAllTemplates()
    {
        return getTemplateSQL("SELECT * FROM templates");
    }

    public static TemplateBean getTemplateById(int Id)
    {
        List<TemplateBean> result = getTemplateSQL("SELECT * FROM templates WHERE id = " + Id);

        return result.size() == 0 ? null : result.get(0);
    }

    public static List<TemplateBean> getTemplateSQL(String qry)
    {
        ArrayList<TemplateBean> result = new ArrayList<>();
        try
        {
            ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
            while (rs.next())
            {
                TemplateBean template = new TemplateBean(rs);
                result.add(template);
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return result;
    }
    public static void updateTemplate(TemplateBean template, UserBean user) throws DataError
    {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try
        {
            boolean newEntry;
            conn = DALHelper.getInstance().getConnection();

            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT * from templates WHERE id = " + template.getId());
            if (!rs.next())
            {
                newEntry = true;
                rs.moveToInsertRow();
            }
            else
            {
                newEntry = false;
            }

            template.writeBean(rs);
            if (newEntry)
                rs.insertRow();
            else
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
}

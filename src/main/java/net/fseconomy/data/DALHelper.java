/*
 * FS Economy
 * Copyright (C) 2013 FSEconomy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.fseconomy.data;

import java.sql.*;
import java.util.Date;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;

import java.io.InputStream;
import java.math.BigDecimal;

import net.fseconomy.fixes.FixedCachedRowSetImpl;
import net.fseconomy.util.GlobalLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DALHelper
{
	private static DataSource dataSource = null;

    //Singleton pattern
	private static DALHelper singleton = new DALHelper();
    public static DALHelper getInstance()
    {
        return singleton;
    }

	public DALHelper()
	{
		GlobalLogger.logApplicationLog("DALHelper constructor called", DALHelper.class);

        try
        {
            Context ctx = new InitialContext();
            dataSource = (DataSource)ctx.lookup("java:jboss/datasources/fseconomy");
        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
	}
	
	public PreparedStatement createPreparedStatement(Connection conn, String qry, int resultsetType, int resultsetConcurrency, Object... args) throws SQLException
	{
        PreparedStatement stmt = conn.prepareStatement(qry, resultsetType, resultsetConcurrency);

        // Add SQL parameters
        mapParams(stmt, args);
        
        return stmt;
	}

    public CallableStatement createCallableStatement(Connection conn, String qry, Object... args) throws SQLException
    {
        CallableStatement stmt = conn.prepareCall(qry);

        // Add SQL parameters
        mapParams(stmt, args);
        return stmt;
    }

    public void mapParams(PreparedStatement ps, Object... args)  throws SQLException
	{
		if(args == null) return;
		
	    int i = 1;
	    for (Object arg : args) 
	    {         
	         if (arg instanceof Date) 
	        	 ps.setTimestamp(i++, new Timestamp(((Date) arg).getTime()));
	         else if (arg instanceof Integer) 
	        	 ps.setInt(i++, (Integer) arg);
	         else if (arg instanceof Long) 
	        	 ps.setLong(i++, (Long) arg);
	         else if (arg instanceof Double) 
	        	 ps.setDouble(i++, (Double) arg);
	         else if (arg instanceof Float) 
	        	 ps.setFloat(i++, (Float) arg);
	         else if (arg instanceof String)
	        	 ps.setString(i++, (String) arg);
	         else if (arg instanceof Boolean)
	        	 ps.setBoolean(i++, (Boolean) arg);
	         else if (arg == null)
	        	 ps.setNull(i++, Types.NULL);
	         else
	        	 throw new SQLException("mapParams error, unhandled parameter of " + arg.getClass().toString());
	    }
	}

//    int getIntSQL(String qry)
//    {
//        try
//        {
//            ResultSet rs = ExecuteReadOnlyQuery(qry);
//            if (rs.next())
//                return rs.getInt(1);
//        }
//        catch (SQLException e)
//        {
//            e.printStackTrace();
//        }
//
//        return -1;
//    }

    public boolean ExecuteStoredProcedureWithStatus(String qry, Object... args) throws SQLException
    {
        Connection conn = null;
        CallableStatement stmt = null;
        try
        {
            conn = getConnection();

            stmt = createCallableStatement(conn, qry, args);
            stmt.registerOutParameter(args.length+1, Types.TINYINT);
            stmt.execute();

            return stmt.getBoolean(args.length+1);
        }
        finally
        {
            tryClose(stmt);
            tryClose(conn);
        }
    }

    public boolean ExecuteNonQuery(String qry, Object... args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try
		{
			conn = getConnection();

			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, args);
			return stmt.execute();
		}
		finally
		{
			tryClose(stmt);
			tryClose(conn);
		}
	}

	public int ExecuteUpdate(String qry, Object... args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		try
		{
			conn = getConnection();

			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, args);
			return stmt.executeUpdate();
		}
		finally
		{
			tryClose(stmt);
			tryClose(conn);
		}
	}

	public int[] ExecuteBatchUpdate(String qry) throws SQLException
	{
		Connection conn = null;
		Statement stmt = null;
		try
		{
			conn = getConnection();
			stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
			
			if(qry.contains(";"))
			{
				String[] split = qry.split(";");
				for(String b : split)
					if(!b.equals(""))
						stmt.addBatch(b);
				
				return stmt.executeBatch();
			}
			else
			{
				throw new SQLException("Invalid query parameter. Expecting multiple statements separated by ';'");
			}
		}
		finally
		{
			tryClose(stmt);
			tryClose(conn);
		}
	}

	public Object ExecuteScalar(String qry, Object...args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try
		{
			conn = getConnection();
			
			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, args);
			rs = stmt.executeQuery();
			
			if(rs.next())
				return rs.getObject(1);
			else
				return null;
		}
		finally
		{
			tryClose(rs);
			tryClose(stmt);
			tryClose(conn);
		}
	}

	public <T> T ExecuteScalar(String qry, ResultTransformer<T> transformer, Object...args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			conn = getConnection();
			
			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, args);
			rs = stmt.executeQuery();
			
			return transformer.transform(rs);
		}
		finally
		{
			tryClose(rs);
			tryClose(stmt);
			tryClose(conn);
		}		
	}
	
	interface ResultTransformer<T> 
	{
	    T transform(ResultSet rs) throws SQLException;
	}

	public static class BigDecimalResultTransformer implements ResultTransformer<BigDecimal>
	{
	    public BigDecimal transform(ResultSet rs) throws SQLException
	    {
	    	if (rs.next()) 
	    	{
	    		return rs.getBigDecimal(1);
	    	}
	    	return new BigDecimal("0.00");
	    }
	}
	  
	public static class BooleanResultTransformer implements ResultTransformer<Boolean> 
	{
	    public Boolean transform(ResultSet rs) throws SQLException 
	    {
            return rs.next() && rs.getBoolean(1);
        }
	}

	public static class DoubleResultTransformer implements ResultTransformer<Double> 
	{
	    public Double transform(ResultSet rs) throws SQLException 
	    {
	    	if(rs.next())
	    		return rs.getDouble(1);
	    	
	    	return 0.0;	        
	    }
	}

	public static class IntegerResultTransformer implements ResultTransformer<Integer> 
	{		
	    public Integer transform(ResultSet rs) throws SQLException 
	    {
	    	if(rs.next())
		        return rs.getInt(1);

	    	return 0;
	    	
	    }
	}

	public static class StringResultTransformer implements ResultTransformer<String> 
	{
	    public String transform(ResultSet rs) throws SQLException 
	    {
	    	if(rs.next())
		        return rs.getString(1);

	    	return null;	    	
	    }
	}

	public static class TimestampResultTransformer implements ResultTransformer<Timestamp> 
	{
	    public Timestamp transform(ResultSet rs) throws SQLException 
	    {
	    	if(rs.next())
	    		return rs.getTimestamp(1);
	    	
	    	return new Timestamp(0);	        
	    }
	}

	public CachedRowSet ExecuteReadOnlyQuery(String qry, Object... args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try
		{
			conn = getConnection();
			
			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, args);
			rs = stmt.executeQuery();
            //still broke
			//CachedRowSet crs = RowSetProvider.newFactory("com.sun.rowset.RowSetFactoryImpl",null).createCachedRowSet();
            CachedRowSet crs = new FixedCachedRowSetImpl();
			crs.populate(rs);
			return crs;
		}
		finally
		{
			tryClose(rs);
			tryClose(stmt);
			tryClose(conn);
		}
	}

//	public CachedRowSet ExecuteUpdatableQuery(String qry, Object... args) throws SQLException
//	{
//		Connection conn = null;
//		PreparedStatement stmt = null;
//		ResultSet rs = null;
//
//		try
//		{
//			conn = getConnection();
//
//			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, args);
//			rs = stmt.executeQuery();
//            //still broke
//            //CachedRowSet crs = RowSetProvider.newFactory("com.sun.rowset.RowSetFactoryImpl",null).createCachedRowSet();
//            System.err.println("Here");
//            CachedRowSet crs = new FixedCachedRowSetImpl();
//			crs.populate(rs);
//			return crs;
//		}
//		finally
//		{
//			tryClose(rs);
//			tryClose(stmt);
//			tryClose(conn);
//		}
//	}

//	public void ExecuteUpdateCachedRowSet(CachedRowSet crs) throws SQLException
//	{
//		Connection conn = null;
//		try
//		{
//			conn = getConnection();
//			conn.setAutoCommit(false);
//			crs.acceptChanges(conn);
//		}
//		finally
//		{
//			tryClose(conn);
//		}
//	}
//
	public boolean ExecuteUpdateBlob(String qry, String blobcolumn, InputStream stream, int length, Object...args) throws SQLException
	{
		Connection conn = getConnection();

		try
		{
			PreparedStatement stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, args);
	
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
			{
				if (stream == null)
					rs.updateNull("invoice");
				else
					rs.updateBinaryStream(blobcolumn, stream, length);
				
				rs.updateRow();
			}		
		}
		finally
		{
			tryClose(conn);
		}
		return true;
	}

	public Blob ExecuteScalarBlob(String qry, Object...args) throws SQLException
	{
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try
		{
			conn = getConnection();
			
			stmt = createPreparedStatement(conn, qry, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, args);
			rs = stmt.executeQuery();
			
			if(rs.next())
				return rs.getBlob(1);
			else
				return null;
		}
		finally
		{
			tryClose(rs);
			tryClose(stmt);
			tryClose(conn);
		}
	}
	
	public void tryClose(ResultSet rs) 
	{
		if (rs == null)
			return;
		try
		{
			rs.close();
		} 
		catch (SQLException e)
		{//just eat it
		}
	}
	
	public void tryClose(Statement st)
	{
		if (st == null)
			return;
		try
		{
			st.close();
		} 
		catch (SQLException e)
		{//just eat it
		}		
	}
	
	public void tryClose(PreparedStatement st)
	{
		if (st == null)
			return;
		try
		{
			st.close();
		} 
		catch (SQLException e)
		{//just eat it
		}		
	}
	
	public void tryClose(Connection conn)
	{
		if (conn == null)
			return;
		
		try
		{
			conn.close();
		}
		catch(SQLException e)
		{//just eat it	
		}		
	}
	
	public Connection getConnection() throws SQLException
	{
		return dataSource.getConnection();
	}
}

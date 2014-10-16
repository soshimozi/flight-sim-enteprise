/*
 * FS Economy
 * Copyright (C) 2005  Marty Bochane
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

package net.fseconomy.servlets;

import net.fseconomy.data.DALHelper;
import net.fseconomy.util.Formatters;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.*;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//  IP bans
//  Maintenance Cycle Status
//  Filter logging ignored items

// Examples to change filter settings in the DB:
//
// You can removed items with the indicated DELETE command
// You do not HAVE to insert the variable back into the table unless needed
// IF you set the maintenace cycle and are not in the allowed MaintenanceIPs list you will
// be locked out of the server as well as everyone else!
//
// NOTE: Any variable stored in the sysvariables table MUST BE DELETED before updating
//
// NOTE: All allowed MaintenanceIPs are placed in the same string seperated by a space ' '
// DELETE FROM sysvariables WHERE variablename='MaintenanceIPs'
// INSERT INTO sysvariables (variablename,svalue) VALUES('MaintenanceIPs', '0:0:0:0:0:0:0:1 75.55.248.213')
//
// DELETE FROM sysvariables WHERE variablename='FilterIgnoredItems'
// INSERT INTO sysvariables (variablename,svalue) VALUES('FilterIgnoreItems', '.css .js .jpg .png .gif')
//
// NOTE: To exit the maintenance cycle just delete the record, to set it use the INSERT below
// DELETE FROM sysvariables WHERE variablename='MaintenanceStatus'
// INSERT INTO sysvariables (variablename,value) VALUES('MaintenanceStatus', 1)
//
// NOTE: This deletes and adds in seperately each banned IP as called for
// DELETE FROM ipban WHERE ip='111.111.111.111'
// INSERT INTO ipban (ip) VALUES('111.111.111.111')
//
public class FullFilter implements Filter 
{
	//Flag to indicate if we are blocking access due to maintenance
	static boolean isClosedForMaintenance = false;
	
	//List of IPs to allow access during maintenance
	static String maintenanceAccessList = "";

	//List of IPs to block due to being banned for misuse
	static HashSet<String> blist = new HashSet<>();
	
    //Filtered items from log
    static String ignoreItems = "";
    
    static boolean loggerEnabled = false;

    //Logging variables
	FileHandler logfile;
	Logger logger;
	
	//Init arguments for Filter servlet
	static FilterConfig filterconfig;
	
	@Override
	public void init(FilterConfig arg0) throws ServletException 
	{
		filterconfig = arg0;
		setupLogging();
		//DALHelper.initDataSource();
		//updateFilter();
	}
	
	@Override
	public void destroy() 
	{
		logfile.close();
	}

	private static final String HEADER_X_FORWARDED_FOR = "X-FORWARDED-FOR";
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException 
	{		
		//Grab our objects with the http interface
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		 
		//Get the IP address of requesting machine.
        //String ipAddress = request.getRemoteAddr().toString();
		String remoteAddr = request.getRemoteAddr();
		String x;
		if ((x = request.getHeader(HEADER_X_FORWARDED_FOR)) != null)
		{
			remoteAddr = x;
			int idx = remoteAddr.indexOf(',');
			if (idx > -1)
			{
				remoteAddr = remoteAddr.substring(0, idx);
			}
		}        

        //First check are we closed for maintenance
        if(isClosedForMaintenance && !maintenanceAccessList.contains(remoteAddr))
        {
        	//print out closed for maintenance response
        	PrintWriter out = response.getWriter();
        	       
        	out.print("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" +
                        "<HTML>\n" +
                        "<HEAD><TITLE>FSEconomy</TITLE></HEAD>\n" +
                        "<BODY>\n" +
                        //"<h3 style=\"color:red\">FSEconomy has moved!</h1>" +
                        //"<p>Please go to <a href=\"http://www.fseconomy.net\">http://www.fseconomy.net</a> and check the forums for the latest information at <a href=\"http://www.fseforums.com\">http://www.fseforums.com</a></p>" +
                        "<h3>FSEconomy message: Currently Closed for Maintenance, please try again in a few minutes...</h3>\n" +
                        "</BODY></HTML>");
            
            response.setHeader("Content-Type", "text/html;charset=UTF-8");
            response.setDateHeader("expires", 0);
            out.flush();
            out.close();
            
            //stop processing and kick it back!
            return; 
        }
 
        //is the ip banned?
        if(blist.contains(remoteAddr))
        {
        	//print out your a bad bad boy response
        	PrintWriter out = response.getWriter();
        	       
        	out.print("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" +
                        "<HTML>\n" +
                        "<HEAD><TITLE>FSEconomy IP Block</TITLE></HEAD>\n" +
                        "<BODY>\n" +
                        "FSEconomy message: your IP (" + remoteAddr + ") is blocked\n" +
                        "\nPlease contact administrator@fseconomy.com if you feel this is a mistake. </BODY></HTML>");
            
            response.setHeader("Content-Type", "text/html;charset=UTF-8");
            response.setDateHeader("expires", 0);
            out.flush();
            out.close();
            
            //stop processing and kick it back!
            return; 
        }

        //logging section
        String ext = "";
        boolean skipInclude = false;
        
        //This lets us filter out page includes (top.jsp, menu.jsp)
        String s = (String)request.getAttribute("javax.servlet.include.request_uri");
        if(s != null && (s.contentEquals("/fseconomy/top.jsp") || s.contentEquals("/fseconomy/menu.jsp")))
        	skipInclude = true;
        
        //find the extention, if any, that are used to compare to ignore items
        String reqUri = request.getRequestURI();
        int extindex = reqUri.lastIndexOf('.');
        if(extindex != -1)
        	ext = reqUri.substring(extindex);
        
        //If no extention found, or no ignore items found and not skip top.jsp/menu.jsp, then log it
        if((extindex == -1 || !ignoreItems.contains(ext)) && !skipInclude)
        {
        	//get any attached parameters
	        String queryString = request.getQueryString();   // ex: id=789&loc=CEX4
	        if (queryString != null) 
	        {
	            reqUri += "?"+queryString;
	        }

	        //log it
	        if(loggerEnabled)
	        	logger.log(Level.INFO, remoteAddr + ", " + reqUri + "\tUser-Agent:" + request.getHeader("User-Agent"));
        }
        
        //All ok, continue processing the request
        chain.doFilter(req, res);
	}
	
	//Setup our file, logger, and formatter
	private void setupLogging()
	{
		try
		{
			LogManager lm = LogManager.getLogManager();
		      
			logfile = new FileHandler("IPPageAccess.log");
			logger = Logger.getLogger("IPPageAccessLogger");

			lm.reset(); //removes all handers including the stdout
			logger.setLevel(Level.INFO);
			lm.addLogger(logger);
			logfile.setFormatter(new MyFormatter());
			
			logger.addHandler(logfile);		      
		}
		catch(Exception e)
		{
			System.out.println("Exception thrown: " + e);
		    e.printStackTrace();		    
		}
	}

	//
	// Custom formatter for logger
	//
	class MyFormatter extends Formatter 
	{
		public String format(LogRecord record) 
		{
			StringBuilder builder = new StringBuilder(1000);
			builder.append(Formatters.dateDataFeed.format(new Date(record.getMillis()))).append(", ");
			builder.append(formatMessage(record));
			builder.append("\r\n");
		
			return builder.toString();
		}
	}	

	//
	// Update all our DB set parameters
	//
	public static void updateFilter(DALHelper dalHelper)
	{
		//data.Data data = Data.getInstance();
		
		ResultSet rs;
		String qry;
		try
		{
			//clear our current banned IP list
			blist.clear();
			
			//Get our banned IPs
			qry = "SELECT * FROM ipban";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			while (rs.next())
			{
				blist.add(rs.getString(1));
				//System.err.println("IPBan: " + rs.getString(1));
			}
			rs.close();

			//Get our maintenance status
			qry = "SELECT value FROM sysvariables WHERE variablename='MaintenanceStatus'";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			if(rs.next())
			{
				isClosedForMaintenance = rs.getDouble(1) != 0;
			} 
			rs.close();

			//Get our maintenance access IPs
			qry = "SELECT sValue FROM sysvariables WHERE variablename='MaintenanceIPs'";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			if(rs.next())
			{
				maintenanceAccessList = rs.getString(1);
			}
			rs.close();

			qry = "SELECT sValue FROM sysvariables WHERE variablename='FilterIgnoreItems'";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			if(rs.next())
			{
				ignoreItems = rs.getString(1);
			}			
			rs.close();

			qry = "SELECT sValue FROM sysvariables WHERE variablename='FilterLoggerEnabled'";
			rs = dalHelper.ExecuteReadOnlyQuery(qry);			
			if(rs.next())
			{
				loggerEnabled = Boolean.parseBoolean(rs.getString(1));
			}			
			rs.close();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}
}

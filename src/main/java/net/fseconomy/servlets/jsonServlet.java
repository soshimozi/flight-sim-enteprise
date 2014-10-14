package net.fseconomy.servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import net.fseconomy.data.DALHelper;
import net.fseconomy.util.Formatters;
import net.fseconomy.data.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * FS Economy
 * Copyright (C) 2014  FSEconomy
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

public class jsonServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	private Data data = null;
    private DALHelper dalHelper = null;

	public void init()
	{
		//Get Data Context, create it if null
		data = (Data) getServletContext().getAttribute("data");
		if (data == null)
			getServletContext().setAttribute("data", data = Data.getInstance());

        Logger logger = LoggerFactory.getLogger(jsonServlet.class);
        dalHelper = new DALHelper(logger);
	}
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	String uri = request.getRequestURI();
    	if(uri.endsWith("pilotstatus"))
    	{
    		getPilotStatus(request, response);
    	}
    	else
    	{
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"uri not found\"}");
    	}
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	String uri = request.getRequestURI();
    	if(uri.endsWith("fuelquote"))
    	{
    		postFuelQuote(request, response);
    	}
    	else if(uri.endsWith("goodsquote/buy"))
    	{
    		postBuyGoodsQuote(request, response);
    	}
    	else if(uri.endsWith("goodsquote/sell"))
    	{
    		postSellGoodsQuote(request, response);
    	}
    	else
    	{
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"uri not found\"}");
    	}
    }
    
    public void getPilotStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	//This is called A LOT.
    	//The PilotStat class uses single character labels to reduce traffic.
        List<Data.PilotStatus> list = data.getPilotStatus();
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Cache-Control", "no-cache");
        response.getWriter().write(new Gson().toJson(list));
    }
    
    public void postFuelQuote(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	String icao = request.getParameter("icao");    	
    	int type = Integer.parseInt(request.getParameter("fueltype"));
    	int amount = Integer.parseInt(request.getParameter("amount"));
    	
    	String price = Formatters.twoDecimals.format(data.quoteFuel(icao, type, amount));
    	
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Cache-Control", "no-cache");
        response.getWriter().write(new Gson().toJson(price));
    }    

    public void postBuyGoodsQuote(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	boolean BUY = true;
    	String icao = request.getParameter("icao");
    	String sgoodstype = request.getParameter("goodstype");
    	String samount = request.getParameter("amount");
    	String ssrc = request.getParameter("src");
    	
    	int type = Integer.parseInt(sgoodstype);
    	int amount = Integer.parseInt(samount);
    	int src = Integer.parseInt(ssrc);
    	
    	String price = Formatters.twoDecimals.format(data.quoteGoods(icao, type, amount, src, BUY));
    	
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Cache-Control", "no-cache");
        response.getWriter().write(new Gson().toJson(price));        
    }    

    public void postSellGoodsQuote(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
    	boolean SELL = false; // Buying is true
    	String icao = request.getParameter("icao");
    	String sgoodstype = request.getParameter("goodstype");
    	String samount = request.getParameter("amount");
    	String ssrc = request.getParameter("src");
    	
    	int type = Integer.parseInt(sgoodstype);
    	int amount = Integer.parseInt(samount);
    	int src = Integer.parseInt(ssrc);
    	
    	String price = Formatters.twoDecimals.format(data.quoteGoods(icao, type, amount, src, SELL));
    	
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Cache-Control", "no-cache");
        response.getWriter().write(new Gson().toJson(price));        
    }    
}

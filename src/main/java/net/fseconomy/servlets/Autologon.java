package net.fseconomy.servlets;

import java.io.IOException;
import java.util.SimpleTimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.fseconomy.data.*;
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
public class Autologon extends HttpServlet {
	private static final long serialVersionUID = 1L;

	Data data;
	public void init()
	{
		data = (Data) getServletContext().getAttribute("data");
		if (data == null)
			getServletContext().setAttribute("data", data = Data.getInstance());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String user = request.getParameter("user");
		String password = request.getParameter("password");
		String sOffset = request.getParameter("offset");
		UserBean userBean;

		if (user == null || password == null || sOffset == null || ((userBean = data.userExists(user, password, true)) == null))
		{
			response.sendRedirect("/welcome.jsp");
			return;			
		}
		
		int offset = 0;
		if (sOffset != null)
			offset = Integer.parseInt(sOffset);
			
		userBean.setTimeZone(new SimpleTimeZone(1000 * 60 * -offset, "Local"));
		userBean.setLoggedIn(true);
		data.reloadMemberships(userBean);
		HttpSession s = request.getSession();
		s.setAttribute("user", userBean);		
		response.sendRedirect("/index.jsp");
	}
	

}

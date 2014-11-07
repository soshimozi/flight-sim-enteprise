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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fseconomy.data.*;

public class Image extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		Data data;
		data = (Data) getServletContext().getAttribute("data");
		String id = req.getParameter("id");
		resp.setContentType("image/jpeg");
		if (id != null)
		{
			InputStream image = Fbos.getInvoiceBackground(Integer.parseInt(id));
			if (image != null)
			{
				byte[] buffer = new byte[10240];
				ServletOutputStream os = resp.getOutputStream();
				int bytes;
				while ((bytes = image.read(buffer)) > 0)
					os.write(buffer, 0, bytes);
			}
		}
	}

}

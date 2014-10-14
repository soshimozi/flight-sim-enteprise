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
			InputStream image = data.getInvoiceBackground(Integer.parseInt(id));
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

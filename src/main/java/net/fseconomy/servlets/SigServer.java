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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fseconomy.data.*;
import net.fseconomy.util.Formatters;

public class SigServer extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	private String pathToTemplate = null;
    private String pathToSig = null;
	static final String fileExt = ".jpg";
	static final String fileFormat = "JPG";
	static final String defaultTemplateName = "template%s";
	static final int cacheAge = 900; //15 minutes

	String sigImageFormat = "%s/%s" + fileExt;
	String templateImageFormat = "%s/template%s" + fileExt;
	String defaultTemplate = "";
	
	public void init()
	{
		String path = getServletContext().getRealPath("/");
        pathToSig = path.substring(0,path.indexOf("standalone")) + "fse-static/signatures";
        pathToTemplate = path.substring(0,path.indexOf("standalone")) + "fse-static/sig-templates";
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		defaultTemplate = String.format(templateImageFormat, pathToTemplate, Data.currMonth, defaultTemplateName);

		// Get requested image by path info.
		// For consistency of image names lower case it here!
        String requestedImage = request.getPathInfo().toLowerCase();

        // Check if file name is actually supplied to the request URI.
        if (requestedImage.equals(""))
        {
            // Throw an exception, or send 404, or show default/warning image, or just ignore it.
            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
            return;
        }

        // Decode the file name (might contain spaces and on) and prepare file object.
        String decodedImage = URLDecoder.decode(requestedImage, "UTF-8").toLowerCase();
    	if(!isValidImageRequest(decodedImage))
    	{
            // Throw an exception, or send 404, or show default/warning image, or just ignore it.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST); // 404.
            return;
    	}

    	//Pull out the user name
        String user = getUserName(decodedImage);
        
        String filename = pathToSig + decodedImage;
        File image = new File(filename);
        
        // Check if file actually exists in file system.
        if (image.exists()) 
        {
	        // Get content type by filename.
	        String contentType = getServletContext().getMimeType(image.getName());
	
	        // Check if file is actually an image (avoid download of other files by hackers!).
	        // For all content types, see: http://www.w3schools.com/media/media_mimeref.asp
	        if (contentType == null || !contentType.startsWith("image")) 
	        {
	            // Throw an exception, or send 404, or show default/warning image, or just ignore it.
	            response.sendError(HttpServletResponse.SC_NOT_FOUND); // 404.
	            return;
	        }

	        // Has file expired or changed?
	        long lastModified = image.lastModified();
	        boolean hasExpired = System.currentTimeMillis() > (lastModified + cacheAge*1000);
	        boolean hasChanged = hasSignatureChanged(user);
	        long expires = System.currentTimeMillis() + cacheAge*1000;

	        // If-Modified-Since header should be greater than LastModified. If so, then return 304.
	        // This header is ignored if any If-None-Match header is specified.
	        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
	        if (ifModifiedSince != -1 && !hasExpired && !hasChanged) 
	        {
	        	Data.cacheCount++;
	            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
	            response.setDateHeader("Expires", expires); // Postpone cache with 1 week.
	            return;
	        }
	        
    		if( Data.statsmap != null && Data.statsmap.containsKey(user))
    		{
    			Data.createCount++;
    			createSignature(user);
    		}
    		else
    		{
    			Data.defaultCount++;
    			filename = defaultTemplate;
    		}
        }
        else
        {
    		if( Data.statsmap != null && Data.statsmap.containsKey(user))
    		{
    			Data.createCount++;
    			createSignature(user);
    		}
    		else
    		{
    			Data.defaultCount++;
    			filename = defaultTemplate;
    		}
        }
        
		//return image
        try
        {
        	sendImage(response, filename);
        }
        catch(IOException e)
        {
        	e.printStackTrace();
        }
	}

	private boolean isValidImageRequest(String imageUri)
	{
		//check the following formatting is correct
		if( imageUri.length() < 7
			|| !imageUri.endsWith(fileExt)
			|| imageUri.substring(1).contains("/")
			|| imageUri.substring(1).contains("\\"))
			return false;

		return true;
	}
	
	private void sendImage(HttpServletResponse response, String filename) throws IOException
	{
		File image = new File(filename);
		
		String fileName = image.getName();
        long length = image.length();
        long lastModified = image.lastModified();
        String eTag = fileName + "_" + length + "_" + lastModified;
        long expires = System.currentTimeMillis() + cacheAge*1000;

        Data.bytesServed += length;
        Data.totalImagesSent++;
        
		response.setContentType("image/jpeg");
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", lastModified);
        response.setDateHeader("Expires", expires);

		BufferedImage bi = ImageIO.read(image);
		OutputStream out = response.getOutputStream();
		ImageIO.write(bi, fileFormat, out);
		
		out.close();
	}
	
	//Get user name stripped out of Uri and removing file .ext
	String getUserName(String path)
	{
		String tmp = path.substring(1);

        return tmp.substring(0, tmp.length()-4);
	}	
	
	//Determine if the statistics have updated
	private boolean hasSignatureChanged(String user)
	{
		if(Data.prevstatsmap == null)
			return false;
		
		Data.statistics curr = Data.statsmap.get(user);
		Data.statistics prev = Data.prevstatsmap.get(user);

        return !(curr.flights == prev.flights && curr.totalFlightTime == prev.totalFlightTime);

    }
	
	//Build out the new signature based upon the user name
	private void createSignature(String user)
	{
		BufferedImage image = null;		

		//get the current template
		try 
		{
            image = ImageIO.read(new File(defaultTemplate));
        } 
		catch (IOException e) 
        {
            e.printStackTrace();
        }

		//Compute the text to embed
		Data.statistics s = Data.statsmap.get(user);		
		String stats = "1st Flight: " + Formatters.datemmyyyy.format(s.firstFlight) + "       Flights: " + s.flights + "       Hours: " + s.totalFlightTime/3600;
		
		//do it
		image = createImage(image, user, stats);
        
        //save image into signatures
		try 
		{
			String filename = String.format(sigImageFormat, pathToSig, user);
            ImageIO.write(image, fileFormat, new FileOutputStream(filename));
        } 
		catch (IOException e) 
        {
            e.printStackTrace();
        }
				
	}
	
	private void setRenderingHints(Graphics2D g) 
	{        
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                           RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private BufferedImage createImage(BufferedImage old, String name, String stats)  
    {
        int width = old.getWidth();
        int height = old.getHeight();
                
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = image.createGraphics();
        setRenderingHints(g1);

        //pre-fill image
        g1.setPaint(new Color(0, 0, 0, 0 ));
        g1.fillRect(0, 0, width, height);

        //Fonts
        Font nameFont = new Font("Arial", Font.BOLD, 40);
        Font statsFont = new Font("Arial", Font.BOLD, 14);

        //layouts
        TextLayout textLayoutName = new TextLayout(name, nameFont, g1.getFontRenderContext());
        TextLayout textLayoutStats = new TextLayout(stats, statsFont, g1.getFontRenderContext());
        

        FontMetrics fm = g1.getFontMetrics(nameFont);
        int nameX = width - fm.stringWidth(name) - 8;
        int nameY = fm.getHeight();        

        FontMetrics fm2 = g1.getFontMetrics(statsFont);
        int statsX = width - fm2.stringWidth(stats) - 8;
        int statsY = height - fm2.getHeight() + 10;        

        //set paint color to black
        g1.setPaint(Color.BLACK);

        //Shadow
        textLayoutName.draw(g1, nameX + 2, nameY + 2);
        textLayoutStats.draw(g1, statsX + 2, statsY + 2);

        g1.dispose();

        //setup blur for shadow
        float[] kernel = {
          1f / 9f, 1f / 9f, 1f / 9f, 
          1f / 9f, 1f / 9f, 1f / 9f, 
          1f / 9f, 1f / 9f, 1f / 9f 
        };

        ConvolveOp op =  new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage image2 = op.filter(image, null);

        //Text
        Graphics2D g2 = image2.createGraphics();
        setRenderingHints(g2);
        
        //Text is Gold
        g2.setPaint(Color.WHITE);
        
        textLayoutName.draw(g2, nameX, nameY);
        textLayoutStats.draw(g2, statsX, statsY);
        
        g2.dispose();

        BufferedImage imgFinal = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g3 = imgFinal.createGraphics();
        
        g3.drawImage(old, 0, 0, null);
        g3.drawImage(image2, 0, 0, null);

        g3.dispose();
        
        return imgFinal;
    }
}

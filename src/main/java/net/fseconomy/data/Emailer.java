/*
 * FS Economy
 * Copyright (C) 2005, 2006, 2007  Marty Bochane
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

import net.fseconomy.util.GlobalLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * purpose: general purpose Emailer class to send emails from the fseconomy platform
 * - will use GMail as the SMTP service provider
 * - this class implements the Singleton pattern
 */
public class Emailer 
{
	private static Emailer instance;
	
	private Properties mailProperties;
	private Session session;

    public static final String ADDRESS_TO = "TO";
	public static final String ADDRESS_BCC = "BCC";

    public final static Logger logger = LoggerFactory.getLogger(Data.class);

	//constructor protected so cannot be used - use getInstance() only
	protected Emailer() {}
	
	public static Emailer getInstance() 
	{
		if (instance == null) 
		{
			instance = new Emailer();
			instance.init();
		}
		
		return instance;
	}
	
	protected void init() 
	{	
		mailProperties = new Properties();
		
		//now get the values from the sysvariables table
		try 
		{
			String qry = "select variableName, sValue from sysvariables where variableName LIKE \'mail.%\'";			
			ResultSet rs = DALHelper.getInstance().ExecuteReadOnlyQuery(qry);
			while (rs.next())
			{				
				mailProperties.put(rs.getString(1), rs.getString(2));
			}			
			
			session = Session.getInstance(mailProperties,
						new javax.mail.Authenticator() 
						{
							protected PasswordAuthentication getPasswordAuthentication()
							{ return new PasswordAuthentication(mailProperties.getProperty("mail.smtp.user"), 
									mailProperties.getProperty("mail.smtp.password"));	}
						});						
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}
	
	/**
	 * 
	 * @param fromAddress - address to specify in the FROM email field
	 * @param fromName - name to specify in the NAME email field
	 * @param subject -subject to specify in the SUBJECT email field
	 * @param body - email message body contents
	 * @param toList - collection of addresses to email
	 * @param addressType - type of address to use.  Options are ADDRESS_TO or ADDRESS_BCC - use the static variables in this class
	 * @throws DataError
	 */
	public synchronized void sendEmail(String fromAddress, String fromName, String subject, String body, List<String> toList, String addressType) throws DataError
	{
		if(toList.size() < 1) 
			return;
		
		try 
		{
            MimeMessage message = new MimeMessage(session);
			
			message.setFrom(new InternetAddress(fromAddress, fromName));
			InternetAddress reply[] = new InternetAddress[1];
			reply[0] = new InternetAddress(fromAddress);			
			message.setReplyTo(reply);
			message.setSubject(subject);
			message.setText(body);			
			
			for(String emailAddress : toList)
			{
				if(!isValidEmailAddress(emailAddress))
				{
					GlobalLogger.logApplicationLog("SendMail Error: invalid email address format for[" + emailAddress + "]", Emailer.class);
					throw new DataError("SendMail Error: invalid email address format for[" + emailAddress + "]");
				}
				
				InternetAddress[] toAddress = InternetAddress.parse(emailAddress, false);
				
				if (addressType.equals(ADDRESS_BCC))
					message.addRecipient(Message.RecipientType.BCC, toAddress[0]);
				else
					message.addRecipient(Message.RecipientType.TO, toAddress[0]);
            }
			
			message.saveChanges();
			Transport.send(message);
        }
		catch (UnsupportedEncodingException | MessagingException e)
		{			
			e.printStackTrace();
			throw new DataError("Error while transmitting: " + e.getMessage());			
		}
    }

	public static boolean isValidEmailAddress(String aEmailAddress)
	{
	    if (aEmailAddress == null)
            return false;
	    
	    boolean result = true;
	    try 
	    {
	    	new InternetAddress(aEmailAddress);
	    }
	    catch (AddressException ex)
	    {
	    	result = false;
	    }
	    
	    return result;
    }
}
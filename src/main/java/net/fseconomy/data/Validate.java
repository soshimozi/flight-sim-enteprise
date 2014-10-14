package net.fseconomy.data;

import java.text.SimpleDateFormat;

/**
 * This package is used to hold reusable static validators.
 */
public final class Validate
{
	private static final long serialVersionUID = 1L;
	
	// Suppress default constructor for noninstantiability 
    private Validate() 
    { 
        throw new AssertionError(); 
    } 
	
	//Validates that an IP address has 4 octets and valid ranges
	//http://www.tek-tips.com/viewthread.cfm?qid=1379040
	public final static boolean IPAddress( String  ipAddress )
	{
	    String[] parts = ipAddress.split( "\\." );

	    if ( parts.length != 4 )
	    {
	        return false;
	    }

	    for ( String s : parts )
	    {
	        int i = Integer.parseInt( s );

	        if ( (i < 0) || (i > 255) )
	        {
	            return false;
	        }
	    }

	    return true;
	}
	
	public final static boolean isValidDate(String inDate) 
	{
		String DATE_FORMAT = "yyyy-MM-dd";	
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		
		if (inDate.trim().length() != sdf.toPattern().length())
		      return false;
		
		sdf.setLenient(false);
		
		try 
		{			
			sdf.parse(inDate);			
		}
		catch (Exception e) 
		{			
			return false;
		}
		
		return true;
	}
}
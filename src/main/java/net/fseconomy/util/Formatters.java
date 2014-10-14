package net.fseconomy.util;
import java.sql.Timestamp;
import java.text.*;
import java.util.Calendar;
import java.util.TimeZone;

/*
 * FS Economy
 * Copyright (C) 2014 FSEconomy
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
 * 
 */
public class Formatters
{
	
	public static NumberFormat nodecimals = new DecimalFormat("#################");
	public static NumberFormat oneDecimal = new  DecimalFormat("###0.0");
	public static NumberFormat twoDecimals = new  DecimalFormat("###0.00");
	public static NumberFormat threeDecimals = new DecimalFormat("###0.000");
	
	public static DecimalFormat oneDigitOneDecimal = new DecimalFormat("0.0;'-'0.0");//format to 1 decimal 

	public static DecimalFormat oneDigit = new DecimalFormat("0");
	public static DecimalFormat twoDigits = new DecimalFormat("00");
	public static DecimalFormat threeDigits = new DecimalFormat("000");

	public static NumberFormat currency = NumberFormat.getCurrencyInstance();
	
	public static SimpleDateFormat dateDataFeed;
	public static SimpleDateFormat dateyyyymmddhhmmss;
	public static SimpleDateFormat dateyyyymmddhhmmzzz;
	public static SimpleDateFormat datemmddyy;
	public static SimpleDateFormat datemmyyyy;
	
	//public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	
	static
	{
		TimeZone gmt = TimeZone.getTimeZone("GMT");
		
		dateDataFeed = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		dateDataFeed.setTimeZone(gmt);
		dateyyyymmddhhmmss  = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		dateyyyymmddhhmmss.setTimeZone(gmt);
		dateyyyymmddhhmmzzz  = new SimpleDateFormat("yyyy/MM/dd HH:mm zzz");
		dateyyyymmddhhmmzzz.setTimeZone(gmt);
		datemmddyy = new SimpleDateFormat("MM/dd/yy");
		datemmddyy.setTimeZone(gmt);
		datemmyyyy = new SimpleDateFormat("MMMM yyyy");
		datemmyyyy.setTimeZone(gmt);
	}
	
	public static DateFormat getUserTimeFormat(net.fseconomy.data.UserBean user)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		if(user.getUserTimezone() == 0)
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		else
			sdf.setTimeZone(user.getTimeZone());

		return sdf;
	}
	
	public static String getHourMin(int seconds)
	{
		int minutes = seconds/60;
		return Formatters.twoDigits.format(minutes/60) + ":" + Formatters.twoDigits.format(minutes%60);
	}
	
	public static String getElapsedYearMonthDay(Timestamp ts)
	{
		Calendar start = Calendar.getInstance();
		start.setTimeInMillis(ts.getTime());        
        Calendar end = Calendar.getInstance();

        Integer[] elapsed = new Integer[3];
        Calendar clone = (Calendar) start.clone(); // Otherwise changes are been reflected.
        elapsed[0] = elapsed(clone, end, Calendar.YEAR);
        clone.add(Calendar.YEAR, elapsed[0]);
        elapsed[1] = elapsed(clone, end, Calendar.MONTH);
        clone.add(Calendar.MONTH, elapsed[1]);
        elapsed[2] = elapsed(clone, end, Calendar.DATE);
        clone.add(Calendar.DATE, elapsed[2]);

        String result = "";
        if(elapsed[0] != 0)
        	result = String.format("%d years, %d months", elapsed[0], elapsed[1]);
        else if(elapsed[1] != 0)
        	result = String.format("%d months, %d days", elapsed[1], elapsed[2]);
        else if(elapsed[2] != 0)
        	result = String.format("%d days", elapsed[2]);
		
        return result;
	}
	
	private static int elapsed(Calendar before, Calendar after, int field) 
	{
        Calendar clone = (Calendar) before.clone(); // Otherwise changes are been reflected.
        int elapsed = -1;
        while (!clone.after(after)) 
        {
            clone.add(field, 1);
            elapsed++;
        }
        return elapsed;
    }

}

package net.fseconomy.util;
import java.sql.Timestamp;

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
 * 
 */
public class Converters
{
    public static String clearHtml(String input)
    {
        if (input == null)
            return null;

        return input.replaceAll("<[^>]*>", "").trim();
    }

	public static String escapeJavaScript(String input)
	{
		if (input == null)
			return null;
		
		input = input.replaceAll("\r", "\\\\r");
		input = input.replaceAll("\n", "\\\\n");
		input = input.replaceAll("\t", "\\\\t");
		input = input.replaceAll("'", "\\\\'");
		return input.replaceAll("\"", "\\\\\"");
	}
	
	public static String escapeSQL(String input)
	{
		if (input == null)
			return null;
		
		return input.replaceAll("'", "\\\\'");
	}
	
	public static class xmlBuffer
	{
		StringBuilder content;
		public xmlBuffer()
		{
			content = new StringBuilder();
		}
		public void appendOpenTag(String name)
		{
			content.append("<");
			content.append(name);
			content.append(">");
		}
		public void appendCloseTag(String name)
		{
			content.append("</");
			content.append(name);
			content.append(">\n");
		}
		public void append(String name, String value)
		{
			appendOpenTag(name);
			content.append(value);
			appendCloseTag(name);
		}
		public void append(String name, double value)
		{
			appendOpenTag(name);
			content.append(value);
			appendCloseTag(name);
		}
		public void append(String name, int value)
		{
			appendOpenTag(name);
			content.append(Formatters.nodecimals.format(value));
			appendCloseTag(name);
		}		
		public void append(String name, long value)
		{
			appendOpenTag(name);
			content.append(Formatters.nodecimals.format(value));
			appendCloseTag(name);
		}	
		public void appendMoney(String name, double value)
		{
			append(name, Formatters.twoDecimals.format(value));
		}
		public void append(String name, Timestamp value)
		{
			append(name, Formatters.nodecimals.format(value.getTime()));
		}
		public void append(String value)
		{
			content.append(value);
		}
		public String toString()
		{
			return content.toString();
		}
	}
	
	public static class csvBuffer
	{
		StringBuffer header;
		StringBuffer content;
		public csvBuffer()
		{
			header = new StringBuffer();
			content = new StringBuffer();
		}

		public boolean isHeaderEmpty()
		{
            return header.length() == 0;
		}

		public boolean isContentEmpty()
		{
            return content.length() == 0;
		}

		public void newrow()
		{
			content.append("\r\n");
		}

		public void appendHeaderItem(String name)
		{
			header.append(name).append(",");
		}

		public void append(String value)
		{
			String s = value;

			if(s.contains(","))
			{
				s = value.replaceAll("\"", "\"\"");
				s = "\"" + s + "\"";
			}
			else if(s.contains("\""))
			{
				s = value.replaceAll("\"", "\"\"");
				s = "\"" + s + "\"";
			}
			content.append(s).append(",");
		}
		public void append(boolean value)
		{
			content.append(value ? "true" + "," : "false" + ",");
		}
		public void append(double value)
		{
			content.append(value).append(",");
		}
		public void append(int value)
		{
			content.append(Formatters.nodecimals.format(value)).append(",");
		}		
		public void append(long value)
		{
			content.append(Formatters.nodecimals.format(value)).append(",");
		}	
		public void appendMoney(double value)
		{
			content.append(Formatters.twoDecimals.format(value)).append(",");
		}
		public void append(Timestamp value)
		{
			append(Formatters.nodecimals.format(value.getTime()) + ",");
		}
		public String toString()
		{
			return header.toString() + "\r\n" + content.toString();
		}
	}
	
	public static class XMLHelper 
	{		 
		/** 
		 * Returns the string where all non-ascii and <, &, > are encoded as numeric entities. I.e. "&lt;A &amp; B &gt;" 
		 * .... (insert result here). The result is safe to include anywhere in a text field in an XML-string. If there was 
		 * no characters to protect, the original string is returned. 
		 *  
		 * @param originalUnprotectedString 
		 *            original string which may contain characters either reserved in XML or with different representation 
		 *            in different encodings (like 8859-1 and UFT-8) 
		 */
		public static String protectSpecialCharacters(String originalUnprotectedString) 
		{ 
		    if (originalUnprotectedString == null) 
		        return null;

		    boolean anyCharactersProtected = false;
		 
		    StringBuilder stringBuffer = new StringBuilder();
		    for (int i = 0; i < originalUnprotectedString.length(); i++) 
		    { 
		        char ch = originalUnprotectedString.charAt(i); 
		 
		        boolean controlCharacter = ch < 32; 
		        boolean unicodeButNotAscii = ch > 126; 
		        boolean characterWithSpecialMeaningInXML = ch == '<' || ch == '&' || ch == '>'; 
		 
		        if (characterWithSpecialMeaningInXML || unicodeButNotAscii || controlCharacter) 
		        { 
		            stringBuffer.append("&#").append((int) ch).append(";");
		            anyCharactersProtected = true; 
		        } 
		        else 
		        { 
		            stringBuffer.append(ch); 
		        } 
		    }

		    if (!anyCharactersProtected)
		        return originalUnprotectedString;

		    return stringBuffer.toString(); 
		} 		 
	} 
}
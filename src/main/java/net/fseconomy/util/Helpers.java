package net.fseconomy.util;

import javax.servlet.http.HttpServletRequest;

public class Helpers
{
    public static String getSessionMessage(HttpServletRequest request)
    {
        String message = (String)request.getSession().getAttribute("message");
        if(message != null)
            request.getSession().setAttribute("message", null);
        else
            return null;

        return message;
    }

    public static String getSessionReturnUrl(HttpServletRequest request)
    {
        String url = (String)request.getSession().getAttribute("returnUrl");
        if(url != null)
            request.getSession().setAttribute("back", null);
        else
            return "javascript:window.history.back();";

        return url;
    }

    public static boolean isNullOrBlank(String s)
    {
        return (s==null || s.trim().equals(""));
    }

    //ok performance, if you need this A LOT find something else.
    public static boolean isInteger( String input ) {
        try
        {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e )
        {
            return false;
        }
    }

    public static String truncate(String str, int maxLen)
    {
        if(str == null)
            return str;

        return (str.length() < maxLen) ? str : str.substring(0, maxLen);
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }
}

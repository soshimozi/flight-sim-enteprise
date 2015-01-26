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
        try {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e ) {
            return false;
        }
    }
}

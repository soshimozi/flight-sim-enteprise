package net.fseconomy.util;

import javax.servlet.http.HttpServletRequest;

public class Helpers
{
    public static String getSessionMessage(HttpServletRequest request)
    {
        String message = (String)request.getSession().getAttribute("message");
        if(message != null)
            request.getSession().setAttribute("message", null);

        return message;
    }

    public static boolean isNullOrBlank(String s)
    {
        return (s==null || s.trim().equals(""));
    }

}

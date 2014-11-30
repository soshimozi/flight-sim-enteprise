<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*"
%>
<%@ page import="static net.fseconomy.data.SimClientRequests.*" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    response.setContentType("text/html");

    String query = request.getParameter("startsWith");
    if(query == null)
        query = "air";

    try
    {
        List<String> ips = lookupClientRequestIps(query);
        if(ips.size() > 0)
        {
            String output = "{\"ips\": [";
            boolean firstLoop = true;
            for(String ip : ips)
            {
                String[] array = ip.split("\\|");
                if(!firstLoop)
                    output += ",";
                else
                    firstLoop = false;

                output += "{\"label\":\"" + array[0] + " [" + array[2] + "]\", \"value\": \"" + array[1] + "\"}\n";
            }
            output += "]}";

%>
<%= output %>
<%
    }
}
catch(Exception e)
{
%>
"Exception is: <%= e %>"
<%
    }
%>
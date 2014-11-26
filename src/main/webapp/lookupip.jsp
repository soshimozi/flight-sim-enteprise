<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, java.text.*, net.fseconomy.beans.*, net.fseconomy.data.*"
        %>
<%@ page import="static net.fseconomy.data.SimClientRequests.getClientRequestIps" %>
<%@ page import="net.fseconomy.dto.ClientRequest" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    response.setContentType("text/html");

    String query = request.getParameter("startsWith");
    String accountType = request.getParameter("accountType");
    String displayHidden = request.getParameter("displayHidden");

    if(query == null)
    {
        query = "air";
    }

    try
    {
        int accttype = Accounts.ACCT_TYPE_ALL;
        if( accountType != null)
            accttype = Integer.parseInt(accountType);
        boolean displayhidden = false;
        if( displayHidden != null)
            displayhidden = Boolean.parseBoolean(displayHidden);

        List<String> ips = getClientRequestIps(query);
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
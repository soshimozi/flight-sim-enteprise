<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.beans.*, java.util.*"
%>
<%@ page import="static net.fseconomy.data.SimClientRequests.*" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }
%>

<%
    response.setContentType("text/html");

    String query = request.getParameter("startsWith");
    if(query == null)
        query = "127";

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

                output += "{\"label\":\"" + array[0] + " [" + array[2] + "]\", \"value\": \"" + array[0] + "\"}\n";
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
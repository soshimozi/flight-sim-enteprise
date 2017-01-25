<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, net.fseconomy.beans.*, net.fseconomy.data.*"
%>

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

        List<UserBean> accounts = Accounts.getAccountNames(query, accttype, 10, displayhidden);
        if(accounts.size() > 0)
        {
            String output = "{\"accounts\": [";
            boolean firstLoop = true;
            for(UserBean account : accounts)
            {
                if(!firstLoop)
                    output += ",";
                else
                    firstLoop = false;

                output += "{\"label\":\"" + account.getName().replaceAll("\"","\\\\\"") + "\", \"value\":" + account.getId() + "}\n";
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
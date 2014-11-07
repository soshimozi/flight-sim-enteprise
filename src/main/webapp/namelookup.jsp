<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, java.text.*, net.fseconomy.beans.*, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    response.setContentType("text/html");

    String query = (String)request.getParameter("startsWith");
    String accountType = (String)request.getParameter("accountType");
    String displayHidden = (String)request.getParameter("displayHidden");

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
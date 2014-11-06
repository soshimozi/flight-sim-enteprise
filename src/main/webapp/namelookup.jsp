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
        //System.out.println("default search string used!");
    }

    try
    {
        int accttype = Data.ACCT_TYPE_ALL;
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
                //String s = "<li onclick='fill("+ accounts[i].getName() +");'>" + accounts[i].getName() + "</li>";
                //output += "<dt onclick=\"fill('" + accounts[i].getName() + "');\">" + accounts[i].getName() + "</dt>";
                //output += "<dt>" + accounts[i].getName() + "</dt>\n";
                if(!firstLoop)
                    output += ",";
                else
                    firstLoop = false;

                //output += "{\"value\":\"" + accounts[i].getName().replaceAll("\"","\\\\\"") + "\", \"label\":\"" + accounts[i].getName().replaceAll("\"","\\\\\"") + "\", \"id\":" + accounts[i].getId() + "}\n";
                output += "{\"label\":\"" + account.getName().replaceAll("\"","\\\\\"") + "\", \"value\":" + account.getId() + "}\n";
            }
            output += "]}";

            //System.out.println(output);
            out.print(output);
        }
    }
    catch(Exception e)
    {
        out.println("Exception is ;"+e);
    }
%>
<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, java.text.*, net.fseconomy.data.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<% response.setContentType("text/html");%>

<%
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
	
	UserBean[] accounts = data.getAccountNames(query, accttype, 10, displayhidden);
	if(accounts != null && accounts.length > 0)
	{
		String output = "{\"accounts\": [";
		
		for(int i = 0; i < accounts.length; i++)
		{
			//String s = "<li onclick='fill("+ accounts[i].getName() +");'>" + accounts[i].getName() + "</li>";
			//output += "<dt onclick=\"fill('" + accounts[i].getName() + "');\">" + accounts[i].getName() + "</dt>";
			//output += "<dt>" + accounts[i].getName() + "</dt>\n";
			if(i != 0)
				output += ",";
			//output += "{\"value\":\"" + accounts[i].getName().replaceAll("\"","\\\\\"") + "\", \"label\":\"" + accounts[i].getName().replaceAll("\"","\\\\\"") + "\", \"id\":" + accounts[i].getId() + "}\n";
			output += "{\"label\":\"" + accounts[i].getName().replaceAll("\"","\\\\\"") + "\", \"value\":" + accounts[i].getId() + "}\n";
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
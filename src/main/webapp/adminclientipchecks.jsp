<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.text.*, net.fseconomy.data.*, java.util.*"
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<% 
	Data data = (Data)application.getAttribute("data");


	if (!Data.needLevel(user, UserBean.LEV_CSR) && !Data.needLevel(user, UserBean.LEV_MODERATOR)) 
	{
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return; 
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel="stylesheet" type="text/css" href="theme/redmond/jquery-ui.css" />
    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

	<script src="scripts/jquery.min.js"></script>
	<script src="scripts/jquery-ui.min.js"></script>
	<script src="scripts/AutoComplete.js"></script>

	<script type="text/javascript">
	
	$(function() {
		initAutoComplete("#username", "#user", <%= Data.ACCT_TYPE_PERSON %>);
	});
	
	</script>

</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<div id="wrapper">
<jsp:include flush="true" page="menu.jsp">
	<jsp:param name="open" value="home"/>
</jsp:include>

<div class="content">
<%
	String message = (String) request.getAttribute("message");
	if (message != null) 
	{
%>	
		<div class="message"><%= message %></div>
<%
	}
%>
<%
	String searchby = request.getParameter("searchby");
	String searchfor = request.getParameter("searchfor");
	if (searchby == null) 
	{ 
%>
	<h2>Client IP Checker</h2>
	<h4>Enter User Account</h4>
	<div class="form" style="width: 400px">
		<form method="post">
			<div>
				Account Name : 
			    <input type="text" id="username" name="username"/>
			    <input type="hidden" id="user" name="user"/>
				<input type="hidden" name="searchby" value="account"/>
				<br/>
				<input type="submit" class="button" value="GO" />
			</div>
		</form>
	</div>
	<h4>Select Ip</h4>
	<div class="form" style="width: 400px">
		<form method="post">
			<div>
				IP : 
			    <input type="text" id="ip" name="ip"/>
				<br/>
				<input type="submit" class="button" value="GO" />
				<input type="hidden" name="searchby" value="ip"/>
			</div>
		</form>
	</div>
<%
	} 
	else if (searchby != null) 
	{
		List<String> list = null;
		UserBean[] inputuser = null;
			
		if("account".equals(searchby))
		{
			searchfor = request.getParameter("searchfor");
			if(searchfor == null)
				searchfor = request.getParameter("username");
			inputuser = data.getAccountByName(searchfor);
			
			if (inputuser.length == 0) 
			{
				message = "User Not Found";
			}
			else
			{	
				try
				{
					list = data.getClientRequestCountsByAccountId(inputuser[0].getId());
				}
				catch(DataError e)
				{
					message = "Error retrieving trend hours.";	
				}
			}
		}
		else if("ip".equals(searchby))
		{
			try
			{
				searchfor = request.getParameter("searchfor");
				if(searchfor == null)
					searchfor = request.getParameter("ip");
				
				list = data.getClientRequestCountsByIp(searchfor);
			}
			catch(DataError e)
			{
				message = "Error retrieving trend hours.";	
			}
		}
		
		if (message != null) 
		{ 
%>	
	<div class="message"><%= message %></div>
<%	
		}
		else if (list.size() > 0) 
		{
%>	<div class="dataTable">	
		<h2>User/ip - <%= searchfor %></h2><br/>
<%
			if (Data.needLevel(user, UserBean.LEV_MODERATOR))
			{
%>		
				<a href="admin.jsp">Return to Admin Page</a><br/><br/>
<%
			}
%>
		<a href="adminclientipchecks.jsp">Select new account or IP</a><br/>
		<table id="sortableTableStats" class="sortable">
			<thead>
			<tr>
<%
			if("ip".equals(searchby))
			{
%>
				<th>name</th>
				<th>hits</th>
<%
			}
			else
			{
%>
				<th>ip</th>
				<th>hits</th>
<%
			}
%>
			</tr>
			</thead>
		
			<tbody>
<%
			for (int c=0; c < list.size(); c++)
			{
				String[] s = list.get(c).split("\\|");
%>
				<tr <%= Data.oddLine(c) %>>
					<td>
						<a href="adminclientipchecks.jsp?searchby=<%="ip".equals(searchby) ? "account" : "ip"%>&searchfor=<%=s[0]%>"><%= s[0] %></a>
					</td>
					<td>
						<a href="adminclientipchecklisting.jsp?searchby=<%="ip".equals(searchby) ? "account" : "ip"%>&searchfor=<%=s[0]%>"><%= s[1] %></a>
					</td>
				</tr>
<%		
			}
%>
			</tbody>
		</table>
	</div>
<%	
		}
		else
		{
			if (Data.needLevel(user, UserBean.LEV_MODERATOR))
			{
%>		
				<a href="admin.jsp">Return to Admin Page</a><br/><br/>
<%
			}
%>
		<a href="adminclientipchecks.jsp">Select new account or IP</a><br/><br/>
		<h2>No Entries found.</h2>
<%
		}
	}
%>
</div>
</div>
</body>
</html>

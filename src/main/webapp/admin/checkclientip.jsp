<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.Helpers, java.util.*"
%>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="net.fseconomy.dto.ClientRequest" %>

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

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css" />
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

	<script src="../scripts/jquery.min.js"></script>
	<script src="../scripts/jquery-ui.min.js"></script>
	<script src="../scripts/AutoCompleteIP.js"></script>

	<script type="text/javascript">

	$(function() {
		initAutoCompleteIP("#ip", "#searchby");
	});

	</script>

</head>

<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	String message = Helpers.getSessionMessage(request);
	if (message != null) 
	{
%>	
		<div class="message"><%= message %></div>
<%
	}
%>
<%
	String searchby = request.getParameter("searchby");
%>
	<h2>Client IP Checker</h2>
	<h4>Enter User Account</h4>
	<h4>Select Ip</h4>
	<div class="form" style="width: 400px">
		<form method="post">
			<div>
				IP:
			    <input type="text" id="ip" name="ip"/>
				<br/>
				<input type="submit" class="button" value="GO" />
				<input type="hidden" id="searchby" name="searchby" value=""/>
			</div>
		</form>
	</div>
<%
	if ( searchby != null && !searchby.equals(""))
	{
		List<ClientRequest> list;

        list = SimClientRequests.getClientRequestsByIp(searchby);

		if (message != null)
		{ 
%>	
	<div class="message"><%= message %></div>
<%	
		}
		else if (list.size() > 0) 
		{
%>	<div class="dataTable">	
		<h2>IP - <%= searchby %></h2><br/>
<%
			if (Accounts.needLevel(user, UserBean.LEV_MODERATOR))
			{
%>		
				<a href="/admin/admin.jsp">Return to Admin Page</a><br/><br/>
<%
			}
%>
		<a href="/admin/checkclientip.jsp">Select new account or IP</a><br/>
		<table id="sortableTableStats" class="sortable">
			<thead>
			<tr>
				<th>name</th>
				<th>hits</th>
			</tr>
			</thead>
		
			<tbody>
<%
			for (ClientRequest item: list)
			{
%>
				<tr>
					<td>
						<%= item.name %>
					</td>
					<td>
						<%= item.ip %>
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
			if (Accounts.needLevel(user, UserBean.LEV_MODERATOR))
			{
%>		
				<a href="/admin/admin.jsp">Return to Admin Page</a><br/><br/>
<%
			}
%>
		<a href="/admin/checkclientip.jsp">Select new account or IP</a><br/><br/>
		<h2>No Entries found.</h2>
<%
		}
	}
%>
</div>
</div>
</body>
</html>

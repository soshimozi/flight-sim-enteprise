<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, java.util.*, net.fseconomy.beans.UserBean"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
	{
%>
        <script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return; 
	}

	String error = null;
	List<String> list = null;
	try
	{
		list = SimClientRequests.getClientRequestIpWithMultipleUsers();
	}
	catch(DataError e)
	{
		error = e.getMessage();
	}
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

	<link href="../css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
	<h2>Client IP used by Multiple Users</h2>
<% 	if (error != null) 
	{ 
%>
	<div class="error"><%= error %></div>
<%
	} 
%>

	<div class="dataTable">	
		<a href="/admin/admin.jsp">Return to Admin page</a><br/>
		<table id="sortableTableStats" class="sortable">
			<thead>
			<tr>
				<th>IP</th>
				<th>users</th>
			</tr>
			</thead>
		
			<tbody>
<%
    for (String aList : list)
    {
        String[] s = aList.split("\\|");
%>
                <tr>
                    <td>
                        <%= s[0] %>
                    </td>
                    <td>
                        <%= s[1] %>
                    </td>
                </tr>
<%
    }
%>
			</tbody>
		</table>
	</div>
</div>
</div>
</body>
</html>

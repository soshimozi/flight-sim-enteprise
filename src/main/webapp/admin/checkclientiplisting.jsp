<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, java.util.*"
%>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@ page import="net.fseconomy.dto.ClientRequest" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

    String searchby = request.getParameter("searchby");
    String searchfor = request.getParameter("searchfor");

    if (searchby == null || searchfor == null)
    {
        response.sendRedirect("/admin/checkclientip.jsp");
    }

    List<ClientRequest> requests = null;
    UserBean inputuser = null;
    String message = null;

    if("account".equals(searchby))
    {
        inputuser = Accounts.getAccountByName(searchfor);
        if (inputuser == null)
        {
            message = "User Not Found";
        }
        else
        {
            try
            {
                requests = SimClientRequests.getClientRequestsByAccountId(inputuser.getId());
            }
            catch(DataError e)
            {
                message = "Error retrieving client ip data.";
            }
        }
    }
    else if("ip".equals(searchby))
    {
        try
        {
            requests = SimClientRequests.getClientRequestsByIp(searchfor);
        }
        catch(DataError e)
        {
            message = "Error retrieving client ip data.";
        }
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

<jsp:include flush="true" page="../top.jsp" />
<jsp:include flush="true" page="../menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	if (message != null)
	{ 
%>
		<div class="message"><%= message %></div>
<%	
	}
	else if ( requests != null && requests.size() > 0)
	{
%>
		<div class="dataTable">	
		<h2>User - <%= "account".equals(searchby) ? inputuser.getName() : request.getParameter("ip") %> - Client request log last 100 entries</h2><br/>
		<a href="admin.jsp">Return to Admin Page</a><br/><br/>
		<a href="/admin/checkclientiplisting.jsp">Select new account or IP</a><br/>
		<table id="sortableTableStats" class="sortable">
		<thead>
		<tr>
			<th>id</th>
			<th>time</th>
			<th>ip</th>
			<th>user id</th>
			<th>name</th>
			<th>client</th>
			<th>state</th>
			<th>aircraft</th>
			<th>params</th>
		</tr>
		</thead>
		<tbody>
<%
        for (ClientRequest cr : requests)
        {
%>
            <tr>
                <td><%= cr.id %>
                </td>
                <td><%= cr.time %>
                </td>
                <td><%= cr.ip %>
                </td>
                <td><%= cr.userid %>
                </td>
                <td><%= cr.name %>
                </td>
                <td><%= cr.client %>
                </td>
                <td><%= cr.state %>
                </td>
                <td><%= cr.aircraft %>
                </td>
                <td><%= cr.params %>
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
%>
	<h4>No items to display</h4>
<%		
	}
%>
</div>
</div>
</body>
</html>

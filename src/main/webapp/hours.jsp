<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.*"
%>
<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
    Data.pendingHours[] pending = null;
    try
    {
        pending = data.getpendingHours(user.getName(),48);
    }
    catch(DataError e)
    {
        //eat it
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script type='text/javascript' src='scripts/common.js'></script>
    <script type='text/javascript' src='scripts/css.js'></script>
    <script type='text/javascript' src='scripts/standardista-table-sorting.js'></script>

</head>
<body>
<jsp:include flush="true" page="top.jsp" />
<div id="wrapper">
<jsp:include flush="true" page="menu.jsp" />

<div class="content">
<div class="dataTable">	
	<table>
	<caption>
	<%= user.getName() %> will have:
	</caption>
	<tbody>
<%
	if(pending.length == 0)
	{
%>
	<tr>
	<td><strong>0.0</strong> hours back</td>
	<td>in 0 hours and 0 minutes.</td>
	</tr>

<%		
	}
		
	for (int c=0; c < pending.length; c++)
	{
%>
	<tr <%= Data.oddLine(c) %>>
	<td><strong><%= Formatters.oneDecimal.format(pending[c].phours) %> </strong>hours back</td>
	<td>in <%= pending[c].phourtime %> hours and <%=pending[c].pminutetime%> minutes.</td>
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

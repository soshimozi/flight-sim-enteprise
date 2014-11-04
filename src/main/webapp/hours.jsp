<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    List<Data.pendingHours> pendingList = null;
    try
    {
        pendingList = data.getPendingHours(user.getName(), 48);
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

    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

    <script type='text/javascript' src='scripts/common.js'></script>
    <script type='text/javascript' src='scripts/css.js'></script>
    <script type='text/javascript' src='scripts/standardista-table-sorting.js'></script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<div class="dataTable">	
	<table>
	<caption>
	<%= user.getName() %> will have:
	</caption>
	<tbody>
<%
	if(pendingList.size() == 0)
	{
%>
        <tr>
            <td><strong>0.0</strong> hours back</td>
            <td>in 0 hours and 0 minutes.</td>
        </tr>
<%		
	}
		
	for (Data.pendingHours hour : pendingList)
	{
%>
        <tr>
            <td><strong><%= Formatters.oneDecimal.format(hour.phours) %> </strong>hours back</td>
            <td>in <%= hour.phourtime %> hours and <%=hour.pminutetime%> minutes.</td>
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

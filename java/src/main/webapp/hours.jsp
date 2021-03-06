<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.List, net.fseconomy.dto.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    List<PendingHours> pendingList = null;
    try
    {
        pendingList = Stats.getInstance().getPendingHours(user.getId(), 48);
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

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

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
	if(pendingList != null && pendingList.size() == 0)
	{
%>
        <tr>
            <td><strong>0.0</strong> hours back</td>
            <td>in 0 hours and 0 minutes.</td>
        </tr>
<%		
	}
	else
    {
	    for (PendingHours hour : pendingList)
	    {
%>
        <tr>
            <td><strong><%= Formatters.oneDecimal.format(hour.phours) %> </strong>hours back</td>
            <td>in <%= hour.phourtime %> hours and <%=hour.pminutetime%> minutes.</td>
        </tr>
<%
        }
	}
%>
	</tbody>
	</table>
</div>
</div>
</div>
</body>
</html>

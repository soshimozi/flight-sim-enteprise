<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import = "java.util.List, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    List<String> users = null;
    String message = null;
    try
    {
        users = Accounts.getUsers("flying");
    }
    catch(DataError e)
    {
        message = "Error: " + e.getMessage();
    }

    if (message != null)
    {
%>
        <div class="message"><%= message %></div>
<%
        return;
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>Who's Flying</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<div class="content">
<div class="dataTable">	
<table>
	<thead>
		<tr>
			<th>Pilot</th>
		</tr>
	</thead>
	<tbody>
<%
	for (String name : users)
	{
%>
		<tr>
			<td><%=name%></td>
        </tr>
<%
	}
%>
	</tbody>
</table>
</div>
</div>
</body>
</html>

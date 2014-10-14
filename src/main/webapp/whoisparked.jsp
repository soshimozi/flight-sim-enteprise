<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import = "net.fseconomy.data.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
    String[] users = null;
    String message = null;
    try
    {
        users = data.getUsers("parked");
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

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

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
	for (int c=0; c < users.length; c++)
	{
		if(users[c]!= null)
		{
%>
		<tr <%= Data.oddLine(c) %>>
			<td><%=users[c]%></td>
<%
		}
	}
%>
		</tr>
	</tbody>
</table>
</div>
</div>
</body>
</html>

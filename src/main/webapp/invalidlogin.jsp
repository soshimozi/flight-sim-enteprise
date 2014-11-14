<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>

<%
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
<%
	String message = (String) request.getAttribute("message");
	if (message != null)
    {
%>
	<div class="message"><%= message %></div>
<%
	}
    else
    {
%>
	<h2>Login incorrect</h2>
	<p>
	    If you don't remember your password you can request a new password here:
	</p>
	<div class="form" style="width: 400px">
        <form method="post" action="userctl">
            Username<br/>
            <input name="user" type="text" class="textarea" size="10" /><br/>
            Email<br/>
            <input name="email" type="text" class="textarea" size="40" /><br/><br/>
            <input type="submit" class="button" value="Request password" />
            <input type="hidden" name="event" value="password"/>
            <input type="hidden" name="return" value="invalidlogin.jsp"/>
        </form>
	</div>
<%
    }
%>
</div>

</div>
</body>
</html>

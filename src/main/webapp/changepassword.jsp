<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.Data"
%>

<%
    Data data = (Data)application.getAttribute("data");
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	String message = (String) request.getAttribute("message");
	if (message != null) {
%>
	<div class="message"><%= message %></div>
<%
	} else {
%>


	<div class="form" style="width: 400px">
	<h2>Change your password</h2>
	<p>
	Enter your old and your new password.
	</p>
	
	<form method="post" action="userctl">
	<div class="formgroup">
	Old Password<br/>
	<input name="password" type="password" class="textarea" size="10" />
	<br/>
	</div>
	<div class="formgroup">
	New Password<br/>
	<input name="newPassword" type="password" class="textarea" size="10" />
	<br/>
	New Password (again)<br/>
	<input name="newPassword2" type="password" class="textarea" size="10" />
	<br/>
	</div>
	<div class="formgroup">
	<input type="submit" class="button" value="Change password" />
	<input type="hidden" name="event" value="changePassword"/>
	<input type="hidden" name="return" value="changepassword.jsp"/>
	</div>
	</form>
	</div>
<%
}
%>
</div>

</div>
</body>
</html>

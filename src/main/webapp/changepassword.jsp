<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.util.*"
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
	String message = Helpers.getSessionMessage(request);
	if (message != null)
    {
%>
	    <div class="message"><%= message %></div>
<%
	}
%>

	<div class="form" style="width: 400px">
    	<h2>Change your password</h2>
	    <p>Enter your old and your new password.</p>
	
	    <form method="post" action="userctl">
            <div>
                <input type="hidden" name="event" value="changePassword"/>
                <input type="hidden" name="returnpage" value="changepassword.jsp"/>
            </div>
	        <div class="formgroup">
	            Old Password<br>
	            <input name="password" type="password" class="textarea" size="10" /><br>
	        </div>
	        <div class="formgroup">
	            New Password<br>
	            <input name="newPassword" type="password" class="textarea" size="10" /><br>
	            New Password (again)<br/>
	            <input name="newPassword2" type="password" class="textarea" size="10" /><br>
	        </div>
	        <div class="formgroup">
	            <input type="submit" class="button" value="Change password" />
	        </div>
	    </form>
	</div>

</div>
</div>
</body>
</html>

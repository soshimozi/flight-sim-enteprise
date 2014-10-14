<%@ page
        language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
        %>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
    Data data = (Data)application.getAttribute("data");

    if(!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
<div class="content">
<%
    if(!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
    }

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
	<div class="form" style="width: 400px">
	<h2>Lock User Account</h2>
	<p>
	Enter the Account Login.
	</p>
	
	<form method="post" action="userctl">
	<div class="formgroup">
	Login to Lock:
	<input name="login" type="text" class="textarea" size="30" id="login" />
	<br/>
	</div>

	<div class="formgroup">
	<input type="submit" class="button" value="Lock Account" />
	<input type="hidden" name="event" value="lockAccount"/>
	<input type="hidden" name="return" value="lockaccount.jsp"/>
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

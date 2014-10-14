<%@ page language="java"
	import="net.fseconomy.data.*"
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<% 
	if (!Data.needLevel(user, UserBean.LEV_CSR) && !Data.needLevel(user, UserBean.LEV_MODERATOR))
	{
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return; 
	}

	String returnPage = request.getRequestURI();

    String message = (String) request.getAttribute("message");
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
		<h2>Sign up</h2>
		<p>To sign up, enter your email address and a user name in the form below.</p>
		<div class="form" style="width: 500px">
			<form method="post" action="userctl">
				Username<br/>
				<input name="user" type="text" class="textarea" size="70" maxlength="45"/><br/>
				Email<br/>
				<input name="email" type="text" class="textarea" size="70"  maxlength="45"/><br/><br/>
				<input type="submit" class="button" value="Sign up" />
				<input type="hidden" name="event" value="create"/>
				<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
			</form>
		</div>
	</div>
</div>
</body>
</html>

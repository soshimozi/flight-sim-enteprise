<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Helpers"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	if (!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
	{
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
		return; 
	}

	String returnPage = request.getRequestURI();
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>
	
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css" />
	<link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
    <script src="../scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#linkedname", "#linkedid", <%= Accounts.ACCT_TYPE_PERSON %>)
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="../top.jsp" />
<jsp:include flush="true" page="../menu.jsp" />

<div id="wrapper">
<%
    String message = Helpers.getSessionMessage(request);
    if (message != null)
    {
%>
    <div class="error"><%= message %></div>
<%
    }
%>
	<div class="content">
		<div class="form" style="width: 500px">
            <h2>Sign up</h2>
            <p>To sign up, enter your email address and a user name in the form below.</p>

			<form method="post" action="/userctl">
                <div>
                    <input type="hidden" name="event" value="create">
                    <input type="hidden" name="returnpage" value="<%=returnPage%>">
                </div>
				Username<br>
				<input name="user" type="text" class="textarea" size="50" maxlength="45"><br>
				Email<br/>
				<input name="email" type="text" class="textarea" size="50"  maxlength="45"><br><br>
                Linked Account (optional)<br>
                <input type="hidden" id="linkedid" name="linkedid" value="">
                <input type="text" id="linkedname" name="linkedname"><br><br>
				<input type="submit" class="button" value="Sign up">
			</form>
		</div>
	</div>
</div>
</body>
</html>

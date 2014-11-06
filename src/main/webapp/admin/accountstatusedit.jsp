<%@ page
        language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<%@ page import="net.fseconomy.beans.UserBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    if(!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"/index.jsp\"</script>");
        return;
    }

    String message = (String) request.getAttribute("message");
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="/theme/redmond/jquery-ui.css" />
    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="/scripts/jquery.min.js"></script>
    <script src="/scripts/jquery-ui.min.js"></script>
    <script src="/scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#username", "#user", <%= Data.ACCT_TYPE_PERSON %>)
        });

    </script>

    <script type="text/javascript">

        function doLockAccount()
        {
            var form = document.getElementById("changeaccount");
            form.event.value = "lockAccount";
            form.submit();
        }

        function doUnlockAccount()
        {
            var form = document.getElementById("changeaccount");
            form.event.value = "unlockAccount";
            form.submit();
        }

    </script>
</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	if (message != null)
    {
%>
	<div class="message"><%= message %></div>
<%
	}
%>
    <a href="/admin/index.jsp">Return to Admin Page</a><br/>
	<div class="form" style="width: 400px">
	<h2>Change User Account Status</h2>
	<p>
	</p>
	
	<form id="changeaccount" method="post" action="/userctl">
	    <div class="formgroup">
            Enter Account:
            <input type="hidden" id="user" name="user" value=""/>
            <input type="text" id="username" name="username"/>
	        <br/>
	    </div>

        <div class="formgroup">
            <input type="button" class="button" onclick="doLockAccount()" value="Lock Account" />&nbsp;&nbsp;&nbsp;&nbsp;
            <input type="button" class="button" onclick="doUnlockAccount()" value="Unlock Account" />
            <input type="hidden" name="event" value="lockAccount"/>
            <input type="hidden" name="returnpage" value="/admin/accountstatusedit.jsp"/>
        </div>
	</form>
	</div>
</div>

</div>
</body>
</html>

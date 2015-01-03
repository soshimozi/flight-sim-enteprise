<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.util.Helpers"
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
    <div class="content error">
    <%= Helpers.getSessionMessage(request) %><br/>
    <a href="<%= (String) request.getSession().getAttribute("back") %>">Back</a>
    </div>
</div>
</body>
</html>

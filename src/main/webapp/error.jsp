<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
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
    <div class="content error">
    <%= (String) request.getAttribute("error") %><br/>
    <a href="<%= (String) request.getAttribute("back") %>">Back</a>
    </div>
</div>
</body>
</html>

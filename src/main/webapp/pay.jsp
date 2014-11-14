<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    String sId = request.getParameter("groupId");
    int id = Integer.parseInt(sId);
    UserBean account = Accounts.getAccountById(id);
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

        <div class="form" style="width: 500px">
            <h2>Pay to flight group <%= account.getName() %></h2>
            <br/>
            <form method="post" action="userctl">
                <div class="formgroup">
                    Amount: <input name="amount" type="text" class="textarea" size="10"/>
                </div>
                <div class="formgroup">
                    <input type="submit" class="button" value="Pay" />
                </div>
                <input type="hidden" name="event" value="payGroup"/>
                <input type="hidden" name="return" value="groups.jsp"/>
                <input type="hidden" name="id" value="<%= account.getId() %>"/>
            </form>
        </div>
    </div>
</div>
</body>
</html>

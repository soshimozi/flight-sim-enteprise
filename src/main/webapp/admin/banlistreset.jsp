<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<%@ page import="net.fseconomy.beans.UserBean" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
<%
        return;
    }

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

    <link rel="stylesheet" type="text/css" href="..css/redmond/jquery-ui.css" />
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script type="text/javascript" src="../scripts/jquery.min.js"></script>
    <script type="text/javascript" src="../scripts/jquery-ui.min.js"></script>
    <script type="text/javascript" src="../scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#accountname", "#account", <%= Accounts.ACCT_TYPE_ALL %>);
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
        <div class="form" style="width: 400px">
            <h2>Reset User Aircraft Rental Ban List</h2>


            <form method="post" action="/userctl">
                <div class="formgroup">
                    Account/Group to reset the ban list for:
                    <input id="accountname" name="accountname" type="text" class="textarea" size="65"/>
                    <input type="hidden" id="account" name="account" />
                    <br/>
                </div>

                <div class="formgroup">
                    <input type="submit" class="button" value="Reset Ban List" />
                    <input type="hidden" name="event" value="resetBanList"/>
                    <input type="hidden" name="return" value="/admin/banlistreset.jsp"/>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>

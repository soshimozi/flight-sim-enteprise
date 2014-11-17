<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.beans.UserBean"
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

    String returnPage = request.getHeader("referer");

    int userId = Integer.parseInt(request.getParameter("id"));

    UserBean linkuser = Accounts.getAccountById(userId);
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css">
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="../scripts/jquery.min.js"></script>
    <script src="../scripts/jquery-ui.min.js"></script>
    <script src="../scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#linkname", "#linkid", <%= Accounts.ACCT_TYPE_PERSON %>)
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
        <h2>Account link</h2>

        <div class="form" style="width: 600px">
            <form method="post" action="/userctl">
                <div>
                    <input type="hidden" name="event" value="linkAccount"/>
                    <input type="hidden" name="userid" value="<%= userId %>"/>
                    <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
                </div>
                <table>
                    <tr>
                        <td>Link User Name: </td>
                        <td><%=linkuser.getName()%></td>
                    </tr>
                    <tr>
                        <td>Link to Account</td>
                        <td>
                            <input type="hidden" id="linkid" name="linkid" value="">
                            <input type="text" id="linkname" name="linkname"><br><br>
                        </td>
                    </tr>
                    <tr>
                        <td><input type="submit" class="button" value="Link" /></td>
                    </tr>
                </table>
            </form>
        </div>
    </div>
</div>
</body>
</html>

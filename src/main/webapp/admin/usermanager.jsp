<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.Helpers, net.fseconomy.beans.UserBean"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!Accounts.needLevel(user, UserBean.LEV_CSR) && !Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
<script type="text/javascript">document.location.href="../index.jsp"</script>
<%
        return;
    }

    String returnPage = request.getRequestURI();
    String message = Helpers.getSessionMessage(request);
    if(message == null)
        message = "";
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css" />
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="../scripts/jquery.min.js"></script>
    <script src="../scripts/jquery-ui.min.js"></script>
    <script src="../scripts/AutoComplete.js"></script>
    <script src="../scripts/AutoCompleteEmail.js"></script>
    <script src="../scripts/AutoCompleteIP.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#username", "#userid", <%= Accounts.ACCT_TYPE_PERSON %>);
            initAutoCompleteEmail("#email", "#emailuserid", <%= Accounts.ACCT_TYPE_PERSON %>);
            initAutoCompleteIP("#ip", "#ipuserid");
        });

    </script>

    <script type="text/javascript">

        function doViewAccount(name)
        {
            var form = document.getElementById(name);

            if(name == 'SearchByEmail')
                form.userid.value = form.emailuserid.value;

            if(name == 'SearchByIP')
                form.userid.value = form.ipuserid.value;

            form.submit();
        }

    </script>
</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
    <div class="content">
        <a href="/admin/admin.jsp">Return to Admin Page</a><br/>

        <p><%= message %></p>

        <div class="form" style="width: 400px">
            <h2>Search by User Account</h2>
            <p>
            </p>

            <form id="SearchByName" method="post" action="usermanageredit.jsp">
                <div class="formgroup">
                    Enter Account:
                    <input type="hidden" id="userid" name="userid" value=""/>
                    <input type="text" id="username" name="username"/>
                    <br/>
                </div>

                <div class="formgroup">
                    <input type="button" class="button" onclick="doViewAccount('SearchByName')" value="View Account" />&nbsp;&nbsp;&nbsp;&nbsp;
                    <input type="hidden" name="returnpage" value="<%= returnPage %>"/>
                </div>
            </form>
        </div>

        <div class="form" style="width: 400px">
            <h2>Search by Email</h2>
            <p>
            </p>

            <form id="SearchByEmail" method="post" action="usermanageredit.jsp">
                <div class="formgroup">
                    Enter Account:
                    <input type="hidden" id="emailuserid" name="emailuserid" value=""/>
                    <input type="hidden" id="userid" name="userid" value=""/>
                    <input type="text" id="email" name="email"/>
                    <br/>
                </div>

                <div class="formgroup">
                    <input type="button" class="button" onclick="doViewAccount('SearchByEmail')" value="View Account" />&nbsp;&nbsp;&nbsp;&nbsp;
                    <input type="hidden" name="returnpage" value="<%= returnPage %>"/>
                </div>
            </form>
        </div>

        <div class="form" style="width: 400px">
            <h2>Search by IP</h2>
            <p>
            </p>
            <form id="SearchByIP" method="post" action="usermanageredit.jsp">
                <div>
                    Enter IP:
                    <input type="text" id="ip" name="ip"/>
                    <br/>
                    <input type="hidden" id="searchby" name="searchby" value=""/>
                    <input type="hidden" id="userid" name="userid" value=""/>
                    <input type="hidden" id="ipuserid" name="ipuserid" value=""/>
                </div>
                <div class="formgroup">
                    <input type="button" class="button" onclick="doViewAccount('SearchByIP')" value="View Account" />&nbsp;&nbsp;&nbsp;&nbsp;
                    <input type="hidden" name="returnpage" value="<%= returnPage %>"/>
                </div>
            </form>
        </div>

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
                <input type="submit" class="button" value="Sign up">
            </form>
        </div>

    </div>
</div>
</body>
</html>

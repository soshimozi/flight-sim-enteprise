<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*"
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
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
        return;
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<meta name="GENERATOR" content="IBM WebSphere Studio" />
<meta http-equiv="Content-Style-Type" content="text/css" />
<link href="theme/Master.css" rel="stylesheet" type="text/css" />
<title>FSEconomy terminal</title>
</head>


<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
    <div class="content">
        <div class="form" style="width: 400px">
            <h2>Unlock User Account</h2>
            <p>
            Enter the Account Login.
            </p>
            <form method="post" action="userctl">
                <div class="formgroup">
                    Login to Unlock:
                    <input name="login" type="text" class="textarea" size="30" id="login" />
                    <br/>
                </div>
                <div class="formgroup">
                    <input type="submit" class="button" value="Unlock Account" />
                    <input type="hidden" name="event" value="unlockAccount"/>
                    <input type="hidden" name="return" value="unlockaccount.jsp"/>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>

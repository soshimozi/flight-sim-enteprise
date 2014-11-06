<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.* "
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

    String sId = request.getParameter("id");

    int id;
    FboBean fbo;
    id = Integer.parseInt(sId);
    fbo = Fbos.getFbo(id);

    String error = null;
    if ("true".equals(request.getParameter("submit")))
    {
        int ibuyer = Integer.parseInt(request.getParameter("buyer"));
        int iseller = Integer.parseInt(request.getParameter("seller"));
        String icao = request.getParameter("icao");
        String goods = request.getParameter("transferGoods");

        if (goods == null)
            goods = "no";

        try
        {
            Fbos.transferFbo(fbo, user, ibuyer, iseller, icao, true);
%>
<jsp:forward page="fbo.jsp">
    <jsp:param name="id" value="<%= fbo.getOwner() %>"/>
</jsp:forward>
<%
            return;
        }
        catch (DataError e)
        {
            error = e.getMessage();
        }
    }
    UserBean account;
    account = Accounts.getAccountById(fbo.getOwner());

    if (error != null)
    {
%>
        <div class="error"><%= error %></div>
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

    <link rel="stylesheet" type="text/css" href="/theme/redmond/jquery-ui.css">
    <link href="/theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="/scripts/jquery.min.js"></script>
    <script src="/scripts/jquery-ui.min.js"></script>
    <script src="/scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#buyername", "#buyer", <%= Data.ACCT_TYPE_ALL %>);
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
	<div class="form" style="width: 600px">
	    <h2>Transfer FBO</h2>
	    <form method="post" action="transferfbo.jsp">
		    <div class="formgroup high">
	            <p>
                <input type="hidden" name="submit" value="true"/>
<%
    if (fbo.getId() > 0)
    {
%>
	            <input type="hidden" name="id" value="<%= fbo.getId() %>"/>
<%
    }
%>
	            <strong>Location:</strong> <%=fbo.getLocation()%>
	            <input type="hidden" name="icao" value="<%= fbo.getLocation() %>"/>
	            <br />
	            <br />
	            <strong>FBO Name:</strong> <%=fbo.getName()%>
	            <input type="hidden" name="fname" value="<%= fbo.getName() %>" />
	            <br />
	            <br />
	            <strong>From:</strong> <%=account.getName()%>
	            <input type="hidden" name="seller" value="<%= account.getId() %>" />
	            <br />
	            <br />
	            <strong>To: </strong>
                <input type="text" id="buyername" name="buyername"/>
                <input type="hidden" id="buyer" name="buyer" value=""/>
                <br />
                <br />
                </p>
		    </div>
            <div class="formgroup">
                <input type="submit" class="button" value="Transfer FBO"/>
            </div>
	    </form>
	</div>
</div>
</div>
</body>
</html>

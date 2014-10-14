<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.text.*, net.fseconomy.data.* "
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%
    if (!Data.needLevel(user, UserBean.LEV_MODERATOR))
    {
        out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
        return;
    }

    String sId = request.getParameter("id");

    int id = 0;
    FboBean fbo;
    id = Integer.parseInt(sId);
    fbo = data.getFbo(id);

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
            data.transferFbo(fbo, user, ibuyer, iseller, icao, goods.equals("checkbox"));
%>
        <jsp:forward page="admin.jsp"></jsp:forward>
<%
            return;
        }
        catch (DataError e)
        {
            error = e.getMessage();
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="theme/redmond/jquery-ui.css">
    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="scripts/jquery.min.js"></script>
    <script src="scripts/jquery-ui.min.js"></script>
    <script src="scripts/AutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#buyername", "#buyer", <%= Data.ACCT_TYPE_ALL %>)
        });

    </script>

</head>
<body>
<jsp:include flush="true" page="top.jsp" />
<div id="wrapper">
<jsp:include flush="true" page="menu.jsp" />

<div class="content">
<%
UserBean account = null;
account = data.getAccountById(fbo.getOwner());
UserBean Accounts[] = data.getAccounts();

if (error != null) 
{
%>
	<div class="error"><%= error %></div>
<%
}	
%>
	<div class="form" style="width: 600px">
	<h2>Transfer FBO</h2>
	
	<form method="post" action="admintransferfbo.jsp">
	<div class="formgroup high">
	<p>
	<input type="hidden" name="submit" value="true"/>
<%	
if (fbo.getId() > 0) 
{ 
%>	<input type="hidden" name="id" value="<%= fbo.getId() %>"/>
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
	<strong>Transfer All Goods With FBO</strong>		  <input name="transferGoods" type="checkbox" id="transferGoods" value="checkbox" checked="checked" />
	</p>
	</div>
	<div class="formgroup high">
	<ul class="footer">
		<li>If the goods checkmark is checked, all goods that belong to the seller at the FBO  will be transferred to the buyer.</li>
	</ul>
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

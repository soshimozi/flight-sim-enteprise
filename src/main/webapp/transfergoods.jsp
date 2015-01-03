<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, java.util.List"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />
<jsp:useBean id="goods" class="net.fseconomy.beans.GoodsBean" scope="session" />

<%
	if(!user.isLoggedIn())
	{
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
		return;
	}

	String fromICAO = request.getParameter("fromICAO");
	int owner = Integer.parseInt(request.getParameter("owner"));
	int commodityId = Integer.parseInt(request.getParameter("commodityId"));

	String error = null;

	UserBean owneraccount;
	owneraccount = Accounts.getAccountById(owner);
	
	List<GoodsBean> goodslist = Goods.getGoodsForAccountAvailable(owner);
	
	GoodsBean gb = null;
	for(GoodsBean good : goodslist)
	{
		if(good.getType() == commodityId && good.getLocation().equals(fromICAO))
			gb = good;
	}		
	
	String buyer = request.getParameter("buyer");
	String amount = request.getParameter("amount");
	
	if( amount == null && "true".equals(request.getParameter("submit")) )
		error = "You must select an account to transfer too.";
	else if( amount != null && !amount.matches("[0-9]+"))
		error = "Amount to transfer Invalid";		
	else if( amount != null && (Integer.parseInt(amount) > gb.getAmount()))
		error = "Amount entered > amount available";
	else if( buyer == null && "true".equals(request.getParameter("submit")) )
		error = "You must select an account to transfer too.";
		
	if( "true".equals(request.getParameter("submit")) && error == null)
	{					

		if( !gb.changeAllowed(owneraccount) )
		{
			error = "Permission denied";
		}
		else 
		{
			try
			{
				Fbos.transferFBOGoods(Integer.parseInt(buyer), owner, request.getParameter("fromICAO"), commodityId, Integer.parseInt(request.getParameter("amount")));
%>
				<jsp:forward page="goods.jsp" />
<%
				return;
			} 
			catch (DataError e)
			{
				error = e.getMessage();
			}	
		}	
	}
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="css/redmond/jquery-ui.css" />
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

	<script src="scripts/jquery.min.js"></script>
	<script src="scripts/jquery-ui.min.js"></script>
	<script src="scripts/AutoComplete.js"></script>

	<script type="text/javascript">
	
		$(function() 
		{
			initAutoComplete("#buyername", "#buyer", <%= Accounts.ACCT_TYPE_ALL %>);
		});
	
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
	<div class="content">
<%
	if (error != null) 
	{
%>
		<div class="error"><%= error %></div>
<%
	}	
%>
		<div class="form" style="width: 600px">
		<h2>Transfer Goods </h2>
		<form method="post" action="transfergoods.jsp">
			<div class="formgroup high">
		  		<input type="hidden" name="submit" value="true"/>
				<input type="hidden" name="owner" value="<%=owner%>"/>
			  	<input type="hidden" name="commodityId" value="<%=commodityId%>"/>
			  	<input type="hidden" name="fromICAO" value="<%=fromICAO%>"/>
			
				<strong>Goods Type:</strong> <%=gb.getCommodity() %>
			  	<br />
			  	<br />
			  	<strong>Amount available:</strong> <%=gb.getAmount() %>
			  	<br />
			  	<br />
				Amount to transfer: <input type="text" class="textarea" name="amount" size="10" value=""/>
				<br />
			  	<br />			  
				<strong>To: </strong>
			    <input type="text" id="buyername" name="buyername"/>
			    <input type="hidden" id="buyer" name="buyer" value=""/>
				<br />
			</div>
			<div class="formgroup">
				<input type="submit" class="button" value="Transfer Goods"/>
			</div>
		</form>
		</div>
	</div>
</div>
</body>
</html>

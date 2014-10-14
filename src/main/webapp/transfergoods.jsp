<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.* "
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<jsp:useBean id="goods" class="net.fseconomy.data.GoodsBean" scope="session" />
<%
	String fromICAO = request.getParameter("fromICAO");
	int owner = Integer.parseInt(request.getParameter("owner"));
	int commodityId = Integer.parseInt(request.getParameter("commodityId"));

	String error = null;

	UserBean owneraccount = null;
	owneraccount = data.getAccountById(owner);
	
	GoodsBean goodsbean[] = data.getGoodsForAccountAvailable(owner);
	
	int gb = -1;
	for(int i=0; i<goodsbean.length; i++)
	{
		if(goodsbean[i].getType() == commodityId && goodsbean[i].getLocation().equals(fromICAO))
			gb = i;
	}		
	
	String buyer = request.getParameter("buyer");
	String amount = request.getParameter("amount");
	
	if( amount == null && "true".equals(request.getParameter("submit")) )
		error = "You must select an account to transfer too.";
	else if( amount != null && !amount.matches("[0-9]+"))
		error = "Amount to transfer Invalid";		
	else if( amount != null && (Integer.parseInt(amount) > goodsbean[gb].getAmount()))
		error = "Amount entered > amount available";
	else if( buyer == null && "true".equals(request.getParameter("submit")) )
		error = "You must select an account to transfer too.";
		
	if( "true".equals(request.getParameter("submit")) && error == null)
	{					

		if( !goodsbean[gb].changeAllowed(owneraccount) )
		{
			error = "Permission denied";
		}
		else 
		{
			try
			{
				data.transferFBOGoods(Integer.parseInt(buyer), owner, request.getParameter("fromICAO"), commodityId, Integer.parseInt(request.getParameter("amount")));
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
	
	UserBean Accounts[] = data.getExposedAccounts();
%>

<!DOCTYPE html>
<html lang="en">
<head>

	<title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link rel="stylesheet" type="text/css" href="theme/redmond/jquery-ui.css" />
    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

	<script src="scripts/jquery.min.js"></script>
	<script src="scripts/jquery-ui.min.js"></script>
	<script src="scripts/AutoComplete.js"></script>

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
			
				<strong>Goods Type:</strong> <%=goodsbean[gb].getCommodity() %>
			  	<br />
			  	<br />
			  	<strong>Amount available:</strong> <%=goodsbean[gb].getAmount() %>
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

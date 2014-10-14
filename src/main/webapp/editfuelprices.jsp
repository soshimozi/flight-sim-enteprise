<%@page language="java"
	    import="net.fseconomy.data.* "
%>

<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<%	
	String returnPage = request.getHeader("referer");

	String ownerName = user.getName();
	int ownerId = user.getId();
	
	String sGroupId = request.getParameter("id");
	if(sGroupId != null && !sGroupId.contentEquals(""))
	{
		int groupId = Integer.parseInt(sGroupId);
		UserBean group = data.getAccountById(groupId);
		ownerName = group.getName();
		ownerId = group.getId();
	}
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

<div class="content">
	<div class="form" style="width: 500px">
	<h2>Edit Fuel Prices for FBOs owned by:</h2><br>
	
	<h3><%= ownerName %></h3>
	
	<div style="padding: 5">
	This is a global change for the at the pump fuel prices for all FBOs<br>
	that the named owner controls.<br><br>
	To leave the price the same, leave the price field blank ("").
	</div>
	<form id="editFboForm" method="post" action="userctl">
		<div>
			<input type="hidden" name="event" value="editFuelPrices"/>
			<input type="hidden" name="owner" value="<%= ownerId %>"/>
			<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
		</div>
		<div class="formgroup high">
			100ll Price: <input type="text" name="price100ll" value="" size="7" maxlength="7"><br><br>
			JetA Price: <input type="text" name="priceJetA" value=""  size="7" maxlength="7">
		</div>
		<div class="formgroup">
			<input type="submit" class="button" value="Update"/>
		</div>
	</form>
	</div>
	
</div>
</body>
</html>

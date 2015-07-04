<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.data.*, net.fseconomy.util.Helpers, net.fseconomy.beans.UserBean"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if (!Accounts.needLevel(user, UserBean.LEV_MODERATOR))
    {
%>
        <script type="text/javascript">document.location.href="index.jsp"</script>
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

    <link rel="stylesheet" type="text/css" href="../css/redmond/jquery-ui.css">
    <link href="../css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
    <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.11.2/jquery-ui.min.js"></script>
    <script src="../scripts/AdminAutoComplete.js"></script>

    <script type="text/javascript">

        $(function()
        {
            initAutoComplete("#ownername", "#owner", <%= Accounts.ACCT_TYPE_PERSON %>)
        });

    </script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div id="wrapper">
<div class="content">

<%
    String message = Helpers.getSessionMessage(request);
    if (request.getParameter("submit") == null && (message == null))
    {
%>
	<h2>Adjust Goods</h2>
	    <div class="form" style="width: 600px">
	        <form method="post">
	            <table>
                    <tr>
                        <td>Owner Name</td>
                        <td>
                            <input type="hidden" id="owner" name="owner" value=""/>
                            <input type="text" id="ownername" name="oname"/>
                        </td>
                    </tr>
                    <tr>
                        <td>Location</td>
                        <td><input name="location" type="text" class="textarea" size="4"/></td>
                    </tr>
                    <tr>
                        <td>Goods Type</td>
                        <td>
                            <select name="commodity" class="formselect">
                                <option class="formselect" value=""> </option>
<%	for (int c=0; c < Goods.commodities.length; c++)
	{ 
		if (Goods.commodities[c] == null)
			continue;
	%>		
		                        <option class="formselect" value="<%= c %>"><%= Goods.commodities[c].getName() %></option>
<% 	} 
%>
	                        </select>
	                    </td>
	                </tr>
	                <tr>
                        <td>
                            <input type="submit" class="button" value="GO" />
                            <input type="hidden" name="submit" value="true" />
                            <input type="hidden" name="return" value="/admin/goodsedit.jsp" />
                        </td>
                    </tr>
	            </table>
	        </form>
	    </div>
<%
    }
    else if (request.getParameter("submit") != null)
    {
		int owner = Integer.parseInt(request.getParameter("owner"));
		String type = request.getParameter("commodity");
		String location = request.getParameter("location").toUpperCase();
		int commodity = Integer.parseInt(request.getParameter("commodity"));
		if (!Airports.isValidIcao(location))
			message = "Invalid Location ICAO";

		int goodsOnSite = Goods.getGoodsQty(location,owner,commodity);
		UserBean account = Accounts.getAccountById(owner);
		String ownername = account.getName();
		String goods = Goods.commodities[Integer.parseInt(type)].getName();
%>
<%
    if (message != null)
	{ 
%>
        <div class="message"><%= message %></div>
<%
        return;
	}
%>
	<h2>Adjust Goods</h2>
	    <div class="form" style="width: 600px">
	        <form method="post" action="/userctl">
	            <table>
                    <tr>
                        <td>Owner </td>
                        <td><%=ownername %></td>
                    </tr>
                    <tr>
                        <td>Location </td>
                        <td><%=location %></td>
                    </tr>
                    <tr>
                        <td>Commodity</td>
                        <td><%=goods %></td>
                    </tr>
                    <tr>
                        <td>Currently On Site</td>
                        <td><%=goodsOnSite %></td>
                    </tr>
                    <tr>
                        <td>Amount (+/-)</td>
                        <td><input name="amount" type="text" class="textarea" size="5"/>Kg</td>
                    </tr>
                    <tr>
                        <td>
                            <input type="submit" class="button" value="Submit" />
                            <input type="hidden" name="submit" value="true"/>
                            <input type="hidden" name="event" value="adjustGoods"/>
                            <input type="hidden" name="return" value="/admin/goodsedit.jsp"/>
                            <input type="hidden" name="owner" value="<%=owner%>" />
                            <input type="hidden" name="location" value="<%=location%>" />
                            <input type="hidden" name="commodity" value="<%=commodity%>" />
                        </td>
                    </tr>
                </table>
            </form>
            <ul class="footer">
                <li>Notes :</li>
                <li>This only changes the quantity of an owners goods at a ICAO. No money is paid or refunded.</li>
                <li>Use a positive amount to add goods or a negitive amount to subtract goods</li>
            </ul>
<% 	
    }
%>
        </div>

</div>
</div>
</body>
</html>

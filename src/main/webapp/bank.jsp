<%@ page language="java"
	import="java.util.*, java.text.*, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<%Data data = (Data)application.getAttribute("data");%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<%
	//setup return page if action used
	String returnPage = request.getRequestURI();
	
	UserBean account = null;
	UserBean Accounts[] = data.getExposedAccounts();
	String sGroupId = request.getParameter("id");
	String message = (String) request.getAttribute("message");

	String groupParam = sGroupId != null ? "?id=" + sGroupId : "";
	returnPage = request.getRequestURI() + groupParam;
    response.addHeader("referer", request.getRequestURI() + groupParam);

	if (sGroupId != null)
	{
		int id = Integer.parseInt(sGroupId);
		if (id == user.getId())
		{
			account = user;
		}
		else
		{
			account = data.getAccountById(id);
			if (account == null)
				message="Account not found";
			else
			{
				if (account.isGroup() == false || user.groupMemberLevel(id) < UserBean.GROUP_STAFF)
					message="Permission denied";
			}
		}
	} 
	else
	{
		account = user;
	}
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
			initAutoComplete("#accountname", "#account", <%= Data.ACCT_TYPE_ALL %>);
		});	
	</script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<div class="content">
<%
	if (message != null) 
	{
%>
	<div class="message"><%= message %></div>
	
<%
	} 
	else 
	{
		double current = account.getBank();
		NumberFormat moneyFormat = NumberFormat.getCurrencyInstance();
%>
	<div class="form" style="width: 500px">
		<h2>Bank account <%= account.getName() %></h2>
		<br/>
		<form method="post" action="userctl">
			<div>
				<input type="hidden" name="event" value="bank"/>
				<input type="hidden" name="id" value="<%= account.getId() %>"/>	
				<input type="hidden" name="returnpage" value="<%=returnPage%>" />
			</div>
			<table>
				<tr><td>Cash balance&nbsp;&nbsp;</td><td><%= Formatters.currency.format(account.getMoney()) %></td></tr>
<%
		if (current < 0) 
		{
%>
				<tr><td>Bank loan</td><td>(<%= Formatters.currency.format(-current) %>)</td></tr>
<% 	
		} 
		else 
		{ 
%>		
				<tr><td>Bank balance</td><td><%= Formatters.currency.format(current) %></td></tr>
<% 	
		} 
%>
				<tr>
					<td>Monthly interest</td><td><%= account.isGroup() ? "N/A" :  Formatters.currency.format(data.getAccountInterest(account.getId())) %> </td>
				</tr>
			</table>
		
			<div class="formgroup">
				<table>
					<tr><td>Withdraw/Loan&nbsp;</td><td><input name="withdraw" type="text" class="textarea" size="10"/></td></tr>
					<tr><td>Deposit/Repay</td><td><input name="deposit" type="text" class="textarea" size="10"/></td></tr>
				</table>
			</div>
			<div class="formgroup">
				<input type="submit" class="button" value="Process" />
			</div>
		</form>
		<br>
		The following applies to Pilot accounts:
		<ul class="footer">
			<li>Annual interest rates: loans:10%, positive balance:5%</li>
			<li>Interest is accumulated daily, and deposited the 1st day of the month</li>
			<li>Maximum balance used for gaining interest is $1,000,000.00</li>
			<li>Maximum loan amount is <%= Formatters.currency.format(account.getLoanLimit()) %></li>
			<li>Parentheses () indicate negative amounts [(32,476.23) = -32,476.00]</li>
		</ul>
	</div>
	<div class="form" style="width: 500px">
		<form method="post" action="userctl">
			<div>
				<input type="hidden" name="event" value="bankTransfer"/>
				<input type="hidden" name="id" value="<%= account.getId() %>"/>	
				<input type="hidden" name="returnpage" value="<%=returnPage%>" />
			</div>
			<h2>Transfer</h2>
			<div class="tf_form">			
				<table>
				<tr><td>From:</td><td> <%= account.getName() %></td></tr>
				<tr><td>To:</td><td>
				    <input type="text" id="accountname" name="accountname"/>
				    <input type="hidden" id="account" name="account" value=""/>
				</td></tr>
				<tr><td>Amount:&nbsp;</td><td><input name="amount" type="text" class="textarea" size="10"/></td></tr>
				<tr><td>Comment:&nbsp;</td><td><input name="comment" type="text" class="textarea" size="50" /></td></tr>
			    </table>
			</div>
			<div class="tf_form">
			    <input type="submit" class="button" value="Transfer" />
		    </div>
		</form>
	</div>
<%
}
%>
</div>

</div>
</body>
</html>

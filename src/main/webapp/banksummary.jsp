<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import="java.util.List, net.fseconomy.beans.*, java.util.Calendar, net.fseconomy.data.*, net.fseconomy.util.Formatters"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");

	//setup return page if action used
	String returnPage = request.getRequestURI();
    response.addHeader("referer", request.getRequestURI());

	String error = (String) request.getAttribute("message");

	List<UserBean> groups = null;
	
	if(error == null)
		groups = Accounts.getGroupsForUser(user.getId());

	Calendar cal = Calendar.getInstance();
	int month = cal.get(Calendar.MONTH)+1;
	int year = cal.get(Calendar.YEAR);
%>

<!DOCTYPE html>
<html>
<head>
	
	<title>FSEconomy terminal</title>

	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
    
	<link href="/theme/Master.css" rel="stylesheet" type="text/css" />

	<script type="text/javaScript">	
		function doSubmit(sel)	
		{
			sel.form.selectedBalance.value = sel.options[sel.selectedIndex].value;
			sel.form.submit();
		}	
	</script>
	
</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
<%
	if (error != null) 
	{ 
%>
	<div class="error"><%= error %></div>
<%	
		return;
	} 
%>
<div class="dataTable">	
	<table>
	
		<caption>
			<b>Summary of Personal and Group Bank Accounts</b><br/>
			<span style="color: #666666; font-size: 9pt;">Group financial information is limited to group staff and group owners.</span>
		</caption>
		
		<thead>
			<tr>
				<th>Account</th>
				<th>Cash Balance</th>
				<th>Bank Balance</th>
				<th>Actions</th>
			</tr>
		</thead>
		
		<tbody>
<%
			//Add user account
%>		
			<tr>
				<td><jsp:getProperty name="user" property="name"/></td>
				<td><%= Formatters.currency.format(user.getMoney()) %></td>
				<td><%= Formatters.currency.format(user.getBank()) %></td>
				<td>
					<a class="link" href="bank.jsp">Banking</a> | <a class="link" href="paymentlog.jsp">Payment Log</a> | 
					<a class="link" href="paymentlog.jsp?month=<%= month %>&year=<%= year %>">Monthly Statement</a>					
					<br/><br/>				
					<form method="post" action="userctl">
						<div>
							<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
						</div>
						<div>
							<input type="hidden" name="event" value="bankBalance"/>							
							<input type="hidden" name="uid" value="<%=user.getId()%>"/>							
							<input type="hidden" name="selectedBalance" value=""/>							
							<select name="Actionbox" class="formselect" onchange="doSubmit(this)" >
								<option value="-1">Quick Transfers - Select action...</option>
								<option value="0">Set cash to $0.00</option>
								<option value="1">Set cash to $1,000</option>
								<option value="5">Set cash to $5,000</option>
								<option value="10">Set cash to $10,000</option>
								<option value="20">Set cash to $20,000</option>
								<option value="30">Set cash to $30,000</option>
								<option value="40">Set cash to $40,000</option>
								<option value="50">Set cash to $50,000</option>
								<option value="60">Set cash to $60,000</option>
								<option value="70">Set cash to $70,000</option>
								<option value="80">Set cash to $80,000</option>
								<option value="100">Set cash to $100,000</option>
								<option value="999">Transfer all in bank to cash.</option>
							</select>
						</div>
					</form>	
				</td>
			</tr>
			<tr>
				<td colspan="4"><hr/></td>
			</tr>	
<%
	//Add group accounts
	
	for (UserBean group : groups)
	{
		int id = group.getId();
		String name = group.getName();
		String url = group.getUrl();
		double grpmoney = group.getMoney();
		double grpbank = group.getBank();
		
		if (url != null)
			url = "<a href=\"" + url + "\" target=\"_blank\">" + name + "</a>";
		else
			url = name;

		int memberLevel = user.groupMemberLevel(id);
		if (memberLevel >= UserBean.GROUP_STAFF ) 
		{
	%>
			<tr>
				<td><%= url %></td>
				<td><%= Formatters.currency.format(grpmoney) %></td>
				<td><%= Formatters.currency.format(grpbank) %></td>
				<td>
					<a class="link" href="bank.jsp?id=<%= id %>">Banking</a> | 
					<a class="link" href="paymentlog.jsp?groupId=<%= id %>">Payment Log</a> | 
					<a class="link" href="paymentlog.jsp?groupId=<%= id %>&month=<%= month %>&year=<%= year %>">Monthly Statement</a>
					<br/><br/>			
					<form method="post" action="userctl">
						<div>
							<div>
								<input type="hidden" name="event" value="bankBalance"/>							
								<input type="hidden" name="uid" value="<%= id %>"/>							
								<input type="hidden" name="selectedBalance" value=""/>							
								<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
							</div>
							<select name="Actionbox" class="formselect" onchange="doSubmit(this)" >
								<option value="-1">Quick Transfers - Select action...</option>
								<option value="0">Set cash to $0.00</option>
								<option value="1">Set cash to $1,000</option>
								<option value="5">Set cash to $5,000</option>
								<option value="10">Set cash to $10,000</option>
								<option value="20">Set cash to $20,000</option>
								<option value="30">Set cash to $30,000</option>
								<option value="40">Set cash to $40,000</option>
								<option value="50">Set cash to $50,000</option>
								<option value="60">Set cash to $60,000</option>
								<option value="70">Set cash to $70,000</option>
								<option value="80">Set cash to $80,000</option>
								<option value="100">Set cash to $100,000</option>
								<option value="999">Transfer all in bank to cash.</option>
							</select>
						</div>
					</form>				
				</td>
			</tr>
			<tr>
				<td colspan="4"><hr/></td>
			</tr>		
<% 		
		} 
	}
%>
		</tbody>
	</table>
<p />
        <form method="post" action="userctl">
        	<div>
        		<div>
	            	<input type="hidden" name="event" value="bankTransfer" />
					<input type="hidden" name="returnpage" value="<%=returnPage%>"/>
				</div>
	            <p>
	            <b>Summary Page Transfers</b><br/>
	            <span style="color: #666666; font-size: 9pt;">Transfers on this page are only allowed <span style="text-decoration: underline">from</span> accounts you own or have staff rights to, and <span style="text-decoration: underline">to</span> accounts you own or have membership in.</span>
	            </p>
	            <div class="tf_form">
	                <table>
	                    <tr>
	                    	<td>From Account:</td>
	                            <td>
	                            <select name="id" class="formselect">
	                                <option class="formselect" value=""></option>
	                                <option class="formselect" value="<%=user.getId()%>"><%= user.getName()%></option>
<% 		
		for (UserBean group : groups)
		{ 
%>
<% 			
			if (user.groupMemberLevel(group.getId()) >= UserBean.GROUP_STAFF)
			{ 
%>
                                	<option class="formselect" value="<%=group.getId()%>"><%= group.getName()%></option>
<% 			
			}
%>
<% 		
		}
%>        
        	                    </select>
								</td>
						</tr>
	                    <tr>
	                        <td>To Account:</td>                        
	                        <td>
	                            <select name="account" class="formselect">
	                                <option class="formselect" value=""></option>
	                                <option class="formselect" value="<%=user.getId()%>"><%= user.getName()%></option>
<% 		
		for (UserBean group : groups)
		{ 
%>
	     							<option class="formselect" value="<%=group.getId()%>"><%= group.getName()%></option>
<% 		
		} 
%>  
        	                	</select>
            	            </td>
                	    </tr>                    
	                    <tr>
	                    	<td>Amount:&nbsp;</td><td><input name="amount" type="text" class="textarea" size="10" /></td>
	                    </tr>
	                    <tr>
	                    	<td>Comment:&nbsp;</td><td><input name="comment" type="text" class="textarea" size="50" /></td>
	                    </tr>
	                </table>
	            </div>
	            
	            <div class="tf_form">
	                <input type="submit" class="button" value="Transfer" />
	            </div>
	            
            </div>
        </form>
</div>
</div>
</body>
</html>
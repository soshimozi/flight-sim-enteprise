<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.Formatters "
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    AssignmentBean assignment;
    String sId = request.getParameter("id");
    String from = request.getParameter("from");
    String to = request.getParameter("to");
    String sCommodityId = request.getParameter("commodityId");
    String sGroup = request.getParameter("groupid");
    String comment = request.getParameter("comment");
    String sPilotFee = request.getParameter("pilotFee");
    String sAmount = request.getParameter("amount");
    String sOwner = request.getParameter("owner");
    String sPay = request.getParameter("pay");
    String repC = request.getParameter("numRep");
    UserBean goodsOwner = null;

    String error = null;
    int id = -1;
    int cnt = 1;
    int distance;
    double ppscale;

    if (sId != null && !sId.equals(""))
        id = Integer.parseInt(sId);

    if (repC != null && repC.matches("[0-9]+"))
        cnt = Integer.parseInt(repC);

    if (id == -1)
    {
        assignment = new AssignmentBean();
        assignment.setId(-1);
        assignment.setCreation(new java.sql.Timestamp(System.currentTimeMillis()));
        assignment.setCreatedByUser(true);
        assignment.setUnits(AssignmentBean.UNIT_KG);
        if (sGroup != null)
        {
            assignment.setGroup(true);
            assignment.setGroupId(Integer.parseInt(sGroup));
        }
        else
        {
            assignment.setGroup(false);
        }
    }
    else
    {
        id = Integer.parseInt(sId);
        assignment = Assignments.getAssignmentById(id);
    }

    if (from != null)
        assignment.setFrom(from);

    if (to != null)
        assignment.setTo(to);

    if (sCommodityId != null && !sCommodityId.contains("99") ) //Ignore aircraft crate
    {
        assignment.setCommodityId(Integer.parseInt(sCommodityId));
        assignment.setCommodity(Goods.commodities[assignment.getCommodityId()].getName());
    }

    if (sPay != null && Airports.isValidIcao(assignment.getTo()))
    {
        if (sAmount != null && sAmount.matches("[0-9]+") && !assignment.isFerry() && assignment.isCreatedByUser())
        {
            distance = (int)Math.round(Airports.getDistance(from, to));

            if (distance < 1)
                distance = 1;

            if (Integer.parseInt(sAmount) > 0)
            {
                ppscale = Double.parseDouble(sPay)*100.0/(distance*Integer.parseInt(sAmount));
                assignment.setPay(ppscale);
            }
        }
    }

    if (sAmount != null && sAmount.matches("[0-9]+"))
        assignment.setAmount(Integer.parseInt(sAmount));

    if (sOwner != null)
    {
        assignment.setOwner(Integer.parseInt(sOwner));
        goodsOwner = Accounts.getAccountById(Integer.parseInt(sOwner));
    }

    if (assignment.getLocation() == null)
        assignment.setLocation(assignment.getFrom());

    if (comment != null)
        assignment.setComment(comment);

    if (sPilotFee != null)
        assignment.setPilotFee(Integer.parseInt(sPilotFee));

    if ("true".equals(request.getParameter("submit")))
    {
        int i;
        try
        {
            if (repC != null && !repC.matches("[0-9]+"))
                throw new DataError("Number of Assignments Invalid");

            if (cnt <= 0)
                throw new DataError("Number of Assignments must be greater than 0");

            if (from.equalsIgnoreCase(to))
                throw new DataError("Goods already at destination");

            if (assignment.getId() <= 0 && assignment.getCommodityId() > 0 && !Goods.checkGoodsAvailable(from, goodsOwner.getId(), assignment.getCommodityId(), assignment.getAmount()*cnt))
                throw new DataError("Not enough Goods available!");

            if (!Airports.isValidIcao(assignment.getFrom()))
                throw new DataError("From airport not found.");

            if (!Airports.isValidIcao(assignment.getTo()))
                throw new DataError("To airport not found.");

            for (i=0;i<cnt;i++)
                Assignments.updateAssignment(assignment, user);

            if (assignment.getOwner() > 0 && assignment.getGroupId() == 0)
            {
                response.sendRedirect("goodsassignments.jsp?transferid=" + assignment.getOwner());
            }
            else if(assignment.getGroupId() > 0)
            {
                response.sendRedirect("goodsassignments.jsp?groupid=" + assignment.getGroupId());
            }
            else
            {
                response.sendRedirect("goodsassignments.jsp");
            }
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

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

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
	<div class="form" style="width: 400px">
	<form method="post" action="editassignment.jsp">
	<div>
	<input type="hidden" name="submit" value="true"/>
<% 
	if (assignment.getCommodityId() > 0) 
	{ 
%>
		<input type="hidden" name="commodityId" value="<%= assignment.getCommodityId() %>"/>
<% 
	} 
%>
	<input type="hidden" name="id" value="<%= assignment.getId() %>"/>
	<input type="hidden" name="owner" value="<%= assignment.getOwner() %>"/>
<% 
	if (assignment.isGroup()) 
	{ 
%> 
		<input type="hidden" name="groupid" value="<%= assignment.getGroupId() %>"/>
<% 
	} 
%>
	<table>
	<caption>Edit Assignment</caption>
<% 
	if (assignment.editFromAllowed(user)) 
	{ 
%>
		<tr><td>From</td><td><input type="text" class="textarea" name="from" size="4" value="<%= assignment.getFrom() %>"/></td></tr>
<% 
	} 
	else 
	{
%>
	<tr><td>From</td><td><%= assignment.getFrom() %></td></tr>
	<input type="hidden" name="from" value="<%= assignment.getFrom() %>"/>
<% 
	} 
%>
<% 
	if (assignment.editToAllowed(user) && assignment.getCommodityId() != 99) 
	{ 
%>
		<tr><td>To</td><td><input type="text" class="textarea" name="to" size="4" value="<%= assignment.getTo() %>"/></td></tr>
<% 	
	} 
	else 
	{ 
%>
	<tr><td>To</td><td><%= assignment.getTo() %></td></tr>
	<input type="hidden" name="to" value="<%= assignment.getTo() %>"/>
<% 
	} 
%>
<% 
	if (assignment.editAmountAllowed(user)  && assignment.getCommodityId() != 99) 
	{ 
%>
		<tr><td>Amount</td><td><input type="text" class="textarea" name="amount" size="10" value="<%= assignment.getAmount() %>"/> Kg</td></tr>
<%		
		if (assignment.getId() == -1)
		{ 
%>
			<tr><td>Number of Assignments</td><td><input type="text" class="textarea" name="numRep" size="10" value="1"/> </td></tr>
<%      
		}
		else
		{ 
%>
 			<input type="hidden" name="numRep" value="1"/>
<%      
		} 
%>
<% 
	} 
	else 
	{ 
%>
		<tr><td>Amount</td><td><%= assignment.getAmount() %></td></tr>
		<input type="hidden" name="amount" value="<%= assignment.getAmount() %>"/>
<% 
	} 
%>
<% 
	if (assignment.editPayAllowed(user)) 
	{
%>
		<tr><td>Pay</td><td><input name="pay" type="text" class="textarea" value="<%= Formatters.oneDigit.format(assignment.getPay()*assignment.getDistance()*assignment.getAmount()/100) %>" size="6"/> per Assignment</td></tr>
<% 	
	} 
	else 
	{ 
%>
		<input type="hidden" name="pay" value="<%=assignment.getPay()%>"/>
<% 
	} 
%>
<% 
	if (assignment.editPilotFeeAllowed(user)) 
	{ 
%>
		<tr><td>Pilot Fee</td><td><input name="pilotFee" type="text" class="textarea" value="<%= assignment.getPilotFee() %>" size="4"/></td></tr>
<% 
	} 
%>
<% 
	if (assignment.editCommentAllowed(user)) 
	{ 
%>
		<tr><td>Comment</td><td><input type="text" class="textarea" name="comment" size="30" value="<%= assignment.getComment() %>"/></td></tr>
<% 
	} 
%>
	<tr><td><input class="button" type="submit" value="Update"/></td></tr>
	
	</table>
	<ul class="footer">
		<li>Number of Assignments: total number of assignments to be created</li>
		<li>Amount: Amount to pay per assignment</li>
	</ul>
	</div>
	</form>
	</div>
</div>
</div>
</body>
</html>

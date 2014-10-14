<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import = "net.fseconomy.data.*, java.util.*"
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%!
    String groupMenu(UserBean user, String name, boolean staffOnly, boolean includeBaseLink, String link, String arg, HttpServletResponse response)
    {
        Map memberships = user.getMemberships();

        StringBuffer returnValue = new StringBuffer();
        String indent = "&nbsp;&nbsp;&nbsp;&nbsp;";

        if( includeBaseLink )
            returnValue.append("<a href=\"" + link + "\" >" + name + "</a><br/><br/>\n");

        int count = 0;
        int stringLen = 0;
        boolean hasGroups = false;
        if (memberships != null)
        {
            for (Iterator i = memberships.values().iterator(); i.hasNext(); )
            {
                Data.groupMemberData memberData = (Data.groupMemberData) i.next();
                if (staffOnly == false || memberData.memberLevel >= UserBean.GROUP_STAFF)
                {
                    int len = memberData.groupName.length();
                    if (len > stringLen)
                        stringLen = len;
                    hasGroups = true;
                }
            }
        }
        if (!hasGroups)
            return includeBaseLink ? returnValue.toString() : "";

        for (Iterator i = memberships.values().iterator(); i.hasNext(); )
        {
            Data.groupMemberData memberData = (Data.groupMemberData) i.next();
            if (staffOnly == false || memberData.memberLevel >= UserBean.GROUP_STAFF)
                returnValue.append(indent + "<a href=" + response.encodeURL(link + arg + memberData.groupId) + ">" + memberData.groupName.replaceAll("\'","\\\\'") + "</a><br/><br/>\n");
        }
        return returnValue.toString();
    }
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy Menu Map</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

</head>
<body>
	<jsp:include flush="true" page="top.jsp" />
	<br/>
	<a href="index.jsp">Home</a><br/><br/>
<% 
if (user.isLoggedIn()) 
{  
%>	
	<a href="edituser.jsp">Change preferences</a><br/><br/>
	<a href="changepassword.jsp">Change password</a><br/><br/>
	<a href="datafeeds.jsp">Data Feeds</a><br/><br/>
	<a href="score.jsp?type=pilots">Statistics - Pilots</a><br/><br/>
	<a href="score.jsp?type=groups">Statistics - Groups</a><br/><br/>
	<a href="aircraftmodels.jsp">Aircraft Models</a><br/><br/>
	<br/><br/>
	<a href="airport.jsp">Airports</a><br/><br/>
	<a href="myflight.jsp">My Flight</a><br/><br/>
	<%= groupMenu(user, "Logs", false, true, "log.jsp", "?groupId=", response) %>
	<%= groupMenu(user, "Payment logs", false, true, "paymentlog.jsp", "?groupId=", response) %>
	<br/><br/>
	<%= groupMenu(user, "Aircraft", false, true, "aircraft.jsp", "?id=", response) %>
	<a href="market.jsp">Aircraft - Purchase</a><br/><br/>
	<br/><br/>
	<%= groupMenu(user, "Bank", true, true, "bank.jsp", "?id=", response) %>
	<a href="banksummary.jsp">Summary of Accounts</a><br/><br/>
	<br/>
	<a href="groups.jsp">Groups - My Groups</a><br/><br/>
	<a href="groups.jsp?all=1">Groups - All</a><br/><br/>
	Group Assignments<br/><br/>
	<%= groupMenu(user, "Group Assignments", false, false, "groupassignments.jsp", "?groupId=", response) %>
	<br/>
	Pay Group<br/><br/>
	<%= groupMenu(user, "Pay Group", true, false, "pay.jsp", "?groupId=", response) %>
	<br/>
	Group Membership<br/><br/>
	<%= groupMenu(user, "Group Membership", true, false, "memberships.jsp", "?groupId=", response) %>
	<br/><br/>
	<%= groupMenu(user, "Goods", true, true, "goods.jsp", "?groupId=", response) %>
	<br/>
	<a href="groupassignments.jsp">Goods Transfer Assignments</a><br/><br/>
	<%= groupMenu(user, "Transfer Assignments", true, false, "groupassignments.jsp", "?transfer=", response) %>
	<br/><br/>
	<%= groupMenu(user, "FBOs", true, true, "fbo.jsp", "?id=", response) %>
	<%= groupMenu(user, "FBO Mgt", true, true, "fbomgt.jsp", "?id=", response) %>
	<%= groupMenu(user, "Facilities", true, true, "fbofacility.jsp", "?id=", response) %>
	<a href="fbomap.jsp">FBO Maps</a><br/><br/>
	<a href="marketfbo.jsp">FBO Purchase</a><br/><br/>
<%
	if (user.getLevel() == UserBean.LEV_MODERATOR || user.getLevel() == UserBean.LEV_ADMIN)
	{
%>
	<a href="admin.jsp">Admin</a><br/><br/>
	<a href="fsmappings.jsp">Modify aircraft mappings</a><br/><br/>
	<a href="models.jsp">Modify aircraft models</a><br/><br/>
	<a href="templates.jsp">Modify assignment templates</a><br/><br/>
	<a href="signup.jsp">Add New User</a><br/><br/>
	<a href="lockaccount.jsp">Lock Account</a><br/><br/>
	<a href="unlockaccount.jsp">Unlock Account</a><br/><br/>
	<a href="resetbanlist.jsp">Reset Rental Ban List</a><br/><br/>
<% 
	}
	if (user.getLevel() == UserBean.LEV_CSR)
	{
%>
	<br/><br/>
	<a href="signup.jsp">Add User</a><br/><br/>
	<a href="admineditaccount.jsp">Edit User</a><br/><br/>
<% 
	}
}
%>
</body>
</html>
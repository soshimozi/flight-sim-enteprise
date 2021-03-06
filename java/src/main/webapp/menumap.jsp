<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import = "net.fseconomy.beans.*, net.fseconomy.data.*, java.util.*"
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
%>
<%!
	String groupMenu(UserBean user, String name, boolean staffOnly, boolean includeBaseLink, String link, String arg, HttpServletResponse response)
    {
        Map memberships = user.getMemberships();

        StringBuilder returnValue = new StringBuilder();
        String indent = "&nbsp;&nbsp;&nbsp;&nbsp;";

        if (includeBaseLink)
        {
            returnValue.append("<a href=\"").append(link).append("\" >").append(name).append("</a><br/><br/>\n");
        }

        int stringLen = 0;
        boolean hasGroups = false;
        if (memberships != null)
        {
            for (Object o : memberships.values())
            {
                Groups.groupMemberData memberData = (Groups.groupMemberData) o;
                if (!staffOnly || memberData.memberLevel >= UserBean.GROUP_STAFF)
                {
                    int len = memberData.groupName.length();
                    if (len > stringLen)
                    {
                        stringLen = len;
                    }
                    hasGroups = true;
                }
            }
        }
        if (!hasGroups)
        {
            return includeBaseLink ? returnValue.toString() : "";
        }

        for (Object o : memberships.values())
        {
            Groups.groupMemberData memberData = (Groups.groupMemberData) o;
            if (!staffOnly || memberData.memberLevel >= UserBean.GROUP_STAFF)
            {
                returnValue.append(indent).append("<a href=").append(response.encodeURL(link + arg + memberData.groupId)).append(">").append(memberData.groupName.replaceAll("\'", "\\\\'")).append("</a><br/><br/>\n");
            }
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

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

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
	<%= groupMenu(user, "Logs", false, true, "log.jsp", "?groupid=", response) %>
	<%= groupMenu(user, "Payment logs", false, true, "paymentlog.jsp", "?groupid=", response) %>
	<br/><br/>
	<%= groupMenu(user, "Aircraft", false, true, "aircraft.jsp", "?id=", response) %>
	<a href="aircraftforsale.jsp">Aircraft - Purchase</a><br/><br/>
	<br/><br/>
	<a href="banksummary.jsp">Banking</a><br/><br/>
	<br/>
	<a href="groups.jsp">Groups - My Groups</a><br/><br/>
	<a href="groups.jsp?all=1">Groups - All</a><br/><br/>
	Group Assignments<br/><br/>
	<%= groupMenu(user, "Group Assignments", false, false, "groupassignments.jsp", "?groupid=", response) %>
	<br/>
	Group Membership<br/><br/>
	<%= groupMenu(user, "Group Membership", true, false, "memberships.jsp", "?groupid=", response) %>
	<br/><br/>
	<%= groupMenu(user, "Goods", true, true, "goods.jsp", "?groupid=", response) %>
	<br/>
	<a href="goodsassignments.jsp">Goods Transfer Assignments</a><br/><br/>
	<%= groupMenu(user, "Transfer Assignments", true, false, "goodsassignments.jsp", "?transferid=", response) %>
	<br/><br/>
	<%= groupMenu(user, "FBOs", true, true, "fbo.jsp", "?id=", response) %>
	<%= groupMenu(user, "FBO Mgt", true, true, "fbomgt.jsp", "?id=", response) %>
	<%= groupMenu(user, "Facilities", true, true, "fbofacility.jsp", "?id=", response) %>
	<a href="fbomap.jsp">FBO Maps</a><br/><br/>
	<a href="fbosforsale.jsp">FBO Purchase</a><br/><br/>
<%
	if (user.getLevel() == UserBean.LEV_MODERATOR || user.getLevel() == UserBean.LEV_ADMIN)
	{
%>
	<a href="admin/admin.jsp">Admin</a><br/><br/>
<%
	}
	if (user.getLevel() == UserBean.LEV_CSR)
	{
%>
	<br/><br/>
    <a href="admin/usermanager.jsp">User Manager</a><br/><br/>
<% 
	}
}
%>
</body>
</html>
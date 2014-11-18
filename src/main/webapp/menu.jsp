<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
	    import = "net.fseconomy.beans.*, net.fseconomy.data.*, java.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%!
String groupMenu(UserBean user, String parent, int id, String name, boolean staffOnly, boolean includeBaseLink, String link, String arg, HttpServletResponse response)
{
	Map memberships = user.getMemberships();

	StringBuilder returnValue = new StringBuilder();
	String menu = parent + "_sub_" + id;
	
	returnValue.append("oM.makeMenu('").append(menu).append("','").append(parent).append("','").append(name).append("','").append(includeBaseLink ? response.encodeURL(link) : "").append("');\n");
	int count = 0;
	int stringLen = 0;
	boolean hasGroups = false;
    if (memberships != null)
    {
        for (Object o : memberships.values())
        {
            Accounts.groupMemberData memberData = (Accounts.groupMemberData) o;
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

	int length = 6 * stringLen + 20;

    for (Object o : memberships.values())
    {
        Accounts.groupMemberData memberData = (Accounts.groupMemberData) o;
        if (!staffOnly || memberData.memberLevel >= UserBean.GROUP_STAFF)
        {
            returnValue.append("oM.makeMenu('").append(menu).append("_").append(count++).append("','").append(menu).append("','").append(memberData.groupName.replaceAll("\'", "\\\\'")).append("','").append(response.encodeURL(link + arg + memberData.groupId)).append("', '', ").append(length).append(");\n");
        }
    }

	return returnValue.toString();
}
%>

<link href="/css/menu.css" rel="stylesheet" type="text/css" />
<script src="/scripts/coolmenus4.js"></script>

<script >
	menuheight = 17;
	oM=new makeCM("oM");
	oM.resizeCheck=1;
	oM.rows=1; 
	oM.onlineRoot="/";
	oM.pxBetween =0; 
	oM.fillImg="/img/empty.gif";
	// menu positions
	oM.fromTop=101; oM.fromLeft=10; oM.wait=300; oM.zIndex=400;
	oM.useBar=1; oM.barWidth=700; oM.barHeight="menu"; oM.barX=0;oM.barY="menu"; oM.barClass="menuclass";
	oM.barBorderX=0; oM.barBorderY=0;
	oM.menuPlacement=0;
	
	oM.level[0]=new cm_makeLevel(90,menuheight,"cl0","cl0over",0,1,"cl0border",0,"bottom",0,0,0,0,0,1);
	oM.level[1]=new cm_makeLevel(150,menuheight,"cl1","cl1over",1,1,"cl1border",0,"right",0,0,0,10,10);
	oM.level[2]=new cm_makeLevel(220,menuheight);

	oM.makeMenu('m1','','Home','<%= response.encodeURL("index.jsp") %>', "", 60);
	oM.makeMenu('sub1_0','m1','Home','<%= response.encodeURL("index.jsp") %>');

<% 
	if (user.isLoggedIn()) 
	{  
%>	
		oM.makeMenu('sub1_1','m1','Change preferences','<%= response.encodeURL("edituser.jsp") %>');
		oM.makeMenu('sub1_2','m1','Change password','<%= response.encodeURL("changepassword.jsp") %>');
		oM.makeMenu('sub1_7','m1','Data Feeds','<%= response.encodeURL("datafeeds.jsp") %>');
<% 
	} 
%>
	oM.makeMenu('sub1_3','m1','Statistics','');
	oM.makeMenu('sub1_3_1','sub1_3','Pilots','<%= response.encodeURL("score.jsp?type=pilots")%>');
	oM.makeMenu('sub1_3_2','sub1_3','Groups','<%= response.encodeURL("score.jsp?type=groups")%>');
	oM.makeMenu('sub1_8','m1','Aircraft Models','<%= response.encodeURL("aircraftmodels.jsp") %>');
	oM.makeMenu('sub1_9','m1','FSE Forums','<%= "http://www.fseconomy.net/forum" %>','_blank');
<% 
	if (user.isLoggedIn()) 
	{ 		 
%>
		oM.makeMenu('m2','','Airports','<%= response.encodeURL("airport.jsp") %>', "", 80);
		oM.makeMenu('m3','','My Flight','<%= response.encodeURL("myflight.jsp") %>', "", 90);
		oM.makeMenu('m4','','Log','<%= response.encodeURL("log.jsp") %>', "", 50);
		<%= groupMenu(user, "m4", 1, "Logs", false, true, "log.jsp", "?groupId=", response) %>
		<%= groupMenu(user, "m4", 2, "Payment logs", false, true, "paymentlog.jsp", "?groupId=", response) %>

		oM.makeMenu('m6','','Aircraft','<%= response.encodeURL("aircraft.jsp")%>', "", 80);
		<%= groupMenu(user, "m6", 1, "Aircraft", false, true, "aircraft.jsp", "?id=", response) %>
		oM.makeMenu('sub6_1','m6','Purchase aircraft','<%= response.encodeURL("market.jsp") %>');

		oM.makeMenu('m7','','Bank','<%= response.encodeURL("bank.jsp") %>', "", 60);
		<%= groupMenu(user, "m7", 1, "Bank account", true, true, "bank.jsp", "?id=", response) %>
		oM.makeMenu('sub7_2','m7','Summary of accounts','<%= response.encodeURL("banksummary.jsp")%>');

		oM.makeMenu('m8','','Groups','<%= response.encodeURL("groups.jsp") %>', "", 80);
		oM.makeMenu('sub8_0','m8','My groups','<%= response.encodeURL("groups.jsp")%>');
		oM.makeMenu('sub8_1','m8','All groups','<%= response.encodeURL("groups.jsp?all=1")%>');
	<%= groupMenu(user, "m8", 1, "Assignments", false, false, "groupassignments.jsp", "?groupId=", response) %>
	<%= groupMenu(user, "m8", 2, "Pay group", false, false, "pay.jsp", "?groupId=", response) %>
	<%= groupMenu(user, "m8", 3, "Memberships", true, false, "memberships.jsp", "?groupId=", response) %>
	
		oM.makeMenu('m9','','Goods','<%= response.encodeURL("goods.jsp") %>', "", 75);
		<%= groupMenu(user, "m9", 1, "Goods", true, true, "goods.jsp", "?groupId=", response) %>
		<%= groupMenu(user, "m9", 2, "Transfer assignments", true, true, "groupassignments.jsp", "?transfer=", response) %>
	
		oM.makeMenu('m10','','FBO','<%= response.encodeURL("fbo.jsp") %>', "", 50);
		<%= groupMenu(user, "m10", 1, "FBO", true, true, "fbo.jsp", "?id=", response) %>
		<%= groupMenu(user, "m10", 2, "FBO Mgt", true, true, "fbomgt.jsp", "?id=", response) %>
		<%= groupMenu(user, "m10", 3, "Facilities", true, true, "fbofacility.jsp", "?id=", response) %>
		oM.makeMenu('sub10_0','m10','FBO Maps','<%= response.encodeURL("fbomap.jsp")%>');
		oM.makeMenu('sub10_1','m10','Purchase FBO','<%= response.encodeURL("marketfbo.jsp") %>');

   		oM.makeMenu('m11','','Site Map','<%= response.encodeURL("menumap.jsp") %>', "", 75);

<%
	if (user.getLevel() == UserBean.LEV_MODERATOR || user.getLevel() == UserBean.LEV_ADMIN)
	{
%>
		oM.makeMenu('m12','','Admin','<%= response.encodeURL("admin/admin.jsp") %>', '', null, null, null, null, 'cl0gold','cl0overgold');
<%
	}

	if (user.getLevel() == UserBean.LEV_CSR)
	{
%>
		oM.makeMenu('m13','','CSR','<%= response.encodeURL("admin/usermanager.jsp") %>', '', null, null, null, null, 'cl0gold','cl0overgold');
<%
	}
	if (user.getLevel() == UserBean.LEV_ACA)
	{
%>
		oM.makeMenu('m14','','Aircraft Mapping','<%= response.encodeURL("admin/aircraftmappings.jsp") %>', '', 120, null, null, null, 'cl0gold','cl0overgold');
<% 
	}
} 
%>

oM.construct()
</script>


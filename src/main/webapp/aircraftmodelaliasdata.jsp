<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.text.*, java.util.*, net.fseconomy.data.*"
 %>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>
<%
	String sId = request.getParameter("id");
	if(sId == null)
	{
		out.print("Invalid Model Id!");
		return;
	}

	int id = Integer.parseInt(sId);
	Data.ModelAliases aliases = data.getModelAliases(id);
%>
<style type="text/css">
.myaliassection {
	margin: 10px;
 	border:2px solid #a1a1a1;
    padding:10px 40px; 
    background:#dddddd;
    border-radius:25px;
    width:500px;
}

.myaliasblock li {
    margin-left: auto;
    margin-right: auto;
}
</style>
<div class="container">
	<a id="aliases"/>

	<div class="myaliassection">
		<div style="font-size: 12pt; font-weight: bold;">Aliases</div>
		<div class="myaliasblock">
		<ol>
<%
	for(int i=0; i<aliases.Aliases.length; i++)
	{
			out.print("<li>" + aliases.Aliases[i]+"</li>");
	}
%>			
		</ol>
		</div>

	</div>

</div>

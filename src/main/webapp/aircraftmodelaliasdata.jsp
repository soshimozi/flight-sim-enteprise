<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.dto.*,net.fseconomy.data.*"
 %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	String sId = request.getParameter("id");
	if(sId == null)
	{
%>
		"Invalid Model Id!"
<%
		return;
	}

	int id = Integer.parseInt(sId);
	ModelAliases aliases = Models.getModelAliases(id);
%>

<div class="row clearfix">
	<div class="col-sm-12 column">
        <a id="aliases"></a>
		<h4>Aliases</h4>
		<div class="panel panel-default">
    		<ol>
<%
	for(int i=0; i<aliases.Aliases.length; i++)
	{
%>
    			<li><%=aliases.Aliases[i]%></li>
<%
	}
%>			
	    	</ol>
		</div>
	</div>
</div>

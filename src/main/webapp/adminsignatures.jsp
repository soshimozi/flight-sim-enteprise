<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.text.*, net.fseconomy.data.* "
%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session" />
<%	
	if (!Data.needLevel(user, UserBean.LEV_MODERATOR)) 
	{
		out.print("<script type=\"text/javascript\">document.location.href=\"index.jsp\"</script>");
		return; 
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>
	
	<title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
	
	<link href="theme/Master.css" rel="stylesheet" type="text/css" />

	<script>

		function updateSig(month)
		{
			window.open(
					'updatesignature.jsp?month=' + month,
					'UpdateSignature',
					'status=no,toolbar=no,height=450,width=600')
		}

	</script>
</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
	<div class="content">
		<ol>
<%
	for(int i=1; i<=12; i++)
	{
%>
			<li>
				<a title="Upload new signature background" onclick="updateSig(<%=i%>)">			
					<img src="sig-templates/template<%=i%>.jpg" />
				</a>
			</li>
<%
	}
%>
		</ol>
	</div>
</body>
</html>

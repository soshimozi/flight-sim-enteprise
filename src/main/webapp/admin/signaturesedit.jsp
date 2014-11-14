<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.* "
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
	
	<link href="../css/Master.css" rel="stylesheet" type="text/css" />

	<script>

		function updateSig(month)
		{
			window.open(
					'/admin/updatesignature.jsp?month=' + month,
					'UpdateSignature',
					'status=no,toolbar=no,height=450,width=600')
		}

	</script>

</head>
<body>

<jsp:include flush="true" page="/top.jsp" />
<jsp:include flush="true" page="/menu.jsp" />

<div class="wrapper">
<div class="content">
		<ol>
<%
	for(int i=1; i<=12; i++)
	{
%>
			<li>
				<a title="Upload new signature background" onclick="updateSig(<%=i%>)">			
					<img src="/sig-templates/template<%=i%>.jpg" />
				</a>
			</li>
<%
	}
%>
		</ol>
</div>
</div>
</body>
</html>

<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, net.fseconomy.data.*, org.apache.commons.fileupload.*, java.io.File, java.io.IOException"%>
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

<%
	String message = null;
	String content = "";
	String style = "";
	String sMon;
	int mon;
	
	if (FileUpload.isMultipartContent(request))
	{
		DiskFileUpload upload = new DiskFileUpload();
		List items = upload.parseRequest(request);
		
		Map itemsMap = new HashMap();
		for (Iterator i = items.iterator(); i.hasNext();)
		{
			FileItem item = (FileItem) i.next();
			itemsMap.put(item.getFieldName(), item);
		}
		
		sMon = request.getParameter("month");
		
		mon = Integer.parseInt(sMon);
		if(mon < 1 || mon > 12)
			message = "Month out of range";
	
		FileItem image = (FileItem) itemsMap.get("template");
		
		if (message == null && image != null)
		{
			try
			{
				if (image.getSize() > 0 && image.getContentType().equals("image/jpeg"))
				{
					String filename = request.getRealPath(File.separator) + "/sig-templates/template" + mon + ".jpg";
					File file = new File(filename);
					image.write(file);
				}
			}
			catch(IOException e)
			{
				message = "Error: " + e.getMessage();
			}
%>
			<script type="text/javascript">
				opener.location.reload();
				self.close();
			</script>
<%			
		}
	} 
	else
	{
		sMon = request.getParameter("month");
		mon = Integer.parseInt(sMon);
	}
%>
</head>
<body>
<%
	if (message != null) 
	{
%>
		<div class="message"><%= message %></div>
<%
	}
%>
<div class="content">
	<div class="form">
	    <h2>Upload new signature template</h2>
	    <img src='<%= "sig-templates/template" + mon + ".jpg" %>'/>
	    <p>Select the desired new template file from your local drive. The filename you select will be changed when uploaded to the required filename.
	    <form method="post" enctype="multipart/form-data">
            <div>
                <input type="hidden" name="mon" value="<%= mon %>" />
                <input name="template" type="file" class="textarea" />
                <input name="custom" type="submit" class="button" value="Upload"/>
            </div>
        </form>
	</div>
</div>
</body>
</html>

<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.util.*, net.fseconomy.beans.*, net.fseconomy.data.*, org.apache.commons.fileupload.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
	String message = null;
	String sId;
	FboBean fbo;
	int id;

    StringBuilder sb = new StringBuilder();

    if (FileUpload.isMultipartContent(request))
	{
		DiskFileUpload upload = new DiskFileUpload();
		List items = upload.parseRequest(request);
		Map itemsMap = new HashMap();

        for (Object item1 : items)
        {
            FileItem item = (FileItem) item1;
            itemsMap.put(item.getFieldName(), item);
        }

		sId = ((FileItem) itemsMap.get("id")).getString();
		FileItem fClear = (FileItem) itemsMap.get("clear");
		String sClear = fClear != null ? fClear.getString() : null;
		id = Integer.parseInt(sId);
		fbo = Fbos.getFbo(id);
		FileItem image = (FileItem) itemsMap.get("bg");
		boolean clear = sClear != null && !sClear.equals("");

		if (image != null)
		{
			try
			{
                if (clear)
                {
                    Fbos.updateInvoiceBackground(fbo, null, 0, user);
                }
                else if (image.getSize() > 0 && (image.getContentType().equals("image/jpeg") || image.getContentType().equals("image/png")))
                {
                    Fbos.updateInvoiceBackground(fbo, image.getInputStream(), (int) image.getSize(), user);
                }
			}
			catch(DataError e)
			{
				message = "Error: " + e.getMessage();
			}

			sb.append("<!DOCTYPE html>");
            sb.append("<html>");
            sb.append("<head>");
            sb.append("<script type='text/javascript'>");

            if (message != null)
            {
                sb.append("alert('Error: ").append(message).append("');");
            }

            sb.append("opener.location.reload();");
            sb.append("self.close();");
            sb.append("</script>");
            sb.append("</head>");
            sb.append("<body></body>");
            sb.append("</html>");
%>
            <%= sb.toString() %>
<%
			return;
		}
	} 
	else
	{
		sId = request.getParameter("id");
		id = Integer.parseInt(sId);
		fbo = Fbos.getFbo(id);
		
		//FSX Client for some reason does not pass session data
		if(user.getId() == -1)
		{
            sb.append("<!DOCTYPE html>");
            sb.append("<html>");
            sb.append("<head>");
            sb.append("<script type='text/javascript'>");
            sb.append("alert('There is a error using the FSX client to upload Invoices. Please use a stand alone browser. Thank you.');");
            sb.append("opener.location.reload();");
            sb.append("self.close();");
            sb.append("</script>");
            sb.append("</head>");
            sb.append("<body></body>");
            sb.append("</html>");

%>
            <%= sb.toString() %>
<%
			return;

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

<div id="wrapper">
<span style="visibility: hidden"><%= user.getMemberships() == null ? "UserId: " + user.getId() : user.getMemberships().size() %></span>
<div class="content">
	<div class="form">
	<h2>Upload custom invoice background</h2>
	<img width="200" src="<%= fbo.getInvoiceBackground() %>"/>
	<p>To set a custom invoice background for your FBO, select a JPG or PNG file from your local drive.
	You can use the <a onclick="this.target='_blank'" href="img/invoice_template.jpg">invoice template</a> as a reference.</p>
	<form method="post" enctype="multipart/form-data">
		<div>
			<input type="hidden" name="id" value="<%= fbo.getId() %>" />
			<input name="bg" type="file" class="textarea" />
			<input name="custom" type="submit" class="button" value="Upload"/>
			<input name="clear" type="submit" class="button" value="Reset to default"/>
		</div>
	</form>
	</div>
</div>
</div>
</body>
</html>

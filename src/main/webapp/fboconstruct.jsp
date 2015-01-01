<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, nl.captcha.Captcha"%>
<%@ page import="net.fseconomy.servlets.UserCtl" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    String error = null;
    String sOwner = request.getParameter("owner");
    String location = request.getParameter("location");
    String name = request.getParameter("name");
    String submit = request.getParameter("submit");

    FboBean fbo;

    if("Submit".equals(submit))
    {
        //Check captcha
        Captcha captcha = (Captcha) session.getAttribute(Captcha.NAME);
        String answer = request.getParameter("answer");
        if (!captcha.isCorrect(answer))
        {
            UserCtl.logger.info("Captcha failed: loc = [" + location + "], owner=[" + sOwner + "], origCapt=["+ captcha.getAnswer() + "], entryCapt=[" + answer + "]");
            error = "Incorrect Captcha, please try again.";
        }
        else if (name == null || name.isEmpty())
        {
            error = "You must fill in the Name, please try again.";
        }
        else
        {
            int owner = Integer.parseInt(sOwner);
            fbo = new FboBean(location, owner);

            if (name != null)
                fbo.setName(name);

            if(error == null)
            {
                try
                {
                    Fbos.createFbo(fbo, user);
                    UserCtl.logger.info("FBO Constructed: loc = [" + location + "], owner=[" + sOwner + "], Name=["+ name + "]");
%>
                    <script type="text/javascript">document.location.href="fbo.jsp?id=<%=owner%>"</script>
<%
                    return;
                }
                catch (DataError e)
                {
                    error = e.getMessage();
                    UserCtl.logger.info("FBO Construct request failed: loc = [" + location + "], owner=[" + sOwner + "], Name=["+ name + "], error=[" + error + "]");
                }
            }
        }
    }

    Captcha captcha = new Captcha.Builder(200, 50)
            .addText(new nl.captcha.text.producer.NumbersAnswerProducer(8))
            .addBackground()
            .addNoise()
                    //.gimp()
            .addBorder()
            .build(); // Required.
    session.setAttribute(Captcha.NAME, captcha);
%>

<!DOCTYPE html>
<html lang="">
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
<% 	if (error != null) 
	{ 
%>	
	<div class="error"><%= error %></div>
<%	
	} 
%>
	<div class="form" style="width: 640px">
		<h2>Construct FBO at <%= location.toUpperCase() %></h2>
		<form method="post" action="fboconstruct.jsp">
	
			<div class="formgroup high">
				<img src="stickyImg" /><br/>
				To build new FBO, please enter the Captcha text below:<br/>
				<input name="answer" /><br/><br/>			
				<input type="hidden" name="location" value="<%= location.toUpperCase() %>"/>
				<input type="hidden" name="owner" value="<%= sOwner %>"/>
				FBO Name: <input name="name" type="text" class="textarea" value="<%= name != null ? name : "" %>" size="40" maxlength="255" />
				
				<input name="submit" type="submit" class="button" value="Submit"/>				
			</div>
	
		</form>
	</div>
</div>
</div>
</body>
</html>

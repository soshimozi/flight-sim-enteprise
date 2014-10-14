<%@ page
        language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="java.text.*, java.util.*, net.fseconomy.data.*"%>
<%
    Data data = (Data)application.getAttribute("data");
%>
<jsp:useBean id="user" class="net.fseconomy.data.UserBean" scope="session"></jsp:useBean>

<!DOCTYPE html>
<html xmlns="https://www.w3.org/1999/xhtml">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <% // http://ivaynberg.github.io/select2/ %>
    <link href="theme/select2.css" rel="stylesheet"/>
    <link href="theme/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
    <script src="scripts/select2.js"></script>

    <script>

        $(document).ready(function() {
            $("#modelSelect").select2({
                matcher: function(term, text, opt){
                     return text.toUpperCase().indexOf(term.toUpperCase())>=0 || opt.parent("optgroup").attr("label").toUpperCase().indexOf(term.toUpperCase())>=0
                }
            });

            $("#modelSelect").change(function(){
                  $("#modelData").load( "aircraftmodeldata.jsp?id=" + $(this).val() );
            });

        });

    </script>

</head>

<body>
<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />
<div id="wrapper">
    <div class="content">
        <div style="font-size: 14pt; font-weight: bold;">
            Select the aircraft model
        </div>
        <a href="aircraftconfigs.jsp">Open Table View</a>
        <select id="modelSelect"  style="width:300px; margin-top:40;" size="20" class="select2">
<%
    Data.MakeModel[] aircraft = data.getMakeModels();
    for(int i=0; i<aircraft.length; i++)
    {
%>
            <optgroup label="<%=aircraft[i].MakeName%>">
<%
        for(int j=0;j<aircraft[i].Models.length; j++)
        {
%>
                <option value="<%=aircraft[i].Models[j].Id%>"><%=aircraft[i].Models[j].ModelName %></option>
<%
        }
%>
            </optgroup>
<%
    }
%>
        </select>
    </div>

    <div id="modelData" class="content">
    </div>
</div>
</body>
</html>

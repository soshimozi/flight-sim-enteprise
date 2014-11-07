<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.dto.*, java.util.*, net.fseconomy.data.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    Data data = (Data)application.getAttribute("data");
%>

<!DOCTYPE html>
<html xmlns="https://www.w3.org/1999/xhtml">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <% // http://ivaynberg.github.io/select2/ %>
    <link href="css/select2.css" rel="stylesheet"/>
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

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
    List<MakeModel> makeModels = Models.getMakeModels();
    for(MakeModel makeModel : makeModels)
    {
%>
            <optgroup label="<%=makeModel.MakeName%>">
<%
        for(int j=0;j<makeModel.Models.length; j++)
        {
%>
                <option value="<%=makeModel.Models[j].Id%>"><%=makeModel.Models[j].ModelName %></option>
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

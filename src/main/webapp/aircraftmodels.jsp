<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.dto.*, java.util.*, net.fseconomy.data.*"
        %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/bootstrap.min.css" rel="stylesheet" type="text/css" />
    <link href="css/bootstrap-theme.min.css" rel="stylesheet" type="text/css" />
    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
    <script src="scripts/bootstrap.min.js"></script>

    <script>

        $(document).ready(function() {

            $("#modelSelect").change(function(){
                $("#modelData").load( "aircraftmodeldata.jsp?id=" + $(this).val() );
            });

        });

        function selectModel(modelId, modelName)
        {
            //var e = document.getElementById("dropdownSelectModel");
            //e.innerHTML = modelName + '<span class="caret"></span>';

            $("#modelData").load( "aircraftmodeldata.jsp?id=" + modelId );
        }

    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper" style="padding: 10px;">
    <div class="container">
        <div class="row clearfix">
            <div class="col-sm-6 col-md-6 col-lg-6 column  panel panel-primary" style="padding: 15px; min-width: 500px">
                <h3>
                    Aircraft Model Data
                </h3>
                <a href="aircraftconfigs.jsp">Open Table View</a>
                <div class="dropdown">
                    <button class="btn btn-default dropdown-toggle" type="button" id="dropdownSelectModel" data-toggle="dropdown" aria-expanded="true">
                        Select Aircraft Model...
                        <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu scrollable-menu" role="menu" aria-labelledby="dropdownSelectModel">
                        <%
                            List<MakeModel> makeModels = Models.getMakeModels();
                            for(MakeModel makeModel : makeModels)
                            {
                        %>
                        <li role="presentation" class="dropdown-header"><%=makeModel.MakeName%></li>
                        <%
                            for(int j=0;j<makeModel.Models.length; j++)
                            {
                        %>
                        <li role="presentation"><a role="menuitem" tabindex="-1" onclick="selectModel(<%=makeModel.Models[j].Id%>, '<%=makeModel.Models[j].ModelName %>')"><%=makeModel.Models[j].ModelName %></a></li>
                        <%
                                }
                            }
                        %>
                    </ul>
                </div>

                <div id="modelData" style="margin: 10px">
                </div>

                <div id="aliasData" style="margin: 10px">
                </div>
            </div>
            <div class="col-md-6 column">
            </div>
        </div>
    </div>
</div>
</body>
</html>

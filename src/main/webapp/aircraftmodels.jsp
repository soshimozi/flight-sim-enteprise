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
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/js/bootstrap.min.js"></script>
    <script src="scripts/bootstrap-3-typeahead.js"></script>


    <script>

        $(document).ready(function() {

            var $input = $('.typeahead');
            $input.typeahead(
                {source:
                [
<%
    List<MakeModel> makeModels = Models.getMakeModels();
    for(MakeModel makeModel : makeModels)
    {
        for(Model model: makeModel.Models)
        {
%>
                    {id: "<%=model.Id%>", name: "<%= makeModel.MakeName + " " + model.ModelName%>"},
<%
        }
    }
%>
                ],
                autoSelect: true}
            );

            $input.change(function() {
                var current = $input.typeahead("getActive");
                if (current)
                {
                    // Some item from your model is active!
                    $("#modelData").load( "aircraftmodeldata.jsp?id=" + current.id );
                }
            });

        });

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
                <div class="form-group">
                    <label>Search: </label>
                    <input type="text" class="typeahead form-control" data-provide="typeahead">
                </div>
                <div>
                    <small><a href="aircraftconfigs.jsp">Open Table View</a></small>
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

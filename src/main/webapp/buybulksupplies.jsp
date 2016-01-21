<%@page language="java"
        contentType="text/html; charset=ISO-8859-1"
        import="net.fseconomy.beans.*, net.fseconomy.data.*, net.fseconomy.util.*"
%>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session" />

<%
    if(!user.isLoggedIn())
    {
%>
<script type="text/javascript">document.location.href="../index.jsp"</script>
<%
        return;
    }

    String returnPage = request.getHeader("referer");

    int baseBulkSuppliesKg=2500;

    if (user == null || user.getId() == 0 || user.getId() == -1)
    {
%>
<script type="text/javascript">document.location.href="/index.jsp"</script>
<%
        return;
    }

    String fboID = request.getParameter("id");

    //use the account for the owner of the FBO
    FboBean fboAccount = Fbos.getFboByID(Integer.parseInt(fboID));

    //get current Supply levels
    int suppliesleft = Goods.getGoods(fboAccount.getLocation(), fboAccount.getOwner(), GoodsBean.GOODS_SUPPLIES).getAmount();
%>

<!DOCTYPE html>
<html lang="en">
<head>

    <title>FSEconomy terminal</title>

    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>

    <link href="css/Master.css" rel="stylesheet" type="text/css" />

    <script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>

    <script>
        function checkForm()
        {
            var supplies = document.getElementById("supplies");
            if(supplies.selectedIndex == 0)
            {
                alert("You have not selected a Supply amount to order!");
                return false;
            }

            return true;
        }

        $(document).ready(function() {
            $("#getFuelQuote").click(function(){
                var icao = $("#icao").val();
                var fueltype = 2; //supplies
                var amount = $("#amount").val();
                $.ajax({
                    type: "GET",
                    url: "/rest/api/fuelquote/"+fueltype+'/'+amount+'/'+icao,
                    dataType: "json",
                    success: function(response){
                        $("#fuelamt").html($("#amount").find("option:selected").text());
                        $("#fueltype").html($("#type").find("option:selected").text());
                        $("#fuelprice").html(response.data);
                    },
                    error: function(e){
                        alert('Error: ' + e);
                    }
                });
            });
        });
    </script>

</head>
<body>

<jsp:include flush="true" page="top.jsp" />
<jsp:include flush="true" page="menu.jsp" />

<div id="wrapper">
    <%
        if (Fbos.doesBulkGoodsRequestExist(fboAccount.getId(), Fbos.FBO_ORDER_SUPPLIES) )
        {
    %>
    <div class="form" style="width: 500px">
        <h5>A request for bulk supplies for <span style="color: blue;"><%=fboAccount.getLocation() %></span> was made on <span style="color: blue;"><%=Formatters.dateyyyymmddhhmmzzz.format(fboAccount.getBulkSupplyOrderTimeStamp())%> for <%=fboAccount.getBulkSuppliesOrdered() %> Kg of Supplies.</span></h5>
        <%
            if (fboAccount.getBulkSupplyDeliveryDateTime() != null)
            {
        %>
        <h5>Delivery is scheduled for <span style="color: blue;"><%=Formatters.dateyyyymmddhhmmzzz.format(fboAccount.getBulkSupplyDeliveryDateTime()) %></span> </h5> <%
        }
    %>
        <h5>You can only make bulk supply requests every 24 hours or until your order has been delivered.  Please try again later.</h5>
    </div>
    <%
    }
    else
    {
    %>
    <div class="form" style="width: 500px">
        <form method="post" action="buybulksuppliesconfirm.jsp"  onsubmit="return checkForm()">
            <div>
                <input type="hidden" name="returnpage" value="<%=returnPage%>"/>
            </div>
            <div>
                <h2>Bulk Supply Order</h2>
                You currently have the following Supplies at <span style="color: blue;"> <%=fboAccount.getLocation() %>:</span><br/>
                <ul>
                    <li><span style="color: blue;"><%= suppliesleft %> Kg </span></li>
                </ul>
                <br/>
                <h3>Specify the quantity supplies you want to order</h3>
                Amount in Kg:
                <select id="supplies" name="amount">
                    <%
                        int i;
                        for (i=0; i<=4; i++)
                        {
                    %>	<option value="<%=baseBulkSuppliesKg*i %>"><%=baseBulkSuppliesKg*i %> Kg</option>
                    <%
                        }
                    %>
                </select>
                <input type="submit" class="button" value="Submit" />
                <h5>Note, once you click the submit button, you will be given an estimated  delivery date between
                    <%=GoodsBean.GOODS_ORDER_SUPPLIES_MIN%> and <%=GoodsBean.GOODS_ORDER_SUPPLIES_MAX%> days.
                    You can accept or decline the offer.
                    If you accept the offer, you will pay for your order immediately, and you cannot re-order at this location until delivery is complete.
                    If you decline the offer, you cannot return to this screen for 24 hours.</h5>
                <h5><span style="color: red;">Please be sure you want to proceed before clicking submit!</span></h5>
                <input type="hidden" name="id" value="<%=fboID %>" />
            </div>
        </form>
    </div>
    <div class="form" style="width: 500px">
        <form>
            <div>
                <h2>Price Quote</h2>
                <h3><i>No goods or money will be transferred</i></h3>
                Supply Amount:
                <select id="amount">
                    <%
                        //loop through adding the values in the drop down for quantities
                        for (i=1; i<=4; i++)
                        {
                    %>
                    <option value="<%=baseBulkSuppliesKg*i %>"><%=baseBulkSuppliesKg*i %> Kg</option>
                    <%
                        }
                    %>
                </select><br/>
                <h5>The cost for <span id="fuelamt">0</span> of supplies equals <span id="fuelprice"></span></h5>
                <input type="button" id="getFuelQuote" class="button" value="Get Quote"/>
                <input type="hidden" id="icao" value="<%= fboAccount.getLocation() %>"/>
            </div>
        </form>
    </div>
    <%
        }
    %>
</div>
</body>
</html>
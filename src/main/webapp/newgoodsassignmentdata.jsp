<%@ page import="net.fseconomy.data.Goods" %>
<%@ page import="net.fseconomy.util.Helpers" %>
<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    String fromIcao = request.getParameter("fromicao");
    String sCommodityId = request.getParameter("commodityid");
    String sOwnerId = request.getParameter("ownerid");
    String sGroupId = request.getParameter("groupid");
    String commodityName = Goods.commodities[Integer.parseInt(sCommodityId)].getName();
%>

<div class="modal-header alert-info">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
    <h4 class="modal-title">Create Goods Transfer Assignment (<%=commodityName%>)</h4>
</div>
<div class="modal-body">
    <form class="form-horizontal" id="formNewAssignmentModal" method="post" action="userctl">
        <input type="hidden" name="event" value="newGoodsAssignment"/>
        <input type="hidden" name="ownerid" value="<%=sOwnerId%>"/>
<%
    if(!Helpers.isNullOrBlank(sGroupId))
    {
%>
        <input type="hidden" name="groupid"  value="<%=sGroupId%>"/>
<%
    }
%>
        <input type="hidden" name="fromicao" value="<%=fromIcao%>"/>
        <input type="hidden" name="commodityid" value="<%=sCommodityId%>"/>
        <input type="hidden" name="returnpage" value=""/>

        <div class="panel panel-primary">
            <div class="form-group">
                <label class="col-sm-3 control-label">From</label>
                <div class="col-sm-2">
                    <p class="form-control-static"><%=fromIcao%>
                    </p>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-3 control-label" for="toicao">To</label>
                <div class="col-sm-4">
                    <input class="form-control" id="toicao" name="toicao" type="text" value="" size="4" maxlength="4"/>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-3 control-label">Amount (Kg)</label>
                <div class="col-sm-4">
                    <input class="form-control" id="amount" name="amount" type="text" value=""
                           size="8" maxlength="8"/>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-3 control-label" for="pay">Pay (each)</label>
                <div class="col-sm-4">
                    <input class="form-control" id="pay" name="pay" type="text" value="0" size="8"
                           maxlength="8"/>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-3 control-label" for="numtocreate"># to Create</label>
                <div class="col-sm-4">
                    <input class="form-control" id="numtocreate" name="numtocreate" type="text" value="1" size="8"
                           maxlength="8"/>
                </div>
            </div>
        </div>

    </form>
</div>

<div class="modal-footer">
    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
    <button type="button" class="btn btn-primary" onclick="newAssignment();">Add</button>
</div>

<script>

    function newAssignment() {
        var form = document.getElementById("formNewAssignmentModal");
        form.event.value = 'newGoodsAssignment';
        form.submit();
    }

    function newGoodsAssignmentInit(returnUrl) {
        var form = document.getElementById("formNewAssignmentModal");
        form.returnpage.value = returnUrl;
    }

</script>


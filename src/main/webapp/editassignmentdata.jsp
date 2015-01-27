<%@ page import="net.fseconomy.beans.AssignmentBean" %>
<%@ page import="net.fseconomy.data.Assignments" %>
<%@ page import="net.fseconomy.data.Groups" %>
<%@ page import="net.fseconomy.beans.UserBean" %>
<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
    String sAssignmentId = request.getParameter("assignmentid");
    if (sAssignmentId == null)
    {
        return;
    }
    AssignmentBean assignment = Assignments.getAssignmentById(Integer.parseInt(sAssignmentId));

%>

<div class="modal-header alert-success">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
<%
    if(assignment.isGoods())
    {
%>
    <h4 class="modal-title">Edit Goods Assignment (<%=assignment.getCommodity()%>)</h4>

<%
    }
    else
    {
%>
    <h4 class="modal-title">Edit Assignment</h4>
<%
    }
%>
</div>
<div class="modal-body">
    <form class="form-horizontal" id="formEditAssignmentModal" method="post" action="userctl">
        <input type="hidden" name="event" value="<%=assignment.isGoods() ? "updateGoodsAssignment" : "updateAssignment"%>"/>
        <input type="hidden" name="returnpage" value=""/>
        <input id="groupid" name="groupid" type="hidden" value="<%= assignment.getGroupId() %>">
        <input id="ownerid" name="ownerid" type="hidden" value="<%= assignment.getOwner() %>">
        <input id="assignmentid" name="assignmentid" type="hidden" value="<%= assignment.getId() %>">
        <div class="panel panel-primary">
            <div class="form-group">
                <label class="col-sm-2 control-label">From</label>
                <div class="col-sm-2">
                    <p class="form-control-static"><%= assignment.getFrom() %></p>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">To</label>
                <div class="col-sm-2">
                    <p class="form-control-static"><%= assignment.getTo() %></p>
                </div>
            </div>
<%
    if(assignment.isGoods())
    {
%>
            <div class="form-group">
                <label class="col-sm-2 control-label">Amount</label>
                <div class="col-sm-2">
                    <input class="form-control" id="editAmount" name="amount" type="text"
                           value="<%= assignment.getAmount() %>" size="10"/>
                </div>
            </div>
<%
    }
    else
    {
%>
            <div class="form-group">
                <label class="col-sm-2 control-label">Amount</label>
                <div class="col-sm-2">
                    <p class="form-control-static"><%= assignment.getAmount() %></p>
                </div>
            </div>
<%
    }
%>
            <div class="form-group">
                <label class="col-sm-2 control-label" for="editPilotFee">Pilot Fee</label>
                <div class="col-sm-2">
                    <input class="form-control" id="editPilotFee" name="pilotfee" type="text" value="<%= assignment.getPilotFee() %>" size="10"/>
                </div>
                <div class="col-sm-5">
                * - Blank = Group default pay.
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label" for="editComment">Comment</label>
                <div class="col-sm-8">
                    <input class="form-control" id="editComment" name="comment" type="text" value="<%= assignment.getComment() %>" size="45"/>
                </div>
            </div>
        </div>
    </form>
</div>
<div class="modal-footer">
    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
    <button type="button" class="btn btn-primary" onclick="updateAssignment();">Update</button>
<%
    boolean isferry = assignment.isFerry();
    boolean isstaff = user.getId() >= UserBean.GROUP_STAFF;
    if(isferry && isstaff)
    {
%>
    <button type="button" class="btn btn-primary" onclick="deleteGroupAssignment();">Delete</button>
<%
    }
%>
</div>
<script>

    function updateAssignment() {
        var form = document.getElementById("formEditAssignmentModal");
        form.returnpage.value = returnUrl;
        form.submit();
    }

    function deleteGroupAssignment() {
        var form = document.getElementById("formEditAssignmentModal");
        form.returnpage.value = returnUrl;
        form.event.value = 'deleteAssignment';
        form.submit();
    }

</script>


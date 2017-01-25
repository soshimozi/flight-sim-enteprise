<%@page language="java" contentType="text/html; charset=ISO-8859-1" %>

<jsp:useBean id="user" class="net.fseconomy.beans.UserBean" scope="session"/>

<%
%>

<div class="modal-header alert-info">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
    <h4 class="modal-title">New Assignment</h4>
</div>
<div class="modal-body">
    <form class="form-horizontal" id="formNewAssignmentModal" method="post" action="userctl">
        <input type="hidden" name="event" value="updateAssignment"/>
        <input type="hidden" name="returnpage" value=""/>
        <input id="groupid" name="groupid" type="hidden" value="">

        <div class="panel panel-primary">
            <div class="form-group">
                <label class="col-sm-2 control-label" for="fromicao">From</label>
                <div class="col-sm-2">
                    <input class="form-control" id="fromicao" name="fromicao" type="text" class="textarea" value="" size="4" maxlength="4"/>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label" for="toicao">To</label>
                <div class="col-sm-2">
                    <input class="form-control" id="toicao" name="toicao" type="text" class="textarea" value="" size="4" maxlength="4"/>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label">Amount</label>
                <div class="col-sm-2">
                    <p class="form-control-static">0</p>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label" for="editPilotFee">Pilot Fee</label>
                <div class="col-sm-3">
                    <input class="form-control" id="editPilotFee" name="pilotfee" type="text" value="0.00" size="10" maxlength="10"/>
                </div>
            </div>
            <div class="form-group">
                <label class="col-sm-2 control-label" for="editComment">Comment</label>
                <div class="col-sm-8">
                    <input class="form-control" id="editComment" name="comment" type="text" value="" size="45" maxlength="200"/>
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
        form.event.value = 'newAssignment';
        form.submit();
    }

    function newAssignmentInit(groupId, returnUrl) {
        var form = document.getElementById("formNewAssignmentModal");
        form.groupid.value = groupId;
        form.returnpage.value = returnUrl;
    }

</script>


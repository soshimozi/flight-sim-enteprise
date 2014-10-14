<%@ page language="java"
         contentType="text/html; charset=ISO-8859-1"
%>
<% response.setContentType("text/javascript");%>
$(function() {
	$('#location').blur(function(){
		$('#constructFboForm').bind('submit',function(){
			$('#constructFboForm').attr('action', 'fboconstruct.jsp');
			this.submit();
    	});
    });
});

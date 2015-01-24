/*!
 * Extract dates using popular natural language date parsers
 */
/*jshint jquery:true */
;(function($){
"use strict";

	$.tablesorter.addParser({
		  id: 'checkbox',
		  is: function(s) {
		    return false;
		  },
		  format: function(s, table, cell, cellIndex) {
		    var $c = $(cell);
		    // return 1 for true, 2 for false, so true sorts before false
		    if (!$c.hasClass('updateCheckbox')) {
		      $c.addClass('updateCheckbox').bind('change', function() {
		        // resort the table after the checkbox status has changed
		        var resort = false;
		        $(table).trigger('updateCell', [cell, resort]);
		      });
		    }
		    var cb = $c.find('input[type=checkbox]')[0];
			if(cb == undefined)
				return 2;

		    return (cb.checked) ? 1 : cb.disabled ? 3 : 2;
		  },
		  type: 'numeric'
		});
	
})(jQuery);




/*!
 * Extract dates using popular natural language date parsers
 */
/*jshint jquery:true */
;(function($){
"use strict";

	$.tablesorter.addParser({
		id: "timeHrMin",
		is: function(s) {
			return (/^([0-9]*:[05][0-9])$/).test(s);
		},
		format: function(s, table) {
			var parts = s.split(":");
			var hrs = parseInt(parts[0]);
			var mins = parseInt(parts[1]);
			var fracHr = hrs + mins/60.0;
			return fracHr;
		},
		type: "numeric"
	});

})(jQuery);

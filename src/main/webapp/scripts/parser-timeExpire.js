/*!
 * Extract dates using popular natural language date parsers
 */
/*jshint jquery:true */
;(function($){
"use strict";

	$.tablesorter.addParser({
		id: "timeExpire",
		is: function(s) {
			false;
		},
		format: function(s, table) {
			var parts = s.split(" ");
			var amt = parts[0];
			var units = parts[1];
			if(amt.indexOf("expired") != -1){
				return 0.0;
			}
			else if(amt.indexOf("never") == -1){
				var multiplier = 1.0;
				if(units.indexOf("day") != -1){
					multiplier = 86400.0; //60.0 * 60.0 * 24.0;
				}
				else if(units.indexOf("hour") != -1){
					multiplier = 3600.0; //60.0 * 60.0;					
				}
				else if(units.indexOf("minute") != -1){
					multiplier = 60.0;
				}
				return amt * multiplier;
			}
			else
				return Number.MAX_VALUE;
				
		},
		type: "numeric"
	});

})(jQuery);
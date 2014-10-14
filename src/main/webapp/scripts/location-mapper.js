$(document).ready( function () {
	// Some default values
	var _settings = {
		MAP_CANVAS: 'map_canvas',
		DEFAULT_ICON: 'http://maps.google.com/mapfiles/kml/pal2/icon50.png',
		LOC_DEFAULTS: {
			latl: 43,
			lonl: -79,
			status: '_default'
		},
		FANCYBOX_SRC: 'fancybox2/jquery.fancybox.pack.js',
		FANCYBOX_STYLESHEET_HREF: 'fancybox/jquery.fancybox.css',
		MAP_CENTER: {
			latl: (typeof mapCenter === 'undefined' ? 43 : mapCenter.latl),
			lonl: (typeof mapCenter === 'undefined' ? -79 : mapCenter.lonl)
		},
		MAP_TYPE_ID: google.maps.MapTypeId.ROADMAP,
		MAP_ZOOM: 8,
		LIGHTBOX_BUTTON: 'show-map'
	}

	// Import dependencies
	//$.getScript(_settings.FANCYBOX_SRC);
	//$.getScript('http://maps.google.com/maps/api/js?sensor=false');
	
	// Some defaults for the round 32x32 icons.
	var icoSize = new google.maps.Size(32, 32, 'px', 'px');
	var icoOrigin = new google.maps.Point(0, 0);
	var icoAnchor = new google.maps.Point(16, 16);
	
	// Object containing possible Marker images, hashed by flight "status"
	var images = {
		_default: _settings.DEFAULT_ICON,
		plane: new google.maps.MarkerImage('http://maps.google.com/mapfiles/kml/pal2/icon48.png',
			icoSize,
			icoOrigin,
			icoAnchor
		),
		selected: 'http://google-maps-icons.googlecode.com/files/airport.png',
		enroute: 'http://google-maps-icons.googlecode.com/files/airport.png',
		planejob: 'http://google-maps-icons.googlecode.com/files/airport-runway.png',
		dest: new google.maps.MarkerImage('http://maps.google.com/mapfiles/kml/pal2/icon56.png',
			icoSize,
			icoOrigin,
			icoAnchor
		)
		
		// MarkerImage(url:string, size?:Size, origin?:Point, anchor?:Point, scaledSize?:Size)
		// Size(width:number, height:number, widthUnit?:string, heightUnit?:string)
		// Point(x:number, y:number)
	}

	var drawMap = function () {				
		var _map = new google.maps.Map(document.getElementById(_settings.MAP_CANVAS), {
			center: new google.maps.LatLng(_settings.MAP_CENTER.latl, _settings.MAP_CENTER.lonl),
			mapTypeId: _settings.MAP_TYPE_ID,
			zoom: _settings.MAP_ZOOM
		});
		
		var drawLines = !!document.getElementById('draw-lines').checked;
		
		for (x in loc) {
			// Set defaults for any possibly unassigned properties
			$.extend(_settings.LOC_DEFAULTS, loc[x][0]);
			var point = [];
			var windowContent = [];
			
			if (loc[x][0].latl != 0 && loc[x][0].lonl != 0) {
				if (x == 'plane') {
					var latLon = new google.maps.LatLng(loc[x][0].latl, loc[x][0].lonl);
					if (typeof loc[loc[x][0].icao] != 'undefined') {
						var point = new google.maps.Marker({
							map: _map,
							position: latLon,
							icon: images['planejob'],
							title: loc[x][0].icao,
							zIndex: 1000,
							clickable: false
						});
					} else {
						var point = new google.maps.Marker({
							map: _map,
							position: latLon,
							icon: images['plane'],
							title: 'Plane located at ' + loc[x][0].icao,
							zIndex: 1000
						});
					}
				} else {
					var latLon = new google.maps.LatLng(loc[x][0].latl, loc[x][0].lonl);
					var iconImage = images[loc[x][0].status.replace(/\s/g,'').toLowerCase()];
					
					// The following two lines define the table which appears in the InfoWindow, when you click on an assignment
					// Try to make sure that there are the same number of <th>s as there are <td>s below (see *)
					windowContent[x] = "<p>Assignments at <strong>" + x + "</strong></p>";
					windowContent[x] += "<table border='1' cellpadding='4' style='text-align:center;font-size:10px;'>";
					windowContent[x] += "<tr><th>ICAO</th><th>Cargo</th><th>Pay</th><th>NM</th></tr>";
					
					for (var i = 0; i< loc[x].length; i++) {
						var place = loc[x][i]; 
						// alert("from: " + place.latl + ", " + place.lonl + "\nto: " + place.dest.latl + ", " + place.dest.lonl);
						var destLatLon = new google.maps.LatLng(place.dest.latl, place.dest.lonl);

						// * (as referenced above), these are the actual rows of data.
						windowContent[x] += "<tr><td>" + place.dest.icao + "</td><td>" + place.cargo + "</td><td>" + place.pay + "</td><td>" + place.dist + "NM</td></tr>";
						
						var destPoint = new google.maps.Marker({
							map: _map,
							position: destLatLon,
							icon: images['dest'],
							title: place.dest.icao
						});
						
						if (drawLines) {
							var line = new google.maps.Polyline({
								map: _map,
								path: [latLon, destLatLon],
								geodesic: true
							});
						}
					}
					
					windowContent[x] += "</table>";
					
					point = new google.maps.Marker({
						map: _map,
						position: latLon,
						icon: iconImage,
						title: x,
						zIndex: 999
					});
					//alert(windowContent);
					(function(){
						var infoWindow = new google.maps.InfoWindow({
							content: windowContent[x]
						});
						google.maps.event.addListener(point, 'click', function() {
							infoWindow.open(_map,this);
						});
					})();
					//infoWindow.open(_map,point);
				}
			}
			else {
				var latLon = new google.maps.LatLng(loc[x][0].latl, loc[x][0].lonl);
				var point = new google.maps.Marker({
					map: _map,
					position: latLon,
					icon: images.plane,
					title: x
				});
			}
		}
	}
	
	// Attach click event for lightbox
	$("#" + _settings.LIGHTBOX_BUTTON).fancybox({
		'hideOnContentClick': false,
		onComplete: drawMap,
		autoDimensions: false,
		height: 750,
		width: 900
	});
	$('#draw-lines').click(function(){
		drawMap();
	});
});
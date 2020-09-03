var requestUtils={
	'getParamValue':function(paramName){		
			let value= (location.search.split(paramName + '=')[1]||'').split('&')[0];
			if (value && value.length > 0){
				return value;
			}
			return null;
		},
	'isIOS':function(){
		return /(iPad|iPhone|iPod)/g.test( navigator.userAgent );
	},
	'redirect':function(newUrl){
		document.location = newUrl;
	}
};

var goMap = {
	'config':{
		'containerid': 'gocontainer',		
		'defaults':{
			'lat':50.27,
			'lon':30.30,
			'zoom':13
			}
	},
	'utils':{
		'getPointFromUrl':function(){
			let point = {};
			point.lat = requestUtils.getParamValue('lat');
			point.lon = requestUtils.getParamValue('lon');
			point.zoom = requestUtils.getParamValue('z');
			return point;
		},
		'isPointComplete':function(point){		
			if (!point.lat || !point.lon){
				return false;
			}
			return true;
		},
		'extendPoint':function(initialPoint, newPoint){
			let point={};
			point.lat=newPoint.lat;
			if (!point.lat || point.lat == null){
				point.lat = initialPoint.lat;
			}
			point.lon=newPoint.lon;
			if (!point.lon || point.lon == null){
				point.lon = initialPoint.lon;
			}
			point.zoom=newPoint.zoom;
			if (!point.zoom || point.zoom == null){
				point.zoom = initialPoint.zoom;
			}
			return point;
		}
	},
	'init': function(config){
		if (config && typeof (config) == 'object') {
            $.extend(goMap.config, config);
        }
		goMap.$container = $('#' + goMap.config.containerid);
		goMap.$footer = goMap.$container.find('.gofooter');
		goMap.$latitude = goMap.$container.find('.latitude');
		goMap.$longitude = goMap.$container.find('.longitude');
		
		let inputPoint = goMap.utils.getPointFromUrl();					
		goMap.point = goMap.utils.extendPoint(goMap.config.defaults, inputPoint);
		goMap.refreshCoordinates();
		
		goMap.map =$.mapwidget();		
		goMap.map.showPoint(goMap.point);
		
		let inputComplete = goMap.utils.isPointComplete(inputPoint);
		if (inputComplete){
			goMap.map.addMarker(goMap.point);
		}
		goMap.point = goMap.utils.getPointFromUrl();	
	},	
	'refreshCoordinates':function(){
		goMap.$latitude.text(goMap.point.lat);
		goMap.$longitude.text(goMap.point.lon);
	}
};

function toColor(num) {
            num >>>= 0;
            var b = num & 0xFF,
                g = (num & 0xFF00) >>> 8,
                r = (num & 0xFF0000) >>> 16,
                a = ( (num & 0xFF000000) >>> 24 ) / 255 ;
            return "rgba(" + [r, g, b, a].join(",") + ")";
         }

(function($) {
	$.mapwidget = function(config) {
		var loc = goMap.point.lat + '/' + goMap.point.lon;
		var lparams = '?mlat='+goMap.point.lat + '&mlon=' + goMap.point.lon;
		var mapobj={
			config: $.extend({
				'mapid':'map',
                'maxzoom':20,
				'maxnativezoom':19,
				'sourceurl':'https://tile.osmand.net/hd/{z}/{x}/{y}.png',
				'attribution':'&copy; <a href="https://www.openstreetmap.org/'+lparams+'#map=15/'+loc+'">OpenStreetMap</a> contributors'
            }, config),
			init:function(){
				mapobj.map = L.map(mapobj.config.mapid);
				L.tileLayer(mapobj.config.sourceurl, {
					attribution: mapobj.config.attribution,
					maxZoom: mapobj.config.maxzoom,
					maxNativeZoom: mapobj.config.maxnativezoom
				}).addTo(mapobj.map);				
			},
			showPoint:function(point){
				mapobj.map.setView([point.lat, point.lon], point.zoom);				
			},
			addMarker:function(point){
				L.marker([point.lat, point.lon]).addTo(mapobj.map);
			},
			addPopupMarker:function(favorite,onClickEvent){
			    window.point = favorite;
                var point = {};
		    	    point.lat = favorite.latitude;
		    	    point.lon = favorite.longitude;
			    var popup = L.popup().setContent(
			                "name: <b>" + favorite.name + "</b><br/>" +
			                "address: <i>" + favorite.address + "</i><br/>"
			                 + "category: " + favorite.category);
				var customMarker = L.AwesomeMarkers.icon({
					icon: 'home',
					markerColor: toColor(favorite.color),
					iconColor: toColor(favorite.color)
				});
				L.marker([point.lat, point.lon],{icon: customMarker})
			        .bindPopup(popup)
			        .addTo(mapobj.map)
			        .on('click', function(e) {
                        onClickEvent(e);
                    });
			}
		};
		mapobj.init();
		return {            
            showPoint: mapobj.showPoint,
            addMarker: mapobj.addMarker,
            addPopupMarker: mapobj.addPopupMarker
        };
	};
})(jQuery);

(function($) {
	$.timer=function(config){
		var timerobj={
			config: $.extend({
				'timeoutInMs':300,
                'maxActionDelayInMs':2000,
				'action':function(){},
				'actionparams':null
            }, config),
			init:function(){
				timerobj.timer = null;
				timerobj.startDate = null;
			},
			start:function(){
				timerobj.cancel();
				timerobj.startDate = new Date();
				timerobj.timer=setTimeout(timerobj.onTimer, timerobj.config.timeoutInMs);
			},
			cancel:function(){
				if (timerobj.timer != null){
					clearTimeout(timerobj.timer);
					timerobj.timer = null;
					timerobj.startDate = null;
				}
			},
			onTimer:function(){
				timerobj.timer= null;
				let now = new Date();
				if(now - timerobj.startDate < timerobj.config.maxActionDelayInMs){
					timerobj.config.action(timerobj.config.actionparams);
				}				
			}
		};
		timerobj.init();
		return {
			start:timerobj.start,
			cancel:timerobj.cancel
		};
	};
})(jQuery);

var iosAppRedirect = {
	config:{
		appPrefix:'osmandmaps://',
		containerid:'gocontainer',
		cookieName:'OsmAndInstalled',
		cookieNoExpirationTimeoutInDays:30
	},
	init:function(config){
		if (config && typeof (config) == 'object') {
            $.extend(iosAppRedirect.config, config);
        }
		
		if (!requestUtils.isIOS()){	
			return;
		}
		iosAppRedirect.$container = $('#' + iosAppRedirect.config.containerid);
		iosAppRedirect.$overlay = iosAppRedirect.$container.find('.overlay');
		iosAppRedirect.$popup = iosAppRedirect.$container.find('.popup');
		iosAppRedirect.$yesBtn =  iosAppRedirect.$container.find('.yes');
		iosAppRedirect.$noBtn =  iosAppRedirect.$container.find('.no');
		iosAppRedirect.$cancelBtn =  iosAppRedirect.$container.find('.cancel');
		iosAppRedirect.applestorelink = iosAppRedirect.$container.find('.gobadges .apple a').attr('href');	
		iosAppRedirect.applink = iosAppRedirect.config.appPrefix + document.location.search;	
		
				
		if (iosAppRedirect.isAppInstalled() === "yes"){			
			iosAppRedirect.redirectToApp();			
			return;
		}
		if (iosAppRedirect.isAppInstalled() === "no"){
			return;
		}
		
		iosAppRedirect.$yesBtn.on('click', function(){		    
			iosAppRedirect.redirectToApp();
			iosAppRedirect.closePopup();
		});
		
		iosAppRedirect.$noBtn.on('click', function(){
			iosAppRedirect.setCookie(true);
			iosAppRedirect.closePopup();
			window.open(iosAppRedirect.applestorelink , '_blank');
		});
		
		iosAppRedirect.$cancelBtn.on('click', function(){
			iosAppRedirect.setCookie(false);
			iosAppRedirect.closePopup();			
		});
		iosAppRedirect.openPopup();
	},
	isAppInstalled:function(){
		return Cookies.get('OsmAndInstalled');		
	},
	redirectToApp:function(){
		iosAppRedirect.timer = $.timer({action:iosAppRedirect.clearCookie});
		iosAppRedirect.timer.start();
		requestUtils.redirect(iosAppRedirect.applink);
	},
	setCookie:function(appInstalled){
		if (appInstalled === true){
			Cookies.set(iosAppRedirect.config.cookieName, "yes");
		}else{
			Cookies.set(iosAppRedirect.config.cookieName, "no", { expires: iosAppRedirect.config.cookieNoExpirationTimeoutInDays });
		}
	},	
	clearCookie:function(){
		Cookies.remove('OsmAndInstalled'); 
	},
	openPopup:function(){
		iosAppRedirect.$overlay.show();
		iosAppRedirect.$popup.show();
	},
	closePopup:function(){
		iosAppRedirect.$overlay.hide();
		iosAppRedirect.$popup.hide();
	}
};

 $( document ).ready(function() {
    goMap.init();
	iosAppRedirect.init();
  });

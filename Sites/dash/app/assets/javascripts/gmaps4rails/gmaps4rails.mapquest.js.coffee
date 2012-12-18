#######################################################################################################
##############################################  Map Quest #############################################
#######################################################################################################
# http://developer.mapquest.com/web/documentation/sdk/javascript/v7.0/api/MQA.Poi.html

class @Gmaps4RailsMapquest extends Gmaps4Rails

  constructor: ->
    super
    #Map settings
    @map_options  = {type:  "map"}  #map type (map, sat, hyb)
    @markers_conf = {}
    @mergeWithDefault "markers_conf"
    @mergeWithDefault "map_options"

  #////////////////////////////////////////////////////
  #/////////////// Basic Objects         //////////////
  #////////////////////////////////////////////////////

  createPoint: (lat, lng) ->
    return new MQA.Poi({lat: lat, lng: lng})

  createLatLng: (lat, lng) ->
    return {lat: lat, lng: lng}

  createLatLngBounds: ->

  createMap: ->
    map = new MQA.TileMap(                        #// Constructs an instance of MQA.TileMap
      document.getElementById(@map_options.id),   #//the id of the element on the page you want the map to be added into 
      @map_options.zoom,                          #//intial zoom level of the map
      {lat: @map_options.center_latitude, lng: @map_options.center_longitude}, 
      @map_options.type)                          #//map type (map, sat, hyb)
    
    MQA.withModule('zoomcontrol3', (->
      map.addControl(
        new MQA.LargeZoomControl3(), 
        new MQA.MapCornerPlacement(MQA.MapCorner.TOP_LEFT)
      )
    ))
    return map

  createMarkerImage: (markerPicture, markerSize, origin, anchor, scaledSize) ->

  #////////////////////////////////////////////////////
  #////////////////////// Markers /////////////////////
  #////////////////////////////////////////////////////

  createMarker: (args)->
    marker = new MQA.Poi( {lat: args.Lat, lng: args.Lng} )

    if args.marker_picture != ""  
      icon = new MQA.Icon(args.marker_picture, args.marker_height, args.marker_width)
      marker.setIcon(icon)
      if args.marker_anchor != null
        marker.setBias({x: args.marker_anchor[0], y: args.marker_anchor[1]})

    if args.shadow_picture != "" 
      icon = new MQA.Icon(args.shadow_picture, args.shadow_height, args.shadow_width)
      marker.setShadow(icon)

      if args.shadow_anchor != null
        marker.setShadowOffset({x: args.shadow_anchor[0], y: args.shadow_anchor[1]})

    @addToMap marker
    return marker


  #// clear markers
  clearMarkers: ->
    for marker in markers
      @clearMarker marker

  #//show and hide markers
  showMarkers: ->
    for marker in markers
      @showMarker marker

  hideMarkers: ->
    for marker in markers
      @hideMarker marker

  clearMarker: (marker) ->
    @removeFromMap(marker.serviceObject)

  showMarker: (marker) ->
    #// marker.serviceObject

  hideMarker: (marker) ->
    #// marker.serviceObject

  extendBoundsWithMarkers: ->
    if @markers.length >=2
      @boundsObject = new MQA.RectLL(@markers[0].serviceObject.latLng, @markers[1].serviceObject.latLng)
      for marker in @markers
        @boundsObject.extend marker.serviceObject.latLng

  #////////////////////////////////////////////////////
  #/////////////////// Clusterer //////////////////////
  #////////////////////////////////////////////////////

  createClusterer: (markers_array) ->

  clearClusterer: ->

  #//creates clusters
  clusterize: ->

  #////////////////////////////////////////////////////
  #/////////////////// INFO WINDOW ////////////////////
  #////////////////////////////////////////////////////

  #// creates infowindows
  createInfoWindow: (marker_container) ->
    marker_container.serviceObject.setInfoTitleHTML(marker_container.description)
    #//TODO: how to disable the mouseover display when using setInfoContentHTML?
    #//marker_container.serviceObject.setInfoContentHTML(marker_container.description);

  #////////////////////////////////////////////////////
  #/////////////////// Other methods //////////////////
  #////////////////////////////////////////////////////

  fitBounds: ->
    @serviceObject.zoomToRect @boundsObject if @markers.length >=2
    @serviceObject.setCenter @markers[0].serviceObject.latLng if @markers.length == 1 

  centerMapOnUser: -> 
    @serviceObject.setCenter @userLocation
    
  addToMap: (object) ->
    @serviceObject.addShape object

  removeFromMap: (object)->
    @serviceObject.removeShape object
    
  updateBoundsWithPolylines: ()->

  updateBoundsWithPolygons: ()->

  updateBoundsWithCircles: ()->
    
  extendMapBounds :->

  adaptMapToBounds: ->
    @fitBounds()
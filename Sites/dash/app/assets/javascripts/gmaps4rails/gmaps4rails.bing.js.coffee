######################################################################################################
##############################################  Bing Maps  ##########################################
######################################################################################################

#// http://wiki.openstreetmap.org/wiki/OpenLayers
#// http://openlayers.org/dev/examples
#//http://docs.openlayers.org/contents.html

class @Gmaps4RailsBing extends Gmaps4Rails

  constructor: ->
    super
    @map_options =
      type:  "road"   # aerial, auto, birdseye, collinsBart, mercator, ordnanceSurvey, road
    @markers_conf =
      infobox:  "description" #description or htmlContent

    @mergeWithDefault("map_options")
    @mergeWithDefault("markers_conf")

  #////////////////////////////////////////////////////
  #/////////////// Basic Objects         //////////////
  #////////////////////////////////////////////////////

  getMapType: -> 
    switch @map_options.type
      when "road"           then return Microsoft.Maps.MapTypeId.road
      when "aerial"         then return Microsoft.Maps.MapTypeId.aerial
      when "auto"           then return Microsoft.Maps.MapTypeId.auto
      when "birdseye"       then return Microsoft.Maps.MapTypeId.birdseye
      when "collinsBart"    then return Microsoft.Maps.MapTypeId.collinsBart
      when "mercator"       then return Microsoft.Maps.MapTypeId.mercator
      when "ordnanceSurvey" then return Microsoft.Maps.MapTypeId.ordnanceSurvey
      else return Microsoft.Maps.MapTypeId.auto

  createPoint: (lat, lng) ->
    return new Microsoft.Maps.Point(lat, lng)

  createLatLng:(lat, lng) ->
    return new Microsoft.Maps.Location(lat, lng)
  
  createLatLngBounds: ->

  createMap: ->
    return new Microsoft.Maps.Map(document.getElementById(@map_options.id), { 
      credentials:  @map_options.provider_key,
      mapTypeId:    @getMapType(),
      center:       @createLatLng(@map_options.center_latitude, @map_options.center_longitude),
      zoom:         @map_options.zoom
    })

  createSize: (width, height) ->
    return new google.maps.Size(width, height)

  #////////////////////////////////////////////////////
  #////////////////////// Markers /////////////////////
  #////////////////////////////////////////////////////

  createMarker: (args) ->
    markerLatLng = @createLatLng(args.Lat, args.Lng) 
    marker
    #// Marker sizes are expressed as a Size of X,Y
    if args.marker_picture == ""  
      marker = new Microsoft.Maps.Pushpin(@createLatLng(args.Lat, args.Lng), {
        draggable: args.marker_draggable,
        anchor:    @createImageAnchorPosition(args.Lat, args.Lng),
        text:      args.marker_title
        }
      );
    else 
      marker = new Microsoft.Maps.Pushpin(@createLatLng(args.Lat, args.Lng), {
        draggable: args.marker_draggable,
        anchor:    @createImageAnchorPosition(args.Lat, args.Lng),
        icon:      args.marker_picture,
        height:    args.marker_height,
        text:      args.marker_title,
        width:     args.marker_width
        }
      );
    @addToMap(marker)
    return marker

  #// clear markers
  clearMarkers: ->  
    for marker in @markers
      @clearMarker marker

  clearMarker: (marker) ->
    @removeFromMap(marker.serviceObject)

  #//show and hide markers
  showMarkers: ->
    for marker in @markers
      @showMarker marker

  showMarker: (marker) ->
    marker.serviceObject.setOptions({ visible: true })

  hideMarkers: ->
    for marker in @markers
      @hideMarker marker

  hideMarker: (marker) ->
    marker.serviceObject.setOptions({ visible: false })

  extendBoundsWithMarkers: ->
    locationsArray = []
    for marker in @markers
      locationsArray.push(marker.serviceObject.getLocation())
    @boundsObject = Microsoft.Maps.LocationRect.fromLocations(locationsArray)

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
    if marker_container.description?
      #//create the infowindow
      if @markers_conf.infobox == "description"
        marker_container.info_window = new Microsoft.Maps.Infobox(marker_container.serviceObject.getLocation(), { description: marker_container.description, visible: false, showCloseButton: true})
      else
        marker_container.info_window = new Microsoft.Maps.Infobox(marker_container.serviceObject.getLocation(), { htmlContent: marker_container.description, visible: false})

      #//add the listener associated
      currentMap = this
      Microsoft.Maps.Events.addHandler(marker_container.serviceObject, 'click', @openInfoWindow(currentMap, marker_container.info_window))
      @addToMap(marker_container.info_window)

  openInfoWindow: (currentMap, infoWindow) ->
    return ->
      # Close the latest selected marker before opening the current one.
      if currentMap.visibleInfoWindow
        currentMap.visibleInfoWindow.setOptions({ visible: false })
      infoWindow.setOptions({ visible:true })
      currentMap.visibleInfoWindow = infoWindow

  #////////////////////////////////////////////////////
  #/////////////////// Other methods //////////////////
  #////////////////////////////////////////////////////

  fitBounds: ->
    @serviceObject.setView({bounds: @boundsObject})

  addToMap: (object)->
    @serviceObject.entities.push(object)

  removeFromMap: (object)->
    @serviceObject.entities.remove(object)

  centerMapOnUser: ->
    @serviceObject.setView({ center: @userLocation})

  updateBoundsWithPolylines: ()->

  updateBoundsWithPolygons: ()->

  updateBoundsWithCircles: ()->
  
  extendMapBounds :->

  adaptMapToBounds: ->
    @fitBounds()
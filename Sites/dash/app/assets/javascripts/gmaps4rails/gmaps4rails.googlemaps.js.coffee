#######################################################################################################
##############################################  Google maps  ##########################################
#######################################################################################################

class @Gmaps4RailsGoogle extends Gmaps4Rails

  constructor: ->
    super
    #Map settings
    @map_options =
      disableDefaultUI:       false
      disableDoubleClickZoom: false
      type:                   "ROADMAP" # HYBRID, ROADMAP, SATELLITE, TERRAIN

    #markers + info styling
    @markers_conf =
      clusterer_gridSize:      50
      clusterer_maxZoom:       5
      custom_cluster_pictures: null
      custom_infowindow_class: null

    @mergeWithDefault("map_options")
    @mergeWithDefault("markers_conf")

    @kml_options =
      clickable: true
      preserveViewport: false
      suppressInfoWindows: false

    #Polygon Styling
    @polygons_conf =         # default style for polygons
      strokeColor: "#FFAA00"
      strokeOpacity: 0.8
      strokeWeight: 2
      fillColor: "#000000"
      fillOpacity: 0.35
      clickable: false

    #Circle Styling
    @circles_conf =           #default style for circles
      fillColor: "#00AAFF"
      fillOpacity: 0.35
      strokeColor: "#FFAA00"
      strokeOpacity: 0.8
      strokeWeight: 2
      clickable: false
      zIndex: null

    #Direction Settings
    @direction_conf =
      panel_id:           null
      display_panel:      false
      origin:             null
      destination:        null
      waypoints:          []       #[{location: "toulouse,fr", stopover: true}, {location: "Clermont-Ferrand, fr", stopover: true}]
      optimizeWaypoints:  false
      unitSystem:         "METRIC" #IMPERIAL
      avoidHighways:      false
      avoidTolls:         false
      region:             null
      travelMode:         "DRIVING" #WALKING, BICYCLING

  #////////////////////////////////////////////////////
  #/////////////// Basic Objects         //////////////
  #////////////////////////////////////////////////////

  createPoint : (lat, lng) ->
    return new google.maps.Point(lat, lng)

  createLatLng : (lat, lng) ->
    return new google.maps.LatLng(lat, lng)

  createLatLngBounds : ->
    return new google.maps.LatLngBounds()

  createMap : ->
    defaultOptions =
      maxZoom:                @map_options.maxZoom
      minZoom:                @map_options.minZoom
      zoom:                   @map_options.zoom
      center:                 @createLatLng(@map_options.center_latitude, @map_options.center_longitude)
      mapTypeId:              google.maps.MapTypeId[@map_options.type]
      mapTypeControl:         @map_options.mapTypeControl
      disableDefaultUI:       @map_options.disableDefaultUI
      disableDoubleClickZoom: @map_options.disableDoubleClickZoom
      draggable:              @map_options.draggable

    mergedOptions = @mergeObjectWithDefault @map_options.raw, defaultOptions

    return new google.maps.Map document.getElementById(@map_options.id), mergedOptions


  createMarkerImage : (markerPicture, markerSize, origin, anchor, scaledSize) ->
    return new google.maps.MarkerImage(markerPicture, markerSize, origin, anchor, scaledSize)

  createSize : (width, height) ->
    return new google.maps.Size(width, height)

  #////////////////////////////////////////////////////
  #////////////////////// Markers /////////////////////
  #////////////////////////////////////////////////////

  createMarker : (args) ->
    markerLatLng = @createLatLng(args.Lat, args.Lng)
    #Marker sizes are expressed as a Size of X,Y
    if args.marker_picture == "" and args.rich_marker == null
      defaultOptions = {position: markerLatLng, map: @serviceObject, title: args.marker_title, draggable: args.marker_draggable, zIndex: args.zindex}
      mergedOptions  = @mergeObjectWithDefault @markers_conf.raw, defaultOptions
      return new google.maps.Marker mergedOptions

    if (args.rich_marker != null)
      return new RichMarker({
        position: markerLatLng
        map:       @serviceObject
        draggable: args.marker_draggable
        content:   args.rich_marker
        flat:      if args.marker_anchor == null then false else args.marker_anchor[1]
        anchor:    if args.marker_anchor == null then 0     else args.marker_anchor[0]
        zIndex:    args.zindex
      })

    #default behavior
    #calculate MarkerImage anchor location
    imageAnchorPosition  = @createImageAnchorPosition args.marker_anchor
    shadowAnchorPosition = @createImageAnchorPosition args.shadow_anchor
    #create or retrieve existing MarkerImages
    markerImage = @createOrRetrieveImage(args.marker_picture, args.marker_width, args.marker_height, imageAnchorPosition)
    shadowImage = @createOrRetrieveImage(args.shadow_picture, args.shadow_width, args.shadow_height, shadowAnchorPosition)
    defaultOptions = {position: markerLatLng, map: @serviceObject, icon: markerImage, title: args.marker_title, draggable: args.marker_draggable, shadow: shadowImage,  zIndex: args.zindex}
    mergedOptions  = @mergeObjectWithDefault @markers_conf.raw, defaultOptions
    return new google.maps.Marker mergedOptions

  #checks if obj is included in arr Array and returns the position or false
  includeMarkerImage : (arr, obj) ->
    for object, index in arr
      return index if object.url == obj
    return false

  #checks if MarkerImage exists before creating a new one
  #returns a MarkerImage or false if ever something wrong is passed as argument
  createOrRetrieveImage : (currentMarkerPicture, markerWidth, markerHeight, imageAnchorPosition) ->
    return null if (currentMarkerPicture == "" or currentMarkerPicture == null )

    test_image_index = @includeMarkerImage(@markerImages, currentMarkerPicture)
    switch test_image_index
      when false
        markerImage = @createMarkerImage(currentMarkerPicture, @createSize(markerWidth, markerHeight), null, imageAnchorPosition, null )
        @markerImages.push(markerImage)
        return markerImage
        break
      else
        return @markerImages[test_image_index] if typeof test_image_index == 'number'
        return false

  #clear markers
  clearMarkers : ->
    for marker in @markers
      @clearMarker marker

  #show and hide markers
  showMarkers : ->
    for marker in @markers
      @showMarker marker

  hideMarkers : ->
    for marker in @markers
      @hideMarker marker

  clearMarker : (marker) ->
    marker.serviceObject.setMap(null)

  showMarker : (marker) ->
    marker.serviceObject.setVisible(true)

  hideMarker : (marker) ->
    marker.serviceObject.setVisible(false)

  extendBoundsWithMarkers : ->
    for marker in @markers
      @boundsObject.extend(marker.serviceObject.position)

  #////////////////////////////////////////////////////
  #/////////////////// Clusterer //////////////////////
  #////////////////////////////////////////////////////

  createClusterer : (markers_array) ->
    return new MarkerClusterer( @serviceObject, markers_array, {  maxZoom: @markers_conf.clusterer_maxZoom, gridSize: @markers_conf.clusterer_gridSize, styles: @customClusterer() })

  clearClusterer : ->
    @markerClusterer.clearMarkers()

  #creates clusters
  clusterize : ->
    if @markers_conf.do_clustering == true
      #first clear the existing clusterer if any
      @clearClusterer() if @markerClusterer != null

      markers_array = new Array
      for marker in @markers
        markers_array.push(marker.serviceObject)

      @markerClusterer = @createClusterer(markers_array)

  #////////////////////////////////////////////////////
  #/////////////////// INFO WINDOW ////////////////////
  #////////////////////////////////////////////////////

  #// creates infowindows
  createInfoWindow : (marker_container) ->
    if typeof(@jsTemplate) == "function" or marker_container.description?
      marker_container.description = @jsTemplate(marker_container) if typeof(@jsTemplate) == "function"
      if @markers_conf.custom_infowindow_class != null
        #creating custom infowindow
        boxText = document.createElement("div")
        boxText.setAttribute("class", @markers_conf.custom_infowindow_class) #to customize
        boxText.innerHTML = marker_container.description
        marker_container.infowindow = new InfoBox(@infobox(boxText))
        currentMap = this
        google.maps.event.addListener(marker_container.serviceObject, 'click', @openInfoWindow(currentMap, marker_container.infowindow, marker_container.serviceObject))
      else
        #create default infowindow
        marker_container.infowindow = new google.maps.InfoWindow({content: marker_container.description })
        #add the listener associated
        currentMap = this
        google.maps.event.addListener(marker_container.serviceObject, 'click', @openInfoWindow(currentMap, marker_container.infowindow, marker_container.serviceObject))

  openInfoWindow : (currentMap, infoWindow, marker) ->
    return ->
      # Close the latest selected marker before opening the current one.
      currentMap.visibleInfoWindow.close() if currentMap.visibleInfoWindow != null
      infoWindow.open(currentMap.serviceObject, marker)
      currentMap.visibleInfoWindow = infoWindow

  #////////////////////////////////////////////////////
  #/////////////////        KML      //////////////////
  #////////////////////////////////////////////////////

  createKmlLayer : (kml) ->
    kml_options = kml.options || {}
    kml_options = @mergeObjectWithDefault(kml_options, @kml_options)
    kml =  new google.maps.KmlLayer( kml.url, kml_options)
    kml.setMap(@serviceObject)
    return kml

  #////////////////////////////////////////////////////
  #/////////////////// POLYLINES //////////////////////
  #////////////////////////////////////////////////////

  #creates a single polyline, triggered by create_polylines
  create_polyline : (polyline) ->
    polyline_coordinates = []

    #2 cases here, either we have a coded array of LatLng or we have an Array of LatLng
    for element in polyline
      #if we have a coded array
      if element.coded_array?
        decoded_array = new google.maps.geometry.encoding.decodePath(element.coded_array)
        #loop through every point in the array
        for point in decoded_array
          polyline_coordinates.push(point)

      #or we have an array of latlng
      else
        #by convention, a single polyline could be customized in the first array or it uses default values
        if element == polyline[0]
          strokeColor   = element.strokeColor   || @polylines_conf.strokeColor
          strokeOpacity = element.strokeOpacity || @polylines_conf.strokeOpacity
          strokeWeight  = element.strokeWeight  || @polylines_conf.strokeWeight
          clickable     = element.clickable     || @polylines_conf.clickable
          zIndex        = element.zIndex        || @polylines_conf.zIndex

        #add latlng if positions provided
        if element.lat? && element.lng?
          latlng = @createLatLng(element.lat, element.lng)
          polyline_coordinates.push(latlng)

    # Construct the polyline
    new_poly = new google.maps.Polyline
      path:         polyline_coordinates
      strokeColor:  strokeColor
      strokeOpacity: strokeOpacity
      strokeWeight: strokeWeight
      clickable:    clickable
      zIndex:       zIndex

    #save polyline
    polyline.serviceObject = new_poly
    new_poly.setMap(@serviceObject)

  
  updateBoundsWithPolylines: ()->
    for polyline in @polylines
      polyline_points = polyline.serviceObject.latLngs.getArray()[0].getArray()
      for point in polyline_points
        @boundsObject.extend point
  
  #////////////////////////////////////////////////////
  #/////////////////        KML      //////////////////
  #////////////////////////////////////////////////////

  create_kml : ->
    for kml in @kml
      kml.serviceObject = @createKmlLayer kml
            
  #////////////////////////////////////////////////////
  #/////////////////// Other methods //////////////////
  #////////////////////////////////////////////////////

  fitBounds : ->
    @serviceObject.fitBounds(@boundsObject) unless @boundsObject.isEmpty()

  centerMapOnUser : ->
    @serviceObject.setCenter(@userLocation)
  
  updateBoundsWithPolygons: ()->
    for polygon in @polygons
      polygon_points = polygon.serviceObject.latLngs.getArray()[0].getArray()
      for point in polygon_points
        @boundsObject.extend point

  updateBoundsWithCircles: ()->
    for circle in @circles
      @boundsObject.extend(circle.serviceObject.getBounds().getNorthEast())
      @boundsObject.extend(circle.serviceObject.getBounds().getSouthWest())

  extendMapBounds: ()->
    for bound in @map_options.bounds
      #create points from bounds provided
      @boundsObject.extend @createLatLng(bound.lat, bound.lng)
  
  adaptMapToBounds:()->
    #if autozoom is false, take user info into account
    if !@map_options.auto_zoom
      map_center = @boundsObject.getCenter()
      @map_options.center_latitude  = map_center.lat()
      @map_options.center_longitude = map_center.lng()
      @serviceObject.setCenter(map_center)
    else
      @fitBounds()

Gmaps = {}

Gmaps.triggerOldOnload = ->
  Gmaps.oldOnload() if typeof(Gmaps.oldOnload) == 'function'

Gmaps.loadMaps = ->
  #loop through all variable names.
  #there should only be maps inside so it trigger their load function
  for key, value of Gmaps
    searchLoadIncluded = key.search(/load/)
    if searchLoadIncluded == -1
      load_function_name = "load_" + key
      Gmaps[load_function_name]()

window.Gmaps = Gmaps

class @Gmaps4Rails

  constructor: ->
    #map config
    @map =  null               #DEPRECATED: will still contain a copy of serviceObject below as transition
    @serviceObject = null      #contains the map we're working on
    @visibleInfoWindow = null  #contains the current opened infowindow
    @userLocation = null       #contains user's location if geolocalization was performed and successful

    #empty slots
    @geolocationSuccess = -> false  #triggered when geolocation succeeds. Can be customized.
    @geolocationFailure = -> false  #triggered when geolocation fails. If customized, must be like= function(navigator_handles_geolocation){} where 'navigator_handles_geolocation' is a boolean
    @callback           = -> false  #to let user set a custom callback function
    @customClusterer    = -> false  #to let user set custom clusterer pictures
    @infobox            = -> false  #to let user use custom infoboxes
    @jsTemplate         = false     #to let user create infowindows client side

    @default_map_options =
      id: 'map'
      draggable: true
      detect_location: false  # should the browser attempt to use geolocation detection features of HTML5?
      center_on_user: false   # centers map on the location detected through the browser
      center_latitude: 0
      center_longitude: 0
      zoom: 7
      maxZoom: null
      minZoom: null
      auto_adjust : true      # adjust the map to the markers if set to true
      auto_zoom: true         # zoom given by auto-adjust
      bounds: []              # adjust map to these limits. Should be [{"lat": , "lng": }]
      raw: {}                  # raw json to pass additional options

    @default_markers_conf =
      #Marker config
      title: ""
      #MarkerImage config
      picture : ""
      width: 22
      length: 32
      draggable: false         # how to modify: <%= gmaps( "markers" => { "data" => @object.to_gmaps4rails, "options" => { "draggable" => true }}) %>
      #clustering config
      do_clustering: false     # do clustering if set to true
      randomize: false         # Google maps can't display two markers which have the same coordinates. This randomizer enables to prevent this situation from happening.
      max_random_distance: 100 # in meters. Each marker coordinate could be altered by this distance in a random direction
      list_container: null     # id of the ul that will host links to all markers
      offset: 0                # used when adding_markers to an existing map. Because new markers are concated with previous one, offset is here to prevent the existing from being re-created.
      raw: {}                  # raw json to pass additional options

    #Stored variables
    @markers = []            # contains all markers. A marker contains the following: {"description": , "longitude": , "title":, "latitude":, "picture": "", "width": "", "length": "", "sidebar": "", "serviceObject": google_marker}
    @boundsObject = null     # contains current bounds from markers, polylines etc...
    @polygons = []           # contains raw data, array of arrays (first element could be a hash containing options)
    @polylines = []          # contains raw data, array of arrays (first element could be a hash containing options)
    @circles = []            # contains raw data, array of hash
    @markerClusterer = null  # contains all marker clusterers
    @markerImages = []

	#Polyline Styling
    @polylines_conf =         #default style for polylines
      strokeColor: "#FF0000"
      strokeOpacity: 1
      strokeWeight: 2
      clickable: false
      zIndex: null

  #tnitializes the map
  initialize : ->
    @serviceObject = @createMap()
    @map = @serviceObject #beware, soon deprecated
    if (@map_options.detect_location == true or @map_options.center_on_user == true)
      @findUserLocation(this)
    #resets sidebar if needed
    @resetSidebarContent()

  findUserLocation : (map_object) ->
    if (navigator.geolocation)
      #try to retrieve user's position
      positionSuccessful = (position) ->
        map_object.userLocation = map_object.createLatLng(position.coords.latitude, position.coords.longitude)
        #change map's center to focus on user's geoloc if asked
        if(map_object.map_options.center_on_user == true)
          map_object.centerMapOnUser()
        map_object.geolocationSuccess()
      positionFailure = ->
        map_object.geolocationFailure(true)

      navigator.geolocation.getCurrentPosition( positionSuccessful, positionFailure)
    else
      #failure but the navigator doesn't handle geolocation
      map_object.geolocationFailure(false)


  #////////////////////////////////////////////////////
  #//////////////////// DIRECTIONS ////////////////////
  #////////////////////////////////////////////////////

  create_direction : ->
    directionsDisplay = new google.maps.DirectionsRenderer()
    directionsService = new google.maps.DirectionsService()

    directionsDisplay.setMap(@serviceObject)
    #display panel only if required
    if @direction_conf.display_panel
      directionsDisplay.setPanel(document.getElementById(@direction_conf.panel_id))

    directionsDisplay.setOptions
      suppressMarkers:     false
      suppressInfoWindows: false
      suppressPolylines:   false

    request =
      origin:             @direction_conf.origin
      destination:        @direction_conf.destination
      waypoints:          @direction_conf.waypoints
      optimizeWaypoints:  @direction_conf.optimizeWaypoints
      unitSystem:         google.maps.DirectionsUnitSystem[@direction_conf.unitSystem]
      avoidHighways:      @direction_conf.avoidHighways
      avoidTolls:         @direction_conf.avoidTolls
      region:             @direction_conf.region
      travelMode:         google.maps.DirectionsTravelMode[@direction_conf.travelMode]
      language:           "en"

    directionsService.route request, (response, status) ->
      if (status == google.maps.DirectionsStatus.OK)
        directionsDisplay.setDirections(response)

  #////////////////////////////////////////////////////
  #///////////////////// CIRCLES //////////////////////
  #////////////////////////////////////////////////////

  #Loops through all circles
  #Loops through all circles and draws them
  create_circles : ->
    for circle in @circles
      @create_circle circle

  create_circle : (circle) ->
    #by convention, default style configuration could be integrated in the first element
    if circle == @circles[0]
      @circles_conf.strokeColor   = circle.strokeColor   if circle.strokeColor?
      @circles_conf.strokeOpacity = circle.strokeOpacity if circle.strokeOpacity?
      @circles_conf.strokeWeight  = circle.strokeWeight  if circle.strokeWeight?
      @circles_conf.fillColor     = circle.fillColor     if circle.fillColor?
      @circles_conf.fillOpacity   = circle.fillOpacity   if circle.fillOpacity?

    if circle.lat? and circle.lng?
      # always check if a config is given, if not, use defaults
      # NOTE: is there a cleaner way to do this? Maybe a hash merge of some sort?
      newCircle = new google.maps.Circle
        center:        @createLatLng(circle.lat, circle.lng)
        strokeColor:   circle.strokeColor   || @circles_conf.strokeColor
        strokeOpacity: circle.strokeOpacity || @circles_conf.strokeOpacity
        strokeWeight:  circle.strokeWeight  || @circles_conf.strokeWeight
        fillOpacity:   circle.fillOpacity   || @circles_conf.fillOpacity
        fillColor:     circle.fillColor     || @circles_conf.fillColor
        clickable:     circle.clickable     || @circles_conf.clickable
        zIndex:        circle.zIndex        || @circles_conf.zIndex
        radius:        circle.radius

      circle.serviceObject = newCircle
      newCircle.setMap(@serviceObject)

  # clear circles
  clear_circles : ->
    for circle in @circles
      @clear_circle circle

  clear_circle : (circle) ->
    circle.serviceObject.setMap(null)

  hide_circles : ->
    for circle in @circles
      @hide_circle circle

  hide_circle : (circle) ->
    circle.serviceObject.setMap(null)

  show_circles : ->
    for circle in @circles
      @show_circle @circle

  show_circle : (circle) ->
    circle.serviceObject.setMap(@serviceObject)

  #////////////////////////////////////////////////////
  #///////////////////// POLYGONS /////////////////////
  #////////////////////////////////////////////////////

  #polygons is an array of arrays. It loops.
  create_polygons : ->
    for polygon in @polygons
      @create_polygon(polygon)

  #creates a single polygon, triggered by create_polygons
  create_polygon : (polygon) ->
    polygon_coordinates = []

    #Polygon points are in an Array, that's why looping is necessary
    for point in polygon
      latlng = @createLatLng(point.lat, point.lng)
      polygon_coordinates.push(latlng)
      #first element of an Array could contain specific configuration for this particular polygon. If no config given, use default
      if point == polygon[0]
        strokeColor   = point.strokeColor   || @polygons_conf.strokeColor
        strokeOpacity = point.strokeOpacity || @polygons_conf.strokeOpacity
        strokeWeight  = point.strokeWeight  || @polygons_conf.strokeWeight
        fillColor     = point.fillColor     || @polygons_conf.fillColor
        fillOpacity   = point.fillOpacity   || @polygons_conf.fillOpacity
        clickable     = point.clickable     || @polygons_conf.clickable
        
    #Construct the polygon
    new_poly = new google.maps.Polygon
      paths:          polygon_coordinates
      strokeColor:    strokeColor
      strokeOpacity:  strokeOpacity
      strokeWeight:   strokeWeight
      fillColor:      fillColor
      fillOpacity:    fillOpacity
      clickable:      clickable
      map:            @serviceObject

    #save polygon in list
    polygon.serviceObject = new_poly

  

  #////////////////////////////////////////////////////
  #///////////////////// MARKERS //////////////////////
  #////////////////////////////////////////////////////

  #creates, clusterizes and adjusts map
  create_markers : ->
    @createServiceMarkersFromMarkers()
    @clusterize()

  #create google.maps Markers from data provided by user
  createServiceMarkersFromMarkers : ->
    for marker, index in @markers
      if not @markers[index].serviceObject?
        #extract options, test if value passed or use default
        Lat = @markers[index].lat
        Lng = @markers[index].lng

        #alter coordinates if randomize is true
        if @markers_conf.randomize
          LatLng = @randomize(Lat, Lng)
          #retrieve coordinates from the array
          Lat = LatLng[0]
          Lng = LatLng[1]

        #save object
        @markers[index].serviceObject = @createMarker
          "marker_picture":   if @markers[index].picture  then @markers[index].picture else @markers_conf.picture
          "marker_width":     if @markers[index].width    then @markers[index].width   else @markers_conf.width
          "marker_height":    if @markers[index].height   then @markers[index].height  else @markers_conf.length
          "marker_title":     if @markers[index].title    then @markers[index].title   else null
          "marker_anchor":    if @markers[index].marker_anchor  then @markers[index].marker_anchor  else null
          "shadow_anchor":    if @markers[index].shadow_anchor  then @markers[index].shadow_anchor  else null
          "shadow_picture":   if @markers[index].shadow_picture then @markers[index].shadow_picture else null
          "shadow_width":     if @markers[index].shadow_width   then @markers[index].shadow_width   else null
          "shadow_height":    if @markers[index].shadow_height  then @markers[index].shadow_height  else null
          "marker_draggable": if @markers[index].draggable      then @markers[index].draggable      else @markers_conf.draggable
          "rich_marker":      if @markers[index].rich_marker    then @markers[index].rich_marker    else null
          "zindex":           if @markers[index].zindex         then @markers[index].zindex         else null
          "Lat":              Lat
          "Lng":              Lng
          "index":            index

        #add infowindowstuff if enabled
        @createInfoWindow(@markers[index])
        #create sidebar if enabled
        @createSidebar(@markers[index])

    @markers_conf.offset = @markers.length

  #creates Image Anchor Position or return null if nothing passed
  createImageAnchorPosition : (anchorLocation) ->
    if (anchorLocation == null)
      return null
    else
      return @createPoint(anchorLocation[0], anchorLocation[1])


  #replace old markers with new markers on an existing map
  replaceMarkers : (new_markers, adjustBounds = true) ->
    @clearMarkers()
    #reset previous markers
    @markers = new Array
    #reset current bounds
    @boundsObject = @createLatLngBounds() if adjustBounds
    #reset sidebar content if exists
    @resetSidebarContent()
    #add new markers
    @markers_conf.offset = 0
    @addMarkers(new_markers, adjustBounds)

  #add new markers to on an existing map
  addMarkers : (new_markers, adjustBounds = true) ->
    #update the list of markers to take into account
    @markers = @markers.concat(new_markers)
    #put markers on the map
    @create_markers()
    @adjustMapToBounds() if adjustBounds

  #////////////////////////////////////////////////////
  #///////////////////// SIDEBAR //////////////////////
  #////////////////////////////////////////////////////

  #//creates sidebar
  createSidebar : (marker_container) ->
    if (@markers_conf.list_container)
      ul = document.getElementById(@markers_conf.list_container)
      li = document.createElement('li')
      aSel = document.createElement('a')
      aSel.href = 'javascript:void(0);'
      html = if marker_container.sidebar? then marker_container.sidebar else "Marker"
      aSel.innerHTML = html
      currentMap = this
      aSel.onclick = @sidebar_element_handler(currentMap, marker_container.serviceObject, 'click')
      li.appendChild(aSel)
      ul.appendChild(li)

  #moves map to marker clicked + open infowindow
  sidebar_element_handler : (currentMap, marker, eventType) ->
    return () ->
      currentMap.map.panTo(marker.position)
      google.maps.event.trigger(marker, eventType)


  resetSidebarContent : ->
    if @markers_conf.list_container isnt null
      ul = document.getElementById(@markers_conf.list_container)
      ul.innerHTML = ""

  #////////////////////////////////////////////////////
  #////////////////// MISCELLANEOUS ///////////////////
  #////////////////////////////////////////////////////

  #to make the map fit the different LatLng points
  adjustMapToBounds : ->
    #FIRST_STEP: retrieve all bounds
    #create the bounds object only if necessary
    if @map_options.auto_adjust or @map_options.bounds isnt null
      @boundsObject = @createLatLngBounds()

      #if autodjust is true, must get bounds from markers polylines etc...
      if @map_options.auto_adjust
        #from markers
        @extendBoundsWithMarkers()

        #from polylines:
        @updateBoundsWithPolylines()

        #from polygons:
        @updateBoundsWithPolygons()

        #from circles
        @updateBoundsWithCircles()

      #in every case, I've to take into account the bounds set up by the user
      @extendMapBounds()

      #SECOND_STEP: ajust the map to the bounds
      @adaptMapToBounds()

  #////////////////////////////////////////////////////
  #/////////////////// POLYLINES //////////////////////
  #////////////////////////////////////////////////////

  #replace old markers with new markers on an existing map
  replacePolylines : (new_polylines) ->
    #reset previous polylines and kill them from map
    @destroy_polylines()
    #set new polylines
    @polylines = new_polylines
    #create
    @create_polylines()
    #.... and adjust map boundaries
    @adjustMapToBounds()

  destroy_polylines : ->
    for polyline in @polylines
      #delete polylines from map
      polyline.serviceObject.setMap(null)
    #empty array
    @polylines = []

  #polylines is an array of arrays. It loops.
  create_polylines : ->
    for polyline in @polylines
      @create_polyline polyline

  #////////////////////////////////////////////////////
  #///////////////// Basic functions //////////////////
  #///////////////////tests coded//////////////////////

  #//basic function to check existence of a variable
  exists : (var_name) ->
    return (var_name	!= "" and typeof var_name != "undefined")


  #randomize
  randomize : (Lat0, Lng0) ->
    #distance in meters between 0 and max_random_distance (positive or negative)
    dx = @markers_conf.max_random_distance * @random()
    dy = @markers_conf.max_random_distance * @random()
    Lat = parseFloat(Lat0) + (180/Math.PI)*(dy/6378137)
    Lng = parseFloat(Lng0) + ( 90/Math.PI)*(dx/6378137)/Math.cos(Lat0)
    return [Lat, Lng]

  mergeObjectWithDefault : (object1, object2) ->
    copy_object1 = {}
    for key, value of object1
      copy_object1[key] = value

    for key, value of object2
      unless copy_object1[key]?
        copy_object1[key] = value
    return copy_object1

  mergeWithDefault : (objectName) ->
    default_object = @["default_" + objectName]
    object = @[objectName]
    @[objectName] = @mergeObjectWithDefault(object, default_object)
    return true

  #gives a value between -1 and 1
  random : -> return(Math.random() * 2 -1)

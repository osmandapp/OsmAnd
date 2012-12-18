#######################################################################################################
##############################################  Open Layers  ##########################################
#######################################################################################################

#// http://wiki.openstreetmap.org/wiki/OpenLayers
#// http://openlayers.org/dev/examples
#//http://docs.openlayers.org/contents.html

class @Gmaps4RailsOpenlayers extends Gmaps4Rails

  constructor: ->
    super
    @map_options = {}
    @mergeWithDefault "map_options"
    @markers_conf = {}
    @mergeWithDefault "markers_conf"

    @openMarkers = null
    @markersLayer = null
    @markersControl = null
    @polylinesLayer = null

  #////////////////////////////////////////////////////
  #/////////////// Basic Objects   ////////////////////
  #////////////////////////////////////////////////////

  createPoint: (lat, lng)->

  createLatLng: (lat, lng)->
    return new OpenLayers.LonLat(lng, lat).transform(new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913")) # transform from WGS 1984 to Spherical Mercator Projection
              
  createAnchor: (offset)->
    return null if offset == null
    return new OpenLayers.Pixel(offset[0], offset[1])

  createSize: (width, height)->
    return new OpenLayers.Size(width, height)

  createLatLngBounds: ->
     return new OpenLayers.Bounds()
  
  createMap: ->
    #//todo add customization: kind of map and other map options
    map = new OpenLayers.Map(@map_options.id)
    map.addLayer(new OpenLayers.Layer.OSM())
    map.setCenter(@createLatLng(@map_options.center_latitude, @map_options.center_longitude), #// Center of the map
                  @map_options.zoom) #// Zoom level
    return map

  #////////////////////////////////////////////////////
  #////////////////////// Markers /////////////////////
  #////////////////////////////////////////////////////
  #//http://openlayers.org/dev/examples/marker-shadow.html
  createMarker: (args) ->
    style_mark = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default'])
    style_mark.fillOpacity = 1
  
    #//creating markers' dedicated layer 
    if (@markersLayer == null) 
      @markersLayer = new OpenLayers.Layer.Vector("Markers", null)
      @serviceObject.addLayer(@markersLayer)
      #//TODO move?
      @markersLayer.events.register("featureselected", @markersLayer, @onFeatureSelect)
      @markersLayer.events.register("featureunselected", @markersLayer, @onFeatureUnselect)
      @markersControl = new OpenLayers.Control.SelectFeature(@markersLayer)
      @serviceObject.addControl(@markersControl)
      @markersControl.activate()
    #//showing default pic if none available
    if args.marker_picture == ""  
      #style_mark.graphicWidth = 24
      style_mark.graphicHeight = 30
      style_mark.externalGraphic = "http://openlayers.org/dev/img/marker-blue.png"
    #//creating custom pic
    else
      style_mark.graphicWidth    = args.marker_width
      style_mark.graphicHeight   = args.marker_height
      style_mark.externalGraphic = args.marker_picture
      #//adding anchor if any
      if args.marker_anchor != null 
        style_mark.graphicXOffset = args.marker_anchor[0]
        style_mark.graphicYOffset = args.marker_anchor[1]
      #//adding shadow if any
      if args.shadow_picture != ""
        style_mark.backgroundGraphic = args.shadow_picture
        style_mark.backgroundWidth   = args.shadow_width
        style_mark.backgroundHeight  = args.shadow_height
        #//adding shadow's anchor if any
        if args.shadow_anchor != null
          style_mark.backgroundXOffset = args.shadow_anchor[0]
          style_mark.backgroundYOffset = args.shadow_anchor[1]
      
    style_mark.graphicTitle = args.marker_title
    marker = new OpenLayers.Feature.Vector(
               new OpenLayers.Geometry.Point(args.Lng, args.Lat),
               null,
              style_mark)
    #//changing coordinates so that it actually appears on the map!
    marker.geometry.transform(new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913"))
    #//adding layer to the map
    @markersLayer.addFeatures([marker])
  
    return marker

  #//clear markers
  clearMarkers: ->
    @clearMarkersLayerIfExists()
    @markersLayer = null
    @boundsObject = new OpenLayers.Bounds()
  
  clearMarkersLayerIfExists: -> 
    @serviceObject.removeLayer(@markersLayer) if @markersLayer != null and @serviceObject.getLayer(@markersLayer.id) != null
  
  extendBoundsWithMarkers: ->
    console.log "here"
    for marker in @markers
      @boundsObject.extend(@createLatLng(marker.lat,marker.lng))        

  #////////////////////////////////////////////////////
  #/////////////////// Clusterer //////////////////////
  #////////////////////////////////////////////////////
  #//too ugly to be considered valid :(

  createClusterer: (markers_array)->
     options = 
       pointRadius: "${radius}"
       fillColor: "#ffcc66"
       fillOpacity: 0.8
       strokeColor: "#cc6633"
       strokeWidth: "${width}"
       strokeOpacity: 0.8
     funcs =
       context:
         width: (feature) ->
           return (feature.cluster) ? 2 : 1
         radius: (feature) ->
           pix = 2
           pix = Math.min(feature.attributes.count, 7) + 2 if feature.cluster
           return pix
     
     style = new OpenLayers.Style options, funcs
   
     strategy = new OpenLayers.Strategy.Cluster()
   
     clusters = new OpenLayers.Layer.Vector "Clusters", 
       strategies: [strategy]
       styleMap: new OpenLayers.StyleMap
         "default": style
         "select": 
            fillColor: "#8aeeef"
            strokeColor: "#32a8a9"
       
     @clearMarkersLayerIfExists()
     @serviceObject.addLayer(clusters)
     clusters.addFeatures(markers_array)
     return clusters
   
   clusterize: ->
   
     if @markers_conf.do_clustering == true
       #//first clear the existing clusterer if any
       if @markerClusterer != null
         @clearClusterer()
       markers_array = new Array
       for marker in @markers
         markers_array.push(marker.serviceObject)
       @markerClusterer = @createClusterer markers_array
   
   clearClusterer: ->
     @serviceObject.removeLayer @markerClusterer

  #////////////////////////////////////////////////////
  #/////////////////// INFO WINDOW ////////////////////
  #////////////////////////////////////////////////////

  #// creates infowindows
  createInfoWindow: (marker_container) ->
    marker_container.serviceObject.infoWindow = marker_container.description if marker_container.description?
  
  onPopupClose: (evt) ->
    #// 'this' is the popup.
    @markersControl.unselect @feature
  
  onFeatureSelect: (evt) ->
    feature = evt.feature    
    popup = new OpenLayers.Popup.FramedCloud("featurePopup",
                               feature.geometry.getBounds().getCenterLonLat(),
                               new OpenLayers.Size(300,200),
                               feature.infoWindow,
                               null, true, @onPopupClose)
    feature.popup = popup
    popup.feature = feature
    @map.addPopup popup

  onFeatureUnselect: (evt) ->
    feature = evt.feature
    if feature.popup
      #//popup.feature = null;
      @map.removePopup feature.popup
      feature.popup.destroy()
      feature.popup = null

  #////////////////////////////////////////////////////
  #/////////////////// POLYLINES //////////////////////
  #////////////////////////////////////////////////////

  create_polyline : (polyline) ->
	
    if(@polylinesLayer == null)
      @polylinesLayer = new OpenLayers.Layer.Vector("Polylines", null)
      @serviceObject.addLayer(@polylinesLayer)
      @polylinesLayer.events.register("featureselected", @polylinesLayer, @onFeatureSelect)
      @polylinesLayer.events.register("featureunselected", @polylinesLayer, @onFeatureUnselect)
      @polylinesControl = new OpenLayers.Control.DrawFeature(@polylinesLayer, OpenLayers.Handler.Path)
      @serviceObject.addControl(@polylinesControl)
    
    polyline_coordinates = []

    for element in polyline
      #by convention, a single polyline could be customized in the first array or it uses default values
      if element == polyline[0]
        strokeColor   = element.strokeColor   || @polylines_conf.strokeColor
        strokeOpacity = element.strokeOpacity || @polylines_conf.strokeOpacity
        strokeWeight  = element.strokeWeight  || @polylines_conf.strokeWeight
        clickable     = element.clickable     || @polylines_conf.clickable
        zIndex        = element.zIndex        || @polylines_conf.zIndex	  
      
      #add latlng if positions provided
      if element.lat? && element.lng?
        latlng = new OpenLayers.Geometry.Point(element.lng, element.lat)
        polyline_coordinates.push(latlng)
    
    line_points = new OpenLayers.Geometry.LineString(polyline_coordinates);
    line_style = { strokeColor: strokeColor, strokeOpacity: strokeOpacity, strokeWidth: strokeWeight };
   
    polyline = new OpenLayers.Feature.Vector(line_points, null, line_style);
    polyline.geometry.transform(new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913"))

    @polylinesLayer.addFeatures([polyline])

    return polyline

  updateBoundsWithPolylines: ()->
  
  updateBoundsWithPolygons: ()->
    
  updateBoundsWithCircles: ()->
  
  # #////////////////////////////////////////////////////
  # #/////////////////// Other methods //////////////////
  # #////////////////////////////////////////////////////
 
  fitBounds: ->
    @serviceObject.zoomToExtent(@boundsObject, true)
  
  centerMapOnUser: ->
    @serviceObject.setCenter @userLocation
    
  extendMapBounds :->
    
  adaptMapToBounds: ->
    @fitBounds()

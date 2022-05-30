package net.osmand.plus.views.layers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;

import android.content.Context;
import android.graphics.PointF;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.IndexConstants;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.RenderingContext;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AmenitySymbolsProvider.AmenitySymbolsGroup;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IBillboardMapSymbol;
import net.osmand.core.jni.IMapRenderer.MapSymbolInformation;
import net.osmand.core.jni.MapObject;
import net.osmand.core.jni.MapObjectsSymbolsProvider.MapObjectSymbolsGroup;
import net.osmand.core.jni.MapSymbolInformationList;
import net.osmand.core.jni.MapSymbolsGroup.AdditionalBillboardSymbolInstanceParameters;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.Utilities;
import net.osmand.data.Amenity;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer.OpenStreetNote;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.router.network.NetworkRouteContext.NetworkRouteSegment;
import net.osmand.router.network.NetworkRouteSelector;
import net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import net.osmand.router.network.NetworkRouteSelector.RouteType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class MapSelectionHelper {

	private static final Log log = PlatformUtil.getLog(ContextMenuLayer.class);
	private static final int AMENITY_SEARCH_RADIUS = 50;
	private static final int TILE_SIZE = 256;

	private final OsmandApplication app;
	private final OsmandMapTileView view;
	private final MapLayers mapLayers;

	private List<String> publicTransportTypes;

	private Map<LatLon, BackgroundType> pressedLatLonFull = new HashMap<>();
	private Map<LatLon, BackgroundType> pressedLatLonSmall = new HashMap<>();

	public MapSelectionHelper(@NonNull Context context) {
		app = (OsmandApplication) context.getApplicationContext();
		view = app.getOsmandMap().getMapView();
		mapLayers = app.getOsmandMap().getMapLayers();
	}

	@NonNull
	public Map<LatLon, BackgroundType> getPressedLatLonFull() {
		return pressedLatLonFull;
	}

	@NonNull
	public Map<LatLon, BackgroundType> getPressedLatLonSmall() {
		return pressedLatLonSmall;
	}

	public boolean hasPressedLatLon() {
		return !pressedLatLonSmall.isEmpty() || !pressedLatLonFull.isEmpty();
	}

	@NonNull
	protected MapSelectionResult selectObjectsFromMap(@NonNull PointF point, @NonNull RotatedTileBox tileBox, boolean showUnknownLocation) {
		LatLon pointLatLon = NativeUtilities.getLatLonFromPixel(view.getMapRenderer(), tileBox, point.x, point.y);
		NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
		Map<Object, IContextMenuProvider> selectedObjects = selectObjectsFromMap(tileBox, point, false, showUnknownLocation);

		MapSelectionResult result = new MapSelectionResult(selectedObjects, pointLatLon);
		if (app.getSettings().USE_OPENGL_RENDER.get() && NativeCoreContext.isInit()) {
			selectObjectsFromOpenGl(result, tileBox, point);
		} else if (nativeLib != null) {
			selectObjectsFromNative(result, nativeLib, tileBox, point);
		}
		processTransportStops(selectedObjects);
		return result;
	}

	@NonNull
	protected Map<Object, IContextMenuProvider> selectObjectsFromMap(@NonNull RotatedTileBox tileBox,
	                                                                 @NonNull PointF point,
	                                                                 boolean acquireObjLatLon,
	                                                                 boolean unknownLocation) {
		Map<LatLon, BackgroundType> pressedLatLonFull = new HashMap<>();
		Map<LatLon, BackgroundType> pressedLatLonSmall = new HashMap<>();
		Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
		List<Object> s = new ArrayList<>();
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IContextMenuProvider) {
				s.clear();
				final IContextMenuProvider provider = (IContextMenuProvider) layer;
				provider.collectObjectsFromPoint(point, tileBox, s, unknownLocation);
				for (Object o : s) {
					selectedObjects.put(o, provider);
					if (acquireObjLatLon && provider.isObjectClickable(o)) {
						LatLon latLon = provider.getObjectLocation(o);
						BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;
						if (o instanceof OpenStreetNote) {
							backgroundType = BackgroundType.COMMENT;
						}
						if (o instanceof FavouritePoint) {
							backgroundType = ((FavouritePoint) o).getBackgroundType();
						}
						if (o instanceof WptPt) {
							backgroundType = BackgroundType.getByTypeName(((WptPt) o).getBackgroundType(), DEFAULT_BACKGROUND_TYPE);
						}
						if (layer.isPresentInFullObjects(latLon) && !pressedLatLonFull.containsKey(latLon)) {
							pressedLatLonFull.put(latLon, backgroundType);
						} else if (layer.isPresentInSmallObjects(latLon) && !pressedLatLonSmall.containsKey(latLon)) {
							pressedLatLonSmall.put(latLon, backgroundType);
						}
					}
				}
			}
		}
		if (acquireObjLatLon) {
			this.pressedLatLonFull = pressedLatLonFull;
			this.pressedLatLonSmall = pressedLatLonSmall;
		}
		return selectedObjects;
	}

	private void selectObjectsFromNative(@NonNull MapSelectionResult result, @NonNull NativeOsmandLibrary nativeLib,
	                                     @NonNull RotatedTileBox tileBox, @NonNull PointF point) {
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingContext rc = maps.getVisibleRenderingContext();
		RenderedObject[] renderedObjects = null;
		if (rc != null && rc.zoom == tileBox.getZoom()) {
			double sinRotate = Math.sin(Math.toRadians(rc.rotate - tileBox.getRotate()));
			double cosRotate = Math.cos(Math.toRadians(rc.rotate - tileBox.getRotate()));
			float x = tileBox.getPixXFrom31((int) (rc.leftX * rc.tileDivisor), (int) (rc.topY * rc.tileDivisor));
			float y = tileBox.getPixYFrom31((int) (rc.leftX * rc.tileDivisor), (int) (rc.topY * rc.tileDivisor));
			float dx = point.x - x;
			float dy = point.y - y;
			int coordX = (int) (dx * cosRotate - dy * sinRotate);
			int coordY = (int) (dy * cosRotate + dx * sinRotate);

			renderedObjects = nativeLib.searchRenderedObjectsFromContext(rc, coordX, coordY, true);
		}
		if (renderedObjects != null) {
			double cosRotateTileSize = Math.cos(Math.toRadians(rc.rotate)) * TILE_SIZE;
			double sinRotateTileSize = Math.sin(Math.toRadians(rc.rotate)) * TILE_SIZE;

			for (RenderedObject renderedObject : renderedObjects) {
				String routeID = renderedObject.getRouteID();
				String fileName = renderedObject.getGpxFileName();
				String filter = routeID != null ? routeID : fileName;

				boolean isTravelGpx = !Algorithms.isEmpty(filter);
				boolean isRoute = !Algorithms.isEmpty(RouteType.getRouteKeys(renderedObject));
				if (!isTravelGpx && !isRoute && (renderedObject.getId() == null
						|| !renderedObject.isVisible() || renderedObject.isDrawOnPath())) {
					continue;
				}

				if (renderedObject.getLabelX() != 0 && renderedObject.getLabelY() != 0) {
					double lat = MapUtils.get31LatitudeY(renderedObject.getLabelY());
					double lon = MapUtils.get31LongitudeX(renderedObject.getLabelX());
					renderedObject.setLabelLatLon(new LatLon(lat, lon));
				} else {
					double cx = renderedObject.getBbox().centerX();
					double cy = renderedObject.getBbox().centerY();
					double dTileX = (cx * cosRotateTileSize + cy * sinRotateTileSize) / (TILE_SIZE * TILE_SIZE);
					double dTileY = (cy * cosRotateTileSize - cx * sinRotateTileSize) / (TILE_SIZE * TILE_SIZE);
					int x31 = (int) ((dTileX + rc.leftX) * rc.tileDivisor);
					int y31 = (int) ((dTileY + rc.topY) * rc.tileDivisor);
					double lat = MapUtils.get31LatitudeY(y31);
					double lon = MapUtils.get31LongitudeX(x31);
					renderedObject.setLabelLatLon(new LatLon(lat, lon));
				}

				if (renderedObject.getX() != null && renderedObject.getX().size() == 1
						&& renderedObject.getY() != null && renderedObject.getY().size() == 1) {
					result.objectLatLon = new LatLon(MapUtils.get31LatitudeY(renderedObject.getY().get(0)),
							MapUtils.get31LongitudeX(renderedObject.getX().get(0)));
				} else if (renderedObject.getLabelLatLon() != null) {
					result.objectLatLon = renderedObject.getLabelLatLon();
				}
				LatLon searchLatLon = result.objectLatLon != null ? result.objectLatLon : result.pointLatLon;
				if (isTravelGpx) {
					addTravelGpx(result, renderedObject, filter);
				} else if (isRoute) {
					addRoute(result, tileBox, point);
				} else {
					addRenderedObject(result, renderedObject, searchLatLon);
				}
			}
		}
	}

	private void selectObjectsFromOpenGl(@NonNull MapSelectionResult result, @NonNull RotatedTileBox tileBox,
	                                     @NonNull PointF point) {
		MapRendererView rendererView = view.getMapRenderer();
		if (rendererView != null) {
			int delta = 20;
			PointI tl = new PointI((int) point.x - delta, (int) point.y - delta);
			PointI br = new PointI((int) point.x + delta, (int) point.y + delta);
			MapSymbolInformationList symbols = rendererView.getSymbolsIn(new AreaI(tl, br), false);
			for (int i = 0; i < symbols.size(); i++) {
				MapSymbolInformation symbolInfo = symbols.get(i);
				IBillboardMapSymbol billboardMapSymbol;
				try {
					billboardMapSymbol = IBillboardMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
				} catch (Exception eBillboard) {
					billboardMapSymbol = null;
				}
				if (billboardMapSymbol != null) {
					double lat = Utilities.get31LatitudeY(billboardMapSymbol.getPosition31().getY());
					double lon = Utilities.get31LongitudeX(billboardMapSymbol.getPosition31().getX());
					result.objectLatLon = new LatLon(lat, lon);

					AdditionalBillboardSymbolInstanceParameters billboardAdditionalParams;
					try {
						billboardAdditionalParams = AdditionalBillboardSymbolInstanceParameters
								.dynamic_pointer_cast(symbolInfo.getInstanceParameters());
					} catch (Exception eBillboardParams) {
						billboardAdditionalParams = null;
					}
					if (billboardAdditionalParams != null && billboardAdditionalParams.getOverridesPosition31()) {
						lat = Utilities.get31LatitudeY(billboardAdditionalParams.getPosition31().getY());
						lon = Utilities.get31LongitudeX(billboardAdditionalParams.getPosition31().getX());
						result.objectLatLon = new LatLon(lat, lon);
					}

					Amenity amenity = null;
					net.osmand.core.jni.Amenity jniAmenity;
					try {
						jniAmenity = AmenitySymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr()).getAmenity();
					} catch (Exception eAmenity) {
						jniAmenity = null;
					}
					if (jniAmenity != null) {
						List<String> names = getValues(jniAmenity.getLocalizedNames());
						names.add(jniAmenity.getNativeName());
						long id = jniAmenity.getId().getId().longValue() >> 7;
						amenity = findAmenity(app, result.objectLatLon, names, id, AMENITY_SEARCH_RADIUS);
					} else {
						MapObject mapObject;
						try {
							mapObject = MapObjectSymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr()).getMapObject();
						} catch (Exception eMapObject) {
							mapObject = null;
						}
						if (mapObject != null) {
							ObfMapObject obfMapObject;
							try {
								obfMapObject = ObfMapObject.dynamic_pointer_cast(mapObject);
							} catch (Exception eObfMapObject) {
								obfMapObject = null;
							}
							if (obfMapObject != null) {
								Map<String, String> tags = getTags(obfMapObject.getResolvedAttributes());
								boolean isRoute = !Algorithms.isEmpty(RouteType.getRouteKeys(tags));
								if (isRoute) {
									addRoute(result, tileBox, point);
								} else {
									List<String> names = getValues(obfMapObject.getCaptionsInAllLanguages());
									names.add(obfMapObject.getCaptionInNativeLanguage());
									long id = obfMapObject.getId().getId().longValue() >> 7;
									amenity = findAmenity(app, result.objectLatLon, names, id, AMENITY_SEARCH_RADIUS);
									if (amenity != null && mapObject.getPoints31().size() > 1) {
										QVectorPointI points31 = mapObject.getPoints31();
										for (int k = 0; k < points31.size(); k++) {
											amenity.getX().add(points31.get(k).getX());
											amenity.getY().add(points31.get(k).getY());
										}
									}
								}
							}
						}
					}
					if (amenity != null && isUniqueAmenity(result.selectedObjects.keySet(), amenity)) {
						result.selectedObjects.put(amenity, mapLayers.getPoiMapLayer());
					}
				}
			}
		}
	}

	private void addTravelGpx(@NonNull MapSelectionResult result, @NonNull RenderedObject object, @Nullable String filter) {
		TravelGpx travelGpx = app.getTravelHelper().searchGpx(result.pointLatLon, filter, object.getTagValue("ref"));
		if (travelGpx != null && isUniqueGpx(result.selectedObjects, travelGpx)) {
			WptPt selectedPoint = new WptPt();
			selectedPoint.lat = result.pointLatLon.getLatitude();
			selectedPoint.lon = result.pointLatLon.getLongitude();
			SelectedGpxPoint selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
			result.selectedObjects.put(new Pair<>(travelGpx, selectedGpxPoint), mapLayers.getGpxLayer());
		}
	}

	private boolean isUniqueGpx(@NonNull Map<Object, IContextMenuProvider> selectedObjects,
	                            @NonNull TravelGpx travelGpx) {
		String tracksDir = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR).getPath();
		File file = new File(tracksDir, travelGpx.getRouteId() + GPX_FILE_EXT);
		if (file.exists()) {
			return false;
		}
		for (Map.Entry<Object, IContextMenuProvider> entry : selectedObjects.entrySet()) {
			if (entry.getKey() instanceof Pair && entry.getValue() instanceof GPXLayer
					&& ((Pair<?, ?>) entry.getKey()).first instanceof TravelGpx) {
				TravelGpx object = (TravelGpx) ((Pair<?, ?>) entry.getKey()).first;
				if (travelGpx.equals(object)) {
					return false;
				}
			}
		}
		return true;
	}

	private void addRoute(@NonNull MapSelectionResult result, @NonNull RotatedTileBox tileBox, @NonNull PointF point) {
		int searchRadius = (int) (OsmandMapLayer.getScaledTouchRadius(app, OsmandMapLayer.getDefaultRadiusPoi(tileBox)) * 1.5f);
		LatLon minLatLon = NativeUtilities.getLatLonFromPixel(view.getMapRenderer(), tileBox,
				point.x - searchRadius, point.y - searchRadius);
		LatLon maxLatLon = NativeUtilities.getLatLonFromPixel(view.getMapRenderer(), tileBox,
				point.x + searchRadius, point.y + searchRadius);
		QuadRect rect = new QuadRect(minLatLon.getLongitude(), minLatLon.getLatitude(),
				maxLatLon.getLongitude(), maxLatLon.getLatitude());

		double searchDistance = RulerWidget.getRulerDistance(app, tileBox) / 2;
		putRouteGpxToSelected(result.selectedObjects, mapLayers.getGpxLayer(), rect, searchDistance);
	}

	private void putRouteGpxToSelected(@NonNull Map<Object, IContextMenuProvider> selectedObjects,
	                                   @NonNull IContextMenuProvider gpxMenuProvider,
	                                   @NonNull QuadRect rect, double searchDistance) {
		BinaryMapIndexReader[] readers = app.getResourceManager().getRoutingMapFiles();
		NetworkRouteSelectorFilter selectorFilter = new NetworkRouteSelectorFilter();
		NetworkRouteSelector routeSelector = new NetworkRouteSelector(readers, selectorFilter);
		List<NetworkRouteSegment> segmentList = new ArrayList<>();
		try {
			segmentList.addAll(routeSelector.getFirstSegments(rect, null, searchDistance));
		} catch (IOException e) {
			log.error(e);
		}
		for (NetworkRouteSegment routeSegment : segmentList) {
			if (isUniqueRoute(selectedObjects.keySet(), routeSegment)) {
				selectedObjects.put(new Pair<>(routeSegment, rect), gpxMenuProvider);
			}
		}
	}

	private boolean isUniqueRoute(@NonNull Set<Object> set, @NonNull NetworkRouteSegment routeSegment) {
		for (Object selectedObject : set) {
			if (selectedObject instanceof Pair && ((Pair<?, ?>) selectedObject).first instanceof NetworkRouteSegment) {
				NetworkRouteSegment rs = (NetworkRouteSegment) ((Pair<?, ?>) selectedObject).first;
				if (rs.routeKey.equals(routeSegment.routeKey)) {
					return false;
				}
			}
		}
		return true;
	}

	private void addRenderedObject(@NonNull MapSelectionResult result, @NonNull RenderedObject object, @NonNull LatLon searchLatLon) {
		Amenity amenity = findAmenity(app, searchLatLon, object.getOriginalNames(), object.getId() >> 7, AMENITY_SEARCH_RADIUS);
		if (amenity != null) {
			if (object.getX() != null && object.getX().size() > 1 && object.getY() != null && object.getY().size() > 1) {
				amenity.getX().addAll(object.getX());
				amenity.getY().addAll(object.getY());
			}
			if (isUniqueAmenity(result.selectedObjects.keySet(), amenity)) {
				result.selectedObjects.put(amenity, mapLayers.getPoiMapLayer());
			}
		} else {
			result.selectedObjects.put(object, null);
		}
	}

	private boolean isUniqueAmenity(@NonNull Set<Object> set, @NonNull Amenity amenity) {
		for (Object o : set) {
			if (o instanceof Amenity && ((Amenity) o).compareTo(amenity) == 0) {
				return false;
			} else if (o instanceof TransportStop && ((TransportStop) o).getName().startsWith(amenity.getName())) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private List<String> getPublicTransportTypes() {
		if (publicTransportTypes == null && !app.isApplicationInitializing()) {
			PoiCategory category = app.getPoiTypes().getPoiCategoryByName("transportation");
			if (category != null) {
				publicTransportTypes = new ArrayList<>();
				List<PoiFilter> filters = category.getPoiFilters();
				for (PoiFilter poiFilter : filters) {
					if (poiFilter.getKeyName().equals("public_transport")) {
						for (PoiType poiType : poiFilter.getPoiTypes()) {
							publicTransportTypes.add(poiType.getKeyName());
							for (PoiType poiAdditionalType : poiType.getPoiAdditionals()) {
								publicTransportTypes.add(poiAdditionalType.getKeyName());
							}
						}
					}
				}
			}
		}
		return publicTransportTypes;
	}

	private void processTransportStops(@NonNull Map<Object, IContextMenuProvider> selectedObjects) {
		List<String> publicTransportTypes = getPublicTransportTypes();
		if (publicTransportTypes != null) {
			List<Amenity> transportStopAmenities = new ArrayList<>();
			for (Object object : selectedObjects.keySet()) {
				if (object instanceof Amenity) {
					Amenity amenity = (Amenity) object;
					if (!TextUtils.isEmpty(amenity.getSubType()) && publicTransportTypes.contains(amenity.getSubType())) {
						transportStopAmenities.add(amenity);
					}
				}
			}
			if (transportStopAmenities.size() > 0) {
				TransportStopsLayer transportStopsLayer = mapLayers.getTransportStopsLayer();
				for (Amenity amenity : transportStopAmenities) {
					TransportStop transportStop = TransportStopController.findBestTransportStopForAmenity(app, amenity);
					if (transportStop != null && transportStopsLayer != null) {
						selectedObjects.remove(amenity);
						selectedObjects.put(transportStop, transportStopsLayer);
					}
				}
			}
		}
	}

	@NonNull
	private static List<String> getValues(@Nullable QStringStringHash set) {
		List<String> res = new ArrayList<>();
		if (set != null) {
			QStringList keys = set.keys();
			for (int i = 0; i < keys.size(); i++) {
				res.add(set.get(keys.get(i)));
			}
		}
		return res;
	}

	@NonNull
	private static Map<String, String> getTags(@Nullable QStringStringHash set) {
		Map<String, String> res = new HashMap<>();
		if (set != null) {
			QStringList keys = set.keys();
			for (int i = 0; i < keys.size(); i++) {
				String key = keys.get(i);
				res.put(key, set.get(key));
			}
		}
		return res;
	}

	@Nullable
	public static Amenity findAmenity(@NonNull OsmandApplication app, @NonNull LatLon latLon,
	                                  @Nullable List<String> names, long id, int radius) {
		SearchPoiTypeFilter filter = new SearchPoiTypeFilter() {
			@Override
			public boolean accept(PoiCategory type, String subcategory) {
				return true;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}
		};
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(filter, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		Amenity res = null;
		for (Amenity amenity : amenities) {
			Long initAmenityId = amenity.getId();
			if (initAmenityId != null) {
				long amenityId = initAmenityId >> 1;
				if (amenityId == id && !amenity.isClosed()) {
					res = amenity;
					break;
				}
			}
		}
		if (res == null && !Algorithms.isEmpty(names)) {
			for (Amenity amenity : amenities) {
				for (String name : names) {
					if (name.equals(amenity.getName()) && !amenity.isClosed()) {
						res = amenity;
						break;
					}
				}
				if (res != null) {
					break;
				}
			}
		}
		return res;
	}

	static class MapSelectionResult {

		final LatLon pointLatLon;
		final Map<Object, IContextMenuProvider> selectedObjects;

		private LatLon objectLatLon;

		public MapSelectionResult(@NonNull Map<Object, IContextMenuProvider> selectedObjects,
		                          @NonNull LatLon pointLatLon) {
			this.pointLatLon = pointLatLon;
			this.selectedObjects = selectedObjects;
		}

		public LatLon getObjectLatLon() {
			return objectLatLon;
		}
	}
}
package net.osmand.plus.views.layers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.binary.BinaryMapIndexReader.ACCEPT_ALL_POI_TYPE_FILTER;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN;
import static net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import static net.osmand.router.network.NetworkRouteSelector.RouteKey;

import android.content.Context;
import android.graphics.PointF;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.RenderingContext;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AmenitySymbolsProvider.AmenitySymbolsGroup;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IBillboardMapSymbol;
import net.osmand.core.jni.IMapRenderer.MapSymbolInformation;
import net.osmand.core.jni.IOnPathMapSymbol;
import net.osmand.core.jni.MapObject;
import net.osmand.core.jni.MapObjectsSymbolsProvider.MapObjectSymbolsGroup;
import net.osmand.core.jni.MapSymbol;
import net.osmand.core.jni.MapSymbolInformationList;
import net.osmand.core.jni.MapSymbolsGroup.AdditionalBillboardSymbolInstanceParameters;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QStringList;
import net.osmand.core.jni.QStringStringHash;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.RasterMapSymbol;
import net.osmand.core.jni.Utilities;
import net.osmand.data.Amenity;
import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.gpx.GPXFile;
import net.osmand.osm.OsmRouteType;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.mapcontextmenu.controllers.TransportStopController;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer.OpenStreetNote;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.router.network.NetworkRouteSelector;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapSelectionHelper {

	private static final Log log = PlatformUtil.getLog(ContextMenuLayer.class);
	private static final int AMENITY_SEARCH_RADIUS = 50;
	private static final int AMENITY_SEARCH_RADIUS_FOR_RELATION = 500;
	private static final int TILE_SIZE = 256;

	private final OsmandApplication app;
	private final OsmandMapTileView view;
	private final MapLayers mapLayers;

	private List<String> publicTransportTypes;

	private Map<LatLon, BackgroundType> touchedFullMapObjects = new HashMap<>();
	private Map<LatLon, BackgroundType> touchedSmallMapObjects = new HashMap<>();

	public MapSelectionHelper(@NonNull Context context) {
		app = (OsmandApplication) context.getApplicationContext();
		view = app.getOsmandMap().getMapView();
		mapLayers = app.getOsmandMap().getMapLayers();
	}

	@NonNull
	public Map<LatLon, BackgroundType> getTouchedFullMapObjects() {
		return touchedFullMapObjects;
	}

	@NonNull
	public Map<LatLon, BackgroundType> getTouchedSmallMapObjects() {
		return touchedSmallMapObjects;
	}

	public boolean hasTouchedMapObjects() {
		return !touchedSmallMapObjects.isEmpty() || !touchedFullMapObjects.isEmpty();
	}

	public void clearTouchedMapObjects() {
		touchedFullMapObjects.clear();
		touchedSmallMapObjects.clear();
	}

	@NonNull
	protected MapSelectionResult selectObjectsFromMap(@NonNull PointF point, @NonNull RotatedTileBox tileBox, boolean showUnknownLocation) {
		LatLon pointLatLon = NativeUtilities.getLatLonFromElevatedPixel(view.getMapRenderer(), tileBox, point);
		NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
		Map<Object, IContextMenuProvider> selectedObjects = selectObjectsFromMap(tileBox, point, showUnknownLocation);

		MapSelectionResult result = new MapSelectionResult(selectedObjects, pointLatLon);
		if (app.useOpenGlRenderer()) {
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
	                                                                 boolean unknownLocation) {
		Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IContextMenuProvider) {
				List<Object> objects = new ArrayList<>();
				IContextMenuProvider provider = (IContextMenuProvider) layer;
				provider.collectObjectsFromPoint(point, tileBox, objects, unknownLocation, false);
				for (Object o : objects) {
					selectedObjects.put(o, provider);
				}
			}
		}
		return selectedObjects;
	}

	public void acquireTouchedMapObjects(@NonNull RotatedTileBox tileBox, @NonNull PointF point, boolean unknownLocation) {
		Map<LatLon, BackgroundType> touchedMapObjectsFull = new HashMap<>();
		Map<LatLon, BackgroundType> touchedMapObjectsSmall = new HashMap<>();
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IContextMenuProvider) {
				IContextMenuProvider provider = (IContextMenuProvider) layer;
				List<Object> collectedObjects = new ArrayList<>();
				provider.collectObjectsFromPoint(point, tileBox, collectedObjects, unknownLocation, true);
				for (Object o : collectedObjects) {
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
					if (layer.isPresentInFullObjects(latLon) && !touchedMapObjectsFull.containsKey(latLon)) {
						touchedMapObjectsFull.put(latLon, backgroundType);
					} else if (layer.isPresentInSmallObjects(latLon) && !touchedMapObjectsSmall.containsKey(latLon)) {
						touchedMapObjectsSmall.put(latLon, backgroundType);
					}
				}
			}
		}
		this.touchedFullMapObjects = touchedMapObjectsFull;
		this.touchedSmallMapObjects = touchedMapObjectsSmall;
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
			boolean selectedRoutes = false;
			for (RenderedObject renderedObject : renderedObjects) {
				String routeID = renderedObject.getRouteID();
				String fileName = renderedObject.getGpxFileName();
				String filter = routeID != null ? routeID : fileName;

				boolean isTravelGpx = !Algorithms.isEmpty(filter);
				boolean isRoute = !Algorithms.isEmpty(OsmRouteType.getRouteKeys(renderedObject.getTags()));
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
				} else {
					if (isRoute && !selectedRoutes) {
						selectedRoutes = true;
						NetworkRouteSelectorFilter routeFilter = createRouteFilter();
						if (!Algorithms.isEmpty(routeFilter.typeFilter)) {
							addRoute(result, tileBox, point, routeFilter);
						}
					}
					boolean amenityAdded = addAmenity(result, renderedObject, searchLatLon);
					if (!amenityAdded && !isRoute) {
						result.selectedObjects.put(renderedObject, null);
					}
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
			boolean selectedRoutes = false;
			MapSymbolInformationList symbols = rendererView.getSymbolsIn(new AreaI(tl, br), false);
			for (int i = 0; i < symbols.size(); i++) {
				MapSymbolInformation symbolInfo = symbols.get(i);
				IBillboardMapSymbol billboardMapSymbol = null;
				Amenity amenity = null;
				net.osmand.core.jni.Amenity jniAmenity = null;
				try {
					billboardMapSymbol = IBillboardMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
				} catch (Exception ignore) {
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

					try {
						jniAmenity = AmenitySymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr()).getAmenity();
					} catch (Exception ignore) {
					}
				} else {
					result.objectLatLon = NativeUtilities.getLatLonFromElevatedPixel(rendererView, tileBox, point);
				}
				if (jniAmenity != null) {
					List<String> names = getValues(jniAmenity.getLocalizedNames());
					names.add(jniAmenity.getNativeName());
					long id = jniAmenity.getId().getId().longValue();
					amenity = findAmenity(app, result.objectLatLon, names, id);
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
							boolean isRoute = !Algorithms.isEmpty(OsmRouteType.getRouteKeys(tags));
							if (isRoute && !selectedRoutes) {
								selectedRoutes = true;
								NetworkRouteSelectorFilter routeFilter = createRouteFilter();
								if (!Algorithms.isEmpty(routeFilter.typeFilter)) {
									addRoute(result, tileBox, point, routeFilter);
								}
							}
							IOnPathMapSymbol onPathMapSymbol = getOnPathMapSymbol(symbolInfo);
							if (onPathMapSymbol == null) {
								amenity = getAmenity(result.objectLatLon, obfMapObject);
								if (amenity != null) {
									amenity.setMapIconName(getMapIconName(symbolInfo));
								} else if (!isRoute) {
									addRenderedObject(result, symbolInfo, obfMapObject);
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

	@Nullable
	private String getMapIconName(MapSymbolInformation symbolInfo) {
		RasterMapSymbol rasterMapSymbol = getRasterMapSymbol(symbolInfo);
		if (rasterMapSymbol != null && rasterMapSymbol.getContentClass() == MapSymbol.ContentClass.Icon) {
			return rasterMapSymbol.getContent();
		}
		return null;
	}

	private void addRenderedObject(@NonNull MapSelectionResult result, @NonNull MapSymbolInformation symbolInfo,
	                               @NonNull ObfMapObject obfMapObject) {
		RasterMapSymbol rasterMapSymbol = getRasterMapSymbol(symbolInfo);
		if (rasterMapSymbol != null) {
			RenderedObject renderedObject = new RenderedObject();
			renderedObject.setId(obfMapObject.getId().getId().longValue());
			QVectorPointI points31 = obfMapObject.getPoints31();
			for (int k = 0; k < points31.size(); k++) {
				PointI pointI = points31.get(k);
				renderedObject.addLocation(pointI.getX(), pointI.getY());
			}
			double lat = MapUtils.get31LatitudeY(obfMapObject.getLabelCoordinateY());
			double lon = MapUtils.get31LongitudeX(obfMapObject.getLabelCoordinateX());
			renderedObject.setLabelLatLon(new LatLon(lat, lon));

			if (rasterMapSymbol.getContentClass() == MapSymbol.ContentClass.Caption) {
				renderedObject.setName(rasterMapSymbol.getContent());
			}
			if (rasterMapSymbol.getContentClass() == MapSymbol.ContentClass.Icon) {
				renderedObject.setIconRes(rasterMapSymbol.getContent());
			}
			result.selectedObjects.put(renderedObject, null);
		}
	}

	@Nullable
	private IOnPathMapSymbol getOnPathMapSymbol(@NonNull MapSymbolInformation symbolInfo) {
		try {
			return IOnPathMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
		} catch (Exception ignore) {
		}
		return null;
	}

	@Nullable
	private RasterMapSymbol getRasterMapSymbol(@NonNull MapSymbolInformation symbolInfo) {
		try {
			return RasterMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
		} catch (Exception ignore) {
		}
		return null;
	}

	private Amenity getAmenity(LatLon latLon, ObfMapObject obfMapObject) {
		Amenity amenity;
		List<String> names = getValues(obfMapObject.getCaptionsInAllLanguages());
		String caption = obfMapObject.getCaptionInNativeLanguage();
		if (!caption.isEmpty()) {
			names.add(caption);
		}
		long id = obfMapObject.getId().getId().longValue();
		amenity = findAmenity(app, latLon, names, id);
		if (amenity != null && obfMapObject.getPoints31().size() > 1) {
			QVectorPointI points31 = obfMapObject.getPoints31();
			for (int k = 0; k < points31.size(); k++) {
				amenity.getX().add(points31.get(k).getX());
				amenity.getY().add(points31.get(k).getY());
			}
		}
		return amenity;
	}

	private void addTravelGpx(@NonNull MapSelectionResult result, @NonNull RenderedObject object, @Nullable String filter) {
		TravelGpx travelGpx = app.getTravelHelper().searchGpx(result.pointLatLon, filter, object.getTagValue("ref"));
		if (travelGpx != null && isUniqueGpx(result.selectedObjects, travelGpx)) {
			WptPt selectedPoint = new WptPt();
			selectedPoint.setLat(result.pointLatLon.getLatitude());
			selectedPoint.setLon(result.pointLatLon.getLongitude());
			SelectedGpxPoint selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
			result.selectedObjects.put(new Pair<>(travelGpx, selectedGpxPoint), mapLayers.getTravelSelectionLayer());
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

	private void addRoute(@NonNull MapSelectionResult result, @NonNull RotatedTileBox tileBox, @NonNull PointF point,
	                      @NonNull NetworkRouteSelectorFilter selectorFilter) {
		int searchRadius = (int) (OsmandMapLayer.getScaledTouchRadius(app, tileBox.getDefaultRadiusPoi()) * 1.5f);
		LatLon minLatLon = NativeUtilities.getLatLonFromElevatedPixel(view.getMapRenderer(), tileBox,
				point.x - searchRadius, point.y - searchRadius);
		LatLon maxLatLon = NativeUtilities.getLatLonFromElevatedPixel(view.getMapRenderer(), tileBox,
				point.x + searchRadius, point.y + searchRadius);
		QuadRect rect = new QuadRect(minLatLon.getLongitude(), minLatLon.getLatitude(),
				maxLatLon.getLongitude(), maxLatLon.getLatitude());
		putRouteGpxToSelected(result.selectedObjects, mapLayers.getRouteSelectionLayer(), rect, selectorFilter);
	}

	private NetworkRouteSelectorFilter createRouteFilter() {
		NetworkRouteSelectorFilter routeSelectorFilter = new NetworkRouteSelectorFilter();
		Set<OsmRouteType> filteredOsmRouteTypes = new HashSet<>();
		List<RenderingRuleProperty> customRules = ConfigureMapUtils.getCustomRules(app,
				UI_CATEGORY_HIDDEN, RENDERING_CATEGORY_TRANSPORT);
		for (RenderingRuleProperty property : customRules) {
			String attrName = property.getAttrName();
			OsmRouteType osmRouteType = OsmRouteType.getByRenderingProperty(attrName);
			if (osmRouteType != null) {
				boolean enabled;
				if (HIKING.getRenderingPropertyAttr().equals(attrName)) {
					CommonPreference<String> pref = app.getSettings().getCustomRenderProperty(attrName);
					enabled = Arrays.asList(property.getPossibleValues()).contains(pref.get());
				} else {
					CommonPreference<Boolean> pref = app.getSettings().getCustomRenderBooleanProperty(attrName);
					enabled = pref.get();
				}
				if (enabled) {
					filteredOsmRouteTypes.add(osmRouteType);
				}
			}
		}
		if (!Algorithms.isEmpty(filteredOsmRouteTypes)) {
			routeSelectorFilter.typeFilter = filteredOsmRouteTypes;
		}
		return routeSelectorFilter;
	}

	private void putRouteGpxToSelected(@NonNull Map<Object, IContextMenuProvider> selectedObjects,
	                                   @NonNull IContextMenuProvider provider, @NonNull QuadRect rect,
	                                   @NonNull NetworkRouteSelectorFilter selectorFilter) {
		BinaryMapIndexReader[] readers = app.getResourceManager().getReverseGeocodingMapFiles();
		NetworkRouteSelector routeSelector = new NetworkRouteSelector(readers, selectorFilter, null);
		Map<RouteKey, GPXFile> routes = new LinkedHashMap<>();
		try {
			routes = routeSelector.getRoutes(rect, false, null);
		} catch (Exception e) {
			log.error(e);
		}
		for (RouteKey routeKey : routes.keySet()) {
			if (isUniqueRoute(selectedObjects.keySet(), routeKey)) {
				selectedObjects.put(new Pair<>(routeKey, rect), provider);
			}
		}
	}

	private boolean isUniqueRoute(@NonNull Set<Object> set, @NonNull RouteKey tmpRouteKey) {
		for (Object selectedObject : set) {
			if (selectedObject instanceof Pair && ((Pair<?, ?>) selectedObject).first instanceof RouteKey) {
				RouteKey routeKey = (RouteKey) ((Pair<?, ?>) selectedObject).first;
				if (routeKey.equals(tmpRouteKey)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean addAmenity(@NonNull MapSelectionResult result, @NonNull RenderedObject object, @NonNull LatLon searchLatLon) {
		Amenity amenity = findAmenity(app, searchLatLon, object.getOriginalNames(), object.getId());
		if (amenity != null) {
			if (object.getX() != null && object.getX().size() > 1 && object.getY() != null && object.getY().size() > 1) {
				amenity.getX().addAll(object.getX());
				amenity.getY().addAll(object.getY());
			}
			amenity.setMapIconName(object.getIconRes());
			if (isUniqueAmenity(result.selectedObjects.keySet(), amenity)) {
				result.selectedObjects.put(amenity, mapLayers.getPoiMapLayer());
			}
			return true;
		}
		return false;
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
					if (poiFilter.getKeyName().equals("public_transport") || poiFilter.getKeyName().equals("water_transport")) {
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

	public static Amenity findAmenity(@NonNull OsmandApplication app, @NonNull LatLon latLon,
	                                  @Nullable List<String> names, long id) {
		int searchRadius = ObfConstants.isIdFromRelation(id >> AMENITY_ID_RIGHT_SHIFT)
				? AMENITY_SEARCH_RADIUS_FOR_RELATION
				: AMENITY_SEARCH_RADIUS;
		return findAmenity(app, latLon, names, id, searchRadius);
	}

	@Nullable
	public static Amenity findAmenity(@NonNull OsmandApplication app, @NonNull LatLon latLon,
	                                  @Nullable List<String> names, long id, int radius) {
		id = ObfConstants.getOsmId(id >> AMENITY_ID_RIGHT_SHIFT);
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(ACCEPT_ALL_POI_TYPE_FILTER, rect, true);

		Amenity amenity = findAmenityByOsmId(amenities, id);
		if (amenity == null) {
			amenity = findAmenityByName(amenities, names);
		}
		return amenity;
	}

	@Nullable
	public static Amenity findAmenityByOsmId(@NonNull OsmandApplication app, @NonNull LatLon latLon, long osmId) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), AMENITY_SEARCH_RADIUS);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(ACCEPT_ALL_POI_TYPE_FILTER, rect, true);

		return findAmenityByOsmId(amenities, osmId);
	}

	@Nullable
	public static Amenity findAmenityByOsmId(@NonNull List<Amenity> amenities, long id) {
		for (Amenity amenity : amenities) {
			Long initAmenityId = amenity.getId();
			if (initAmenityId != null) {
				long amenityId;
				if (ObfConstants.isShiftedID(initAmenityId)) {
					amenityId = ObfConstants.getOsmId(initAmenityId);
				} else {
					amenityId = initAmenityId >> AMENITY_ID_RIGHT_SHIFT;
				}
				if (amenityId == id && !amenity.isClosed()) {
					return amenity;
				}
			}
		}
		return null;
	}

	@Nullable
	public static Amenity findAmenityByName(@NonNull List<Amenity> amenities, @Nullable List<String> names) {
		if (!Algorithms.isEmpty(names)) {
			for (Amenity amenity : amenities) {
				for (String name : names) {
					if (name.equals(amenity.getName()) && !amenity.isClosed()) {
						return amenity;
					}
				}
			}
		}
		return null;
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
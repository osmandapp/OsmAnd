package net.osmand.plus.views.layers;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.data.Amenity.ROUTE;
import static net.osmand.data.Amenity.ROUTE_ID;
import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.FavouritePoint.DEFAULT_BACKGROUND_TYPE;
import static net.osmand.osm.OsmRouteType.HIKING;
import static net.osmand.plus.transport.TransportLinesMenu.RENDERING_CATEGORY_TRANSPORT;
import static net.osmand.plus.wikivoyage.data.TravelGpx.TRAVEL_MAP_TO_POI_TAG;
import static net.osmand.render.RenderingRuleStorageProperties.UI_CATEGORY_HIDDEN;
import static net.osmand.router.network.NetworkRouteSelector.NetworkRouteSelectorFilter;
import static net.osmand.router.network.NetworkRouteSelector.RouteKey;

import android.content.Context;
import android.graphics.PointF;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.RenderingContext;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.AmenitySymbolsProvider.AmenitySymbolsGroup;
import net.osmand.core.jni.*;
import net.osmand.core.jni.MapObject;
import net.osmand.core.jni.IMapRenderer.MapSymbolInformation;
import net.osmand.core.jni.MapObjectsSymbolsProvider.MapObjectSymbolsGroup;
import net.osmand.core.jni.MapSymbolsGroup.AdditionalBillboardSymbolInstanceParameters;
import net.osmand.data.LatLon;
import net.osmand.data.*;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint;
import net.osmand.plus.plugins.osmedit.OsmBugsLayer.OpenStreetNote;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.track.clickable.ClickableWay;
import net.osmand.plus.track.clickable.ClickableWayHelper;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult.SelectedMapObject;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.router.network.NetworkRouteSelector;
import net.osmand.search.FullAmenitySearch;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.*;


public class MapSelectionHelper {

	private static final Log log = PlatformUtil.getLog(MapSelectionHelper.class);
	private static final int TILE_SIZE = 256;

	private static final String TAG_POI_LAT_LON = "osmand_poi_lat_lon";

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView view;
	private final MapLayers mapLayers;

	private final TransportStopHelper transportStopHelper;

	private Map<LatLon, BackgroundType> touchedFullMapObjects = new HashMap<>();
	private Map<LatLon, BackgroundType> touchedSmallMapObjects = new HashMap<>();

	private final ClickableWayHelper clickableWayHelper;

	public MapSelectionHelper(@NonNull Context context) {
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		view = app.getOsmandMap().getMapView();
		mapLayers = app.getOsmandMap().getMapLayers();
		transportStopHelper = new TransportStopHelper(app);
		clickableWayHelper = new ClickableWayHelper(app, view);
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
	MapSelectionResult collectObjectsFromMap(@NonNull PointF point,
			@NonNull RotatedTileBox tileBox, boolean showUnknownLocation) {
		MapSelectionResult result = new MapSelectionResult(app, tileBox, point);

		collectObjectsFromLayers(result, showUnknownLocation, false);
		collectObjectsFromMap(result, point, tileBox);

		//transportStopHelper.processTransportStops(result.getAllObjects());
		if (result.isEmpty()) {
			collectObjectsFromLayers(result, showUnknownLocation, true);
		}
		if (result.getAllObjects().size() > 1) {
			recollectForMultiselectMenu(result);
		}
		result.groupPoiByOsmIdAndWikidata();
		result.groupOtherObjects();
		return result;
	}

	private void collectObjectsFromMap(@NonNull MapSelectionResult result,
			@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		NativeOsmandLibrary nativeLib = NativeOsmandLibrary.getLoadedLibrary();
		if (app.useOpenGlRenderer()) {
			selectObjectsFromOpenGl(result, tileBox, point);
		} else if (nativeLib != null) {
			selectObjectsFromNative(result, nativeLib, tileBox, point);
		}
	}

	private void recollectForMultiselectMenu(MapSelectionResult result) {
		List<SelectedMapObject> selectedObjects = result.getAllObjects();
		for (int i = 0; i < selectedObjects.size(); i++) {
			SelectedMapObject sel = selectedObjects.get(i);
			if (sel.object() instanceof RenderedObject renderedObject) {
				LatLon l = renderedObject.getLatLon();
				if (l != null) {
					BaseDetailsObject pdo = findPlaceDetails(l, renderedObject.getId(), renderedObject.getOriginalNames(), renderedObject.getTagValue(WIKIDATA));
					if (pdo != null) {
						selectedObjects.set(i, new SelectedMapObject(pdo, sel.provider()));
					}
				}
			}
		}
	}

	protected void collectObjectsFromLayers(@NonNull MapSelectionResult result,
			boolean unknownLocation, boolean secondaryObjects) {
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IContextMenuProvider provider && (!provider.isSecondaryProvider() || secondaryObjects)) {
				provider.collectObjectsFromPoint(result, unknownLocation, false);
			}
		}
	}

	public void acquireTouchedMapObjects(@NonNull RotatedTileBox tileBox, @NonNull PointF point,
			boolean unknownLocation) {
		Map<LatLon, BackgroundType> touchedMapObjectsFull = new HashMap<>();
		Map<LatLon, BackgroundType> touchedMapObjectsSmall = new HashMap<>();
		for (OsmandMapLayer layer : view.getLayers()) {
			if (layer instanceof IContextMenuProvider provider) {
				MapSelectionResult result = new MapSelectionResult(app, tileBox, point);
				provider.collectObjectsFromPoint(result, unknownLocation, true);
				for (SelectedMapObject selectedObject : result.getAllObjects()) {
					Object object = selectedObject.object();
					LatLon latLon = provider.getObjectLocation(object);
					BackgroundType backgroundType = DEFAULT_BACKGROUND_TYPE;
					if (object instanceof OpenStreetNote) {
						backgroundType = BackgroundType.COMMENT;
					}
					if (object instanceof FavouritePoint) {
						backgroundType = ((FavouritePoint) object).getBackgroundType();
					}
					if (object instanceof WptPt) {
						backgroundType = BackgroundType.getByTypeName(((WptPt) object).getBackgroundType(), DEFAULT_BACKGROUND_TYPE);
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

	private void selectObjectsFromNative(@NonNull MapSelectionResult result,
			@NonNull NativeOsmandLibrary nativeLib, @NonNull RotatedTileBox tileBox,
			@NonNull PointF point) {
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
			Set<Long> uniqueRenderedObjectIds = new HashSet<>();
			boolean osmRoutesAlreadyAdded = false;
			for (RenderedObject renderedObject : renderedObjects) {
				Long objectId = renderedObject.getId();
				if (objectId != null && uniqueRenderedObjectIds.contains(objectId)) {
					log.warn("selectObjectsFromNative(v1) got duplicate: " + renderedObject);
					continue;
				}
				Map<String, String> tags = renderedObject.getTags();
				String travelGpxFilter = renderedObject.getRouteID();

				boolean isTravelGpx = app.getTravelHelper().isTravelGpxTags(tags);
				boolean isOsmRoute = !Algorithms.isEmpty(NetworkRouteSelector.getRouteKeys(tags));
				boolean isClickableWay = clickableWayHelper.isClickableWay(renderedObject);

				if (!isClickableWay && !isTravelGpx && !isOsmRoute && (renderedObject.getId() == null
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
					result.setObjectLatLon(new LatLon(MapUtils.get31LatitudeY(renderedObject.getY().get(0)),
							MapUtils.get31LongitudeX(renderedObject.getX().get(0))));
				} else if (renderedObject.getLabelLatLon() != null) {
					result.setObjectLatLon(renderedObject.getLabelLatLon());
				}
				LatLon searchLatLon = result.objectLatLon != null ? result.objectLatLon : result.getPointLatLon();

				if (isOsmRoute && !osmRoutesAlreadyAdded) {
					osmRoutesAlreadyAdded = addOsmRoutesAround(result, tileBox, point, createRouteFilter());
				}

				if (!isOsmRoute || !osmRoutesAlreadyAdded) {
					if (isTravelGpx) {
						addTravelGpx(result, travelGpxFilter);
					} else if (isClickableWay) {
						addClickableWay(result, clickableWayHelper.loadClickableWay(result.getPointLatLon(), renderedObject));
					}
				}

				boolean allowAmenityObjects = !isTravelGpx;

				if (allowAmenityObjects) {
					boolean amenityAdded = addAmenity(result, renderedObject, searchLatLon);
					if (!amenityAdded) {
						result.collect(renderedObject, null);
					}
				}

				if (objectId != null) {
					uniqueRenderedObjectIds.add(objectId);
				}
			}
		}
	}

	private void selectObjectsFromOpenGl(@NonNull MapSelectionResult result,
			@NonNull RotatedTileBox tileBox, @NonNull PointF point) {
		MapRendererView rendererView = view.getMapRenderer();
		if (rendererView != null) {
			int delta = 20;
			PointI tl = new PointI((int) point.x - delta, (int) point.y - delta);
			PointI br = new PointI((int) point.x + delta, (int) point.y + delta);
			boolean osmRoutesAlreadyAdded = false;
			MapSymbolInformationList symbols = rendererView.getSymbolsIn(new AreaI(tl, br), false);
			for (int i = 0; i < symbols.size(); i++) {
				MapSymbolInformation symbolInfo = symbols.get(i);
				if (symbolInfo.getMapSymbol().getIgnoreClick()) {
					continue;
				}
				IBillboardMapSymbol billboardMapSymbol = null;
				BaseDetailsObject detailsObject = null;
				net.osmand.core.jni.Amenity jniAmenity = null;
				try {
					billboardMapSymbol = IBillboardMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
				} catch (Exception ignore) {
				}
				if (billboardMapSymbol != null) {
					double lat = Utilities.get31LatitudeY(billboardMapSymbol.getPosition31().getY());
					double lon = Utilities.get31LongitudeX(billboardMapSymbol.getPosition31().getX());
					result.setObjectLatLon(new LatLon(lat, lon));

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
						result.setObjectLatLon(new LatLon(lat, lon));
					}

					try {
						jniAmenity = AmenitySymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr()).getAmenity();
					} catch (Exception ignore) {
					}
				} else {
					result.setObjectLatLon(NativeUtilities.getLatLonFromElevatedPixel(rendererView, tileBox, point));
				}
				if (jniAmenity != null) {
					List<String> names = getValues(jniAmenity.getLocalizedNames());
					names.add(jniAmenity.getNativeName());
					long id = jniAmenity.getId().getId().longValue();
					detailsObject = findPlaceDetails(result.objectLatLon, id, names, null);
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
							Map<String, String> tags = getOrderedTags(obfMapObject.getResolvedAttributesListPairs());

							boolean isTravelGpx = app.getTravelHelper().isTravelGpxTags(tags);
							boolean isOsmRoute = !Algorithms.isEmpty(NetworkRouteSelector.getRouteKeys(tags));
							boolean isClickableWay = clickableWayHelper.isClickableWay(obfMapObject, tags);

							if (isOsmRoute && !osmRoutesAlreadyAdded) {
								osmRoutesAlreadyAdded = addOsmRoutesAround(result, tileBox, point, createRouteFilter());
							}

							if (!isOsmRoute || !osmRoutesAlreadyAdded) {
								if (isTravelGpx) {
									addTravelGpx(result, tags.get(ROUTE_ID));
								} else if (isClickableWay) {
									addClickableWay(result,
											clickableWayHelper.loadClickableWay(result.getPointLatLon(), obfMapObject, tags));
								}
							}

							boolean allowAmenityObjects = !isTravelGpx;

							if (allowAmenityObjects) {
								IOnPathMapSymbol onPathMapSymbol = getOnPathMapSymbol(symbolInfo);
								if (onPathMapSymbol == null) {
									LatLon latLon = result.objectLatLon;
									if (tags.containsKey(TAG_POI_LAT_LON)) {
										LatLon l = parsePoiLatLon(tags.get(TAG_POI_LAT_LON));
										latLon = l == null ? latLon : l;
										tags.remove(TAG_POI_LAT_LON);
									}

									boolean allowRenderedObjects = !isOsmRoute && !isClickableWay
											&& !NetworkRouteSelector.containsUnsupportedRouteTags(tags);

									if (allowRenderedObjects) {
										addRenderedObject(result, symbolInfo, obfMapObject, tags);
									} else {
										long id = obfMapObject.getId().getId().longValue();
										detailsObject = findPlaceDetails(latLon, id, getNames(obfMapObject, tags), tags.get(WIKIDATA));
										if (detailsObject != null) {
											detailsObject.setMapIconName(getMapIconName(symbolInfo));
											addGeometry(detailsObject, obfMapObject);
											detailsObject.setObfResourceName(obfMapObject.getObfSection().getName());
										}
									}
								}
							}
						}
					}
				}
				if (detailsObject != null && !isTransportStop(result.getAllObjects(), detailsObject)) {
					result.collect(detailsObject, mapLayers.getPoiMapLayer());
				}
			}
		}
	}

	@Nullable
	private LatLon parsePoiLatLon(String value) {
		if (value == null) {
			return null;
		}
		GeoParsedPoint p = MapUtils.decodeShortLinkString(value);
		return new LatLon(p.getLatitude(), p.getLongitude());
	}

	@Nullable
	private String getMapIconName(MapSymbolInformation symbolInfo) {
		RasterMapSymbol rasterMapSymbol = getRasterMapSymbol(symbolInfo);
		if (rasterMapSymbol != null && rasterMapSymbol.getContentClass() == MapSymbol.ContentClass.Icon) {
			return rasterMapSymbol.getContent();
		}
		return null;
	}

	private void addRenderedObject(@NonNull MapSelectionResult result,
			@NonNull MapSymbolInformation symbolInfo,
			@NonNull ObfMapObject obfMapObject, Map<String, String> tags) {
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
			for (Map.Entry<String, String> entry : tags.entrySet()) {
				renderedObject.putTag(entry.getKey(), entry.getValue());
			}
			result.collect(renderedObject, null);
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

	private void addTravelGpx(@NonNull MapSelectionResult result, @Nullable String routeId) {
		TravelGpx travelGpx = app.getTravelHelper().searchTravelGpx(result.getPointLatLon(), routeId);
		if (travelGpx != null && isUniqueTravelGpx(result.getAllObjects(), travelGpx)) {
			WptPt selectedPoint = new WptPt();
			selectedPoint.setLat(result.getPointLatLon().getLatitude());
			selectedPoint.setLon(result.getPointLatLon().getLongitude());
			SelectedGpxPoint selectedGpxPoint = new SelectedGpxPoint(null, selectedPoint);
			result.collect(new Pair<>(travelGpx, selectedGpxPoint), mapLayers.getTravelSelectionLayer());
		} else if (travelGpx == null) {
			log.error("addTravelGpx() searchTravelGpx() travelGpx is null");
		}
	}

	private boolean addClickableWay(@NonNull MapSelectionResult result,
			@Nullable ClickableWay clickableWay) {
		if (clickableWay != null && isUniqueClickableWay(result.getAllObjects(), clickableWay)) {
			result.collect(clickableWay, clickableWayHelper.getContextMenuProvider());
			return true;
		}
		return false;
	}

	private List<String> getNames(@NonNull ObfMapObject obfMapObject, @NonNull Map<String, String> tags) {
		List<String> names = getValues(obfMapObject.getCaptionsInAllLanguages());
		String caption = obfMapObject.getCaptionInNativeLanguage();
		if (!caption.isEmpty()) {
			names.add(caption);
		}
		if (!Algorithms.isEmpty(tags) && tags.containsKey(TRAVEL_MAP_TO_POI_TAG) && "point".equals(tags.get(ROUTE))) {
			names.add(tags.get(TRAVEL_MAP_TO_POI_TAG));
		}
		return names;
	}

	private void addGeometry(@Nullable BaseDetailsObject detailObj, @NonNull ObfMapObject obfMapObject) {
		if (detailObj != null && !detailObj.hasGeometry() && obfMapObject.getPoints31().size() > 1) {
			QVectorPointI points31 = obfMapObject.getPoints31();
			for (int k = 0; k < points31.size(); k++) {
				detailObj.addX(points31.get(k).getX());
				detailObj.addY(points31.get(k).getY());
			}
		}
	}

	@Nullable
	private BaseDetailsObject findPlaceDetails(LatLon latLon, long id, @Nullable Collection<String> names, String wikidata) {
		FullAmenitySearch fullAmenitySearch = app.getResourceManager().getAmenitySearcher();
		BaseDetailsObject base = fullAmenitySearch.findPlaceDetails(latLon, id, names, wikidata);
		if (base != null) {
			return new BaseDetailsObject(base, LocaleHelper.getPreferredPlacesLanguage(app));
		}
		return null;
	}

	private boolean isUniqueGpxFileName(@NonNull List<SelectedMapObject> selectedObjects,
			@NonNull String gpxFileName) {
		for (SelectedMapObject selectedObject : selectedObjects) {
			Object object = selectedObject.object();
			if (object instanceof SelectedGpxPoint gpxPoint && selectedObject.provider() instanceof GPXLayer) {
				if (gpxPoint.getSelectedGpxFile().getGpxFile().getPath().endsWith(gpxFileName)) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isUniqueClickableWay(@NonNull List<SelectedMapObject> selectedObjects,
			@NonNull ClickableWay clickableWay) {
		for (SelectedMapObject selectedObject : selectedObjects) {
			if (selectedObject.object() instanceof ClickableWay that && clickableWay.getOsmId() == that.getOsmId()) {
				return false;
			}
		}
		return isUniqueGpxFileName(selectedObjects, clickableWay.getGpxFileName() + GPX_FILE_EXT);
	}

	private boolean isUniqueTravelGpx(@NonNull List<SelectedMapObject> selectedObjects,
			@NonNull TravelGpx travelGpx) {
		for (SelectedMapObject selectedObject : selectedObjects) {
			Object object = selectedObject.object();
			if (object instanceof Pair && selectedObject.provider() instanceof GPXLayer
					&& ((Pair<?, ?>) object).first instanceof TravelGpx gpx && travelGpx.equals(gpx)) {
				return false;
			}
		}
		return isUniqueGpxFileName(selectedObjects, travelGpx.getGpxFileName() + GPX_FILE_EXT);
	}

	private boolean addOsmRoutesAround(@NonNull MapSelectionResult result,
			@NonNull RotatedTileBox tileBox, @NonNull PointF point,
			@NonNull NetworkRouteSelectorFilter selectorFilter) {
		if (Algorithms.isEmpty(selectorFilter.typeFilter)) {
			return false;
		}
		int searchRadius = (int) (OsmandMapLayer.getScaledTouchRadius(app, tileBox.getDefaultRadiusPoi()) * 1.5f);
		LatLon minLatLon = NativeUtilities.getLatLonFromElevatedPixel(view.getMapRenderer(), tileBox,
				point.x - searchRadius, point.y - searchRadius);
		LatLon maxLatLon = NativeUtilities.getLatLonFromElevatedPixel(view.getMapRenderer(), tileBox,
				point.x + searchRadius, point.y + searchRadius);
		QuadRect rect = new QuadRect(minLatLon.getLongitude(), minLatLon.getLatitude(),
				maxLatLon.getLongitude(), maxLatLon.getLatitude());
		return putRouteGpxToSelected(result.getAllObjects(), mapLayers.getRouteSelectionLayer(), rect, selectorFilter);
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
					CommonPreference<String> pref = settings.getCustomRenderProperty(attrName);
					enabled = property.containsValue(pref.get());
				} else {
					enabled = settings.getRenderBooleanPropertyValue(attrName);
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

	private boolean putRouteGpxToSelected(@NonNull List<SelectedMapObject> selectedObjects,
			@NonNull IContextMenuProvider provider, @NonNull QuadRect rect,
			@NonNull NetworkRouteSelectorFilter selectorFilter) {
		int added = 0;
		BinaryMapIndexReader[] readers = app.getResourceManager().getReverseGeocodingMapFiles();
		NetworkRouteSelector routeSelector = new NetworkRouteSelector(readers, selectorFilter, null);
		Map<RouteKey, GpxFile> routes = new LinkedHashMap<>();
		try {
			routes = routeSelector.getRoutes(rect, false, null);
		} catch (Exception e) {
			log.error(e);
		}
		for (RouteKey routeKey : routes.keySet()) {
			if (isUniqueOsmRoute(selectedObjects, routeKey)) {
				selectedObjects.add(new SelectedMapObject(new Pair<>(routeKey, rect), provider));
				added++;
			}
		}
		return added > 0;
	}

	private boolean isUniqueOsmRoute(@NonNull List<SelectedMapObject> selectedObjects, @NonNull RouteKey tmpKey) {
		for (SelectedMapObject selectedObject : selectedObjects) {
			Object object = selectedObject.object();
			if (object instanceof Pair && ((Pair<?, ?>) object).first instanceof RouteKey key && key.equals(tmpKey)) {
				return false;
			}
		}
		return true;
	}

	private boolean addAmenity(@NonNull MapSelectionResult result,
			@NonNull RenderedObject object, @NonNull LatLon searchLatLon) {
		BaseDetailsObject detail = findPlaceDetails(searchLatLon, object.getId(), object.getOriginalNames(), null);
		if (detail != null) {
			if (object.getX() != null && object.getX().size() > 1 && object.getY() != null && object.getY().size() > 1) {
				detail.setX(object.getX());
				detail.setY(object.getY());
			}
			detail.setMapIconName(object.getIconRes());
			if (!isTransportStop(result.getAllObjects(), detail)) {
				result.collect(detail, mapLayers.getPoiMapLayer());
			}
			return true;
		}
		return false;
	}

	private boolean isTransportStop(@NonNull List<SelectedMapObject> selectedObjects, @NonNull BaseDetailsObject detail) {
		for (SelectedMapObject selectedObject : selectedObjects) {
			Object sel = selectedObject.object();
			if (sel instanceof TransportStop stop && stop.getName().startsWith(detail.getSyntheticAmenity().getName())) {
				return true;
			}
		}
		return false;
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
	private static Map<String, String> getOrderedTags(@Nullable QStringStringList tagsList) {
		Map<String, String> tagsMap = new LinkedHashMap<>();
		if (tagsList != null) {
			for (int i = 0; i < tagsList.size(); i++) {
				QStringStringPair pair = tagsList.get(i);
				tagsMap.put(pair.getFirst(), pair.getSecond());
			}
		}
		return tagsMap;
	}
}
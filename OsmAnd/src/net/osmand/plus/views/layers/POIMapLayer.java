package net.osmand.plus.views.layers;

import static net.osmand.core.android.MapRendererContext.SELECTED_POI_SECTION;
import static net.osmand.core.android.MapRendererContext.TOP_PLACES_POI_SECTION;
import static net.osmand.data.PointDescription.POINT_TYPE_POI;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.plus.utils.AndroidUtils.dpToPx;

import android.app.Dialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QListMapMarker;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.*;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.exploreplaces.ExplorePlacesFragment;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiFilterUtils;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.render.TravelRendererHelper.OnFileVisibilityChangeListener;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.listitems.QuickSearchWikiItem;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.clickable.ClickableWayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.POITileProvider;
import net.osmand.plus.widgets.WebViewEx;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.shared.util.ImageLoaderCallback;
import net.osmand.shared.util.LoadingImage;
import net.osmand.shared.util.NetworkImageLoader;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.*;
import java.util.stream.Collectors;

public class POIMapLayer extends OsmandMapLayer implements IContextMenuProvider,
		MapTextProvider<Amenity>, IRouteInformationListener, OnFileVisibilityChangeListener {
	private static final int START_ZOOM = 5;
	private static final int START_ZOOM_ROUTE_TRACK = 11;
	private static final int END_ZOOM_ROUTE_TRACK = 22;
	private static final int TOP_PLACES_LIMIT = 20;
	public static final int TILE_POINTS_LIMIT = 25;

	private static final Log LOG = PlatformUtil.getLog(POIMapLayer.class);

	private final OsmandApplication app;
	private final RoutingHelper routingHelper;
	private Set<PoiUIFilter> filters = new TreeSet<>();
	private MapTextLayer mapTextLayer;

	private POITileProvider poiTileProvider;
	private float textScale = 1f;
	private boolean nightMode;
	private boolean textVisible;

	private final TravelRendererHelper travelRendererHelper;
	private boolean showTravel;
	private boolean routeArticleFilterEnabled;
	private boolean routeArticlePointsFilterEnabled;
	private boolean routeTrackFilterEnabled;
	private boolean routeTrackAsPoiFilterEnabled;
	private PoiUIFilter routeArticleFilter;
	private PoiUIFilter routeArticlePointsFilter;
	private Set<PoiUIFilter> routeTrackFilters;
	private String routeArticlePointsFilterByName;
	private boolean fileVisibilityChanged;
	public CustomMapObjects<Amenity> customObjectsDelegate;
	private int topPlacesLimit = TOP_PLACES_LIMIT;

	private static final int SELECTED_MARKER_ID = -1;
	private static final int IMAGE_ICON_BORDER_DP = 2;
	private static final int IMAGE_ICON_SIZE_DP = 45;
	private static final int IMAGE_ICON_OUTER_COLOR = 0xffffffff;
	private static Bitmap imageCircleBitmap;
	private NetworkImageLoader imageLoader;
	private Map<String, LoadingImage> loadingImages;
	private Map<Long, Amenity> topPlaces;
	private Map<Long, Bitmap> topPlacesBitmaps;
	private List<Amenity> visiblePlaces;
	private DataSourceType wikiDataSource;
	private boolean showTopPlacesPreviews;
	private PoiUIFilter topPlacesFilter;
	private RotatedTileBox topPlacesBox;
	private Pair<BaseDetailsObject, Amenity> selectedTopPlace;
	protected MapMarkersCollection selectedTopPlaceCollection;

	/// cache for displayed POI
	// Work with cache (for map copied from AmenityIndexRepositoryOdb)
	private final MapLayerData<List<Amenity>> data;

	private record MapTopPlace(int placeId, @NonNull PointI position, @Nullable Bitmap imageBitmap,
	                           boolean alreadyExists) {
	}

	public interface PoiUIFilterResultMatcher<T> extends ResultMatcher<T> {
		void defferedResults();
	}

	public POIMapLayer(@NonNull Context context) {
		super(context);
		app = (OsmandApplication) context.getApplicationContext();
		routingHelper = app.getRoutingHelper();
		updateTopPlacesLimit();
		travelRendererHelper = app.getTravelRendererHelper();
		showTravel = app.getSettings().SHOW_TRAVEL.get();
		routeArticleFilterEnabled = travelRendererHelper.getRouteArticlesProperty().get();
		routeArticlePointsFilterEnabled = travelRendererHelper.getRouteArticlePointsProperty().get();
		routeArticleFilter = travelRendererHelper.getRouteArticleFilter();
		routeArticlePointsFilter = travelRendererHelper.getRouteArticlePointsFilter();
		routeTrackFilters = travelRendererHelper.getRouteTrackFilters();
		routeArticlePointsFilterByName = routeArticlePointsFilter != null ? routeArticlePointsFilter.getFilterByName() : null;

		routingHelper.addListener(this);
		travelRendererHelper.addFileVisibilityListener(this);
		data = new MapLayerData<>() {

            Set<PoiUIFilter> calculatedFilters;

            {
                ZOOM_THRESHOLD = 0;
            }

            @Override
            public boolean isInterrupted() {
                return super.isInterrupted();
            }

            @Override
            public void layerOnPreExecute() {
                calculatedFilters = collectFilters();
            }

            @Override
            public void layerOnPostExecute() {
				if (isDefferedResults()) {
					clearPoiTileProvider();
					setDefferedResults(false);
				}
				MapRendererView mapRenderer = getMapRenderer();
				if (mapRenderer != null) {
					mapRenderer.updateSubsection(TOP_PLACES_POI_SECTION);
					mapRenderer.updateSubsection(SELECTED_POI_SECTION);
				}
				topPlacesBox = null;
                app.getOsmandMap().refreshMap();
            }

            @Override
            protected Pair<List<Amenity>, List<Amenity>> calculateResult(@NonNull QuadRect latLonBounds, int zoom) {
                if (customObjectsDelegate != null) {
					List<Amenity> mapObjects = customObjectsDelegate.getMapObjects();
					return new Pair<>(mapObjects, mapObjects);
                }
                if (calculatedFilters.isEmpty()) {
					topPlacesFilter = null;
                    return new Pair<>(Collections.emptyList(), Collections.emptyList());
                }
                int z = (int) Math.floor(zoom + Math.log(getMapDensity()) / Math.log(2));

                List<Amenity> res = new ArrayList<>();
                Set<String> uniqueRouteIds = new HashSet<>();
				topPlacesFilter = null;
				for (PoiUIFilter filter : calculatedFilters) {
					if (filter.isTopImagesFilter()) {
						topPlacesFilter = filter;
					}
				}
                PoiFilterUtils.combineStandardPoiFilters(calculatedFilters, app);
                for (PoiUIFilter filter : calculatedFilters) {
                    List<Amenity> amenities = filter.searchAmenities(latLonBounds.top, latLonBounds.left,
							latLonBounds.bottom, latLonBounds.right, z, new PoiUIFilterResultMatcher<>() {

								@Override
								public void defferedResults() {
									setDefferedResults(true);
								}

								@Override
                                public boolean publish(Amenity object) {
                                    return true;
                                }

			                    @Override
			                    public boolean isCancelled() {
				                    return isInterrupted();
			                    }
		                    });
					if (filter.isTopWikiFilter()) {
						PoiFilterUtils.sortByElo(amenities);
						res.addAll(0, amenities);
					} else {
						for (Amenity amenity : amenities) {
							if (amenity.isRouteTrack()) {
								String routeId = amenity.getRouteId();
								if (routeId != null && !uniqueRouteIds.add(routeId)) {
									continue; // duplicate
								}
							}
							res.add(amenity);
						}
					}
				}
				Set<Amenity> displayedPoints = collectDisplayedPoints(latLonBounds, zoom, res);
				return new Pair<>(res, new ArrayList<>(displayedPoints));
			}

			@NonNull
			private Set<Amenity> collectDisplayedPoints(@NonNull QuadRect latLonBounds, int zoom, List<Amenity> res) {
				Set<Amenity> displayedPoints = new HashSet<>();
				int i = 0;
				for (Amenity amenity : res) {
					if (shouldDraw(amenity, zoom)) {
						displayedPoints.add(amenity);
						if (i++ > TOP_PLACES_LIMIT) {
							break;
						}
					}
				}
				float minTileX = (float) MapUtils.getTileNumberX(zoom, latLonBounds.left);
				float maxTileX = (float) MapUtils.getTileNumberX(zoom, latLonBounds.right);
				float minTileY = (float) MapUtils.getTileNumberY(zoom, latLonBounds.top);
				float maxTileY = (float) MapUtils.getTileNumberY(zoom, latLonBounds.bottom);
				for (int tileX = (int) minTileX; tileX <= (int) maxTileX; tileX++) {
					for (int tileY = (int) minTileY; tileY <= (int) maxTileY; tileY++) {
						QuadRect tileLatLonBounds = new QuadRect(
								MapUtils.getLongitudeFromTile(zoom, alignTile(zoom, tileX)),
								MapUtils.getLatitudeFromTile(zoom, alignTile(zoom, tileY)),
								MapUtils.getLongitudeFromTile(zoom, alignTile(zoom, tileX + 1.0)),
								MapUtils.getLatitudeFromTile(zoom, alignTile(zoom, tileY + 1.0)));
						QuadRect extTileLatLonBounds = new QuadRect(
								MapUtils.getLongitudeFromTile(zoom, alignTile(zoom, tileX - 0.5)),
								MapUtils.getLatitudeFromTile(zoom, alignTile(zoom, tileY - 0.5)),
								MapUtils.getLongitudeFromTile(zoom, alignTile(zoom, tileX + 1.5)),
								MapUtils.getLatitudeFromTile(zoom, alignTile(zoom, tileY + 1.5)));

						i = 0;
						for (Amenity amenity : res) {
							if (!shouldDraw(amenity, zoom)) {
								continue;
							}
							LatLon latLon = amenity.getLocation();
							if (extTileLatLonBounds.contains(latLon.getLongitude(), latLon.getLatitude(),
									latLon.getLongitude(), latLon.getLatitude())) {
								if (tileLatLonBounds.contains(latLon.getLongitude(), latLon.getLatitude(),
										latLon.getLongitude(), latLon.getLatitude())) {
									displayedPoints.add(amenity);
									if (++i == TILE_POINTS_LIMIT) {
										break;
									}
								}
							}
						}
					}
				}
				return displayedPoints;
			}

			private double alignTile(double zoom, double tile) {
				if (tile < 0) {
					return 0;
				}
				if (tile >= MapUtils.getPowZoom(zoom)) {
					return MapUtils.getPowZoom(zoom) - .000001;
				}
				return tile;
			}
		};
	}

	@Nullable
	public List<Amenity> getCurrentResults() {
		return data.getResults();
	}

	@Nullable
	public List<Amenity> getCurrentDisplayedResults() {
		return data.getDisplayedResults();
	}

	@Nullable
	public Map<Long, Amenity> getTopPlaces() {
		return topPlaces;
	}

	public List<Amenity> getVisiblePlaces() {
		return visiblePlaces;
	}

	@Nullable
	private Bitmap getTopPlaceBitmap(@NonNull Amenity place) {
		return topPlacesBitmaps != null ? topPlacesBitmaps.get(place.getId()) : null;
	}

	private void updateVisiblePlaces(@Nullable List<Amenity> places, @NonNull QuadRect latLonBounds) {
		if (places == null) {
			visiblePlaces = null;
			return;
		}
		List<Amenity> res = new ArrayList<>();
		for (Amenity place : places) {
			LatLon location = place.getLocation();
			if (latLonBounds.contains(location.getLongitude(), location.getLatitude(), location.getLongitude(), location.getLatitude())) {
				res.add(place);
			}
		}
		visiblePlaces = res;
	}

	private void updateTopPlaces(@NonNull List<Amenity> places, @NonNull QuadRect latLonBounds, int zoom) {
		Collection<Amenity> topPlacesList = null;
		if (topPlacesFilter != null) {
			topPlaces = obtainTopPlacesToDisplay(places, latLonBounds, zoom);
			topPlacesBitmaps = new HashMap<>();
			topPlacesList = topPlaces.values();
		}
		if (topPlacesList != null) {
			if (!topPlacesList.isEmpty()) {
				fetchImages(topPlacesList);
			} else {
				cancelLoadingImages();
			}
		}
	}

	private void fetchImages(@NonNull Collection<Amenity> places) {
		if (imageLoader == null) {
			imageLoader = new NetworkImageLoader(app, true);
		}
		if (loadingImages == null) {
			loadingImages = new HashMap<>();
		}
		Set<String> imagesToLoad = places.stream()
				.map(Amenity::getWikiIconUrl).collect(Collectors.toSet());
		loadingImages.entrySet().removeIf(entry -> {
			if (!imagesToLoad.contains(entry.getKey())) {
				entry.getValue().cancel();
				return true;
			}
			return false;
		});

		for (Amenity place : places) {
			Long placeId = place.getId();
			String url = place.getWikiIconUrl();
			if (getTopPlaceBitmap(place) != null || loadingImages.containsKey(url) || Algorithms.isEmpty(url)) {
				continue;
			}
			loadingImages.put(url, imageLoader.loadImage(url, new ImageLoaderCallback() {
				@Override
				public void onStart(@Nullable Bitmap bitmap) {
				}

				@Override
				public void onSuccess(@NonNull Bitmap bitmap) {
					app.runInUIThread(() -> {
						if (loadingImages != null) {
							loadingImages.remove(url);
						}
						if (topPlaces != null && topPlacesBitmaps != null) {
							Amenity p = topPlaces.get(placeId);
							if (p != null) {
								topPlacesBitmaps.put(placeId, bitmap);
								updateTopPlacesCollection();
							}
						}
					});
				}

				@Override
				public void onError() {
					app.runInUIThread(() -> {
						if (loadingImages != null) {
							loadingImages.remove(url);
						}
					});
					LOG.error(String.format("Coil failed to load %s", url));
				}
			}, false));
		}
	}

	private void cancelLoadingImages() {
		if (loadingImages != null) {
			loadingImages.values().forEach(LoadingImage::cancel);
			loadingImages = null;
			topPlaces = null;
			topPlacesBitmaps = null;
			visiblePlaces = null;
		}
	}

	@NonNull
	private Map<Long, Amenity> obtainTopPlacesToDisplay(@NonNull List<Amenity> places, @NonNull QuadRect latLonBounds, int zoom) {
		Map<Long, Amenity> res = new HashMap<>();

		long tileSize31 = (1L << (31 - zoom));
		double from31toPixelsScale = 256.0 / tileSize31;
		double estimatedIconSize = IMAGE_ICON_SIZE_DP * getTextScale();
		float iconSize31 = (float) (estimatedIconSize / from31toPixelsScale);

		int left = MapUtils.get31TileNumberX(latLonBounds.left);
		int top = MapUtils.get31TileNumberY(latLonBounds.top);
		int right = MapUtils.get31TileNumberX(latLonBounds.right);
		int bottom = MapUtils.get31TileNumberY(latLonBounds.bottom);
		QuadTree<QuadRect> boundIntersections = initBoundIntersections(left, top, right, bottom);
		int i = 0;
		for (Amenity place : places) {
			double lat = place.getLocation().getLatitude();
			double lon = place.getLocation().getLongitude();
			if (!latLonBounds.contains(lon, lat, lon, lat) || Algorithms.isEmpty(place.getWikiIconUrl())) {
				continue;
			}
			int x31 = MapUtils.get31TileNumberX(lon);
			int y31 = MapUtils.get31TileNumberY(lat);
			if (!intersectsD(boundIntersections, x31, y31, iconSize31, iconSize31)) {
				res.put(place.getId(), place);
			}
			if (i++ > topPlacesLimit) {
				break;
			}
		}
		return res;
	}

	@UiThread
	private void updateTopPlacesCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}

		List<Amenity> places = topPlaces != null ? new ArrayList<>(topPlaces.values()) : null;
		if (places == null) {
			clearMapMarkersCollections();
			return;
		}
		if (mapMarkersCollection == null) {
			mapMarkersCollection = new MapMarkersCollection();
		}
		QListMapMarker existingMapPoints = mapMarkersCollection.getMarkers();
		int[] existingIds = new int[(int) existingMapPoints.size()];
		for (int i = 0; i < existingMapPoints.size(); i++) {
			MapMarker mapPoint = existingMapPoints.get(i);
			existingIds[i] = mapPoint.getMarkerId();
		}
		List<MapTopPlace> mapPlaces = new ArrayList<>();
		for (Amenity place : places) {
			int placeId = place.getId() != null ? place.getId().intValue() : place.getTravelEloNumber();
			PointI position = NativeUtilities.getPoint31FromLatLon(place.getLocation().getLatitude(),
					place.getLocation().getLongitude());
			boolean alreadyExists = false;
			for (int i = 0; i < existingIds.length; i++) {
				if (placeId == existingIds[i]) {
					existingIds[i] = 0;
					alreadyExists = true;
					break;
				}
			}
			Bitmap topPlaceBitmap = getTopPlaceBitmap(place);
			if(topPlaceBitmap != null) {
				mapPlaces.add(new MapTopPlace(placeId, position, topPlaceBitmap, alreadyExists));
			}
			if(mapPlaces.size() == topPlacesLimit) {
				break;
			}
		}
		for (int i = 0; i < existingIds.length; i++) {
			if (existingIds[i] != 0) {
				mapMarkersCollection.removeMarker(existingMapPoints.get(i));
			}
		}
		for (MapTopPlace place : mapPlaces) {
			Bitmap imageBitmap = place.imageBitmap;
			if (place.alreadyExists) {
				continue;
			}
			Bitmap imageMapBitmap = createImageBitmap(imageBitmap, false);

			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			mapMarkerBuilder.setIsAccuracyCircleSupported(false)
					.setMarkerId(place.placeId)
					.setBaseOrder(getTopPlaceBaseOrder())
					.setPinIcon(NativeUtilities.createSkImageFromBitmap(imageMapBitmap))
					.setPosition(place.position)
					.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
					.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
					.buildAndAddToCollection(mapMarkersCollection);
		}
		mapRenderer.addSymbolsProvider(TOP_PLACES_POI_SECTION, mapMarkersCollection);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		updateTopPlacesLimit();
		cleanupResources();
		data.clearCache();
	}

	private void updateTopPlacesLimit() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			int screenSize = Math.min(AndroidUtils.getScreenHeight(activity), AndroidUtils.getScreenWidth(activity));
			topPlacesLimit = (int) ((float) screenSize / getBigIconSize() * 1.5);
		}
	}

	private Set<PoiUIFilter> collectFilters() {
		Set<PoiUIFilter> calculatedFilters = new TreeSet<>(filters);
		if (showTravel) {
			boolean routeArticleFilterEnabled = this.routeArticleFilterEnabled;
			PoiUIFilter routeArticleFilter = this.routeArticleFilter;
			if (routeArticleFilterEnabled && routeArticleFilter != null) {
				calculatedFilters.add(routeArticleFilter);
			}
			boolean routeArticlePointsFilterEnabled = this.routeArticlePointsFilterEnabled;
			PoiUIFilter routeArticlePointsFilter = this.routeArticlePointsFilter;
			if (routeArticlePointsFilterEnabled && routeArticlePointsFilter != null
					&& !Algorithms.isEmpty(routeArticlePointsFilter.getFilterByName())) {
				calculatedFilters.add(routeArticlePointsFilter);
			}
			boolean routeTrackAsPoiFilterEnabled = this.routeTrackAsPoiFilterEnabled;
			if (routeTrackAsPoiFilterEnabled && this.routeTrackFilters != null) {
				calculatedFilters.addAll(this.routeTrackFilters);
			}
		}
		return calculatedFilters;
	}

	public void collectAmenitiesFromPoint(@NonNull MapSelectionResult result) {
		PointF point = result.getPoint();
		RotatedTileBox tileBox = result.getTileBox();
		List<Amenity> objects = data.getDisplayedResults();

		if (tileBox.getZoom() >= START_ZOOM && !Algorithms.isEmpty(objects)) {
			MapRendererView mapRenderer = getMapRenderer();
			float radius = getScaledTouchRadius(view.getApplication(), getRadiusPoi(tileBox)) * TOUCH_RADIUS_MULTIPLIER;
			List<PointI> touchPolygon31 = null;
			if (mapRenderer != null) {
				touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
				if (touchPolygon31 == null) {
					return;
				}
			}

			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity amenity = objects.get(i);
					LatLon latLon = amenity.getLocation();
					boolean add = mapRenderer != null
							? NativeUtilities.isPointInsidePolygon(latLon, touchPolygon31)
							: tileBox.isLatLonNearPixel(latLon, point.x, point.y, radius);
					if (add) {
						if (topPlaces != null && topPlaces.containsValue(amenity)) {
							result.collect(amenity, this);
							break;
						}
						result.collect(amenity, this);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		double zoom = tb.getZoom();
		if (zoom < START_ZOOM) {
			r = 0;
		} else if (zoom <= 15) {
			r = 10;
		} else if (zoom <= 16) {
			r = 14;
		} else if (zoom <= 17) {
			r = 16;
		} else {
			r = 18;
		}

		return (int) (r * view.getScaleCoefficient());
	}

	private int getColor(@NonNull Amenity amenity) {
		int color = 0;
		if (ROUTE_ARTICLE_POINT.equals(amenity.getSubType())) {
			String colorStr = amenity.getColor();
			if (colorStr != null) {
				color = DefaultColors.valueOf(colorStr);
			}
		}
		return color != 0 ? color : ContextCompat.getColor(app, R.color.osmand_orange);
	}

	private boolean shouldDraw(int zoom) {
		if (!filters.isEmpty() && zoom >= START_ZOOM || customObjectsDelegate != null) {
			return true;
		} else if (filters.isEmpty()) {
			if ((travelRendererHelper.getRouteArticlesProperty().get() && routeArticleFilter != null
					|| travelRendererHelper.getRouteArticlePointsProperty().get() && routeArticlePointsFilter != null)
					&& zoom >= START_ZOOM) {
				return true;
			}
			if (travelRendererHelper.getRouteTracksAsPoiProperty().get() && routeTrackFilters != null) {
				return travelRendererHelper.getRouteTracksProperty().get()
						? zoom >= START_ZOOM : zoom >= START_ZOOM_ROUTE_TRACK;
			}
		}
		return false;
	}

	private boolean shouldDraw(@NonNull Amenity amenity, int zoom) {
		if (customObjectsDelegate != null) {
			return true;
		} else {
			boolean routeArticle = ROUTE_ARTICLE_POINT.equals(amenity.getSubType())
					|| ROUTE_ARTICLE.equals(amenity.getSubType());
			boolean routeTrack = amenity.isRouteTrack();
			if (routeArticle) {
				return zoom >= START_ZOOM;
			} else if (routeTrack) {
				if (travelRendererHelper.getRouteTracksProperty().get()) {
					return zoom >= START_ZOOM && zoom <= END_ZOOM_ROUTE_TRACK;
				} else {
					return zoom >= START_ZOOM_ROUTE_TRACK;
				}
			} else {
				return zoom >= START_ZOOM;
			}
		}
	}

	@Override
	public void fileVisibilityChanged() {
		this.fileVisibilityChanged = true;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		Set<PoiUIFilter> selectedPoiFilters = app.getPoiFilters().getSelectedPoiFilters();
		boolean showTravel = app.getSettings().SHOW_TRAVEL.get();
		boolean routeArticleFilterEnabled = travelRendererHelper.getRouteArticlesProperty().get();
		boolean routeArticlePointsFilterEnabled = travelRendererHelper.getRouteArticlePointsProperty().get();
		boolean routeTrackFilterEnabled = travelRendererHelper.getRouteTracksProperty().get();
		boolean routeTrackAsPoiFilterEnabled = travelRendererHelper.getRouteTracksAsPoiProperty().get();
		PoiUIFilter routeArticleFilter = travelRendererHelper.getRouteArticleFilter();
		PoiUIFilter routeArticlePointsFilter = travelRendererHelper.getRouteArticlePointsFilter();
		Set<PoiUIFilter> routeTrackFilters = travelRendererHelper.getRouteTrackFilters();
		String routeArticlePointsFilterByName = routeArticlePointsFilter != null ? routeArticlePointsFilter.getFilterByName() : null;
		DataSourceType wikiDataSource = app.getSettings().WIKI_DATA_SOURCE_TYPE.get();
		boolean dataChanged = false;
		if (this.filters != selectedPoiFilters
				|| this.wikiDataSource != wikiDataSource
				|| this.showTravel != showTravel
				|| this.routeArticleFilterEnabled != routeArticleFilterEnabled
				|| this.routeArticlePointsFilterEnabled != routeArticlePointsFilterEnabled
				|| this.routeTrackFilterEnabled != routeTrackFilterEnabled
				|| this.routeTrackAsPoiFilterEnabled != routeTrackAsPoiFilterEnabled
				|| this.routeArticleFilter != routeArticleFilter
				|| this.routeArticlePointsFilter != routeArticlePointsFilter
				|| this.routeTrackFilters != routeTrackFilters
				|| this.fileVisibilityChanged
				|| !Algorithms.stringsEqual(this.routeArticlePointsFilterByName, routeArticlePointsFilterByName)) {
			this.filters = selectedPoiFilters;
			this.wikiDataSource = wikiDataSource;
			this.showTravel = showTravel;
			this.routeArticleFilterEnabled = routeArticleFilterEnabled;
			this.routeArticlePointsFilterEnabled = routeArticlePointsFilterEnabled;
			this.routeTrackFilterEnabled = routeTrackFilterEnabled;
			this.routeTrackAsPoiFilterEnabled = routeTrackAsPoiFilterEnabled;
			this.routeArticleFilter = routeArticleFilter;
			this.routeArticlePointsFilter = routeArticlePointsFilter;
			this.routeTrackFilters = routeTrackFilters;
			this.routeArticlePointsFilterByName = routeArticlePointsFilterByName;
			this.fileVisibilityChanged = false;
			data.clearCache();
			dataChanged = true;
		}
		int zoom = tileBox.getZoom();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (shouldDraw(zoom)) {
				float textScale = app.getOsmandMap().getTextScale();
				boolean textScaleChanged = this.textScale != textScale;
				this.textScale = textScale;
				boolean nightMode = settings != null && settings.isNightMode();
				boolean nightModeChanged = this.nightMode != nightMode;
				this.nightMode = nightMode;
				boolean textVisible = isTextVisible();
				boolean textVisibleChanged = this.textVisible != textVisible;
				this.textVisible = textVisible;
				boolean updated = false;
				if (poiTileProvider == null || dataChanged || textScaleChanged || nightModeChanged
						|| textVisibleChanged || mapActivityInvalidated) {
					clearPoiTileProvider();
					clearMapMarkersCollections();
					if (!collectFilters().isEmpty()) {
						float density = view.getDensity();
						TextRasterizer.Style textStyle = MapTextLayer.getTextStyle(getContext(),
								nightMode, textScale, density);
						poiTileProvider = new POITileProvider(getContext(), data, getPointsOrder(), textVisible,
								textStyle, textScale, density);
						poiTileProvider.drawSymbols(mapRenderer);
					}
					updated = true;
				}
				boolean showTopPlacesPreviews = app.getSettings().WIKI_SHOW_IMAGE_PREVIEWS.get();
				boolean showTopPlacesPreviewsChanged = this.showTopPlacesPreviews != showTopPlacesPreviews;
				this.showTopPlacesPreviews = showTopPlacesPreviews;
				if (updated || showTopPlacesPreviewsChanged || topPlacesBox == null || !topPlacesBox.containsTileBox(tileBox)) {
					List<Amenity> places = data.getResults();
					updateVisiblePlaces(data.getDisplayedResults(), tileBox.getLatLonBounds());
					if (showTopPlacesPreviews && places != null) {
						RotatedTileBox extendedBox = tileBox.copy();
						int bigIconSize = getBigIconSize();
						extendedBox.increasePixelDimensions(bigIconSize * 2, bigIconSize * 2);
						topPlacesBox = extendedBox;
						updateTopPlaces(places, tileBox.getLatLonBounds(), zoom);
						updateTopPlacesCollection();
					} else {
						clearMapMarkersCollections();
						cancelLoadingImages();
					}
				}
				MapActivity mapActivity = getMapActivity();
				MapContextMenu contextMenu = mapActivity != null ? mapActivity.getContextMenu() : null;
				if (contextMenu != null) {
					if (selectedTopPlace == null && contextMenu.isVisible()) {
						Object object = contextMenu.getObject();
						if (object instanceof BaseDetailsObject detailsObject) {
							Amenity amenity = getSelectedTopPlace(detailsObject);
							if (amenity != null) {
								updateSelectedTopPlace(Pair.create(detailsObject, amenity));
							}
						}
					} else if (!contextMenu.isVisible() || contextMenu.getObject() != selectedTopPlace.first) {
						updateSelectedTopPlace(null);
					}
				}
			} else {
				clearPoiTileProvider();
				clearMapMarkersCollections();
				cancelLoadingImages();
			}
			mapActivityInvalidated = false;
			return;
		}
		List<Amenity> fullObjects = new ArrayList<>();
		List<LatLon> fullObjectsLatLon = new ArrayList<>();
		List<LatLon> smallObjectsLatLon = new ArrayList<>();
		if (shouldDraw(zoom)) {
			data.queryNewData(tileBox);
			List<Amenity> objects = data.getDisplayedResults();
			updateVisiblePlaces(data.getDisplayedResults(), tileBox.getLatLonBounds());
			if (objects != null) {
				float textScale = getTextScale();
				float iconSize = getIconSize(app);
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				WaypointHelper wph = app.getWaypointHelper();
				for (Amenity o : objects) {
					PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(
							getContext(), getColor(o), true);
					pointImageDrawable.setAlpha(0.8f);
					LatLon latLon = o.getLocation();
					float x = tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					float y = tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());

					if (tileBox.containsPoint(x, y, iconSize)) {
						boolean intersects = intersects(boundIntersections, x, y, iconSize, iconSize);
						boolean shouldShowNearbyPoi = app.getSettings().SHOW_NEARBY_POI.get()
								&& routingHelper.isFollowingMode();
						if (intersects || shouldShowNearbyPoi && !wph.isAmenityNoPassed(o)) {
							pointImageDrawable.drawSmallPoint(canvas, x, y, textScale);
							smallObjectsLatLon.add(latLon);
						} else {
							fullObjects.add(o);
							fullObjectsLatLon.add(latLon);
						}
					}
				}
				for (Amenity o : fullObjects) {
					LatLon latLon = o.getLocation();
					int x = (int) tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					if (tileBox.containsPoint(x, y, iconSize)) {
						String id = o.getGpxIcon();
						if (id == null) {
							id = RenderingIcons.getIconNameForAmenity(app, o);
						}
						if (id != null) {
							PointImageDrawable pointImageDrawable = PointImageUtils.getOrCreate(
									getContext(), getColor(o), true, RenderingIcons.getResId(id));
							pointImageDrawable.setAlpha(0.8f);
							pointImageDrawable.drawPoint(canvas, x, y, textScale, false);
						}
					}
				}
				this.fullObjectsLatLon = fullObjectsLatLon;
				this.smallObjectsLatLon = smallObjectsLatLon;
			}
		}
		mapTextLayer.putData(this, fullObjects);
		mapActivityInvalidated = false;
	}

	private void clearPoiTileProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && poiTileProvider != null) {
			poiTileProvider.clearSymbols(mapRenderer);
			poiTileProvider = null;
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		routingHelper.removeListener(this);
		travelRendererHelper.removeFileVisibilityListener(this);
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		imageCircleBitmap = null;
		clearSelectedTopPlaceCollection();
		clearPoiTileProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	private void clearSelectedTopPlaceCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && selectedTopPlaceCollection != null) {
			mapRenderer.removeSymbolsProvider(selectedTopPlaceCollection);
			selectedTopPlaceCollection = null;
		}
	}

	public static void showPlainDescriptionDialog(Context ctx, OsmandApplication app, String text, String title) {
		TextView textView = new TextView(ctx);
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int textMargin = dpToPx(app, 10f);
		boolean light = !app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		textView.setLayoutParams(llTextParams);
		textView.setPadding(textMargin, textMargin, textMargin, textMargin);
		textView.setTextSize(16);
		textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, !light));
		textView.setAutoLinkMask(Linkify.ALL);
		textView.setLinksClickable(true);
		textView.setText(text);

		showText(ctx, app, textView, title);
	}

	public static void showHtmlDescriptionDialog(Context ctx, OsmandApplication app, String html, String title) {
		WebViewEx webView = new WebViewEx(ctx);
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		webView.setLayoutParams(llTextParams);
		int margin = dpToPx(app, 10f);
		webView.setPadding(margin, margin, margin, margin);
		webView.setScrollbarFadingEnabled(true);
		webView.setVerticalScrollBarEnabled(false);
		webView.setBackgroundColor(Color.TRANSPARENT);
		webView.getSettings().setTextZoom((int) (app.getResources().getConfiguration().fontScale * 100f));
		boolean light = !app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		int textColor = ColorUtilities.getPrimaryTextColor(app, !light);
		String rgbHex = Algorithms.colorToString(textColor);
		html = "<body style=\"color:" + rgbHex + ";\">" + html + "</body>";
		String encoded = Base64.encodeToString(html.getBytes(), Base64.NO_PADDING);
		webView.loadData(encoded, "text/html", "base64");

		showText(ctx, app, webView, title);
	}

	static int getResIdFromAttribute(Context ctx, int attr) {
		if (attr == 0) {
			return 0;
		}
		TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	private static void showText(Context ctx, OsmandApplication app, View view, String title) {
		boolean light = !app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		Dialog dialog = new Dialog(ctx, light ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);

		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(ctx));
		topBar.setNavigationIcon(icBack);
		topBar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		topBar.setTitle(title);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTabBackground)));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTextColor)));
		topBar.setNavigationOnClickListener(v -> dialog.dismiss());

		ScrollView scrollView = new ScrollView(ctx);
		ll.addView(topBar);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1;
		ll.addView(scrollView, lp);
		scrollView.addView(view);

		dialog.setContentView(ll);
		dialog.setCancelable(true);
		dialog.show();
	}

	@Override
	public PointDescription getObjectName(Object o) {
		Amenity amenity = getAmenity(o);
		return amenity != null ? new PointDescription(POINT_TYPE_POI, getAmenityName(amenity)) : null;
	}

	@Nullable
	public Amenity getAmenity(@Nullable Object object) {
		if (object instanceof Amenity amenity) {
			return amenity;
		} else if (object instanceof BaseDetailsObject detailsObject) {
			return detailsObject.getSyntheticAmenity();
		}
		return null;
	}

	@Nullable
	public Amenity getSelectedTopPlace(@NonNull BaseDetailsObject detailsObject) {
		if (!Algorithms.isEmpty(topPlaces)) {
			for (Amenity amenity : detailsObject.getAmenities()) {
				Amenity topPlace = topPlaces.get(amenity.getId());
				if (topPlace != null) {
					return topPlace;
				}
			}
		}
		return null;
	}

	private void hideExplorePlacesFragment(@NonNull MapActivity mapActivity) {
		ExplorePlacesFragment explorePlacesFragment = mapActivity.getFragmentsHelper().getExplorePlacesFragment();
		if (explorePlacesFragment != null) {
			explorePlacesFragment.hideList();
		}
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (result.getTileBox().getZoom() >= START_ZOOM) {
			collectAmenitiesFromPoint(result);
		}
	}

	@Override
	public LatLon getObjectLocation(Object object) {
		Amenity amenity = getAmenity(object);
		if (amenity != null) {
			return amenity.getLocation();
		} else if (object instanceof RenderedObject renderedObject) {
			return renderedObject.getLatLon();
		}
		return null;
	}

	@Override
	public boolean showMenuAction(@Nullable Object object) {
		Amenity amenity = getAmenity(object);
		MapActivity activity = view.getMapActivity();
		if (activity != null && amenity != null) {
			TravelHelper travelHelper = app.getTravelHelper();
			ClickableWayHelper clickableWayHelper = app.getClickableWayHelper();
			if (amenity.isRouteTrack()) {
				TravelGpx travelGpx = new TravelGpx(amenity);
				travelHelper.openTrackMenu(travelGpx, activity, amenity.getGpxFileName(null), amenity.getLocation(), false);
				return true; // TravelGpx
			} else if (amenity.isRouteArticle()) {
				String lang = app.getLanguage();
				lang = amenity.getContentLanguage(Amenity.DESCRIPTION, lang, "en");
				String name = amenity.getGpxFileName(lang);
				TravelArticle article = travelHelper.getArticleByTitle(name, lang, true, null);
				if (article == null) {
					return false;
				}
				travelHelper.openTrackMenu(article, activity, name, amenity.getLocation(), false);
				return true; // TravelArticle
			} else if (clickableWayHelper.isClickableWayAmenity(amenity)) {
				clickableWayHelper.openClickableWayAmenity(amenity, false);
				return true; // ClickableWay
			}
		}
		return false;
	}

	@Override
	public LatLon getTextLocation(Amenity o) {
		return o.getLocation();
	}

	@Override
	public int getTextShift(Amenity amenity, RotatedTileBox rb) {
		return (int) (16 * rb.getDensity() * getTextScale());
	}

	@Override
	public String getText(Amenity o) {
		return getAmenityName(o);
	}

	private String getAmenityName(@NonNull Amenity amenity) {
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (amenity.getType().isWiki()) {
			if (Algorithms.isEmpty(locale)) {
				locale = app.getLanguage();
			}
			locale = PluginsHelper.onGetMapObjectsLocale(amenity, locale);
		}
		return amenity.getName(locale, app.getSettings().MAP_TRANSLITERATE_NAMES.get());
	}

	@Override
	public boolean isTextVisible() {
		return app.getSettings().SHOW_POI_LABEL.get();
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
	}

	public void setCustomMapObjects(List<Amenity> amenities) {
		if (customObjectsDelegate != null) {
			data.clearCache();
			customObjectsDelegate.setCustomMapObjects(amenities);
			getApplication().getOsmandMap().refreshMap();
		}
	}

	public void updateSelectedTopPlace(@Nullable Pair<BaseDetailsObject, Amenity> selectedPlace) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (selectedTopPlaceCollection == null) {
			selectedTopPlaceCollection = new MapMarkersCollection();
		}
		MapMarker previousSelectedMarker = null;
		QListMapMarker existingMapPoints = selectedTopPlaceCollection.getMarkers();
		for (int i = 0; i < existingMapPoints.size(); i++) {
			MapMarker mapPoint = existingMapPoints.get(i);
			if (mapPoint.getMarkerId() == SELECTED_MARKER_ID) {
				previousSelectedMarker = mapPoint;
				break;
			}
		}
		this.selectedTopPlace = selectedPlace;
		if (selectedPlace == null || topPlaces != null && !topPlaces.containsValue(selectedPlace.second)) {
			if (previousSelectedMarker != null) {
				selectedTopPlaceCollection.removeMarker(previousSelectedMarker);
			}
			return;
		}

		Bitmap imageBitmap = getTopPlaceBitmap(selectedPlace.second);
		if (imageBitmap != null) {
			LatLon latLon = selectedPlace.second.getLocation();
			Bitmap imageMapBitmap = createImageBitmap(imageBitmap, true);
			MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
			mapMarkerBuilder.setIsAccuracyCircleSupported(false)
					.setMarkerId(SELECTED_MARKER_ID)
					.setBaseOrder(getSelectedTopPlaceBaseOrder()) //context menu layer baseOrder to be over object polygon
					.setPinIcon(NativeUtilities.createSkImageFromBitmap(imageMapBitmap))
					.setPosition(NativeUtilities.getPoint31FromLatLon(latLon.getLatitude(), latLon.getLongitude()))
					.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.CenterVertical)
					.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
					.buildAndAddToCollection(selectedTopPlaceCollection);
			mapRenderer.addSymbolsProvider(SELECTED_POI_SECTION, selectedTopPlaceCollection);
		}
		if (previousSelectedMarker != null) {
			selectedTopPlaceCollection.removeMarker(previousSelectedMarker);
		}
	}

	private void showTopPlaceContextMenu(@NonNull MapActivity mapActivity,
			@NonNull BaseDetailsObject object, @NonNull Amenity topPlace) {
		Amenity amenity = object.getSyntheticAmenity();
		updateSelectedTopPlace(Pair.create(object, topPlace));

		MapContextMenu contextMenu = mapActivity.getContextMenu();
		contextMenu.setCenterMarker(true);
		contextMenu.show(topPlace.getLocation(), QuickSearchWikiItem.getPointDescription(app, amenity), object);
	}

	private Bitmap createImageBitmap(Bitmap bitmap, boolean isSelected) {
		OsmandApplication app = getApplication();
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.OVER_MAP);
		int borderWidth = AndroidUtils.dpToPxAuto(app, IMAGE_ICON_BORDER_DP);
		int bigIconSize = getBigIconSize();
		Bitmap circle = getCircle(bigIconSize);
		Bitmap bitmapResult = Bitmap.createBitmap(bigIconSize, bigIconSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		Paint bitmapPaint = createBitmapPaint();
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(isSelected ? app.getColor(ColorUtilities.getActiveColorId(nightMode)) : IMAGE_ICON_OUTER_COLOR, PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(circle, 0f, 0f, bitmapPaint);
		int cx = circle.getWidth() / 2;
		int cy = circle.getHeight() / 2;
		int radius = (Math.min(cx, cy) - borderWidth * 2);
		canvas.save();
//		canvas.clipRect(0, 0, circle.getWidth(), circle.getHeight());
		Path circularPath = new Path();
		circularPath.addCircle((float) cx, (float) cy, (float) radius, Path.Direction.CW);
		canvas.clipPath(circularPath);
		Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		RectF dstRect = new RectF(0f, 0f, (float) circle.getWidth(), (float) circle.getHeight());
		bitmapPaint.setColorFilter(null);
		canvas.drawBitmap(bitmap, srcRect, dstRect, bitmapPaint);
		canvas.restore();
		bitmapResult = AndroidUtils.scaleBitmap(bitmapResult, bigIconSize, bigIconSize, false);
		return bitmapResult;
	}

	private Bitmap getCircle(int size) {
		if (imageCircleBitmap == null || imageCircleBitmap.getWidth() != size || imageCircleBitmap.getHeight() != size) {
			imageCircleBitmap = RenderingIcons.getBitmapFromVectorDrawable(getContext(),
					R.drawable.bg_point_circle, size, size);
		}
		return imageCircleBitmap;
	}

	private Paint createBitmapPaint() {
		Paint bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
		return bitmapPaint;
	}

	private int getBigIconSize() {
		return (int) (AndroidUtils.dpToPxAuto(getContext(), IMAGE_ICON_SIZE_DP) * getTextScale() / app.getOsmandMap().getCarDensityScaleCoef());
	}

	@Override
	public long getSelectionPointOrder(Object selectedObject) {
		if (isTopPlace(selectedObject)) {
			return getTopPlaceBaseOrder();
		} else {
			return IContextMenuProvider.super.getSelectionPointOrder(selectedObject);
		}
	}

	private boolean isTopPlace(Object object) {
		if (topPlaces != null) {
			Long id;
			if (object instanceof Amenity) {
				id = ((Amenity) object).getId();
			} else if (object instanceof BaseDetailsObject) {
				id = ((BaseDetailsObject) object).getSyntheticAmenity().getId();
			} else {
				id = null;
			}
			return id != null && topPlaces.containsKey(id);
		}
		return false;
	}

	private int getTopPlaceBaseOrder() {
		return getPointsOrder() - 100;
	}

	private int getSelectedTopPlaceBaseOrder() {
		ContextMenuLayer contextLayer = app.getOsmandMap().getMapLayers().getContextMenuLayer();
		int baseOrder = contextLayer.getBaseOrder();
		int markerOrder = contextLayer.getMarkerBaseOrder();
		return baseOrder + (markerOrder - baseOrder) / 2;
	}

	@Override
	public long getPointOrder(Object object) {
		return isTopPlace(object) ? getTopPlaceBaseOrder() : getPointsOrder();
	}
}

package net.osmand.plus.views.layers;

import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE;
import static net.osmand.osm.MapPoiTypes.ROUTE_ARTICLE_POINT;
import static net.osmand.osm.MapPoiTypes.ROUTE_TRACK;
import static net.osmand.plus.utils.AndroidUtils.dpToPx;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiFilterUtils;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.render.TravelRendererHelper.OnFileVisibilityChangeListener;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
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
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class POIMapLayer extends OsmandMapLayer implements IContextMenuProvider,
		MapTextProvider<Amenity>, IRouteInformationListener, OnFileVisibilityChangeListener {
	private static final int START_ZOOM = 9;
	private static final int START_ZOOM_ROUTE_TRACK = 11;
	private static final int END_ZOOM_ROUTE_TRACK = 13;

	public static final org.apache.commons.logging.Log log = PlatformUtil.getLog(POIMapLayer.class);

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
	private PoiUIFilter routeTrackFilter;
	private String routeArticlePointsFilterByName;
	private boolean fileVisibilityChanged;
	public CustomMapObjects<Amenity> customObjectsDelegate;

	/// cache for displayed POI
	// Work with cache (for map copied from AmenityIndexRepositoryOdb)
	private final MapLayerData<List<Amenity>> data;

	public POIMapLayer(@NonNull Context context) {
		super(context);
		app = (OsmandApplication) context.getApplicationContext();
		routingHelper = app.getRoutingHelper();

		travelRendererHelper = app.getTravelRendererHelper();
		showTravel = app.getSettings().SHOW_TRAVEL.get();
		routeArticleFilterEnabled = travelRendererHelper.getRouteArticlesProperty().get();
		routeArticlePointsFilterEnabled = travelRendererHelper.getRouteArticlePointsProperty().get();
		routeArticleFilter = travelRendererHelper.getRouteArticleFilter();
		routeArticlePointsFilter = travelRendererHelper.getRouteArticlePointsFilter();
		routeTrackFilter = travelRendererHelper.getRouteTrackFilter();
		routeArticlePointsFilterByName = routeArticlePointsFilter != null ? routeArticlePointsFilter.getFilterByName() : null;

		routingHelper.addListener(this);
		travelRendererHelper.addFileVisibilityListener(this);
		data = new MapLayerData<List<Amenity>>() {

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
				app.getOsmandMap().refreshMap();
			}

			@Override
			protected List<Amenity> calculateResult(@NonNull QuadRect latLonBounds, int zoom) {
				if (customObjectsDelegate != null) {
					return customObjectsDelegate.getMapObjects();
				}
				if (calculatedFilters.isEmpty()) {
					return new ArrayList<>();
				}
				int z = (int) Math.floor(zoom + Math.log(getMapDensity()) / Math.log(2));

				List<Amenity> res = new ArrayList<>();
				PoiFilterUtils.combineStandardPoiFilters(calculatedFilters, app);
				for (PoiUIFilter filter : calculatedFilters) {
					List<Amenity> amenities = filter.searchAmenities(latLonBounds.top, latLonBounds.left,
							latLonBounds.bottom, latLonBounds.right, z, new ResultMatcher<Amenity>() {

								@Override
								public boolean publish(Amenity object) {
									return true;
								}

								@Override
								public boolean isCancelled() {
									return isInterrupted();
								}
							});
					if (filter.isRouteTrackFilter()) {
						for (Amenity amenity : amenities) {
							boolean hasRoute = false;
							String routeId = amenity.getRouteId();
							if (!Algorithms.isEmpty(routeId)) {
								for (Amenity a : res) {
									if (routeId.equals(a.getRouteId())) {
										hasRoute = true;
										break;
									}
								}
							}
							if (!hasRoute) {
								res.add(amenity);
							}
						}
					} else {
						res.addAll(amenities);
					}
				}

				Collections.sort(res, (lhs, rhs) -> lhs.getId() < rhs.getId()
						? -1 : (lhs.getId().longValue() == rhs.getId().longValue() ? 0 : 1));

				return res;
			}
		};
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
			PoiUIFilter routeTrackFilter = this.routeTrackFilter;
			if (routeTrackAsPoiFilterEnabled && routeTrackFilter != null) {
				calculatedFilters.add(routeTrackFilter);
			}
		}
		return calculatedFilters;
	}

	public void getAmenityFromPoint(RotatedTileBox tb, PointF point, List<? super Amenity> result) {
		List<Amenity> objects = data.getResults();
		if (tb.getZoom() >= START_ZOOM && !Algorithms.isEmpty(objects)) {
			MapRendererView mapRenderer = getMapRenderer();
			float radius = getScaledTouchRadius(view.getApplication(), getRadiusPoi(tb)) * TOUCH_RADIUS_MULTIPLIER;
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
							: tb.isLatLonNearPixel(latLon, point.x, point.y, radius);
					if (add) {
						result.add(amenity);
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
			if (travelRendererHelper.getRouteTracksAsPoiProperty().get() && routeTrackFilter != null) {
				return travelRendererHelper.getRouteTracksProperty().get()
						? zoom >= START_ZOOM : zoom >= START_ZOOM_ROUTE_TRACK;
			}
		}
		return false;
	}

	private boolean shouldDraw(@NonNull RotatedTileBox tileBox, @NonNull Amenity amenity) {
		if(customObjectsDelegate != null){
			return true;
		} else {
			boolean routeArticle = ROUTE_ARTICLE_POINT.equals(amenity.getSubType())
					|| ROUTE_ARTICLE.equals(amenity.getSubType());
			boolean routeTrack = ROUTE_TRACK.equals(amenity.getSubType());
			if (routeArticle) {
				return tileBox.getZoom() >= START_ZOOM;
			}  else if (routeTrack) {
				if (travelRendererHelper.getRouteTracksProperty().get()) {
					return tileBox.getZoom() >= START_ZOOM && tileBox.getZoom() <= END_ZOOM_ROUTE_TRACK;
				} else {
					return tileBox.getZoom() >= START_ZOOM_ROUTE_TRACK;
				}
			} else {
				return tileBox.getZoom() >= START_ZOOM;
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
		PoiUIFilter routeTrackFilter = travelRendererHelper.getRouteTrackFilter();
		String routeArticlePointsFilterByName = routeArticlePointsFilter != null ? routeArticlePointsFilter.getFilterByName() : null;
		boolean dataChanged = false;
		if (this.filters != selectedPoiFilters
				|| this.showTravel != showTravel
				|| this.routeArticleFilterEnabled != routeArticleFilterEnabled
				|| this.routeArticlePointsFilterEnabled != routeArticlePointsFilterEnabled
				|| this.routeTrackFilterEnabled != routeTrackFilterEnabled
				|| this.routeTrackAsPoiFilterEnabled != routeTrackAsPoiFilterEnabled
				|| this.routeArticleFilter != routeArticleFilter
				|| this.routeArticlePointsFilter != routeArticlePointsFilter
				|| this.routeTrackFilter != routeTrackFilter
				|| this.fileVisibilityChanged
				|| !Algorithms.stringsEqual(this.routeArticlePointsFilterByName, routeArticlePointsFilterByName)) {
			this.filters = selectedPoiFilters;
			this.showTravel = showTravel;
			this.routeArticleFilterEnabled = routeArticleFilterEnabled;
			this.routeArticlePointsFilterEnabled = routeArticlePointsFilterEnabled;
			this.routeTrackFilterEnabled = routeTrackFilterEnabled;
			this.routeTrackAsPoiFilterEnabled = routeTrackAsPoiFilterEnabled;
			this.routeArticleFilter = routeArticleFilter;
			this.routeArticlePointsFilter = routeArticlePointsFilter;
			this.routeTrackFilter = routeTrackFilter;
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
				if (poiTileProvider == null || dataChanged || textScaleChanged || nightModeChanged
						|| textVisibleChanged || mapActivityInvalidated) {
					clearPoiTileProvider();
					if (!collectFilters().isEmpty()) {
						float density = view.getDensity();
						TextRasterizer.Style textStyle = MapTextLayer.getTextStyle(getContext(),
								nightMode, textScale, density);
						poiTileProvider = new POITileProvider(getContext(), data, getPointsOrder(), textVisible,
								textStyle, textScale, density);
						poiTileProvider.drawSymbols(mapRenderer);
					}
				}
			} else {
				clearPoiTileProvider();
			}
			mapActivityInvalidated = false;
			return;
		}
		List<Amenity> fullObjects = new ArrayList<>();
		List<LatLon> fullObjectsLatLon = new ArrayList<>();
		List<LatLon> smallObjectsLatLon = new ArrayList<>();
		if (shouldDraw(zoom)) {
			data.queryNewData(tileBox);
			List<Amenity> objects = data.getResults();
			if (objects != null) {
				float textScale = getTextScale();
				float iconSize = getIconSize(app);
				QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
				WaypointHelper wph = app.getWaypointHelper();
				for (Amenity o : objects) {
					if (shouldDraw(tileBox, o)) {
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
				}
				for (Amenity o : fullObjects) {
					LatLon latLon = o.getLocation();
					int x = (int) tileBox.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
					if (tileBox.containsPoint(x, y, iconSize)) {
						String id = o.getGpxIcon();
						if (id == null) {
							id = RenderingIcons.getIconNameForAmenity(o);
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
		clearPoiTileProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public static void showPlainDescriptionDialog(Context ctx, OsmandApplication app, String text, String title) {
		TextView textView = new TextView(ctx);
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int textMargin = dpToPx(app, 10f);
		boolean light = app.getSettings().isLightContent();
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
		boolean light = app.getSettings().isLightContent();
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
		Dialog dialog = new Dialog(ctx,
				app.getSettings().isLightContent() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);

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
		if (o instanceof Amenity) {
			return new PointDescription(PointDescription.POINT_TYPE_POI, getAmenityName((Amenity) o));
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (tileBox.getZoom() >= START_ZOOM) {
			getAmenityFromPoint(tileBox, point, objects);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof Amenity) {
			return ((Amenity) o).getLocation();
		}
		return null;
	}

	@Override
	public boolean showMenuAction(@Nullable Object object) {
		OsmandApplication app = view.getApplication();
		MapActivity mapActivity = view.getMapActivity();
		TravelHelper travelHelper = app.getTravelHelper();
		if (mapActivity != null && object instanceof Amenity) {
			Amenity amenity = (Amenity) object;
			if (amenity.getSubType().equals(ROUTE_TRACK)) {
				TravelGpx travelGpx = travelHelper.searchGpx(amenity.getLocation(), amenity.getRouteId(), amenity.getRef());
				if (travelGpx == null) {
					return true;
				}
				travelHelper.openTrackMenu(travelGpx, mapActivity, amenity.getRouteId(), amenity.getLocation());
				return true;
			} else if (amenity.getSubType().equals(ROUTE_ARTICLE)) {
				String lang = app.getLanguage();
				lang = amenity.getContentLanguage(Amenity.DESCRIPTION, lang, "en");
				String name = amenity.getName(lang);
				TravelArticle article = travelHelper.getArticleByTitle(name, lang, true, null);
				if (article == null) {
					return true;
				}
				travelHelper.openTrackMenu(article, mapActivity, name, amenity.getLocation());
				return true;
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

	private String getAmenityName(Amenity amenity) {
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

	public void setCustomMapObjects(List<Amenity> poiUIFilters) {
		if (customObjectsDelegate != null) {
			data.clearCache();
			customObjectsDelegate.setCustomMapObjects(poiUIFilters);
			getApplication().getOsmandMap().refreshMap();
		}
	}
}

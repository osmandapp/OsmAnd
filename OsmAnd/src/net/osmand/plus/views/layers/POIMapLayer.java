package net.osmand.plus.views.layers;

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

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.ValueHolder;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiType;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import static net.osmand.AndroidUtils.dpToPx;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.ROUTE_ARTICLE;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.ROUTE_TRACK;

public class POIMapLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
		MapTextProvider<Amenity>, IRouteInformationListener {
	private static final int startZoom = 9;

	public static final org.apache.commons.logging.Log log = PlatformUtil.getLog(POIMapLayer.class);

	private OsmandMapTileView view;

	private final OsmandApplication app;
	private final RoutingHelper routingHelper;
	private Set<PoiUIFilter> filters = new TreeSet<>();
	private MapTextLayer mapTextLayer;

	/// cache for displayed POI
	// Work with cache (for map copied from AmenityIndexRepositoryOdb)
	private final MapLayerData<List<Amenity>> data;

	public POIMapLayer(@NonNull final Context context) {
		super(context);
		app = (OsmandApplication) context.getApplicationContext();
		routingHelper = app.getRoutingHelper();
		routingHelper.addListener(this);
		data = new OsmandMapLayer.MapLayerData<List<Amenity>>() {

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
				calculatedFilters = new TreeSet<>(filters);
			}

			@Override
			public void layerOnPostExecute() {
				app.getOsmandMap().refreshMap();
			}

			@Override
			protected List<Amenity> calculateResult(RotatedTileBox tileBox) {
				QuadRect latLonBounds = tileBox.getLatLonBounds();
				if (calculatedFilters.isEmpty() || latLonBounds == null) {
					return new ArrayList<>();
				}
				int z = (int) Math.floor(tileBox.getZoom() + Math.log(view.getSettings().MAP_DENSITY.get()) / Math.log(2));

				List<Amenity> res = new ArrayList<>();
				PoiUIFilter.combineStandardPoiFilters(calculatedFilters, app);
				for (PoiUIFilter filter : calculatedFilters) {
					res.addAll(filter.searchAmenities(latLonBounds.top, latLonBounds.left,
							latLonBounds.bottom, latLonBounds.right, z, new ResultMatcher<Amenity>() {

								@Override
								public boolean publish(Amenity object) {
									return true;
								}

								@Override
								public boolean isCancelled() {
									return isInterrupted();
								}
							}));
				}

				Collections.sort(res, (lhs, rhs) -> lhs.getId() < rhs.getId()
						? -1 : (lhs.getId().longValue() == rhs.getId().longValue() ? 0 : 1));

				return res;
			}
		};
	}

	public void getAmenityFromPoint(RotatedTileBox tb, PointF point, List<? super Amenity> am) {
		List<Amenity> objects = data.getResults();
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int compare = getScaledTouchRadius(view.getApplication(), getRadiusPoi(tb));
			int radius = compare * 3 / 2;
			try {
				for (int i = 0; i < objects.size(); i++) {
					Amenity n = objects.get(i);
					int x = (int) tb.getPixXFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					int y = (int) tb.getPixYFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
					if (Math.abs(x - ex) <= compare && Math.abs(y - ey) <= compare) {
						compare = radius;
						am.add(n);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				// that's really rare case, but is much efficient than introduce synchronized block
			}
		}
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		this.view = view;
		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
	}

	public int getRadiusPoi(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom < startZoom) {
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

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		Set<PoiUIFilter> selectedPoiFilters = app.getPoiFilters().getSelectedPoiFilters();
		if (this.filters != selectedPoiFilters) {
			this.filters = selectedPoiFilters;
			data.clearCache();
		}

		List<Amenity> fullObjects = new ArrayList<>();
		List<LatLon> fullObjectsLatLon = new ArrayList<>();
		List<LatLon> smallObjectsLatLon = new ArrayList<>();
		if (!filters.isEmpty()) {
			if (tileBox.getZoom() >= startZoom) {
				data.queryNewData(tileBox);
				List<Amenity> objects = data.getResults();
				if (objects != null) {
					float textScale = app.getSettings().TEXT_SCALE.get();
					float iconSize = getIconSize(app);
					QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);
					WaypointHelper wph = app.getWaypointHelper();
					PointImageDrawable pointImageDrawable = PointImageDrawable.getOrCreate(view.getContext(),
							ContextCompat.getColor(app, R.color.osmand_orange), true);
					pointImageDrawable.setAlpha(0.8f);
					for (Amenity o : objects) {
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
								PoiType st = o.getType().getPoiTypeByKeyName(o.getSubType());
								if (st != null) {
									if (RenderingIcons.containsSmallIcon(st.getIconKeyName())) {
										id = st.getIconKeyName();
									} else if (RenderingIcons.containsSmallIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
										id = st.getOsmTag() + "_" + st.getOsmValue();
									}
								}
							}
							if (id != null) {
								pointImageDrawable = PointImageDrawable.getOrCreate(view.getContext(),
										ContextCompat.getColor(app, R.color.osmand_orange), true,
										RenderingIcons.getResId(id));
								pointImageDrawable.setAlpha(0.8f);
								pointImageDrawable.drawPoint(canvas, x, y, textScale, false);
							}
						}
					}
					this.fullObjectsLatLon = fullObjectsLatLon;
					this.smallObjectsLatLon = smallObjectsLatLon;
				}
			}
		}
		mapTextLayer.putData(this, fullObjects);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void destroyLayer() {
		routingHelper.removeListener(this);
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public static void showPlainDescriptionDialog(Context ctx, OsmandApplication app, String text, String title) {
		final TextView textView = new TextView(ctx);
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
		final WebViewEx webView = new WebViewEx(ctx);
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

	static int getResIdFromAttribute(final Context ctx, final int attr) {
		if (attr == 0) {
			return 0;
		}
		final TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	private static void showText(final Context ctx, final OsmandApplication app, final View view, String title) {
		final Dialog dialog = new Dialog(ctx,
				app.getSettings().isLightContent() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);

		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		final Toolbar topBar = new Toolbar(ctx);
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
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects, boolean unknownLocation) {
		if (tileBox.getZoom() >= startZoom) {
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
	public boolean isObjectClickable(Object o) {
		return o instanceof Amenity;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
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
		int radiusPoi = getRadiusPoi(rb);
		if (isPresentInFullObjects(amenity.getLocation())) {
			radiusPoi += (app.getResources().getDimensionPixelSize(R.dimen.favorites_icon_outline_size)
					- app.getResources().getDimensionPixelSize(R.dimen.favorites_icon_size_small)) / 2;
		}
		return radiusPoi;
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
			locale = OsmandPlugin.onGetMapObjectsLocale(amenity, locale);
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
}

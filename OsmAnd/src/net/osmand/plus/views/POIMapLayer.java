package net.osmand.plus.views;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.MapTextLayer.MapTextProvider;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class POIMapLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider,
		MapTextProvider<Amenity>, IRouteInformationListener {
	private static final int startZoom = 9;

	public static final org.apache.commons.logging.Log log = PlatformUtil.getLog(POIMapLayer.class);

	private Paint paintIcon;

	private Paint paintIconBackground;
	private Bitmap poiBackground;
	private Bitmap poiBackgroundSmall;

	private OsmandMapTileView view;

	private RoutingHelper routingHelper;
	private Set<PoiUIFilter> filters = new TreeSet<>();
	private MapTextLayer mapTextLayer;

	/// cache for displayed POI
	// Work with cache (for map copied from AmenityIndexRepositoryOdb)
	private MapLayerData<List<Amenity>> data;

	private OsmandApplication app;


	public POIMapLayer(final MapActivity activity) {
		routingHelper = activity.getRoutingHelper();
		routingHelper.addListener(this);
		app = activity.getMyApplication();
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
				activity.getMapView().refreshMap();
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

				Collections.sort(res, new Comparator<Amenity>() {
					@Override
					public int compare(Amenity lhs, Amenity rhs) {
						return lhs.getId() < rhs.getId() ? -1 : (lhs.getId().longValue() == rhs.getId().longValue() ? 0 : 1);
					}
				});

				return res;
			}
		};
	}


	public void getAmenityFromPoint(RotatedTileBox tb, PointF point, List<? super Amenity> am) {
		List<Amenity> objects = data.getResults();
		if (objects != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rp = getRadiusPoi(tb);
			int compare = rp;
			int radius = rp * 3 / 2;
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
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		paintIcon = new Paint();
		//paintIcon.setStrokeWidth(1);
		//paintIcon.setStyle(Style.STROKE);
		//paintIcon.setColor(Color.BLUE);
		paintIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
		paintIconBackground = new Paint();
		poiBackground = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_orange_poi_shield);
		poiBackgroundSmall = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_white_orange_poi_shield_small);

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
		if (!this.filters.equals(selectedPoiFilters)) {
            this.filters = new TreeSet<>(selectedPoiFilters);
			data.clearCache();
		}

		List<Amenity> objects = Collections.emptyList();
		List<Amenity> fullObjects = new ArrayList<>();
		List<LatLon> fullObjectsLatLon = new ArrayList<>();
		List<LatLon> smallObjectsLatLon = new ArrayList<>();
		if (!filters.isEmpty()) {
			if (tileBox.getZoom() >= startZoom) {
				data.queryNewData(tileBox);
				objects = data.getResults();
				if (objects != null) {
					float iconSize = poiBackground.getWidth() * 3 / 2;
					QuadTree<QuadRect> boundIntersections = initBoundIntersections(tileBox);

					for (Amenity o : objects) {
						float x = tileBox.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());
						float y = tileBox.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());

						if (tileBox.containsPoint(x, y, iconSize)) {
							if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
								canvas.drawBitmap(poiBackgroundSmall, x - poiBackgroundSmall.getWidth() / 2, y - poiBackgroundSmall.getHeight() / 2, paintIconBackground);
								smallObjectsLatLon.add(new LatLon(o.getLocation().getLatitude(),
										o.getLocation().getLongitude()));
							} else {
								fullObjects.add(o);
								fullObjectsLatLon.add(new LatLon(o.getLocation().getLatitude(),
										o.getLocation().getLongitude()));
							}
						}
					}
					for (Amenity o : fullObjects) {
						int x = (int) tileBox.getPixXFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());
						int y = (int) tileBox.getPixYFromLatLon(o.getLocation().getLatitude(), o.getLocation()
								.getLongitude());
						if (tileBox.containsPoint(x, y, iconSize)) {
							canvas.drawBitmap(poiBackground, x - poiBackground.getWidth() / 2, y - poiBackground.getHeight() / 2, paintIconBackground);
							String id = null;
							PoiType st = o.getType().getPoiTypeByKeyName(o.getSubType());
							if (st != null) {
								if (RenderingIcons.containsSmallIcon(st.getIconKeyName())) {
									id = st.getIconKeyName();
								} else if (RenderingIcons.containsSmallIcon(st.getOsmTag() + "_" + st.getOsmValue())) {
									id = st.getOsmTag() + "_" + st.getOsmValue();
								}
							}
							if (id != null) {
								Bitmap bmp = RenderingIcons.getIcon(view.getContext(), id, false);
								if (bmp != null) {
									canvas.drawBitmap(bmp, x - bmp.getWidth() / 2, y - bmp.getHeight() / 2, paintIcon);
								}
							}
						}
					}
					this.fullObjectsLatLon = fullObjectsLatLon;
					this.smallObjectsLatLon = smallObjectsLatLon;
				}
			}
		}
		mapTextLayer.putData(this, objects);
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

	public static void showDescriptionDialog(Context ctx, OsmandApplication app, String text, String title) {
		showText(ctx, app, text, title);
	}

	static int getResIdFromAttribute(final Context ctx, final int attr) {
		if (attr == 0) {
			return 0;
		}
		final TypedValue typedvalueattr = new TypedValue();
		ctx.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	private static void showText(final Context ctx, final OsmandApplication app, final String text, String title) {
		final Dialog dialog = new Dialog(ctx,
				app.getSettings().isLightContent() ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);

		LinearLayout ll = new LinearLayout(ctx);
		ll.setOrientation(LinearLayout.VERTICAL);

		final Toolbar topBar = new Toolbar(ctx);
		topBar.setClickable(true);
		Drawable back = app.getUIUtilities().getIcon(R.drawable.ic_arrow_back);
		topBar.setNavigationIcon(back);
		topBar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		topBar.setTitle(title);
		topBar.setBackgroundColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTabBackground)));
		topBar.setTitleTextColor(ContextCompat.getColor(ctx, getResIdFromAttribute(ctx, R.attr.pstsTextColor)));
		topBar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.dismiss();
			}
		});

		final TextView textView = new TextView(ctx);
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int textMargin = dpToPx(app, 10f);
		boolean light = app.getSettings().isLightContent();
		textView.setLayoutParams(llTextParams);
		textView.setPadding(textMargin, textMargin, textMargin, textMargin);
		textView.setTextSize(16);
		textView.setTextColor(ContextCompat.getColor(app, light ? R.color.text_color_primary_light : R.color.text_color_primary_dark));
		textView.setAutoLinkMask(Linkify.ALL);
		textView.setLinksClickable(true);
		textView.setText(text);

		ScrollView scrollView = new ScrollView(ctx);
		ll.addView(topBar);
		LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0);
		lp.weight = 1;
		ll.addView(scrollView, lp);
		scrollView.addView(textView);

		dialog.setContentView(ll);
		dialog.setCancelable(true);
		dialog.show();
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof Amenity) {
			Amenity a = (Amenity) o;
			String preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
			String preferredMapAppLang = preferredMapLang;
			if (Algorithms.isEmpty(preferredMapAppLang)) {
				preferredMapAppLang = app.getLanguage();
			}
			boolean transliterateNames = app.getSettings().MAP_TRANSLITERATE_NAMES.get();

			return new PointDescription(PointDescription.POINT_TYPE_POI, a.getName(
					a.getType().isWiki() ? preferredMapAppLang : preferredMapLang,
					transliterateNames));
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
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
	public LatLon getTextLocation(Amenity o) {
		return o.getLocation();
	}

	@Override
	public int getTextShift(Amenity amenity, RotatedTileBox rb) {
		int radiusPoi = getRadiusPoi(rb);
		if (isPresentInFullObjects(amenity.getLocation())) {
			radiusPoi += poiBackground.getHeight() / 2 - poiBackgroundSmall.getHeight() / 2;
		}

		return radiusPoi;
	}

	@Override
	public String getText(Amenity o) {
		return o.getName(view.getSettings().MAP_PREFERRED_LOCALE.get(),
				view.getSettings().MAP_TRANSLITERATE_NAMES.get());
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

	public static int dpToPx(Context ctx, float dp) {
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}


}

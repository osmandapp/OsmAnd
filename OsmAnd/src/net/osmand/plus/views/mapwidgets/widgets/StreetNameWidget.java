package net.osmand.plus.views.mapwidgets.widgets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.CurrentPositionHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.helpers.WaypointHelper.LocationPointWrapper;
import net.osmand.plus.render.TextDrawInfo;
import net.osmand.plus.render.TextRenderer;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.ShowAlongTheRouteBottomSheet;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import static net.osmand.plus.render.OsmandRenderer.RenderingContext;
import static net.osmand.plus.views.mapwidgets.WidgetType.STREET_NAME;

public class StreetNameWidget extends MapWidget {

	private static final int MAX_MARKER_DISTANCE = 50;

	private final WaypointHelper waypointHelper;
	private LocationPointWrapper lastPoint;

	private final TextView addressText;
	private final TextView addressTextShadow;
	private final TextView exitRefText;
	private final ImageView shieldImage;
	private final ImageView turnIcon;
	private final View waypointInfoBar;

	private final TurnDrawable turnDrawable;
	private int shadowRadius;
	private boolean showMarker;


	@Override
	protected int getLayoutId() {
		return R.layout.street_name_widget;
	}

	@Nullable
	@Override
	public OsmandPreference<Boolean> getWidgetVisibilityPref() {
		return settings.SHOW_STREET_NAME;
	}

	public StreetNameWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, STREET_NAME);

		waypointHelper = app.getWaypointHelper();

		addressText = view.findViewById(R.id.map_address_text);
		addressTextShadow = view.findViewById(R.id.map_address_text_shadow);
		waypointInfoBar = view.findViewById(R.id.waypoint_info_bar);
		exitRefText = view.findViewById(R.id.map_exit_ref);
		shieldImage = view.findViewById(R.id.map_shield_icon);
		turnIcon = view.findViewById(R.id.map_turn_icon);

		turnDrawable = new TurnDrawable(mapActivity, true);

		updateVisibility(false);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		StreetNameWidgetParams params = new StreetNameWidgetParams(mapActivity);
		CurrentStreetName streetName = params.streetName;
		int turnArrowColorId = params.turnArrowColorId;
		boolean showClosestWaypointFirstInAddress = params.showClosestWaypointFirstInAddress;

		if (turnArrowColorId != 0) {
			turnDrawable.setColor(turnArrowColorId);
		}

		boolean hideStreetName = mapActivity.isTopToolbarActive()
				|| mapActivity.shouldHideTopControls()
				|| MapRouteInfoMenu.chooseRoutesVisible
				|| MapRouteInfoMenu.waypointsVisible;
		if (hideStreetName) {
			updateVisibility(false);
		} else if (showClosestWaypointFirstInAddress && updateWaypoint()) {
			updateVisibility(true);
			AndroidUiHelper.updateVisibility(addressText, false);
			AndroidUiHelper.updateVisibility(addressTextShadow, false);
			AndroidUiHelper.updateVisibility(turnIcon, false);
			AndroidUiHelper.updateVisibility(shieldImage, false);
			AndroidUiHelper.updateVisibility(exitRefText, false);
		} else if (streetName == null) {
			updateVisibility(false);
		} else {
			updateVisibility(true);
			AndroidUiHelper.updateVisibility(waypointInfoBar, false);
			AndroidUiHelper.updateVisibility(addressText, true);
			AndroidUiHelper.updateVisibility(addressTextShadow, shadowRadius > 0);

			RouteDataObject shieldObject = streetName.shieldObject;
			if (shieldObject != null && shieldObject.nameIds != null && setRoadShield(shieldObject)) {
				AndroidUiHelper.updateVisibility(shieldImage, true);
				int indexOf = streetName.text.indexOf("»");
				if (indexOf > 0) {
					streetName.text = streetName.text.substring(indexOf);
				}
			} else {
				AndroidUiHelper.updateVisibility(shieldImage, false);
			}

			if (Algorithms.isEmpty(streetName.exitRef)) {
				AndroidUiHelper.updateVisibility(exitRefText, false);
			} else {
				exitRefText.setText(streetName.exitRef);
				AndroidUiHelper.updateVisibility(exitRefText, true);
			}

			if (turnDrawable.setTurnType(streetName.turnType) || streetName.showMarker != showMarker) {
				showMarker = streetName.showMarker;
				if (streetName.turnType != null) {
					turnIcon.invalidateDrawable(turnDrawable);
					turnIcon.setImageDrawable(turnDrawable);
					AndroidUiHelper.updateVisibility(turnIcon, true);
				} else if (streetName.showMarker) {
					Drawable marker = iconsCache.getIcon(R.drawable.ic_action_start_navigation, R.color.color_myloc_distance);
					turnIcon.setImageDrawable(marker);
					AndroidUiHelper.updateVisibility(turnIcon, true);
				} else {
					AndroidUiHelper.updateVisibility(turnIcon, false);
				}
			}
			if (Algorithms.isEmpty(streetName.text)) {
				addressTextShadow.setText("");
				addressText.setText("");
			} else if (!streetName.text.equals(addressText.getText().toString())) {
				addressTextShadow.setText(streetName.text);
				addressText.setText(streetName.text);
			}
		}
	}

	public boolean updateWaypoint() {
		LocationPointWrapper point = waypointHelper.getMostImportantLocationPoint(null);
		boolean changed = lastPoint != point;
		lastPoint = point;
		if (point == null) {
			view.setOnClickListener(null);
			AndroidUiHelper.updateVisibility(waypointInfoBar, false);
			return false;
		} else {
			AndroidUiHelper.updateVisibility(addressText, false);
			AndroidUiHelper.updateVisibility(addressTextShadow, false);
			boolean updated = AndroidUiHelper.updateVisibility(waypointInfoBar, true);
			// pass top bar to make it clickable
			WaypointDialogHelper.updatePointInfoView(app, mapActivity, view, point, true,
					isNightMode(), false, true);
			if (updated || changed) {
				ImageView moreButton = waypointInfoBar.findViewById(R.id.waypoint_more);
				ImageView closeButton = waypointInfoBar.findViewById(R.id.waypoint_close);
				moreButton.setOnClickListener(view -> {
					mapActivity.hideContextAndRouteInfoMenues();
					ShowAlongTheRouteBottomSheet fragment = new ShowAlongTheRouteBottomSheet();
					Bundle args = new Bundle();
					args.putInt(ShowAlongTheRouteBottomSheet.EXPAND_TYPE_KEY, point.type);
					fragment.setArguments(args);
					fragment.setUsedOnMap(true);
					fragment.show(mapActivity.getSupportFragmentManager(), ShowAlongTheRouteBottomSheet.TAG);
				});
				closeButton.setOnClickListener(view -> {
					waypointHelper.removeVisibleLocationPoint(point);
					mapActivity.refreshMap();
				});
			}
			return true;
		}
	}

	private boolean setRoadShield(@NonNull RouteDataObject object) {
		StringBuilder additional = new StringBuilder();
		for (int i = 0; i < object.nameIds.length; i++) {
			String key = object.region.routeEncodingRules.get(object.nameIds[i]).getTag();
			String val = object.names.get(object.nameIds[i]);
			if (!key.endsWith("_ref") && !key.startsWith("route_road")) {
				additional.append(key).append("=").append(val).append(";");
			}
		}
		for (int i = 0; i < object.nameIds.length; i++) {
			String key = object.region.routeEncodingRules.get(object.nameIds[i]).getTag();
			String val = object.names.get(object.nameIds[i]);
			if (key.startsWith("route_road") && key.endsWith("_ref")) {
				boolean visible = setRoadShield(object, key, val, additional);
				if (visible) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean setRoadShield(@NonNull RouteDataObject object, @NonNull String nameTag,
	                              @NonNull String name, @NonNull StringBuilder additional) {
		int[] types = object.getTypes();
		RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRuleSearchRequest rreq = app.getResourceManager().getRenderer()
				.getSearchRequestWithAppliedCustomRules(storage, isNightMode());

		for (int type : types) {
			RouteTypeRule routeTypeRule = object.region.quickGetEncodingRule(type);
			String tag = routeTypeRule.getTag();
			String value = routeTypeRule.getValue();
			if (tag.equals("highway") || tag.equals("route")) {
				rreq.setInitialTagValueZoom(tag, value, 13, null);
			} else {
				additional.append(tag).append("=").append(value).append(";");
			}
		}

		rreq.setIntFilter(rreq.ALL.R_TEXT_LENGTH, name.length());
		rreq.setStringFilter(rreq.ALL.R_NAME_TAG, nameTag);
		rreq.setStringFilter(rreq.ALL.R_ADDITIONAL, additional.toString());
		rreq.search(RenderingRulesStorage.TEXT_RULES);

		RenderingContext rc = new RenderingContext(app);
		TextRenderer textRenderer = new TextRenderer(app);
		TextDrawInfo text = new TextDrawInfo(name);

		int shieldRes = -1;
		if (rreq.isSpecified(rreq.ALL.R_TEXT_SHIELD)) {
			text.setShieldResIcon(rreq.getStringPropertyValue(rreq.ALL.R_TEXT_SHIELD));
			shieldRes = app.getResources().getIdentifier("h_" + text.getShieldResIcon(),
					"drawable", app.getPackageName());
		}
		if (shieldRes == -1) {
			return false;
		}

		Drawable shield = AppCompatResources.getDrawable(mapActivity, shieldRes);
		if (shield == null) {
			return false;
		}

		float xSize = shield.getIntrinsicWidth();
		float ySize = shield.getIntrinsicHeight();
		float xyRatio = xSize / ySize;
		//setting view propotions (height is fixed by toolbar size - 48dp);
		int viewHeightPx = AndroidUtils.dpToPx(app, 48);
		int viewWidthPx = (int) (viewHeightPx * xyRatio);

		ViewGroup.LayoutParams params = shieldImage.getLayoutParams();
		params.width = viewWidthPx;
		shieldImage.setLayoutParams(params);

		Bitmap bitmap = Bitmap.createBitmap((int) xSize, (int) ySize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = setupTextPaint(app, textRenderer.getPaintText(), rreq);

		float centerX = xSize / 2f;
		float centerY = ySize / 2f - paint.getFontMetrics().ascent / 2f;
		text.fillProperties(rc, rreq, centerX, centerY);
		textRenderer.drawShieldIcon(rc, canvas, text, text.getShieldResIcon());
		textRenderer.drawWrappedText(canvas, text, 20f);

		shieldImage.setImageBitmap(bitmap);
		return true;
	}

	@NonNull
	public static Paint setupTextPaint(@NonNull OsmandApplication app, @NonNull Paint paint,
	                                   @NonNull RenderingRuleSearchRequest request) {
		paint.setTypeface(Typeface.create(TextRenderer.DROID_SERIF, Typeface.BOLD));

		if (request.isSpecified(request.ALL.R_TEXT_COLOR)) {
			paint.setColor(request.getIntPropertyValue(request.ALL.R_TEXT_COLOR));
		}

		if (request.isSpecified(request.ALL.R_TEXT_SIZE)) {
			float textSize = request.getFloatPropertyValue(request.ALL.R_TEXT_SIZE);
			DisplayMetrics displayMetrics = app.getResources().getDisplayMetrics();
			paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, displayMetrics));
		}

		return paint;
	}

	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);

		shadowRadius = textState.textShadowRadius;

		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		view.setBackgroundResource(portrait ? textState.boxTop : textState.boxFree);

		TextView waypointText = view.findViewById(R.id.waypoint_text);
		TextView waypointTextShadow = view.findViewById(R.id.waypoint_text_shadow);
		updateTextColor(addressText, addressTextShadow, textState.textColor,
				textState.textShadowColor, textState.textBold, shadowRadius);
		updateTextColor(waypointText, waypointTextShadow, textState.textColor,
				textState.textShadowColor, textState.textBold, shadowRadius / 2);

		int exitRefTextColorId = isNightMode()
				? R.color.text_color_primary_dark
				: R.color.color_white;
		exitRefText.setTextColor(ContextCompat.getColor(app, exitRefTextColorId));

		ImageView moreImage = waypointInfoBar.findViewById(R.id.waypoint_more);
		ImageView removeImage = waypointInfoBar.findViewById(R.id.waypoint_close);
		moreImage.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white, isNightMode()));
		removeImage.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark, isNightMode()));
	}

	@Override
	protected boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = super.updateVisibility(visible);
		if (updatedVisibility) {
			MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.recreateTopWidgetsPanel();
			}
			mapActivity.updateStatusBarColor();
		}
		return updatedVisibility;
	}

	@Override
	public void attachView(@NonNull ViewGroup container, int order, @NonNull List<MapWidget> followingWidgets) {
		ViewGroup specialContainer = getSpecialContainer();
		boolean specialPosition = specialContainer != null;
		if (specialPosition) {
			specialContainer.removeAllViews();

			boolean coordinatesVisible = mapActivity.getWidgetsVisibilityHelper().shouldShowTopCoordinatesWidget();
			for (MapWidget widget : followingWidgets) {
				if (widget instanceof CoordinatesWidget && coordinatesVisible) {
					specialPosition = false;
					break;
				}
			}
		}
		if (specialPosition) {
			specialContainer.addView(view);
		} else {
			container.addView(view, order);
		}
	}

	@Override
	public void detachView() {
		super.detachView();
		// Clear in case link to previous view of StreetNameWidget is lost
		ViewGroup specialContainer = getSpecialContainer();
		if (specialContainer != null) {
			specialContainer.removeAllViews();
		}
	}

	@Nullable
	private ViewGroup getSpecialContainer() {
		return mapActivity.findViewById(R.id.street_name_widget_special_container);
	}

	private static class StreetNameWidgetParams {

		private final OsmandApplication app;
		private final OsmandSettings settings;
		private final MapActivity mapActivity;
		private final RoutingHelper routingHelper;

		public CurrentStreetName streetName;
		@ColorRes
		public int turnArrowColorId;
		public boolean showClosestWaypointFirstInAddress = true;

		public StreetNameWidgetParams(@NonNull MapActivity mapActivity) {
			this.app = mapActivity.getMyApplication();
			this.mapActivity = mapActivity;
			this.settings = app.getSettings();
			this.routingHelper = app.getRoutingHelper();

			computeParams();
		}

		private void computeParams() {
			boolean widgetEnabled = settings.SHOW_STREET_NAME.get();
			boolean onRoute = routingHelper.isRouteCalculated() && !routingHelper.isDeviatedFromRoute();
			boolean mapLinkedToLocation = app.getMapViewTrackingUtilities().isMapLinkedToLocation();
			if (onRoute) {
				if (routingHelper.isFollowingMode()) {
					if (widgetEnabled) {
						NextDirectionInfo nextDirInfo =
								routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
						streetName = routingHelper.getCurrentName(nextDirInfo);
						turnArrowColorId = R.color.nav_arrow;
					}
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					boolean routeMenuVisible = mapActivity.getMapRouteInfoMenu().isVisible();
					if (di >= 0 && routeMenuVisible && di < routingHelper.getRouteDirections().size()) {
						NextDirectionInfo nextDirectionInfo =
								routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), true);
						streetName = routingHelper.getCurrentName(nextDirectionInfo);
						turnArrowColorId = R.color.nav_arrow_distant;
						showClosestWaypointFirstInAddress = false;
					}
				}
			} else if (mapLinkedToLocation && widgetEnabled) {
				streetName = new CurrentStreetName();
				OsmAndLocationProvider locationProvider = app.getLocationProvider();
				RouteDataObject lastKnownSegment = locationProvider.getLastKnownRouteSegment();
				Location lastKnownLocation = locationProvider.getLastKnownLocation();
				if (lastKnownSegment != null && lastKnownLocation != null) {
					updateParamsByLastKnown(lastKnownSegment, lastKnownLocation);
				}
			}
		}

		private void updateParamsByLastKnown(@NonNull RouteDataObject lastKnownSegment,
		                                     @NonNull Location lastKnownLocation) {
			String preferredLocale = settings.MAP_PREFERRED_LOCALE.get();
			boolean transliterateNames = settings.MAP_TRANSLITERATE_NAMES.get();
			boolean direction = lastKnownSegment.bearingVsRouteDirection(lastKnownLocation);

			String name = lastKnownSegment.getName(preferredLocale, transliterateNames);
			String ref = lastKnownSegment.getRef(preferredLocale, transliterateNames, direction);
			String destination = lastKnownSegment.getDestinationName(preferredLocale, transliterateNames, direction);

			streetName.text = RoutingHelperUtils.formatStreetName(name, ref, destination, "»");
			if (!Algorithms.isEmpty(streetName.text)) {
				double dist = CurrentPositionHelper.getOrthogonalDistance(lastKnownSegment, lastKnownLocation);
				if (dist < MAX_MARKER_DISTANCE) {
					streetName.showMarker = true;
				} else {
					streetName.text = app.getString(R.string.shared_string_near) + " " + streetName.text;
				}
			}
		}
	}
}
package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.render.OsmandRenderer.RenderingContext;
import static net.osmand.plus.views.mapwidgets.WidgetType.STREET_NAME;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.BOTTOM;
import static net.osmand.plus.views.mapwidgets.widgets.NextTurnBaseWidget.SHIELD_HEIGHT_DP;
import static java.lang.Math.min;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.CurrentPositionHelper;
import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.TextDrawInfo;
import net.osmand.plus.render.TextRenderer;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.ShowAlongTheRouteBottomSheet;
import net.osmand.plus.routing.CurrentStreetName;
import net.osmand.plus.routing.NextDirectionInfo;
import net.osmand.plus.routing.RoadShield;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.TurnDrawable;
import net.osmand.plus.views.mapwidgets.WidgetsContextMenu;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetstates.StreetNameWidgetState;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import java.util.List;

public class StreetNameWidget extends MapWidget {

	private static final int MAX_MARKER_DISTANCE = 50;
	public static final int MAX_SHIELDS_QUANTITY = 3;

	private final WaypointHelper waypointHelper;
	private final RendererRegistry rendererRegistry;
	private final StreetNameWidgetState widgetState;

	private LocationPointWrapper lastPoint;

	private final TextView addressText;
	private final TextView addressTextShadow;
	private final TextView exitRefText;
	private final LinearLayout shieldImagesContainer;
	private final ImageView turnIcon;
	private final View waypointInfoBar;

	private final TurnDrawable turnDrawable;
	private int shadowRadius;
	private boolean showMarker;
	private List<RoadShield> cachedRoadShields;

	@Override
	protected int getLayoutId() {
		return R.layout.street_name_widget;
	}

	public StreetNameWidget(@NonNull MapActivity mapActivity,
	                        @Nullable String customId, @Nullable WidgetsPanel panel) {
		super(mapActivity, STREET_NAME, customId, panel);

		waypointHelper = app.getWaypointHelper();
		rendererRegistry = app.getRendererRegistry();
		widgetState = new StreetNameWidgetState(app, customId);

		addressText = view.findViewById(R.id.map_address_text);
		addressTextShadow = view.findViewById(R.id.map_address_text_shadow);
		waypointInfoBar = view.findViewById(R.id.waypoint_info_bar);
		exitRefText = view.findViewById(R.id.map_exit_ref);
		shieldImagesContainer = view.findViewById(R.id.map_shields_container);
		turnIcon = view.findViewById(R.id.map_turn_icon);

		turnDrawable = new TurnDrawable(mapActivity, true);

		setupLongClickListener();
		updateVisibility(false);
	}

	private void setupLongClickListener() {
		view.setOnLongClickListener(v -> {
			WidgetsContextMenu.showMenu(view, mapActivity, widgetType, customId, null, panel, nightMode, true);
			return true;
		});
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		ApplicationMode appMode = settings.getApplicationMode();
		boolean showNextTurn = isShowNextTurnEnabled(appMode);

		StreetNameWidgetParams params = new StreetNameWidgetParams(mapActivity, showNextTurn);
		CurrentStreetName streetName = params.streetName;
		int turnArrowColorId = params.turnArrowColorId;
		boolean showClosestWaypointFirstInAddress = params.showClosestWaypointFirstInAddress;

		if (turnArrowColorId != 0) {
			turnDrawable.setRouteDirectionColor(turnArrowColorId);
		}

		if (shouldHide() || streetName == null) {
			updateVisibility(false);
		} else if (showClosestWaypointFirstInAddress && updateWaypoint()) {
			updateVisibility(true);
			AndroidUiHelper.updateVisibility(addressText, false);
			AndroidUiHelper.updateVisibility(addressTextShadow, false);
			AndroidUiHelper.updateVisibility(turnIcon, false);
			AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
			AndroidUiHelper.updateVisibility(exitRefText, false);
		} else {
			updateVisibility(true);
			AndroidUiHelper.updateVisibility(waypointInfoBar, false);
			AndroidUiHelper.updateVisibility(addressText, true);
			AndroidUiHelper.updateVisibility(addressTextShadow, shadowRadius > 0);

			List<RoadShield> shields = streetName.shields;
			if (!shields.isEmpty() && !shields.equals(cachedRoadShields) && rendererRegistry.getCurrentSelectedRenderer() != null) {
				if (setRoadShield(shields)) {
					AndroidUiHelper.updateVisibility(shieldImagesContainer, true);
					int indexOf = streetName.text.indexOf("»");
					if (indexOf > 0) {
						streetName.text = streetName.text.substring(indexOf);
					}
				} else {
					AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
				}
				cachedRoadShields = shields;
			} else if (shields.isEmpty()) {
				AndroidUiHelper.updateVisibility(shieldImagesContainer, false);
				cachedRoadShields = null;
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

	protected boolean shouldHide() {
		return MapRouteInfoMenu.chooseRoutesVisible || MapRouteInfoMenu.waypointsVisible
				|| visibilityHelper.shouldHideVerticalWidgets()
				|| panel == BOTTOM && visibilityHelper.shouldHideBottomWidgets();
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
			WaypointDialogHelper.updatePointInfoView(mapActivity, view, point, true,
					isNightMode(), false, true);
			if (updated || changed) {
				ImageView moreButton = waypointInfoBar.findViewById(R.id.waypoint_more);
				ImageView closeButton = waypointInfoBar.findViewById(R.id.waypoint_close);
				moreButton.setOnClickListener(view -> {
					mapActivity.hideContextAndRouteInfoMenues();
					ShowAlongTheRouteBottomSheet.showInstance(
							mapActivity.getSupportFragmentManager(), null, point.type);
				});
				closeButton.setOnClickListener(view -> {
					waypointHelper.removeVisibleLocationPoint(point);
					mapActivity.refreshMap();
				});
			}
			return true;
		}
	}

	private boolean setRoadShield(@NonNull List<RoadShield> shields) {
		if (!Algorithms.isEmpty(shields)) {
			boolean isShieldSet = false;
			shieldImagesContainer.removeAllViews();
			int maxShields = min(shields.size(), MAX_SHIELDS_QUANTITY);
			for (int i = 0; i < maxShields; i++) {
				RoadShield shield = shields.get(i);
				isShieldSet |= setShieldImage(shield, mapActivity, shieldImagesContainer, isNightMode());
			}
			return isShieldSet;
		}
		return false;
	}

	public static boolean setShieldImage(@NonNull RoadShield shield,
			@NonNull MapActivity mapActivity,
			@NonNull LinearLayout shieldImagesContainer, boolean nightMode) {
		OsmandApplication app = mapActivity.getApp();
		RouteDataObject object = shield.getRdo();
		StringBuilder additional = shield.getAdditional();
		String shieldValue = shield.getValue();
		String shieldTag = shield.getTag();
		int[] types = object.getTypes();
		RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (storage == null) {
			return false;
		}
		RenderingRuleSearchRequest rreq = app.getResourceManager().getRenderer()
				.getSearchRequestWithAppliedCustomRules(storage, nightMode);

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

		rreq.setIntFilter(rreq.ALL.R_TEXT_LENGTH, shieldValue.length());
		rreq.setStringFilter(rreq.ALL.R_NAME_TAG, shieldTag);
		rreq.setStringFilter(rreq.ALL.R_ADDITIONAL, additional.toString());
		rreq.search(RenderingRulesStorage.TEXT_RULES);

		RenderingContext rc = new RenderingContext(app);
		TextRenderer textRenderer = new TextRenderer(app);
		TextDrawInfo text = new TextDrawInfo(shieldValue);

		int shieldRes = -1;
		if (rreq.isSpecified(rreq.ALL.R_TEXT_SHIELD)) {
			text.setShieldResIcon(rreq.getStringPropertyValue(rreq.ALL.R_TEXT_SHIELD));
			shieldRes = app.getResources().getIdentifier("h_" + text.getShieldResIcon(),
					"drawable", app.getPackageName());
		}
		if (shieldRes == -1) {
			return false;
		}

		Drawable shieldDrawable = AppCompatResources.getDrawable(mapActivity, shieldRes);
		if (shieldDrawable == null) {
			return false;
		}

		float xSize = shieldDrawable.getIntrinsicWidth();
		float ySize = shieldDrawable.getIntrinsicHeight();
		float xyRatio = xSize / ySize;

		int viewHeightPx = AndroidUtils.dpToPx(app, SHIELD_HEIGHT_DP);
		int viewWidthPx = (int) (viewHeightPx * xyRatio);
		float scaleCoefficient = viewWidthPx / xSize;
		;

		Bitmap bitmap = Bitmap.createBitmap(viewWidthPx, viewHeightPx, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = setupTextPaint(app, textRenderer.getPaintText(), rreq);
		float basePx = paint.getTextSize();
		paint.setTextSize(basePx * scaleCoefficient);

		float centerX = viewWidthPx / 2f;
		float centerY = viewHeightPx / 2f - paint.getFontMetrics().ascent / 2f;
		text.fillProperties(rc, rreq, centerX, centerY);
		textRenderer.drawShieldIcon(rc, canvas, text, text.getShieldResIcon());
		textRenderer.drawWrappedText(canvas, text, 20f);

		ImageView imageView = new ImageView(mapActivity);
		int viewSize = AndroidUtils.dpToPx(app, SHIELD_HEIGHT_DP);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(viewWidthPx, viewSize);
		int padding = AndroidUtils.dpToPx(app, 4f);
		imageView.setPadding(0, 0, 0, padding);
		imageView.setLayoutParams(params);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		imageView.setImageBitmap(bitmap);
		shieldImagesContainer.addView(imageView);
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
		view.setBackgroundResource(textState.widgetBackgroundId);

		TextView waypointText = view.findViewById(R.id.waypoint_text);
		TextView waypointTextShadow = view.findViewById(R.id.waypoint_text_shadow);
		updateTextColor(addressText, addressTextShadow, textState.textColor,
				textState.textShadowColor, textState.textBold, shadowRadius);
		updateTextColor(waypointText, waypointTextShadow, textState.textColor,
				textState.textShadowColor, textState.textBold, shadowRadius / 2);

		int exitRefTextColorId = isNightMode()
				? R.color.text_color_primary_dark
				: R.color.card_and_list_background_light;
		exitRefText.setTextColor(ContextCompat.getColor(app, exitRefTextColorId));

		ImageView moreImage = waypointInfoBar.findViewById(R.id.waypoint_more);
		ImageView removeImage = waypointInfoBar.findViewById(R.id.waypoint_close);
		moreImage.setImageDrawable(iconsCache.getIcon(R.drawable.ic_overflow_menu_white, isNightMode()));
		removeImage.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_remove_dark, isNightMode()));

		turnDrawable.updateColors(nightMode);
		turnDrawable.invalidateSelf();
	}

	@Override
	protected boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = super.updateVisibility(visible);
		if (updatedVisibility && widgetType.getPanel(settings) == WidgetsPanel.TOP) {
			MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				mapInfoLayer.updateVerticalPanels();
			}
			mapActivity.updateStatusBarColor();
		}
		return updatedVisibility;
	}

	private boolean isAnyStreetNameEnabledForMode(@NonNull List<MapWidgetInfo> widgets, @NonNull ApplicationMode mode) {
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.getWidgetType() == STREET_NAME && widgetInfo.isEnabledForAppMode(mode)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	public StreetNameWidgetState getWidgetState() {
		return widgetState;
	}

	public boolean isShowNextTurnEnabled(@NonNull ApplicationMode appMode) {
		return widgetState.isShowNextTurnEnabled(appMode);
	}

	public void setShowNextTurnEnabled(@NonNull ApplicationMode appMode, boolean value) {
		widgetState.setShowNextTurnEnabled(appMode, value);
	}

	static class StreetNameWidgetParams {

		private final OsmandApplication app;
		private final OsmandSettings settings;
		private final MapActivity mapActivity;
		private final RoutingHelper routingHelper;
		private final boolean showNextTurn;

		public CurrentStreetName streetName;
		@ColorRes
		public int turnArrowColorId;
		public boolean showClosestWaypointFirstInAddress = true;

		public StreetNameWidgetParams(@NonNull MapActivity mapActivity, boolean showNextTurn) {
			this.app = mapActivity.getApp();
			this.mapActivity = mapActivity;
			this.settings = app.getSettings();
			this.routingHelper = app.getRoutingHelper();
			this.showNextTurn = showNextTurn;

			computeParams();
		}

		private void computeParams() {
			if (routingHelper.isOnRoute()) {
				if (routingHelper.isFollowingMode()) {
					setupCurrentStreetName(showNextTurn);
					turnArrowColorId = R.color.nav_arrow;
				} else {
					int di = MapRouteInfoMenu.getDirectionInfo();
					boolean routeMenuVisible = mapActivity.getMapRouteInfoMenu().isVisible();
					if (di >= 0 && routeMenuVisible && di < routingHelper.getRouteDirections().size()) {
						setupCurrentStreetName(showNextTurn);
						turnArrowColorId = R.color.nav_arrow_distant;
						showClosestWaypointFirstInAddress = false;
					}
				}
			} else if (app.getMapViewTrackingUtilities().isMapLinkedToLocation()) {
				setupLastKnownStreetName();
			}
		}

		private void setupCurrentStreetName(boolean showNextTurn) {
			NextDirectionInfo nextDirInfo = new NextDirectionInfo();
			nextDirInfo = routingHelper.getNextRouteDirectionInfo(nextDirInfo, true);
			streetName = routingHelper.getCurrentName(nextDirInfo, showNextTurn);
		}

		private void setupLastKnownStreetName() {
			streetName = new CurrentStreetName();
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			RouteDataObject lastKnownSegment = locationProvider.getLastKnownRouteSegment();
			Location lastKnownLocation = locationProvider.getLastKnownLocation();
			if (lastKnownSegment != null && lastKnownLocation != null) {
				String locale = settings.MAP_PREFERRED_LOCALE.get();
				boolean transliterate = settings.MAP_TRANSLITERATE_NAMES.get();
				boolean direction = lastKnownSegment.bearingVsRouteDirection(lastKnownLocation);

				String name = lastKnownSegment.getName(locale, transliterate);
				String ref = lastKnownSegment.getRef(locale, transliterate, direction);
				String destination = lastKnownSegment.getDestinationName(locale, transliterate, direction);

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
}
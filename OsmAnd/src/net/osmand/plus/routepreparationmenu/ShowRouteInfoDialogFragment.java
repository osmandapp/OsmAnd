package net.osmand.plus.routepreparationmenu;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.LockableScrollView;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.PrintDialogActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.mapcontextmenu.InterceptorLinearLayout;
import net.osmand.plus.mapcontextmenu.MenuBuilder.CollapsableView;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routepreparationmenu.cards.RouteInfoCard;
import net.osmand.plus.routepreparationmenu.cards.RouteStatisticCard;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.plus.views.controls.HorizontalSwipeConfirm;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatistics;
import net.osmand.router.RouteStatistics.Incline;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static net.osmand.plus.mapcontextmenu.MapContextMenuFragment.CURRENT_Y_UNDEFINED;
import static net.osmand.plus.mapcontextmenu.MenuBuilder.SHADOW_HEIGHT_TOP_DP;
import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.MenuState.FULL_SCREEN;
import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.MenuState.HALF_SCREEN;
import static net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.MenuState.HEADER_ONLY;

public class ShowRouteInfoDialogFragment extends BaseOsmAndFragment {

	public static final String TAG = "ShowRouteInfoDialogFragment";

	private static final String TRANSPORT_ROUTE_ID_KEY = "route_id_key";

	private InterceptorLinearLayout mainView;
	private View view;
	private View.OnLayoutChangeListener containerLayoutListener;
	private View zoomButtonsView;
	private ImageButton myLocButtonView;
	private Toolbar toolbar;

	private boolean portrait;
	private boolean nightMode;
	private boolean moving;
	private boolean forceUpdateLayout;
	private boolean initLayout = true;
	private boolean wasDrawerDisabled;

	private int minHalfY;
	private int topScreenPosY;
	private int menuFullHeightMax;
	private int menuBottomViewHeight;
	private int menuFullHeight;
	private int screenHeight;
	private int viewHeight;
	private int topShadowMargin;
	private int currentMenuState;
	private int shadowHeight;
	private int zoomButtonsHeight;

	private int transportRouteId = -1;
	private String destinationStreetStr = "";
	private boolean paused;

	private OsmandApplication app;
	private RoutingHelper routingHelper;
	private GPXUtilities.GPXFile gpx;
	private GpxUiHelper.OrderedLineDataSet slopeDataSet;
	private GpxUiHelper.OrderedLineDataSet elevationDataSet;
	private GpxSelectionHelper.GpxDisplayItem gpxItem;
	private List<BaseCard> menuCards = new ArrayList<>();

	private String preferredMapLang;
	private boolean transliterateNames;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		app = getMyApplication();
		final MapActivity mapActivity = requireMapActivity();
		routingHelper = app.getRoutingHelper();

		preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		transliterateNames = app.getSettings().MAP_TRANSLITERATE_NAMES.get();

		view = inflater.inflate(R.layout.route_info_layout, container, false);
		AndroidUtils.addStatusBarPadding21v(getActivity(), view);

		Bundle args = getArguments();
		if (args != null) {
			transportRouteId = args.getInt(TRANSPORT_ROUTE_ID_KEY);
		}
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		topShadowMargin = AndroidUtils.dpToPx(mapActivity, 9f);

		shadowHeight = AndroidUtils.dpToPx(mapActivity, SHADOW_HEIGHT_TOP_DP);
		topScreenPosY = addStatusBarHeightIfNeeded(-shadowHeight);

		mainView = view.findViewById(R.id.main_view);
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
		currentMenuState = getInitialMenuState();

		toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(app.getUIUtilities().getThemedIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		toolbar.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.bg_color_dark : R.color.bg_color_light));

		buildMenuButtons();
		updateCards();

		processScreenHeight(container);

		minHalfY = viewHeight - (int) (viewHeight * .75f);

		// Zoom buttons
		zoomButtonsView = view.findViewById(R.id.map_hud_controls);
		ImageButton zoomInButtonView = (ImageButton) view.findViewById(R.id.map_zoom_in_button);
		ImageButton zoomOutButtonView = (ImageButton) view.findViewById(R.id.map_zoom_out_button);
		myLocButtonView = (ImageButton) view.findViewById(R.id.map_my_location_button);

		AndroidUtils.updateImageButton(app, zoomInButtonView, R.drawable.map_zoom_in, R.drawable.map_zoom_in_night,
				R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
		AndroidUtils.updateImageButton(app, zoomOutButtonView, R.drawable.map_zoom_out, R.drawable.map_zoom_out_night,
				R.drawable.btn_circle_trans, R.drawable.btn_circle_night, nightMode);
		zoomInButtonView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doZoomIn();
			}
		});
		zoomOutButtonView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doZoomOut();
			}
		});

		myLocButtonView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
					mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
				} else {
					ActivityCompat.requestPermissions(mapActivity,
							new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
							OsmAndLocationProvider.REQUEST_LOCATION_PERMISSION);
				}
			}
		});
		updateMyLocation(mapActivity.getRoutingHelper());

		zoomButtonsView.setVisibility(View.VISIBLE);

		LockableScrollView bottomScrollView = (LockableScrollView) view.findViewById(R.id.route_menu_bottom_scroll);
		bottomScrollView.setScrollingEnabled(false);
		AndroidUtils.setBackground(app, bottomScrollView, nightMode, R.color.route_info_bg_light, R.color.route_info_bg_dark);

		AndroidUtils.setBackground(app, mainView, nightMode, R.drawable.bg_map_context_menu_light, R.drawable.bg_map_context_menu_dark);

		if (!portrait) {
			final TypedValue typedValueAttr = new TypedValue();
			mapActivity.getTheme().resolveAttribute(R.attr.left_menu_view_bg, typedValueAttr, true);
			mainView.setBackgroundResource(typedValueAttr.resourceId);
			mainView.setLayoutParams(new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.dashboard_land_width), ViewGroup.LayoutParams.MATCH_PARENT));

			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(AndroidUtils.dpToPx(mapActivity, 345f), ViewGroup.LayoutParams.WRAP_CONTENT);
			params.gravity = Gravity.BOTTOM;
		}

		runLayoutListener();

		final GestureDetector swipeDetector = new GestureDetector(app, new HorizontalSwipeConfirm(true));

		final View.OnTouchListener slideTouchListener = new View.OnTouchListener() {
			private float dy;
			private float dyMain;
			private float mDownY;

			private int minimumVelocity;
			private int maximumVelocity;
			private VelocityTracker velocityTracker;
			private OverScroller scroller;

			private boolean slidingUp;
			private boolean slidingDown;

			{
				scroller = new OverScroller(app);
				final ViewConfiguration configuration = ViewConfiguration.get(app);
				minimumVelocity = configuration.getScaledMinimumFlingVelocity();
				maximumVelocity = configuration.getScaledMaximumFlingVelocity();
			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				if (!portrait) {
					if (swipeDetector.onTouchEvent(event)) {
						dismiss();

						recycleVelocityTracker();
						return true;
					}
				}

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						mDownY = event.getRawY();
						dy = event.getY();
						dyMain = getViewY();

						initOrResetVelocityTracker();
						velocityTracker.addMovement(event);
						break;

					case MotionEvent.ACTION_MOVE:
						if (Math.abs(event.getRawY() - mDownY) > mainView.getTouchSlop()) {
							moving = true;
						}
						if (moving) {
							float y = event.getY();
							float newY = getViewY() + (y - dy);
							if (!portrait && newY > topScreenPosY) {
								newY = topScreenPosY;
							}
							setViewY((int) newY, false, false);

							ViewGroup.LayoutParams lp = mainView.getLayoutParams();
							lp.height = view.getHeight() - (int) newY + 10;
							mainView.setLayoutParams(lp);
							mainView.requestLayout();

							float newEventY = newY - (dyMain - dy);
							MotionEvent ev = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(),
									event.getX(), newEventY, event.getMetaState());

							initVelocityTrackerIfNotExists();
							velocityTracker.addMovement(ev);
						}

						break;

					case MotionEvent.ACTION_UP:
						if (moving) {
							moving = false;
							int currentY = getViewY();

							final VelocityTracker velocityTracker = this.velocityTracker;
							velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
							int initialVelocity = (int) velocityTracker.getYVelocity();

							if ((Math.abs(initialVelocity) > minimumVelocity)) {

								scroller.abortAnimation();
								scroller.fling(0, currentY, 0, initialVelocity, 0, 0,
										Math.min(viewHeight - menuFullHeightMax, getFullScreenTopPosY()),
										screenHeight,
										0, 0);
								currentY = scroller.getFinalY();
								scroller.abortAnimation();

								slidingUp = initialVelocity < -2000;
								slidingDown = initialVelocity > 2000;
							} else {
								slidingUp = false;
								slidingDown = false;
							}

							changeMenuState(currentY, slidingUp, slidingDown);
						}
						recycleVelocityTracker();
						break;
					case MotionEvent.ACTION_CANCEL:
						moving = false;
						recycleVelocityTracker();
						break;

				}
				return true;
			}

			private void initOrResetVelocityTracker() {
				if (velocityTracker == null) {
					velocityTracker = VelocityTracker.obtain();
				} else {
					velocityTracker.clear();
				}
			}

			private void initVelocityTrackerIfNotExists() {
				if (velocityTracker == null) {
					velocityTracker = VelocityTracker.obtain();
					velocityTracker.clear();
				}
			}

			private void recycleVelocityTracker() {
				if (velocityTracker != null) {
					velocityTracker.recycle();
					velocityTracker = null;
				}
			}
		};

		((InterceptorLinearLayout) mainView).setListener(slideTouchListener);
		mainView.setOnTouchListener(slideTouchListener);

		containerLayoutListener = new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View view, int left, int top, int right, int bottom,
			                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
				if (forceUpdateLayout || bottom != oldBottom) {
					forceUpdateLayout = false;
					processScreenHeight(view.getParent());
					runLayoutListener();
				}
			}
		};

		runLayoutListener();

		return view;
	}

	public boolean isPublicTransportInfo() {
		return transportRouteId != -1;
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			LinearLayout cardsContainer = (LinearLayout) view.findViewById(R.id.route_menu_cards_container);
			cardsContainer.removeAllViews();
			if (transportRouteId != -1) {
				List<TransportRouteResult> routes = routingHelper.getTransportRoutingHelper().getRoutes();
				if (routes != null && routes.size() > transportRouteId) {
					TransportRouteResult routeResult = routingHelper.getTransportRoutingHelper().getRoutes().get(transportRouteId);
					PublicTransportCard card = new PublicTransportCard(mapActivity, routeResult, transportRouteId);
					menuCards.add(card);
					cardsContainer.addView(card.build(mapActivity));
					buildRowDivider(cardsContainer, false);
					buildTransportRouteRow(cardsContainer, routeResult, true);
					buildRowDivider(cardsContainer, false);
				}
			} else {
				makeGpx();
				createRouteStatisticCards(cardsContainer);
				createRouteDirections(cardsContainer);
			}
		}
	}

	private void createRouteDirections(LinearLayout cardsContainer) {
		TextViewEx routeDirectionsTitle = new TextViewEx(app);
		AndroidUtils.setTextPrimaryColor(app, routeDirectionsTitle, nightMode);
		routeDirectionsTitle.setTextSize(15);
		routeDirectionsTitle.setGravity(Gravity.CENTER_VERTICAL);
		routeDirectionsTitle.setPadding(dpToPx(16), dpToPx(16), AndroidUtils.dpToPx(app, 16), AndroidUtils.dpToPx(app, 16));
		routeDirectionsTitle.setText(R.string.step_by_step);
		routeDirectionsTitle.setTypeface(FontCache.getRobotoMedium(app));
		cardsContainer.addView(routeDirectionsTitle);

		List<RouteDirectionInfo> routeDirections = routingHelper.getRouteDirections();
		for (int i = 0; i < routeDirections.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = routeDirections.get(i);
			OnClickListener onClickListener = createRouteDirectionInfoViewClickListener(i, routeDirectionInfo);
			View view = getRouteDirectionView(i, cardsContainer, routeDirectionInfo, routeDirections, onClickListener);
			cardsContainer.addView(view);
		}
	}

	private OnClickListener createRouteDirectionInfoViewClickListener(final int directionInfoIndex, final RouteDirectionInfo routeDirectionInfo) {
		return new OnClickListener() {
			@Override
			public void onClick(View view) {
				Location loc = routingHelper.getLocationFromRouteDirection(routeDirectionInfo);
				if (loc != null) {
					MapRouteInfoMenu.directionInfo = directionInfoIndex;
					OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(loc.getLatitude(), loc.getLongitude(),
							Math.max(13, settings.getLastKnownMapZoom()),
							new PointDescription(PointDescription.POINT_TYPE_MARKER, routeDirectionInfo.getDescriptionRoutePart() + " " + getTimeDescription(routeDirectionInfo)),
							false, null);
					MapActivity.launchMapActivityMoveToTop(getActivity());
					dismiss();
				}
			}
		};
	}

	private void createRouteStatisticCards(LinearLayout cardsContainer) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		if (gpx.hasAltitude) {
			RouteStatisticCard statisticCard = new RouteStatisticCard(mapActivity, gpx, new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					mainView.requestDisallowInterceptTouchEvent(true);
					return false;
				}
			});
			menuCards.add(statisticCard);
			cardsContainer.addView(statisticCard.build(mapActivity));
			buildRowDivider(cardsContainer, false);
			slopeDataSet = statisticCard.getSlopeDataSet();
			elevationDataSet = statisticCard.getElevationDataSet();
			List<RouteSegmentResult> route = routingHelper.getRoute().getOriginalRoute();
			if (route != null) {
				RenderingRulesStorage defaultRender = app.getRendererRegistry().defaultRender();
				RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();

				MapRenderRepositories maps = app.getResourceManager().getRenderer();
				RenderingRuleSearchRequest currentSearchRequest = maps.getSearchRequestWithAppliedCustomRules(currentRenderer, nightMode);
				RenderingRuleSearchRequest defaultSearchRequest = maps.getSearchRequestWithAppliedCustomRules(defaultRender, nightMode);

				RouteStatistics routeStatistics = RouteStatistics.newRouteStatistic(route, currentRenderer, defaultRender, currentSearchRequest, defaultSearchRequest);
				GPXUtilities.GPXTrackAnalysis analysis = gpx.getAnalysis(0);

				RouteInfoCard routeClassCard = new RouteInfoCard(mapActivity, routeStatistics.getRouteClassStatistic(), analysis);
				createRouteCard(cardsContainer, routeClassCard);

				RouteInfoCard routeSurfaceCard = new RouteInfoCard(mapActivity, routeStatistics.getRouteSurfaceStatistic(), analysis);
				createRouteCard(cardsContainer, routeSurfaceCard);

				if (slopeDataSet != null) {
					List<Incline> inclines = createInclinesAndAdd100MetersWith0Incline(slopeDataSet.getValues());
					RouteInfoCard routeSteepnessCard = new RouteInfoCard(mapActivity, routeStatistics.getRouteSteepnessStatistic(inclines), analysis);
					createRouteCard(cardsContainer, routeSteepnessCard);
				}

				RouteInfoCard routeSmoothnessCard = new RouteInfoCard(mapActivity, routeStatistics.getRouteSmoothnessStatistic(), analysis);
				createRouteCard(cardsContainer, routeSmoothnessCard);
			}
		}
	}

	private void createRouteCard(LinearLayout cardsContainer, RouteInfoCard routeInfoCard) {
		menuCards.add(routeInfoCard);
		cardsContainer.addView(routeInfoCard.build(app));
		buildRowDivider(cardsContainer, false);
	}

	private void updateMyLocation(RoutingHelper rh) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		Location lastKnownLocation = app.getLocationProvider().getLastKnownLocation();
		boolean enabled = lastKnownLocation != null;
		boolean tracked = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (!enabled) {
			myLocButtonView.setImageDrawable(getIcon(R.drawable.map_my_location, R.color.icon_color));
			AndroidUtils.setBackground(app, myLocButtonView, nightMode, R.drawable.btn_circle, R.drawable.btn_circle_night);
			myLocButtonView.setContentDescription(mapActivity.getString(R.string.unknown_location));
		} else if (tracked) {
			myLocButtonView.setImageDrawable(getIcon(R.drawable.map_my_location, R.color.color_myloc_distance));
			AndroidUtils.setBackground(app, myLocButtonView, nightMode, R.drawable.btn_circle, R.drawable.btn_circle_night);
		} else {
			myLocButtonView.setImageResource(R.drawable.map_my_location);
			AndroidUtils.setBackground(app, myLocButtonView, nightMode, R.drawable.btn_circle_blue, R.drawable.btn_circle_blue);
			myLocButtonView.setContentDescription(mapActivity.getString(R.string.map_widget_back_to_loc));
		}
		if (app.accessibilityEnabled()) {
			myLocButtonView.setClickable(enabled && !tracked && rh.isFollowingMode());
		}
	}

	public void doZoomIn() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandMapTileView map = mapActivity.getMapView();
			if (map.isZooming() && map.hasCustomMapRatio()) {
				mapActivity.changeZoom(2, System.currentTimeMillis());
			} else {
				mapActivity.changeZoom(1, System.currentTimeMillis());
			}
		}
	}

	public void doZoomOut() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.changeZoom(-1, System.currentTimeMillis());
		}
	}

	public Drawable getCollapseIcon(boolean collapsed) {
		return app.getUIUtilities().getIcon(collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up,
				!nightMode ? R.color.ctx_menu_collapse_icon_color_light : R.color.ctx_menu_collapse_icon_color_dark);
	}

	private void buildSegmentItem(View view, TransportRouteResultSegment segment, long startTime) {
		TransportRoute transportRoute = segment.route;
		List<TransportStop> stops = segment.getTravelStops();
		final TransportStop startStop = stops.get(0);
		TransportStopRoute transportStopRoute = TransportStopRoute.getTransportStopRoute(transportRoute, startStop);

		FrameLayout baseContainer = new FrameLayout(view.getContext());

		ImageView routeLine = new ImageView(view.getContext());
		FrameLayout.LayoutParams routeLineParams = new FrameLayout.LayoutParams(dpToPx(8f), ViewGroup.LayoutParams.MATCH_PARENT);
		routeLineParams.gravity = Gravity.START;
		routeLineParams.setMargins(dpToPx(24), dpToPx(14), dpToPx(22), dpToPx(36));
		routeLine.setLayoutParams(routeLineParams);
		int bgColor = transportStopRoute.getColor(app, nightMode);
		routeLine.setBackgroundColor(bgColor);
		baseContainer.addView(routeLine);

		LinearLayout stopsContainer = new LinearLayout(view.getContext());
		stopsContainer.setOrientation(LinearLayout.VERTICAL);
		baseContainer.addView(stopsContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
		Drawable icon = getContentIcon(drawableResId);

		Typeface typeface = FontCache.getRobotoMedium(app);
		String timeText = OsmAndFormatter.getFormattedTime(startTime, false);

		SpannableString secondaryText = new SpannableString(getString(R.string.sit_on_the_stop));
		secondaryText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		SpannableString title = new SpannableString(startStop.getName(preferredMapLang, transliterateNames));
		title.setSpan(new CustomTypefaceSpan(typeface), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.setSpan(new ForegroundColorSpan(getActiveColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildStartStopRow(stopsContainer, icon, timeText, transportStopRoute, title, secondaryText, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(startStop.getLocation());
			}
		});

		CollapsableView collapsableView = null;
		if (stops.size() > 2) {
			collapsableView = getCollapsableTransportStopRoutesView(app, transportStopRoute, stops.subList(1, stops.size() - 1));
		}
		SpannableStringBuilder spannable = new SpannableStringBuilder();
		int startIndex;
		int arrivalTime = segment.getArrivalTime();
		if (arrivalTime > 0) {
			spannable.append("~");
			startIndex = spannable.length();
			spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			spannable.append(OsmAndFormatter.getFormattedDuration(arrivalTime, app));
			spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		} else {
			arrivalTime = 0;
		}
		startIndex = spannable.length();
		if (stops.size() > 2) {
			if (spannable.length() > 0) {
				startIndex = spannable.length();
				spannable.append(" • ");
				spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), startIndex, startIndex + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			startIndex = spannable.length();
			spannable.append(String.valueOf(stops.size())).append(" ").append(getString(R.string.transport_stops));
			spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (spannable.length() > 0) {
			startIndex = spannable.length();
			spannable.append(" • ");
			spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), startIndex, startIndex + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		spannable.append(OsmAndFormatter.getFormattedDistance((float) segment.getTravelDist(), app));
		spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		String type = getString(transportStopRoute.getTypeStrRes()).toLowerCase();
		SpannableString textType = new SpannableString(getString(R.string.by_transport_type, type));
		buildCollapsableRow(stopsContainer, spannable, textType, true, collapsableView, null);

		final TransportStop endStop = stops.get(stops.size() - 1);
		long depTime = segment.depTime + arrivalTime;
		if (depTime <= 0) {
			depTime = startTime + arrivalTime;
		}
		String textTime = OsmAndFormatter.getFormattedTime(depTime, false);

		secondaryText = new SpannableString(getString(R.string.exit_at));
		secondaryText.setSpan(new CustomTypefaceSpan(typeface), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int spaceIndex = secondaryText.toString().indexOf(" ");
		if (spaceIndex != -1) {
			secondaryText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)), 0, spaceIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		title = new SpannableString(endStop.getName(preferredMapLang, transliterateNames));
		title.setSpan(new CustomTypefaceSpan(typeface), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.setSpan(new ForegroundColorSpan(getActiveColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildEndStopRow(stopsContainer, icon, textTime, title, secondaryText, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(endStop.getLocation());
			}
		});

		((ViewGroup) view).addView(baseContainer);
	}

	private View createImagesContainer() {
		LinearLayout imagesContainer = new LinearLayout(view.getContext());
		FrameLayout.LayoutParams imagesContainerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		imagesContainer.setPadding(dpToPx(16), dpToPx(12), dpToPx(24), 0);
		imagesContainer.setOrientation(LinearLayout.VERTICAL);
		imagesContainer.setLayoutParams(imagesContainerParams);
		return imagesContainer;
	}

	private View createInfoContainer() {
		LinearLayout infoContainer = new LinearLayout(view.getContext());
		FrameLayout.LayoutParams infoContainerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		infoContainer.setOrientation(LinearLayout.VERTICAL);
		infoContainer.setLayoutParams(infoContainerParams);
		return infoContainer;
	}

	private void buildTransportRouteRow(ViewGroup parent, TransportRouteResult routeResult, boolean showDivider) {
		Typeface typeface = FontCache.getRobotoMedium(app);
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TargetPoint startPoint = targetPointsHelper.getPointToStart();
		TargetPoint endPoint = targetPointsHelper.getPointToNavigate();
		long startTime = System.currentTimeMillis() / 1000;

		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		boolean previousWalkItemUsed = false;

		for (int i = 0; i < segments.size(); i++) {
			final TransportRouteResultSegment segment = segments.get(i);
			final TransportRouteResultSegment nextSegment = segments.size() > i + 1 ? segments.get(i + 1) : null;
			long walkTime = (long) getWalkTime(segment.walkDist, routeResult.getWalkSpeed());
			if (walkTime < 60) {
				walkTime = 60;
			}
			if (i == 0) {
				buildStartItem(parent, startPoint, startTime, segment, routeResult.getWalkSpeed());
				startTime += walkTime;
			} else if (segment.walkDist > 0 && !previousWalkItemUsed) {
				SpannableStringBuilder spannable = new SpannableStringBuilder("~");
				int startIndex = spannable.length();
				spannable.append(OsmAndFormatter.getFormattedDuration((int) walkTime, app)).append(" ");
				spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				spannable.append(getString(R.string.on_foot)).append(" ").append(", ").append(OsmAndFormatter.getFormattedDistance((float) segment.walkDist, app));

				buildWalkRow(parent, spannable, null, new OnClickListener() {
					@Override
					public void onClick(View v) {
						showWalkingRouteOnMap(segment, nextSegment);
					}
				});
				buildRowDivider(parent, true);
				startTime += walkTime;
			}

			buildSegmentItem(parent, segment, startTime);

			double finishWalkDist = routeResult.getFinishWalkDist();
			if (i == segments.size() - 1) {
				buildDestinationItem(parent, endPoint, startTime, segment, routeResult.getWalkSpeed());
			} else if (finishWalkDist > 0) {
				walkTime = (long) getWalkTime(finishWalkDist, routeResult.getWalkSpeed());
				startTime += walkTime;
				if (nextSegment != null) {
					if (nextSegment.walkDist > 0) {
						finishWalkDist += nextSegment.walkDist;
						walkTime += getWalkTime(nextSegment.walkDist, routeResult.getWalkSpeed());
						previousWalkItemUsed = true;
					} else {
						previousWalkItemUsed = false;
					}
				}
				buildRowDivider(parent, true);

				Spannable title = getWalkTitle(finishWalkDist, walkTime);
				buildWalkRow(parent, title, null, new OnClickListener() {
					@Override
					public void onClick(View v) {
						showWalkingRouteOnMap(segment, nextSegment);
					}
				});
			}
			if (showDivider && i != segments.size() - 1) {
				buildRowDivider(parent, true);
			}
		}
	}

	private Spannable getWalkTitle(double finishWalkDist, double walkTime) {
		if (walkTime < 60) {
			walkTime = 60;
		}
		Typeface typeface = FontCache.getRobotoMedium(app);
		SpannableStringBuilder title = new SpannableStringBuilder("~");
		int startIndex = title.length();
		title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.append(OsmAndFormatter.getFormattedDuration((int) walkTime, app)).append(" ");
		title.setSpan(new CustomTypefaceSpan(typeface), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = title.length();
		title.append(getString(R.string.on_foot)).append(", ").append(OsmAndFormatter.getFormattedDistance((float) finishWalkDist, app));
		title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return title;
	}

	private void buildStartItem(View view, final TargetPoint start, long startTime,
								final TransportRouteResultSegment segment, double walkSpeed) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());

		LinearLayout imagesContainer = (LinearLayout) createImagesContainer();
		baseItemView.addView(imagesContainer);

		LinearLayout infoContainer = (LinearLayout) createInfoContainer();
		baseItemView.addView(infoContainer);

		String name;
		if (start != null) {
			name = start.getOnlyName().length() > 0 ? start.getOnlyName() :
					(getString(R.string.route_descr_map_location) + " " + getRoutePointDescription(start.getLatitude(), start.getLongitude()));
		} else {
			name = getString(R.string.shared_string_my_location);
		}
		Spannable startTitle = new SpannableString(name);
		String text = OsmAndFormatter.getFormattedTime(startTime, false);

		int drawableId = start == null ? R.drawable.ic_action_location_color : R.drawable.list_startpoint;
		Drawable icon = app.getUIUtilities().getIcon(drawableId);

		buildStartRow(infoContainer, icon, text, startTitle, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(start != null ? start.point : null);
			}
		});
		addWalkRouteIcon(imagesContainer);
		buildRowDivider(infoContainer, true);

		long walkTime = (long) getWalkTime(segment.walkDist, walkSpeed);
		if (walkTime < 60) {
			walkTime = 60;
		}
		SpannableStringBuilder title = new SpannableStringBuilder(Algorithms.capitalizeFirstLetter(getString(R.string.on_foot)));
		title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int startIndex = title.length();
		title.append(" ").append(OsmAndFormatter.getFormattedDuration((int) walkTime, app));
		title.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = title.length();
		title.append(", ").append(OsmAndFormatter.getFormattedDistance((float) segment.walkDist, app));
		title.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildWalkRow(infoContainer, title, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showWalkingRouteOnMap(null, segment);
			}
		});
		buildRowDivider(infoContainer, true);

		((ViewGroup) view).addView(baseItemView);
	}

	public void showLocationOnMap(LatLon latLon) {
		OsmandSettings settings = app.getSettings();
		if (latLon == null) {
			latLon = settings.isLastKnownMapLocation() ? settings.getLastKnownMapLocation() : null;
		}
		if (latLon != null) {
			openMenuHeaderOnly();
			showOnMap(latLon);
		}
	}

	public void showWalkingRouteOnMap(TransportRouteResultSegment startSegment, TransportRouteResultSegment endSegment) {
		RouteCalculationResult walkingRouteSegment = app.getTransportRoutingHelper().getWalkingRouteSegment(startSegment, endSegment);
		if (walkingRouteSegment != null) {
			QuadRect rect = walkingRouteSegment.getLocationsRect();
			if (rect != null) {
				openMenuHeaderOnly();
				fitRectOnMap(rect, null, true);
			}
		}
	}

	public void showRouteOnMap(TransportRouteResult result) {
		if (result != null) {
			QuadRect rect = app.getTransportRoutingHelper().getTransportRouteRect(result);
			if (rect != null) {
				openMenuHeaderOnly();
				fitRectOnMap(rect, null, true);
			}
		}
	}

	private void addWalkRouteIcon(LinearLayout container) {
		ImageView walkLineImage = new ImageView(view.getContext());
		walkLineImage.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.walk_route_item_light));
		LinearLayout.LayoutParams walkImageLayoutParams = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(14));
		walkImageLayoutParams.setMargins(dpToPx(7), dpToPx(6), 0, dpToPx(6));
		walkLineImage.setLayoutParams(walkImageLayoutParams);
		container.addView(walkLineImage);
	}

	private void buildDestinationItem(View view, final TargetPoint destination, long startTime, final TransportRouteResultSegment segment, double walkSpeed) {
		Typeface typeface = FontCache.getRobotoMedium(app);
		FrameLayout baseItemView = new FrameLayout(view.getContext());

		LinearLayout imagesContainer = (LinearLayout) createImagesContainer();
		baseItemView.addView(imagesContainer);

		LinearLayout infoContainer = (LinearLayout) createInfoContainer();
		baseItemView.addView(infoContainer);

		buildRowDivider(infoContainer, true);

		long walkTime = (long) getWalkTime(segment.walkDist, walkSpeed);
		if (walkTime < 60) {
			walkTime = 60;
		}
		SpannableStringBuilder spannable = new SpannableStringBuilder("~");
		spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int startIndex = spannable.length();
		spannable.append(OsmAndFormatter.getFormattedDuration((int) walkTime, app)).append(" ");
		spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = spannable.length();
		spannable.append(getString(R.string.on_foot)).append(", ").append(OsmAndFormatter.getFormattedDistance((float) segment.walkDist, app));
		spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.secondary_text_dark : R.color.secondary_text_light)), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildWalkRow(infoContainer, spannable, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showWalkingRouteOnMap(segment, null);
			}
		});
		buildRowDivider(infoContainer, true);
		addWalkRouteIcon(imagesContainer);

		String timeStr = OsmAndFormatter.getFormattedTime(startTime + walkTime, false);
		String name = getRoutePointDescription(destination.point, destination.getOnlyName());
		SpannableString title = new SpannableString(name);
		title.setSpan(new CustomTypefaceSpan(typeface), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.setSpan(new ForegroundColorSpan(getActiveColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		SpannableString secondaryText = new SpannableString(getString(R.string.route_descr_destination));
		secondaryText.setSpan(new CustomTypefaceSpan(typeface), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		secondaryText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildDestinationRow(infoContainer, timeStr, title, secondaryText, destination.point, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(destination.point);
			}
		});

		((ViewGroup) view).addView(baseItemView);
	}

	private int getActiveColor() {
		return ContextCompat.getColor(app, !nightMode ? R.color.ctx_menu_bottom_view_url_color_light : R.color.ctx_menu_bottom_view_url_color_dark);
	}

	public void buildCollapsableRow(final View view, final Spannable title, Spannable secondaryText, boolean collapsable,
	                                final CollapsableView collapsableView, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(48, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);

		if (!TextUtils.isEmpty(secondaryText)) {
			buildDescriptionView(secondaryText, llText, 8, 0);
		}

		buildTitleView(title, llText);

		final ImageView iconViewCollapse = new ImageView(view.getContext());
		if (collapsable && collapsableView != null) {
			// Icon
			LinearLayout llIconCollapse = new LinearLayout(view.getContext());
			llIconCollapse.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(48f)));
			llIconCollapse.setOrientation(LinearLayout.HORIZONTAL);
			llIconCollapse.setGravity(Gravity.CENTER_VERTICAL);
			ll.addView(llIconCollapse);

			LinearLayout.LayoutParams llIconCollapseParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			llIconCollapseParams.setMargins(0, dpToPx(12f), 0, dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(getCollapseIcon(collapsableView.getContenView().getVisibility() == View.GONE));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					LinearLayout contentView = (LinearLayout) collapsableView.getContenView();
					if (contentView.getVisibility() == View.VISIBLE) {
						contentView.setVisibility(View.GONE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(true));
						collapsableView.setCollapsed(true);
						contentView.getChildAt(contentView.getChildCount() - 1).setVisibility(View.VISIBLE);
					} else {
						contentView.setVisibility(View.VISIBLE);
						iconViewCollapse.setImageDrawable(getCollapseIcon(false));
						collapsableView.setCollapsed(false);
					}
					doAfterMenuStateChange(0, 0);
				}
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContenView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			baseView.addView(collapsableView.getContenView());
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildStartStopRow(final View view, Drawable icon, String timeText, TransportStopRoute transportStopRoute,
	                              final Spannable title, Spannable secondaryText, OnClickListener onClickListener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(48, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(28), dpToPx(28));
			imageViewLayoutParams.gravity = Gravity.TOP;
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			imageViewLayoutParams.setMargins(dpToPx(14), dpToPx(8), dpToPx(22), 0);
			iconView.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
			iconView.setBackgroundResource(R.drawable.border_round_solid_light);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);

		if (!TextUtils.isEmpty(secondaryText)) {
			buildDescriptionView(secondaryText, llText, 8, 0);
		}

		buildTitleView(title, llText);

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			AndroidUtils.setTextPrimaryColor(app, timeView, nightMode);

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (transportStopRoute != null) {
			TextView routeTypeView = new TextView(view.getContext());
			LinearLayout.LayoutParams routeTypeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			routeTypeParams.setMargins(0, dpToPx(4), 0, 0);
			routeTypeView.setLayoutParams(routeTypeParams);
			routeTypeView.setTextSize(16);
			AndroidUtils.setTextSecondaryColor(app, routeTypeView, nightMode);
			routeTypeView.setText(transportStopRoute.getDescription(app));
			llText.addView(routeTypeView);

			View routeBadge = createRouteBadge(mapActivity, transportStopRoute, nightMode);
			LinearLayout.LayoutParams routeBadgeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			routeBadgeParams.setMargins(0, dpToPx(6), 0, dpToPx(8));
			routeBadge.setLayoutParams(routeBadgeParams);
			llText.addView(routeBadge);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildEndStopRow(final View view, Drawable icon, String timeText, final Spannable title, Spannable secondaryText, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(48, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(28), dpToPx(28));
			imageViewLayoutParams.gravity = Gravity.TOP;
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			imageViewLayoutParams.setMargins(dpToPx(14), dpToPx(8), dpToPx(22), 0);
			iconView.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
			iconView.setBackgroundResource(R.drawable.border_round_solid_light);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);

		if (!TextUtils.isEmpty(secondaryText)) {
			buildDescriptionView(secondaryText, llText, 8, 0);
		}

		buildTitleView(title, llText);

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			AndroidUtils.setTextPrimaryColor(app, timeView, nightMode);

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}


	protected void updateDestinationStreetName(LatLon latLon) {
		final WeakReference<ShowRouteInfoDialogFragment> fragmentRef = new WeakReference<>(ShowRouteInfoDialogFragment.this);
		GeocodingLookupService.AddressLookupRequest addressLookupRequest = new GeocodingLookupService.AddressLookupRequest(latLon, new GeocodingLookupService.OnAddressLookupResult() {
			@Override
			public void geocodingDone(String address) {
				ShowRouteInfoDialogFragment fragment = fragmentRef.get();
				if (!TextUtils.isEmpty(address) && fragment != null && !fragment.paused) {
					fragment.destinationStreetStr = address;
					fragment.updateCards();
				}
			}
		}, null);

		app.getGeocodingLookupService().lookupAddress(addressLookupRequest);
	}

	public void buildWalkRow(final View view, final Spannable title, LinearLayout imagesContainer, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(48, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		Drawable icon = getIcon(R.drawable.ic_action_pedestrian_dark, !nightMode ? R.color.ctx_menu_bottom_view_url_color_light : R.color.ctx_menu_bottom_view_url_color_dark);
		ImageView iconView = new ImageView(view.getContext());
		iconView.setImageDrawable(icon);
		FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24));
		imageViewLayoutParams.gravity = imagesContainer != null ? Gravity.TOP : Gravity.CENTER_VERTICAL;
		iconView.setLayoutParams(imageViewLayoutParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

		if (imagesContainer != null) {
			imagesContainer.addView(iconView);
		} else {
			imageViewLayoutParams.setMargins(dpToPx(16), 0, dpToPx(24), 0);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);
		buildTitleView(title, llText);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	private void showOnMap(@NonNull LatLon latLon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int currentZoom = mapActivity.getMapView().getZoom();
			mapActivity.getMapView().getAnimatedDraggingThread().startMoving(latLon.getLatitude(), latLon.getLongitude(), Math.max(15, currentZoom), true);
		}
	}

	private void fitRectOnMap(QuadRect rect, LatLon location, boolean forceFit) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
			int tileBoxWidthPx = 0;
			int tileBoxHeightPx = 0;

			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				tileBoxWidthPx = tb.getPixWidth() - view.getWidth();
			} else {
				tileBoxHeightPx = getHeaderOnlyTopY();
			}
			if (tileBoxHeightPx > 0) {
				int topMarginPx = toolbar.getHeight();
				if (forceFit) {
					mapActivity.getMapView().fitRectToMap(rect.left, rect.right, rect.top, rect.bottom,
							tileBoxWidthPx, tileBoxHeightPx, topMarginPx);
				} else if (location != null &&
						!mapActivity.getMapView().getTileBox(tileBoxWidthPx, tileBoxHeightPx, topMarginPx).containsLatLon(location)) {
					boolean animating = mapActivity.getMapView().getAnimatedDraggingThread().isAnimating();
					mapActivity.getMapView().fitLocationToMap(location.getLatitude(), location.getLongitude(),
							mapActivity.getMapView().getZoom(), tileBoxWidthPx, tileBoxHeightPx, topMarginPx, !animating);
				} else {
					mapActivity.refreshMap();
				}
			}
		}
	}

	public void buildStartRow(final View view, Drawable icon, String timeText, final Spannable title, LinearLayout imagesContainer, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(48, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24));
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			if (imagesContainer != null) {
				imagesContainer.addView(iconView);
			} else {
				imageViewLayoutParams.setMargins(dpToPx(16), 0, dpToPx(24), 0);
				baseItemView.addView(iconView);
			}
		}

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);

		buildTitleView(title, llText);

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			AndroidUtils.setTextPrimaryColor(app, timeView, nightMode);

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildDestinationRow(final View view, String timeText, final Spannable title, Spannable secondaryText,
	                                LatLon location, LinearLayout imagesContainer, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(48, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		Drawable destinationIcon = app.getUIUtilities().getIcon(R.drawable.list_destination);
		ImageView iconView = new ImageView(view.getContext());
		iconView.setImageDrawable(destinationIcon);
		FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24));
		iconView.setLayoutParams(imageViewLayoutParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

		if (imagesContainer != null) {
			imagesContainer.addView(iconView);
		} else {
			imageViewLayoutParams.setMargins(dpToPx(16), 0, dpToPx(24), 0);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);

		buildDescriptionView(secondaryText, llText, 8, 0);
		buildTitleView(title, llText);

		if (location != null) {
			if (!TextUtils.isEmpty(destinationStreetStr)) {
				if (!title.toString().equals(destinationStreetStr)) {
					buildDescriptionView(new SpannableString(destinationStreetStr), llText, 4, 4);
				}
			} else {
				updateDestinationStreetName(location);
			}
		}

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			AndroidUtils.setTextPrimaryColor(app, timeView, nightMode);

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	private void buildIntermediateRow(final View view, Drawable icon, final Spannable title, OnClickListener onClickListener) {

		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(36, new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), view.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		// Icon
		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(22), dpToPx(22));
			imageViewLayoutParams.gravity = Gravity.CENTER_VERTICAL;
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			imageViewLayoutParams.setMargins(dpToPx(17), 0, dpToPx(25), 0);
			iconView.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
			iconView.setBackgroundResource(R.drawable.border_round_solid_light_small);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView();
		ll.addView(llText);

		buildTitleView(title, llText);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	private CollapsableView getCollapsableTransportStopRoutesView(final Context context, TransportStopRoute transportStopRoute, final List<TransportStop> stops) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, false);
		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
		Drawable icon = getContentIcon(drawableResId);
		for (int i = 0; i < stops.size(); i++) {
			final TransportStop stop = stops.get(i);
			buildIntermediateRow(view, icon, new SpannableString(stop.getName(preferredMapLang, transliterateNames)), new OnClickListener() {
				@Override
				public void onClick(View v) {
					showLocationOnMap(stop.getLocation());
				}
			});
		}
		return new CollapsableView(view, null, true);
	}

	protected LinearLayout buildCollapsableContentView(Context context, boolean collapsed) {
		final LinearLayout view = new LinearLayout(context);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		view.setLayoutParams(llParams);
		return view;
	}

	private void buildTitleView(Spannable title, LinearLayout container) {
		TextViewEx titleView = new TextViewEx(view.getContext());
		FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		titleParams.gravity = Gravity.CENTER_VERTICAL;
		titleView.setTypeface(FontCache.getRobotoRegular(view.getContext()));
		titleView.setLayoutParams(titleParams);
		titleView.setTextSize(16);
		AndroidUtils.setTextPrimaryColor(app, titleView, nightMode);

		titleView.setText(title);
		container.addView(titleView);
	}

	private void buildDescriptionView(Spannable description, LinearLayout container, int paddingTop, int paddingBottom) {
		TextViewEx textViewDescription = new TextViewEx(view.getContext());
		LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		descriptionParams.setMargins(0, dpToPx(paddingTop), 0, dpToPx(paddingBottom));
		textViewDescription.setLayoutParams(descriptionParams);
		textViewDescription.setTypeface(FontCache.getRobotoRegular(view.getContext()));
		textViewDescription.setTextSize(14);
		AndroidUtils.setTextSecondaryColor(app, textViewDescription, nightMode);
		textViewDescription.setText(description);
		container.addView(textViewDescription);
	}

	private LinearLayout buildTextContainerView() {
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		return llText;
	}

	private LinearLayout buildHorizontalContainerView(int minHeight, View.OnLongClickListener onLongClickListener) {
		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setMinimumHeight(dpToPx(minHeight));
		ll.setPadding(dpToPx(64), 0, dpToPx(16), 0);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(onLongClickListener);
		return ll;
	}

	public View createRouteBadge(MapActivity mapActivity, TransportStopRoute transportStopRoute, boolean nightMode) {
		LinearLayout convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		if (transportStopRoute != null) {
			String routeRef = transportStopRoute.route.getAdjustedRouteRef();
			int bgColor = transportStopRoute.getColor(app, nightMode);

			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			ImageView transportStopRouteImageView = (ImageView) convertView.findViewById(R.id.transport_stop_route_icon);

			int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
			transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, UiUtilities.getContrastColor(mapActivity, bgColor, true)));
			transportStopRouteTextView.setText(routeRef);
			GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(mapActivity, bgColor, true));
		}

		return convertView;
	}

	public String getRoutePointDescription(double lat, double lon) {
		return getString(R.string.route_descr_lat_lon, lat, lon);
	}

	public String getRoutePointDescription(LatLon l, String d) {
		if (d != null && d.length() > 0) {
			return d.replace(':', ' ');
		}
		if (l != null) {
			return getString(R.string.route_descr_lat_lon, l.getLatitude(), l.getLongitude());
		}
		return "";
	}

	private double getWalkTime(double walkDist, double walkSpeed) {
		return walkDist / walkSpeed;
	}

	public void buildRowDivider(View view, boolean needMargin) {
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (needMargin) {
			llHorLineParams.setMargins(dpToPx(64), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(app.getResources().getColor(!nightMode ? R.color.ctx_menu_bottom_view_divider_light : R.color.ctx_menu_bottom_view_divider_dark));
		((LinearLayout) view).addView(horizontalLine);
	}

	public int dpToPx(float dp) {
		return AndroidUtils.dpToPx(app, dp);
	}

	protected void copyToClipboard(String text, Context ctx) {
		((ClipboardManager) app.getSystemService(Activity.CLIPBOARD_SERVICE)).setText(text);
		Toast.makeText(ctx,
				ctx.getResources().getString(R.string.copied_to_clipboard) + ":\n" + text,
				Toast.LENGTH_SHORT).show();
	}

	private List<Incline> createInclinesAndAdd100MetersWith0Incline(List<Entry> entries) {
		int size = entries.size();
		List<Incline> inclines = new ArrayList<>();
		for (Entry entry : entries) {
			Incline incline = new Incline(entry.getY(), entry.getX() * 1000);
			inclines.add(incline);

		}
		for (int i = 0; i < 10; i++) {
			float distance = i * 5;
			inclines.add(i, new Incline(0f, distance));
		}
		float lastDistance = slopeDataSet.getEntryForIndex(size - 1).getX();
		for (int i = 1; i <= 10; i++) {
			float distance = lastDistance * 1000f + i * 5f;
			inclines.add(new Incline(0f, distance));
		}
		return inclines;
	}

	private void makeGpx() {
		gpx = GpxUiHelper.makeGpxFromRoute(routingHelper.getRoute(), app);
		String groupName = getString(R.string.current_route);
		GpxDisplayGroup group = app.getSelectedGpxHelper().buildGpxDisplayGroup(gpx, 0, groupName);
		if (group != null && group.getModifiableList().size() > 0) {
			gpxItem = group.getModifiableList().get(0);
			if (gpxItem != null) {
				gpxItem.route = true;
			}
		}
	}

	void openDetails() {
		if (gpxItem != null) {
			LatLon location = null;
			WptPt wpt = null;
			gpxItem.chartTypes = new GPXDataSetType[]{GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE};
			if (gpxItem.chartHighlightPos != -1) {
				TrkSegment segment = gpx.tracks.get(0).segments.get(0);
				if (segment != null) {
					float distance = gpxItem.chartHighlightPos * elevationDataSet.getDivX();
					for (WptPt p : segment.points) {
						if (p.distance >= distance) {
							wpt = p;
							break;
						}
					}
					if (wpt != null) {
						location = new LatLon(wpt.lat, wpt.lon);
					}
				}
			}

			if (location == null) {
				location = new LatLon(gpxItem.locationStart.lat, gpxItem.locationStart.lon);
			}
			if (wpt != null) {
				gpxItem.locationOnMap = wpt;
			} else {
				gpxItem.locationOnMap = gpxItem.locationStart;
			}

			final MapActivity activity = (MapActivity) getActivity();
			if (activity != null) {
				dismiss();

				final OsmandSettings settings = app.getSettings();
				settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
						false,
						gpxItem);

				final MapRouteInfoMenu mapRouteInfoMenu = activity.getMapRouteInfoMenu();
				if (mapRouteInfoMenu.isVisible()) {
					// We arrived here by the route info menu.
					// First, we close it and then show the details.
					mapRouteInfoMenu.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							mapRouteInfoMenu.setOnDismissListener(null);
							MapActivity.launchMapActivityMoveToTop(activity);
						}
					});
					mapRouteInfoMenu.hide();
				} else {
					// We arrived here by the dashboard.
					MapActivity.launchMapActivityMoveToTop(activity);
				}
			}
		}
	}

	private void buildMenuButtons() {
		UiUtilities iconsCache = app.getUIUtilities();
		boolean publicTransportInfo = isPublicTransportInfo();

		ImageButton printRoute = (ImageButton) view.findViewById(R.id.print_route);
		printRoute.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_gprint_dark));
		printRoute.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				print();
			}
		});
		if (publicTransportInfo) {
			printRoute.setVisibility(View.GONE);
		}

		ImageButton saveRoute = (ImageButton) view.findViewById(R.id.save_as_gpx);
		saveRoute.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_gsave_dark));
		saveRoute.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					MapActivityActions.createSaveDirections(activity, routingHelper).show();
				}
			}
		});
		if (publicTransportInfo) {
			saveRoute.setVisibility(View.GONE);
		}

		ImageButton shareRoute = (ImageButton) view.findViewById(R.id.share_as_gpx);
		shareRoute.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_gshare_dark));
		shareRoute.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final String trackName = new SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(new Date());
				final GPXFile gpx = routingHelper.generateGPXFileWithRoute(trackName);
				final Uri fileUri = AndroidUtils.getUriForFile(app, new File(gpx.path));
				File dir = new File(app.getCacheDir(), "share");
				if (!dir.exists()) {
					dir.mkdir();
				}
				File dst = new File(dir, "route.gpx");
				try {
					FileWriter fw = new FileWriter(dst);
					GPXUtilities.writeGpx(fw, gpx);
					fw.close();
					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(generateHtml(routingHelper.getRouteDirections(),
							routingHelper.getGeneralRouteInformation()).toString()));
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_route_subject));
					sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
					sendIntent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(app, dst));
					sendIntent.setType("text/plain");
					sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
					startActivity(sendIntent);
				} catch (IOException e) {
					// Toast.makeText(getActivity(), "Error sharing favorites: " + e.getMessage(),
					// Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		});
		if (publicTransportInfo) {
			shareRoute.setVisibility(View.GONE);
		}
	}

	public class CumulativeInfo {
		public int distance;
		public int time;

		public CumulativeInfo() {
			distance = 0;
			time = 0;
		}
	}

	public View getRouteDirectionView(int position, ViewGroup parent, RouteDirectionInfo model, List<RouteDirectionInfo> directionsInfo, OnClickListener onClickListener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}
		View row = mapActivity.getLayoutInflater().inflate(R.layout.route_info_list_item, parent, false);
		row.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));

		TextView label = (TextView) row.findViewById(R.id.description);
		TextView distanceLabel = (TextView) row.findViewById(R.id.distance);
		TextView timeLabel = (TextView) row.findViewById(R.id.time);
		TextView cumulativeDistanceLabel = (TextView) row.findViewById(R.id.cumulative_distance);
		TextView cumulativeTimeLabel = (TextView) row.findViewById(R.id.cumulative_time);
		ImageView icon = (ImageView) row.findViewById(R.id.direction);
		row.findViewById(R.id.divider).setVisibility(position == directionsInfo.size() - 1 ? View.INVISIBLE : View.VISIBLE);

		TurnPathHelper.RouteDrawable drawable = new TurnPathHelper.RouteDrawable(getResources(), true);
		drawable.setColorFilter(new PorterDuffColorFilter(!nightMode ? getResources().getColor(R.color.icon_color) : Color.WHITE, PorterDuff.Mode.SRC_ATOP));
		drawable.setRouteType(model.getTurnType());
		icon.setImageDrawable(drawable);

		label.setText(model.getDescriptionRoutePart());
		AndroidUtils.setTextPrimaryColor(app, label, nightMode);
		if (model.distance > 0) {
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, app));
			timeLabel.setText(getTimeDescription(model));
			row.setContentDescription(label.getText() + " " + timeLabel.getText());
		} else {
			if (Algorithms.isEmpty(label.getText().toString())) {
				label.setText(getString((position != directionsInfo.size() - 1) ? R.string.arrived_at_intermediate_point : R.string.arrived_at_destination));
			}
			distanceLabel.setText("");
			timeLabel.setText("");
			row.setContentDescription("");
		}
		CumulativeInfo cumulativeInfo = getRouteDirectionCumulativeInfo(position, directionsInfo);
		cumulativeDistanceLabel.setText(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance, app));
		cumulativeTimeLabel.setText(Algorithms.formatDuration(cumulativeInfo.time, app.accessibilityEnabled()));
		row.setOnClickListener(onClickListener);
		return row;
	}

	public CumulativeInfo getRouteDirectionCumulativeInfo(int position, List<RouteDirectionInfo> routeDirections) {
		CumulativeInfo cumulativeInfo = new CumulativeInfo();
		for (int i = position; i < routeDirections.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = routeDirections.get(i);
			cumulativeInfo.time += routeDirectionInfo.getExpectedTime();
			cumulativeInfo.distance += routeDirectionInfo.distance;
		}
		return cumulativeInfo;
	}

	private String getTimeDescription(RouteDirectionInfo model) {
		final int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}

	void print() {
		File file = generateRouteInfoHtml(routingHelper.getRouteDirections(), routingHelper.getGeneralRouteInformation());
		if (file.exists()) {
			Uri uri = AndroidUtils.getUriForFile(app, file);
			Intent browserIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // use Android Print Framework
				browserIntent = new Intent(getActivity(), PrintDialogActivity.class)
						.setDataAndType(uri, "text/html");
			} else { // just open html document
				browserIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(
						uri, "text/html");
			}
			browserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(browserIntent);
		}
	}

	private File generateRouteInfoHtml(List<RouteDirectionInfo> directionsInfo, String title) {
		File file = null;
		if (directionsInfo == null) {
			return file;
		}

		final String fileName = "route_info.html";
		StringBuilder html = generateHtmlPrint(directionsInfo, title);
		FileOutputStream fos = null;
		try {
			file = app.getAppPath(fileName);
			fos = new FileOutputStream(file);
			fos.write(html.toString().getBytes("UTF-8"));
			fos.flush();
		} catch (IOException e) {
			file = null;
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					file = null;
					e.printStackTrace();
				}
			}
		}

		return file;
	}

	private StringBuilder generateHtml(List<RouteDirectionInfo> directionInfos, String title) {
		StringBuilder html = new StringBuilder();
		if (!TextUtils.isEmpty(title)) {
			html.append("<h1>");
			html.append(title);
			html.append("</h1>");
		}
		final String NBSP = "&nbsp;";
		final String BR = "<br>";
		for (int i = 0; i < directionInfos.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = directionInfos.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(OsmAndFormatter.getFormattedDistance(routeDirectionInfo.distance, app));
			sb.append(", ").append(NBSP);
			sb.append(getTimeDescription(routeDirectionInfo));
			String distance = sb.toString().replaceAll("\\s", NBSP);
			String description = routeDirectionInfo.getDescriptionRoutePart();
			html.append(BR);
			html.append("<p>" + String.valueOf(i + 1) + ". " + NBSP + description + NBSP + "(" + distance + ")</p>");
		}
		return html;
	}

	private StringBuilder generateHtmlPrint(List<RouteDirectionInfo> directionsInfo, String title) {
		boolean accessibilityEnabled = app.accessibilityEnabled();
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
		html.append("<head>");
		html.append("<title>Route info</title>");
		html.append("<meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />");
		html.append("<style>");
		html.append("table, th, td {");
		html.append("border: 1px solid black;");
		html.append("border-collapse: collapse;}");
		html.append("th, td {");
		html.append("padding: 5px;}");
		html.append("</style>");
		html.append("</head>");
		html.append("<body>");

		if (!TextUtils.isEmpty(title)) {
			html.append("<h1>");
			html.append(title);
			html.append("</h1>");
		}
		html.append("<table style=\"width:100%\">");
		final String NBSP = "&nbsp;";
		final String BR = "<br>";
		for (int i = 0; i < directionsInfo.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = directionsInfo.get(i);
			html.append("<tr>");
			StringBuilder sb = new StringBuilder();
			sb.append(OsmAndFormatter.getFormattedDistance(routeDirectionInfo.distance, app));
			sb.append(", ");
			sb.append(getTimeDescription(routeDirectionInfo));
			String distance = sb.toString().replaceAll("\\s", NBSP);
			html.append("<td>");
			html.append(distance);
			html.append("</td>");
			String description = routeDirectionInfo.getDescriptionRoutePart();
			html.append("<td>");
			html.append(String.valueOf(i + 1)).append(". ").append(description);
			html.append("</td>");
			CumulativeInfo cumulativeInfo = getRouteDirectionCumulativeInfo(i, directionsInfo);
			html.append("<td>");
			sb = new StringBuilder();
			sb.append(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance, app));
			sb.append(" - ");
			sb.append(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance + routeDirectionInfo.distance, app));
			sb.append(BR);
			sb.append(Algorithms.formatDuration(cumulativeInfo.time, accessibilityEnabled));
			sb.append(" - ");
			sb.append(Algorithms.formatDuration(cumulativeInfo.time + routeDirectionInfo.getExpectedTime(), accessibilityEnabled));
			String cumulativeTimeAndDistance = sb.toString().replaceAll("\\s", NBSP);
			html.append(cumulativeTimeAndDistance);
			html.append("</td>");
			html.append("</tr>");
		}
		html.append("</table>");
		html.append("</body>");
		html.append("</html>");
		return html;
	}

	private int getInitialMenuState() {
		return FULL_SCREEN;
	}

	public boolean slideUp() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v << 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public boolean slideDown() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v >> 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public int getCurrentMenuState() {
		return currentMenuState;
	}

	public int getSupportedMenuStates() {
		if (!portrait) {
			return FULL_SCREEN;
		} else {
			return getSupportedMenuStatesPortrait();
		}
	}

	protected int getSupportedMenuStatesPortrait() {
		return HEADER_ONLY | HALF_SCREEN | FULL_SCREEN;
	}

	public static boolean showInstance(final MapActivity mapActivity) {
		return showInstance(mapActivity, -1);
	}

	public static boolean showInstance(final MapActivity mapActivity, int transportRouteId) {
		try {
			Bundle args = new Bundle();
			args.putInt(TRANSPORT_ROUTE_ID_KEY, transportRouteId);

			ShowRouteInfoDialogFragment fragment = new ShowRouteInfoDialogFragment();
			fragment.setArguments(args);
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@NonNull
	private MapActivity requireMapActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof MapActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (MapActivity) activity;
	}

	@Override
	public void onResume() {
		super.onResume();
		paused = false;
		ViewParent parent = view.getParent();
		if (parent != null && containerLayoutListener != null) {
			((View) parent).addOnLayoutChangeListener(containerLayoutListener);
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		paused = true;
		if (view != null) {
			ViewParent parent = view.getParent();
			if (parent != null && containerLayoutListener != null) {
				((View) parent).removeOnLayoutChangeListener(containerLayoutListener);
			}
		}
		MapActivity mapActivity = getMapActivity();
		if (!wasDrawerDisabled && mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public int getStatusBarColorId() {
		if (view != null) {
			if (Build.VERSION.SDK_INT >= 23) {
				view.setSystemUiVisibility(view.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
			}
			return nightMode ? R.color.dialog_divider_dark : R.color.dialog_divider_light;
		}
		return -1;
	}

	private int getViewY() {
		return (int) mainView.getY();
	}

	private void setViewY(int y, boolean animated, boolean adjustMapPos) {
		mainView.setY(y);
		zoomButtonsView.setY(getZoomButtonsY(y));
	}

	private void updateZoomButtonsVisibility(int menuState) {
		if (menuState == MenuController.MenuState.HEADER_ONLY) {
			if (zoomButtonsView.getVisibility() != View.VISIBLE) {
				zoomButtonsView.setVisibility(View.VISIBLE);
			}
		} else {
			if (zoomButtonsView.getVisibility() == View.VISIBLE) {
				zoomButtonsView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void processScreenHeight(ViewParent parent) {
		View container = (View) parent;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			screenHeight = container.getHeight() + AndroidUtils.getStatusBarHeight(mapActivity);
			viewHeight = screenHeight - AndroidUtils.getStatusBarHeight(mapActivity);
		}
	}

	private int getFullScreenTopPosY() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return topShadowMargin + mapActivity.getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);
		} else {
			return 0;
		}
	}

	private int addStatusBarHeightIfNeeded(int res) {
		MapActivity mapActivity = getMapActivity();
		if (Build.VERSION.SDK_INT >= 21 && mapActivity != null) {
			return res + AndroidUtils.getStatusBarHeight(mapActivity);
		}
		return res;
	}

	private int getHeaderOnlyTopY() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (!menuCards.isEmpty()) {
				return viewHeight - menuCards.get(0).getViewHeight() - shadowHeight;
			} else {
				return viewHeight - dpToPx(48f) - shadowHeight;
			}
		} else {
			return 0;
		}
	}

	private int getMenuStatePosY(int menuState) {
		if (!portrait) {
			return topScreenPosY;
		}
		switch (menuState) {
			case HEADER_ONLY:
				return getHeaderOnlyTopY();
			case HALF_SCREEN:
				return minHalfY;
			case FULL_SCREEN:
				return getFullScreenTopPosY();
			default:
				return 0;
		}
	}

	public void openMenuFullScreen() {
		changeMenuState(getMenuStatePosY(FULL_SCREEN), false, false);
	}

	public void openMenuHeaderOnly() {
		if (portrait) {
			changeMenuState(getMenuStatePosY(HEADER_ONLY), false, false);
		}
	}

	public void openMenuHalfScreen() {
		if (portrait) {
			changeMenuState(getMenuStatePosY(HALF_SCREEN), false, false);
		}
	}

	private void changeMenuState(int currentY, boolean slidingUp, boolean slidingDown) {
		boolean needCloseMenu = false;

		int currentMenuState = getCurrentMenuState();
		if (portrait) {
			int headerDist = Math.abs(currentY - getMenuStatePosY(HEADER_ONLY));
			int halfDist = Math.abs(currentY - getMenuStatePosY(HALF_SCREEN));
			int fullDist = Math.abs(currentY - getMenuStatePosY(FULL_SCREEN));
			int newState;
			if (headerDist < halfDist && headerDist < fullDist) {
				newState = HEADER_ONLY;
			} else if (halfDist < headerDist && halfDist < fullDist) {
				newState = HALF_SCREEN;
			} else {
				newState = FULL_SCREEN;
			}

			if (slidingDown && currentMenuState == FULL_SCREEN && getViewY() < getFullScreenTopPosY()) {
				slidingDown = false;
				newState = FULL_SCREEN;
			}
			if (menuBottomViewHeight > 0 && slidingUp) {
				while (getCurrentMenuState() != newState) {
					if (!slideUp()) {
						break;
					}
				}
			} else if (slidingDown) {
				if (currentMenuState == HEADER_ONLY) {
					needCloseMenu = true;
				} else {
					while (getCurrentMenuState() != newState) {
						if (!slideDown()) {
							needCloseMenu = true;
							break;
						}
					}
				}
			} else {
				if (currentMenuState < newState) {
					while (getCurrentMenuState() != newState) {
						if (!slideUp()) {
							break;
						}
					}
				} else {
					while (getCurrentMenuState() != newState) {
						if (!slideDown()) {
							break;
						}
					}
				}
			}
		}
		int newMenuState = getCurrentMenuState();
		boolean needMapAdjust = currentMenuState != newMenuState && newMenuState != FULL_SCREEN;

		applyPosY(currentY, needCloseMenu, needMapAdjust, currentMenuState, newMenuState, 0);
	}


	private int getPosY(final int currentY, boolean needCloseMenu, int previousState) {
		if (needCloseMenu) {
			return screenHeight;
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return 0;
		}

		int destinationState = getCurrentMenuState();
		updateZoomButtonsVisibility(destinationState);

		int posY = 0;
		switch (destinationState) {
			case HEADER_ONLY:
				posY = getMenuStatePosY(HEADER_ONLY);
				break;
			case HALF_SCREEN:
				posY = getMenuStatePosY(HALF_SCREEN);
				break;
			case FULL_SCREEN:
				if (currentY != CURRENT_Y_UNDEFINED) {
					int maxPosY = viewHeight - menuFullHeightMax;
					int minPosY = getMenuStatePosY(FULL_SCREEN);
					if (maxPosY > minPosY) {
						maxPosY = minPosY;
					}
					if (currentY > minPosY || previousState != FULL_SCREEN) {
						posY = minPosY;
					} else if (currentY < maxPosY) {
						posY = maxPosY;
					} else {
						posY = currentY;
					}
				} else {
					posY = getMenuStatePosY(FULL_SCREEN);
				}
				break;
			default:
				break;
		}

		return posY;
	}

	private void updateMainViewLayout(int posY) {
		MapActivity mapActivity = getMapActivity();
		if (view != null && mapActivity != null) {
			ViewGroup.LayoutParams lp = mainView.getLayoutParams();
			lp.height = view.getHeight() - posY;
			mainView.setLayoutParams(lp);
			mainView.requestLayout();
		}
	}

	private void applyPosY(final int currentY, final boolean needCloseMenu, boolean needMapAdjust,
	                       final int previousMenuState, final int newMenuState, int dZoom) {
		final int posY = getPosY(currentY, needCloseMenu, previousMenuState);
		if (getViewY() != posY || dZoom != 0) {
			if (posY < getViewY()) {
				updateMainViewLayout(posY);
			}

			mainView.animate().y(posY)
					.setDuration(200)
					.setInterpolator(new DecelerateInterpolator())
					.setListener(new AnimatorListenerAdapter() {

						boolean canceled = false;

						@Override
						public void onAnimationCancel(Animator animation) {
							canceled = true;
						}

						@Override
						public void onAnimationEnd(Animator animation) {
							if (!canceled) {
								if (needCloseMenu) {
									dismiss();
								} else {
									updateMainViewLayout(posY);
									if (previousMenuState != 0 && newMenuState != 0 && previousMenuState != newMenuState) {
										doAfterMenuStateChange(previousMenuState, newMenuState);
									}
								}
							}
						}
					}).start();

			zoomButtonsView.animate().y(getZoomButtonsY(posY))
					.setDuration(200)
					.setInterpolator(new DecelerateInterpolator())
					.start();
		}
	}

	private int getZoomButtonsY(int y) {
		return y - zoomButtonsHeight - shadowHeight;
	}

	private void doAfterMenuStateChange(int previousState, int newState) {
		runLayoutListener();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void runLayoutListener() {
		if (view != null) {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (view != null) {
						ViewTreeObserver obs = view.getViewTreeObserver();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							obs.removeOnGlobalLayoutListener(this);
						} else {
							obs.removeGlobalOnLayoutListener(this);
						}

						if (getActivity() == null) {
							return;
						}
						zoomButtonsHeight = zoomButtonsView.getHeight();

						menuFullHeight = mainView.getHeight();
						menuBottomViewHeight = menuFullHeight;

						menuFullHeightMax = view.findViewById(R.id.route_menu_cards_container).getHeight();

						if (!moving) {
							doLayoutMenu();
						}
						initLayout = false;
					}
				}
			});
		}
	}

	private void doLayoutMenu() {
		final int posY = getPosY(getViewY(), false, getCurrentMenuState());
		setViewY(posY, true, !initLayout);
		updateMainViewLayout(posY);
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG,
						FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				//
			}
		}
	}
}
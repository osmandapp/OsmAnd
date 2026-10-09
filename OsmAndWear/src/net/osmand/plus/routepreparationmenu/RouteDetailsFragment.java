package net.osmand.plus.routepreparationmenu;

import static android.graphics.Typeface.DEFAULT;
import static net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.ChartPointLayer.ROUTE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.mapcontextmenu.CollapsableView;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.graph.BaseChartAdapter;
import net.osmand.plus.measurementtool.graph.ChartAdapterHelper;
import net.osmand.plus.measurementtool.graph.ChartAdapterHelper.RefreshMapCallback;
import net.osmand.plus.measurementtool.graph.CommonChartAdapter;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routepreparationmenu.cards.AttachTrackToRoadsBannerCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard.PublicTransportCardListener;
import net.osmand.plus.routepreparationmenu.cards.RouteDirectionsCard;
import net.osmand.plus.routepreparationmenu.cards.RouteInfoCard;
import net.osmand.plus.routepreparationmenu.cards.RouteStatisticCard;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRouteResult;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RouteDetailsFragment extends ContextMenuFragment
		implements PublicTransportCardListener, CardListener {

	public static final String ROUTE_ID_KEY = "route_id_key";
	private static final float PAGE_MARGIN = 5f;

	private int routeId = -1;
	private String destinationStreetStr = "";
	private int pageMarginPx;
	private int toolbarHeightPx;

	private GpxFile gpxFile;
	@Nullable
	private OrderedLineDataSet elevationDataSet;
	private GpxDisplayItem gpxItem;
	private final List<BaseCard> menuCards = new ArrayList<>();
	@Nullable
	private PublicTransportCard transportCard;
	private RouteDetailsFragmentListener routeDetailsListener;
	private final List<RouteInfoCard> routeInfoCards = new ArrayList<>();
	private RouteDetailsMenu routeDetailsMenu;
	private RefreshMapCallback refreshMapCallback;

	public interface RouteDetailsFragmentListener {
		void onNavigationRequested();
	}

	private class RouteDetailsMenu extends TrackDetailsMenu {

		@Override
		protected int getFragmentWidth() {
			return getWidth();
		}

		@Override
		protected int getFragmentHeight() {
			return getMenuFullHeight();
		}
	}

	@Override
	public int getMainLayoutId() {
		return R.layout.route_info_layout;
	}

	@Override
	public int getHeaderViewHeight() {
		return !menuCards.isEmpty() ? menuCards.get(0).getTopViewHeight() : 0;
	}

	@Override
	public boolean isHeaderViewDetached() {
		return false;
	}

	@Override
	public int getToolbarHeight() {
		return toolbarHeightPx;
	}

	@Override
	public boolean isSingleFragment() {
		return false;
	}

	public int getRouteId() {
		return routeId;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args != null) {
			routeId = args.getInt(ROUTE_ID_KEY);
		}
		pageMarginPx = dpToPx(PAGE_MARGIN);
		toolbarHeightPx = getResources().getDimensionPixelSize(R.dimen.dashboard_map_toolbar);

		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			if (isPortrait()) {
				view.findViewById(getBottomScrollViewId()).setBackground(null);
				LinearLayout cardsContainer = getCardsContainer();
				FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) cardsContainer.getLayoutParams();
				AndroidUtils.setMargins(layoutParams, pageMarginPx, 0, pageMarginPx, 0);
				cardsContainer.setLayoutParams(layoutParams);
				updateCardsLayout();
			}
			updateCards();
			runLayoutListener();
		}
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && isPortrait()) {
			mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(View.GONE);
			if (routeDetailsMenu != null) {
				routeDetailsMenu.setMapActivity(mapActivity);
			}
		}
	}

	@Override
	public void onPause() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && isPortrait()) {
			mapActivity.findViewById(R.id.bottom_controls_container).setVisibility(View.VISIBLE);
			if (routeDetailsMenu != null) {
				routeDetailsMenu.setMapActivity(null);
				mapActivity.getMapLayers().getMapInfoLayer().setTrackChartPoints(null);
			}
		}
		super.onPause();
	}

	@Override
	public int getShadowHeight() {
		int res = super.getShadowHeight();
		if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
			res += pageMarginPx;
		}
		return res;
	}

	@Nullable
	public PublicTransportCard getTransportCard() {
		return transportCard;
	}


	@Override
	protected void updateMainViewLayout(int posY) {
		super.updateMainViewLayout(posY);
		if (isPortrait()) {
			updateCardsLayout();
		}
	}

	public RouteDetailsFragmentListener getRouteDetailsListener() {
		return routeDetailsListener;
	}

	public void setRouteDetailsListener(RouteDetailsFragmentListener routeDetailsListener) {
		this.routeDetailsListener = routeDetailsListener;
	}

	private void updateCardsLayout() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = getCardsContainer();
			View topShadow = getTopShadow();
			FrameLayout bottomContainer = getBottomContainer();
			if (bottomContainer == null) {
				return;
			}
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackground(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				int cardsAndListBgColorId = ColorUtilities.getCardAndListBackgroundColorId(isNightMode());
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, cardsAndListBgColorId);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, cardsAndListBgColorId);
			}
		}
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			RoutingHelper routingHelper = app.getRoutingHelper();
			LinearLayout cardsContainer = getCardsContainer();
			cardsContainer.removeAllViews();
			if (routeId != -1) {
				List<TransportRouteResult> routes = routingHelper.getTransportRoutingHelper().getRoutes();
				if (routes != null && routes.size() > routeId) {
					TransportRouteResult routeResult = routingHelper.getTransportRoutingHelper().getRoutes().get(routeId);
					TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
					PublicTransportCard card = new PublicTransportCard(mapActivity, transportRoutingHelper.getStartLocation(),
							transportRoutingHelper.getEndLocation(), routeResult, routeId);
					card.setShowTopShadow(false);
					card.setShowBottomShadow(false);
					card.setShowDivider(false);
					card.setTransparentBackground(true);
					card.setTransportCardListener(this);
					card.setListener(this);
					transportCard = card;
					menuCards.add(card);
					cardsContainer.addView(card.build(mapActivity));
					buildRowDivider(cardsContainer, false);
					buildTransportRouteRow(cardsContainer, routeResult, true);
					buildRowDivider(cardsContainer, false);
				} else {
					transportCard = null;
					makeGpx();
					createRouteStatisticCards(cardsContainer);
					createRouteDirectionsCard(cardsContainer);
				}
			}
		}
	}

	private void createRouteStatisticCards(LinearLayout cardsContainer) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		RouteStatisticCard statisticCard = new RouteStatisticCard(mapActivity, gpxFile, v -> openDetails());
		statisticCard.setTransparentBackground(true);
		statisticCard.setListener(this);
		menuCards.add(statisticCard);
		cardsContainer.addView(statisticCard.build(mapActivity));
		buildRowDivider(cardsContainer, false);
		elevationDataSet = statisticCard.getElevationDataSet();

		List<RouteSegmentResult> route = app.getRoutingHelper().getRoute().getOriginalRoute();
		if (route != null) {
			List<RouteStatistics> routeStatistics = calculateRouteStatistics(app, route, isNightMode());
			GpxTrackAnalysis analysis = gpxFile.getAnalysis(0);

			for (RouteStatistics statistic : routeStatistics) {
				RouteInfoCard routeClassCard = new RouteInfoCard(mapActivity, statistic, analysis);
				addRouteCard(cardsContainer, routeClassCard);
			}
		}
		routeDetailsMenu = new RouteDetailsMenu();
		GpxDisplayItem gpxItem = statisticCard.getGpxItem();
		if (gpxItem != null) {
			routeDetailsMenu.setGpxItem(gpxItem);
		}
		routeDetailsMenu.setMapActivity(mapActivity);

		CommonChartAdapter mainGraphAdapter = statisticCard.getGraphAdapter();
		if (mainGraphAdapter != null) {
			ChartAdapterHelper.bindGraphAdapters(mainGraphAdapter, getRouteInfoCardsGraphAdapters(), getMainView());
			refreshMapCallback = ChartAdapterHelper.bindToMap(mainGraphAdapter, mapActivity, routeDetailsMenu);
		}
	}

	private List<BaseChartAdapter> getRouteInfoCardsGraphAdapters() {
		List<BaseChartAdapter> adapters = new ArrayList<>();
		for (RouteInfoCard card : routeInfoCards) {
			BaseChartAdapter adapter = card.getGraphAdapter();
			if (adapter != null) {
				adapters.add(adapter);
			}
		}
		return adapters;
	}

	public static List<RouteStatistics> calculateRouteStatistics(OsmandApplication app,
	                                                             List<RouteSegmentResult> route,
	                                                             boolean nightMode) {
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRender = app.getRendererRegistry().defaultRender();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingRuleSearchRequest currentSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(currentRenderer, nightMode);
		RenderingRuleSearchRequest defaultSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(defaultRender, nightMode);
		return RouteStatisticsHelper.calculateRouteStatistic(route, currentRenderer,
				defaultRender, currentSearchRequest, defaultSearchRequest);
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		super.calculateLayout(view, initLayout);
		if (!initLayout && getCurrentMenuState() != MenuState.FULL_SCREEN) {
			if (refreshMapCallback != null) {
				refreshMapCallback.refreshMap(false, false, true);
			}
		}
	}

	private void createRouteDirectionsCard(LinearLayout cardsContainer) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		RouteDirectionsCard directionsCard = new RouteDirectionsCard(mapActivity);
		directionsCard.setTransparentBackground(true);
		directionsCard.setListener(this);
		menuCards.add(directionsCard);
		cardsContainer.addView(directionsCard.build(mapActivity));
		buildRowDivider(cardsContainer, false);
	}

	@SuppressLint("ClickableViewAccessibility")
	private void addRouteCard(LinearLayout cardsContainer,
	                          RouteInfoCard routeInfoCard) {
		menuCards.add(routeInfoCard);
		routeInfoCard.setListener(this);
		cardsContainer.addView(routeInfoCard.build(app));
		buildRowDivider(cardsContainer, false);
		routeInfoCards.add(routeInfoCard);
	}

	public Drawable getCollapseIcon(boolean collapsed) {
		return app.getUIUtilities().getIcon(collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up, R.color.icon_color_default_light);
	}

	private void buildSegmentItem(View view, TransportRouteResultSegment segment,
								  TransportRouteResultSegment nextSegment, int[] startTime, double walkSpeed, double boardingTime) {
		TransportRoute transportRoute = segment.route;
		List<TransportStop> stops = segment.getTravelStops();
		TransportStop startStop = stops.get(0);
		TransportStopRoute transportStopRoute = TransportStopRoute.getTransportStopRoute(transportRoute, startStop);

		FrameLayout baseContainer = new FrameLayout(view.getContext());

		ImageView routeLine = new ImageView(view.getContext());
		FrameLayout.LayoutParams routeLineParams = new FrameLayout.LayoutParams(dpToPx(8f), ViewGroup.LayoutParams.MATCH_PARENT);
		routeLineParams.gravity = Gravity.START;
		AndroidUtils.setMargins(routeLineParams, dpToPx(24), dpToPx(14), dpToPx(22), dpToPx(36));
		routeLine.setLayoutParams(routeLineParams);
		int bgColor = transportStopRoute.getColor(app, isNightMode());
		routeLine.setBackgroundColor(bgColor);
		baseContainer.addView(routeLine);

		LinearLayout stopsContainer = new LinearLayout(view.getContext());
		stopsContainer.setOrientation(LinearLayout.VERTICAL);
		baseContainer.addView(stopsContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
		Drawable icon = getContentIcon(drawableResId);

		Typeface typeface = FontCache.getMediumFont();
		startTime[0] += (int) boardingTime;
		String timeText = OsmAndFormatter.getFormattedDurationShortMinutes(startTime[0]);

		SpannableString secondaryText = new SpannableString(getString(R.string.sit_on_the_stop));
		secondaryText.setSpan(new ForegroundColorSpan(getMainFontColor()), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		SpannableString title = new SpannableString(startStop.getName(getPreferredMapLang(), isTransliterateNames()));
		title.setSpan(new CustomTypefaceSpan(typeface), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.setSpan(new ForegroundColorSpan(getActiveColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildStartStopRow(stopsContainer, icon, timeText, title, secondaryText, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(startStop.getLocation());
			}
		});

		buildTransportStopRouteRow(stopsContainer, transportStopRoute, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showRouteSegmentOnMap(segment);
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
			spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
				spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, startIndex + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			startIndex = spannable.length();
			spannable.append(String.valueOf(stops.size() - 1)).append(" ").append(getString(R.string.transport_stops));
			spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (spannable.length() > 0) {
			startIndex = spannable.length();
			spannable.append(" • ");
			spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, startIndex + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		spannable.append(OsmAndFormatter.getFormattedDistance((float) segment.getTravelDist(), app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS));
		spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		String type = getString(transportStopRoute.getTypeStrRes()).toLowerCase();
		SpannableString textType = new SpannableString(getString(R.string.by_transport_type, type));
		buildCollapsableRow(stopsContainer, spannable, textType, true, collapsableView, null);

		TransportStop endStop = stops.get(stops.size() - 1);
		int depTime = segment.depTime + arrivalTime;
		if (depTime <= 0) {
			depTime = startTime[0] + arrivalTime;
		}
		// TODO: fix later for schedule
		startTime[0] += (int) segment.getTravelTime();
		String textTime = OsmAndFormatter.getFormattedDurationShortMinutes(startTime[0]);

		secondaryText = new SpannableString(getString(R.string.exit_at));
		secondaryText.setSpan(new CustomTypefaceSpan(typeface), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int spaceIndex = secondaryText.toString().indexOf(" ");
		if (spaceIndex != -1) {
			secondaryText.setSpan(new ForegroundColorSpan(getMainFontColor()), 0, spaceIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		title = new SpannableString(endStop.getName(getPreferredMapLang(), isTransliterateNames()));
		title.setSpan(new CustomTypefaceSpan(typeface), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.setSpan(new ForegroundColorSpan(getActiveColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildEndStopRow(stopsContainer, icon, textTime, title, secondaryText, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(endStop.getLocation());
			}
		});

		((ViewGroup) view).addView(baseContainer);

		if (nextSegment != null) {
			double walkDist = (long) getWalkDistance(segment, nextSegment, segment.walkDist);
			if (walkDist > 0) {
				int walkTime = (int) getWalkTime(segment, nextSegment, walkDist, walkSpeed);
				if (walkTime < 60) {
					walkTime = 60;
				}
				spannable = new SpannableStringBuilder("~");
				spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				startIndex = spannable.length();
				spannable.append(OsmAndFormatter.getFormattedDuration(walkTime, app)).append(" ");
				spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				startIndex = spannable.length();
				spannable.append(getString(R.string.shared_string_walk)).append(", ").append(OsmAndFormatter.getFormattedDistance((float) walkDist, app,
						OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS));
				spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				buildRowDivider(view, true);
				buildWalkRow(view, spannable, null, new OnClickListener() {
					@Override
					public void onClick(View v) {
						showWalkingRouteOnMap(segment, nextSegment);
					}
				});
				startTime[0] += walkTime;
			}
		}
	}

	private View createImagesContainer(@NonNull Context context) {
		LinearLayout imagesContainer = new LinearLayout(context);
		FrameLayout.LayoutParams imagesContainerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		AndroidUtils.setPadding(imagesContainer, dpToPx(16), dpToPx(12), dpToPx(24), 0);
		imagesContainer.setOrientation(LinearLayout.VERTICAL);
		imagesContainer.setLayoutParams(imagesContainerParams);
		return imagesContainer;
	}

	private View createInfoContainer(@NonNull Context context) {
		LinearLayout infoContainer = new LinearLayout(context);
		FrameLayout.LayoutParams infoContainerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		infoContainer.setOrientation(LinearLayout.VERTICAL);
		infoContainer.setLayoutParams(infoContainerParams);
		return infoContainer;
	}

	private void buildTransportRouteRow(@NonNull ViewGroup parent, TransportRouteResult routeResult, boolean showDivider) {
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TargetPoint startPoint = targetPointsHelper.getPointToStart();
		TargetPoint endPoint = targetPointsHelper.getPointToNavigate();
		int[] startTime = {0};
		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		for (int i = 0; i < segments.size(); i++) {
			boolean first = i == 0;
			boolean last = i == segments.size() - 1;
			TransportRouteResultSegment segment = segments.get(i);
			if (first) {
				buildStartItem(parent, startPoint, startTime, segment, routeResult.getWalkSpeed());
			}
			buildSegmentItem(parent, segment, !last ? segments.get(i + 1) : null, startTime, routeResult.getWalkSpeed(), routeResult.getBoardingTime());
			if (last) {
				buildDestinationItem(parent, endPoint, startTime, segment, routeResult.getWalkSpeed());
			}
			if (showDivider && !last) {
				buildRowDivider(parent, true);
			}
		}
	}

	private void buildStartItem(@NonNull View view, TargetPoint start, int[] startTime,
								TransportRouteResultSegment segment, double walkSpeed) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());

		LinearLayout imagesContainer = (LinearLayout) createImagesContainer(view.getContext());
		baseItemView.addView(imagesContainer);

		LinearLayout infoContainer = (LinearLayout) createInfoContainer(view.getContext());
		baseItemView.addView(infoContainer);

		String name;
		if (start != null) {
			name = start.getOnlyName().length() > 0 ? start.getOnlyName() :
					(getString(R.string.route_descr_map_location) + " " + getRoutePointDescription(start.getLatitude(), start.getLongitude()));
		} else {
			name = getString(R.string.shared_string_my_location);
		}
		Spannable startTitle = new SpannableString(name);
		String text = OsmAndFormatter.getFormattedDurationShortMinutes(startTime[0]);

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

		double walkDist = (long) getWalkDistance(null, segment, segment.walkDist);
		int walkTime = (int) getWalkTime(null, segment, walkDist, walkSpeed);
		if (walkTime < 60) {
			walkTime = 60;
		}
		startTime[0] += walkTime;
		SpannableStringBuilder title = new SpannableStringBuilder(Algorithms.capitalizeFirstLetter(getString(R.string.shared_string_walk)));
		title.setSpan(new ForegroundColorSpan(getSecondaryColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int startIndex = title.length();
		title.append(" ").append(OsmAndFormatter.getFormattedDuration(walkTime, app));
		title.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = title.length();
		title.append(", ").append(OsmAndFormatter.getFormattedDistance((float) walkDist, app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS));
		title.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildWalkRow(infoContainer, title, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showWalkingRouteOnMap(null, segment);
			}
		});
		buildRowDivider(infoContainer, true);

		((ViewGroup) view).addView(baseItemView);
	}

	public void showRouteOnMap() {
		if (transportCard == null) {
			RouteCalculationResult route = app.getRoutingHelper().getRoute();
			showRouteOnMap(route);
		} else {
			TransportRouteResult route = app.getTransportRoutingHelper().getCurrentRouteResult();
			if (route != null) {
				showRouteOnMap(route);
			}
		}
	}

	public void showRouteOnMap(@NonNull RouteCalculationResult result) {
		QuadRect rect = result.getLocationsRect();
		if (rect != null) {
			openMenuHeaderOnly();
			fitRectOnMap(rect);
		}
	}

	public void showWalkingRouteOnMap(@Nullable TransportRouteResultSegment startSegment, @Nullable TransportRouteResultSegment endSegment) {
		RouteCalculationResult walkingRouteSegment = app.getTransportRoutingHelper().getWalkingRouteSegment(startSegment, endSegment);
		if (walkingRouteSegment != null) {
			showRouteOnMap(walkingRouteSegment);
		}
	}

	public void showRouteSegmentOnMap(@NonNull TransportRouteResultSegment segment) {
		QuadRect rect = segment.getSegmentRect();
		if (rect != null) {
			openMenuHeaderOnly();
			fitRectOnMap(rect);
		}
	}

	public void showRouteOnMap(@NonNull TransportRouteResult result) {
		QuadRect rect = app.getTransportRoutingHelper().getTransportRouteRect(result);
		if (rect != null) {
			openMenuHeaderOnly();
			fitRectOnMap(rect);
		}
	}

	private void addWalkRouteIcon(LinearLayout container) {
		ImageView walkLineImage = new ImageView(container.getContext());
		walkLineImage.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.walk_route_item_light));
		LinearLayout.LayoutParams walkImageLayoutParams = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(14));
		AndroidUtils.setMargins(walkImageLayoutParams, dpToPx(7), dpToPx(6), 0, dpToPx(6));
		walkLineImage.setLayoutParams(walkImageLayoutParams);
		container.addView(walkLineImage);
	}

	private void buildDestinationItem(@NonNull View view, TargetPoint destination, int[] startTime,
									  TransportRouteResultSegment segment, double walkSpeed) {
		Typeface typeface = FontCache.getMediumFont();
		FrameLayout baseItemView = new FrameLayout(view.getContext());

		LinearLayout imagesContainer = (LinearLayout) createImagesContainer(view.getContext());
		baseItemView.addView(imagesContainer);

		LinearLayout infoContainer = (LinearLayout) createInfoContainer(view.getContext());
		baseItemView.addView(infoContainer);

		buildRowDivider(infoContainer, true);

		double walkDist = (long) getWalkDistance(segment, null, segment.walkDist);
		int walkTime = (int) getWalkTime(segment, null, walkDist, walkSpeed);
		if (walkTime < 60) {
			walkTime = 60;
		}
		SpannableStringBuilder spannable = new SpannableStringBuilder(getString(R.string.shared_string_walk)).append(" ");
		spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		int startIndex = spannable.length();
		spannable.append("~").append(OsmAndFormatter.getFormattedDuration(walkTime, app));
		spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = spannable.length();
		spannable.append(", ").append(OsmAndFormatter.getFormattedDistance((float) walkDist, app, OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS));
		spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildWalkRow(infoContainer, spannable, imagesContainer, v -> showWalkingRouteOnMap(segment, null));
		buildRowDivider(infoContainer, true);
		addWalkRouteIcon(imagesContainer);

		String timeStr = OsmAndFormatter.getFormattedDurationShortMinutes(startTime[0] + walkTime);
		String name = getRoutePointDescription(destination.point, destination.getOnlyName());
		SpannableString title = new SpannableString(name);
		title.setSpan(new CustomTypefaceSpan(typeface), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		title.setSpan(new ForegroundColorSpan(getActiveColor()), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		SpannableString secondaryText = new SpannableString(getString(R.string.route_descr_destination));
		secondaryText.setSpan(new CustomTypefaceSpan(typeface), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		secondaryText.setSpan(new ForegroundColorSpan(getMainFontColor()), 0, secondaryText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildDestinationRow(infoContainer, timeStr, title, secondaryText, destination.point, imagesContainer, v -> showLocationOnMap(destination.point));

		((ViewGroup) view).addView(baseItemView);
	}

	@ColorInt
	private int getActiveColor() {
		return ContextCompat.getColor(app, ColorUtilities.getActiveColorId(isNightMode()));
	}

	@ColorInt
	protected int getMainFontColor() {
		return ColorUtilities.getPrimaryTextColor(app, isNightMode());
	}

	@ColorInt
	protected int getSecondaryColor() {
		return ContextCompat.getColor(app, R.color.icon_color_default_light);
	}

	public void buildCollapsableRow(@NonNull View view, Spannable title, Spannable secondaryText, boolean collapsable,
									CollapsableView collapsableView, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
		});
		baseView.addView(ll);

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		if (!TextUtils.isEmpty(secondaryText)) {
			buildDescriptionView(secondaryText, llText, 8, 0);
		}

		buildTitleView(title, llText);

		ImageView iconViewCollapse = new ImageView(view.getContext());
		if (collapsable && collapsableView != null) {
			// Icon
			LinearLayout llIconCollapse = new LinearLayout(view.getContext());
			llIconCollapse.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(48f)));
			llIconCollapse.setOrientation(LinearLayout.HORIZONTAL);
			llIconCollapse.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
			ll.addView(llIconCollapse);

			LinearLayout.LayoutParams llIconCollapseParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
			AndroidUtils.setMargins(llIconCollapseParams,0, dpToPx(12f), 0, dpToPx(12f));
			llIconCollapseParams.gravity = Gravity.CENTER_VERTICAL|Gravity.START;
			iconViewCollapse.setLayoutParams(llIconCollapseParams);
			iconViewCollapse.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			iconViewCollapse.setImageDrawable(getCollapseIcon(collapsableView.getContentView().getVisibility() == View.GONE));
			llIconCollapse.addView(iconViewCollapse);
			ll.setOnClickListener(v -> {
				LinearLayout contentView = (LinearLayout) collapsableView.getContentView();
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
			});
			if (collapsableView.isCollapsed()) {
				collapsableView.getContentView().setVisibility(View.GONE);
				iconViewCollapse.setImageDrawable(getCollapseIcon(true));
			}
			baseView.addView(collapsableView.getContentView());
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildStartStopRow(@NonNull View view, Drawable icon, String timeText, Spannable title,
								  Spannable secondaryText, OnClickListener onClickListener) {
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

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
		});
		baseView.addView(ll);

		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(28), dpToPx(28));
			imageViewLayoutParams.gravity = Gravity.TOP|Gravity.START;
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			AndroidUtils.setMargins(imageViewLayoutParams, dpToPx(14), dpToPx(8), dpToPx(22), 0);
			iconView.setBackgroundResource(R.drawable.border_round_solid_light);
			AndroidUtils.setPadding(iconView, dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		if (!TextUtils.isEmpty(secondaryText)) {
			buildDescriptionView(secondaryText, llText, 8, 0);
		}

		buildTitleView(title, llText);

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			AndroidUtils.setMargins(timeViewParams,0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			AndroidUtils.setPadding(timeView,0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			timeView.setTextColor(getMainFontColor());

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildTransportStopRouteRow(@NonNull View view, @NonNull TransportStopRoute transportStopRoute,
										   OnClickListener onClickListener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();

		String routeDescription = transportStopRoute.getDescription(app);
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 36, v -> {
			copyToClipboard(routeDescription, v.getContext());
			return true;
		});
		baseView.addView(ll);

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		View routeBadge = createRouteBadge(mapActivity, transportStopRoute);
		LinearLayout.LayoutParams routeBadgeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(routeBadgeParams, 0, dpToPx(6), 0, dpToPx(8));
		routeBadge.setLayoutParams(routeBadgeParams);
		llText.addView(routeBadge);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildEndStopRow(@NonNull View view, Drawable icon, String timeText, Spannable title, Spannable secondaryText, OnClickListener onClickListener) {
		
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
		});
		baseView.addView(ll);

		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(28), dpToPx(28));
			imageViewLayoutParams.gravity = Gravity.TOP|Gravity.START;
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			AndroidUtils.setMargins(imageViewLayoutParams, dpToPx(14), dpToPx(8), dpToPx(22), 0);
			iconView.setBackgroundResource(R.drawable.border_round_solid_light);
			AndroidUtils.setPadding(iconView, dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		if (!TextUtils.isEmpty(secondaryText)) {
			buildDescriptionView(secondaryText, llText, 8, 0);
		}

		buildTitleView(title, llText);

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			AndroidUtils.setMargins(timeViewParams, 0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			AndroidUtils.setPadding(timeView, 0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			timeView.setTextColor(getMainFontColor());

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}


	protected void updateDestinationStreetName(LatLon latLon) {
		WeakReference<RouteDetailsFragment> fragmentRef = new WeakReference<>(this);
		GeocodingLookupService.AddressLookupRequest addressLookupRequest = new GeocodingLookupService.AddressLookupRequest(latLon, new GeocodingLookupService.OnAddressLookupResult() {
			@Override
			public void geocodingDone(String address) {
				RouteDetailsFragment fragment = fragmentRef.get();
				if (!TextUtils.isEmpty(address) && fragment != null && !fragment.isPaused()) {
					fragment.destinationStreetStr = address;
					fragment.updateCards();
					doAfterMenuStateChange(0, 0);
				}
			}
		}, null);

		app.getGeocodingLookupService().lookupAddress(addressLookupRequest);
	}

	public void buildWalkRow(@NonNull View view, Spannable title, LinearLayout imagesContainer, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
		});
		baseView.addView(ll);

		Drawable icon = getPaintedContentIcon(R.drawable.ic_action_pedestrian_dark, getActiveColor());
		ImageView iconView = new ImageView(view.getContext());
		iconView.setImageDrawable(AndroidUtils.getDrawableForDirection(view.getContext(), icon));
		FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(24), dpToPx(24));
		imageViewLayoutParams.gravity = Gravity.START| (imagesContainer != null ? Gravity.TOP : Gravity.CENTER_VERTICAL);
		iconView.setLayoutParams(imageViewLayoutParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

		if (imagesContainer != null) {
			imagesContainer.addView(iconView);
		} else {
			AndroidUtils.setMargins(imageViewLayoutParams, dpToPx(16), 0, dpToPx(24), 0);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);
		buildTitleView(title, llText);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildStartRow(@NonNull View view, Drawable icon, String timeText, Spannable title, LinearLayout imagesContainer, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
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
				AndroidUtils.setMargins(imageViewLayoutParams, dpToPx(16), 0, dpToPx(24), 0);
				baseItemView.addView(iconView);
			}
		}

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		buildTitleView(title, llText);

		if (!TextUtils.isEmpty(timeText)) {
			TextView timeView = new TextView(view.getContext());
			FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			timeViewParams.gravity = Gravity.END | Gravity.TOP;
			AndroidUtils.setMargins(timeViewParams, 0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			AndroidUtils.setPadding(timeView, 0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			timeView.setTextColor(getMainFontColor());

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildDestinationRow(@NonNull View view, String timeText, Spannable title, Spannable secondaryText,
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

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
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
			AndroidUtils.setMargins(imageViewLayoutParams, dpToPx(16), 0, dpToPx(24), 0);
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView(view.getContext());
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
			AndroidUtils.setMargins(timeViewParams, 0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			AndroidUtils.setPadding(timeView, 0, 0, dpToPx(16), 0);
			timeView.setTextSize(16);
			timeView.setTextColor(getMainFontColor());

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	private void buildIntermediateRow(@NonNull View view, Drawable icon, Spannable title, OnClickListener onClickListener) {

		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 36, v -> {
			copyToClipboard(title.toString(), v.getContext());
			return true;
		});
		baseView.addView(ll);

		// Icon
		if (icon != null) {
			ImageView iconView = new ImageView(view.getContext());
			iconView.setImageDrawable(icon);
			FrameLayout.LayoutParams imageViewLayoutParams = new FrameLayout.LayoutParams(dpToPx(22), dpToPx(22));
			imageViewLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
			iconView.setLayoutParams(imageViewLayoutParams);
			iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

			AndroidUtils.setMargins(imageViewLayoutParams, dpToPx(17), 0, dpToPx(25), 0);
			iconView.setBackgroundResource(R.drawable.border_round_solid_light_small);
			AndroidUtils.setPadding(iconView, dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
			baseItemView.addView(iconView);
		}

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		buildTitleView(title, llText);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	private CollapsableView getCollapsableTransportStopRoutesView(@NonNull Context context, TransportStopRoute transportStopRoute, List<TransportStop> stops) {
		LinearLayout view = buildCollapsableContentView(context);
		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
		Drawable icon = getContentIcon(drawableResId);
		for (int i = 0; i < stops.size(); i++) {
			TransportStop stop = stops.get(i);
			buildIntermediateRow(view, icon, new SpannableString(stop.getName(getPreferredMapLang(), isTransliterateNames())), v -> showLocationOnMap(stop.getLocation()));
		}
		return new CollapsableView(view, null, true);
	}

	protected LinearLayout buildCollapsableContentView(@NonNull Context context) {
		LinearLayout view = new LinearLayout(context);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(View.VISIBLE);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		view.setLayoutParams(llParams);
		return view;
	}

	private void buildTitleView(Spannable title, LinearLayout container) {
		TextViewEx titleView = new TextViewEx(container.getContext());
		FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		titleParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
		titleView.setTypeface(DEFAULT);
		titleView.setLayoutParams(titleParams);
		titleView.setTextSize(16);
		titleView.setTextColor(getMainFontColor());

		titleView.setText(title);
		container.addView(titleView);
	}

	private void buildDescriptionView(Spannable description, LinearLayout container, int paddingTop, int paddingBottom) {
		TextViewEx textViewDescription = new TextViewEx(container.getContext());
		LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		AndroidUtils.setMargins(descriptionParams, 0, dpToPx(paddingTop), 0, dpToPx(paddingBottom));
		textViewDescription.setLayoutParams(descriptionParams);
		textViewDescription.setTypeface(DEFAULT);
		textViewDescription.setTextSize(14);
		textViewDescription.setTextColor(getSecondaryColor());
		textViewDescription.setText(description);
		container.addView(textViewDescription);
	}

	private LinearLayout buildTextContainerView(@NonNull Context context) {
		LinearLayout llText = new LinearLayout(context);
		llText.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.weight = 1f;
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
		llText.setLayoutParams(llTextViewParams);
		return llText;
	}

	private LinearLayout buildHorizontalContainerView(@NonNull Context context, int minHeight, OnLongClickListener onLongClickListener) {
		LinearLayout ll = new LinearLayout(context);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setMinimumHeight(dpToPx(minHeight));
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(AndroidUtils.resolveAttribute(context, android.R.attr.selectableItemBackground));
		AndroidUtils.setPadding(ll, dpToPx(64f), 0, dpToPx(16f), 0);
		ll.setOnLongClickListener(onLongClickListener);
		return ll;
	}

	public View createRouteBadge(@NonNull MapActivity mapActivity, TransportStopRoute transportStopRoute) {
		OsmandApplication app = mapActivity.getMyApplication();
		LinearLayout convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		if (transportStopRoute != null) {
			String routeDescription = transportStopRoute.getDescription(app);
			String routeRef = transportStopRoute.route.getAdjustedRouteRef(true);
			int bgColor = transportStopRoute.getColor(app, isNightMode());

			TextView transportStopRouteTextView = convertView.findViewById(R.id.transport_stop_route_text);
			ImageView transportStopRouteImageView = convertView.findViewById(R.id.transport_stop_route_icon);

			int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
			transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, ColorUtilities.getContrastColor(mapActivity, bgColor, true)));
			transportStopRouteTextView.setText(routeRef + ": " + routeDescription);
			GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(ColorUtilities.getContrastColor(mapActivity, bgColor, true));
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

	private double getWalkTime(@Nullable TransportRouteResultSegment segment,
							   @Nullable TransportRouteResultSegment nextSegment, double walkDistPT, double walkSpeedPT) {
		RouteCalculationResult walkingRouteSegment = app.getTransportRoutingHelper().getWalkingRouteSegment(segment, nextSegment);
		if (walkingRouteSegment != null) {
			return walkingRouteSegment.getRoutingTime();
		}
		return walkDistPT / walkSpeedPT;
	}

	private double getWalkDistance(@Nullable TransportRouteResultSegment segment,
								   @Nullable TransportRouteResultSegment nextSegment, double walkDistPT) {
		RouteCalculationResult walkingRouteSegment = app.getTransportRoutingHelper().getWalkingRouteSegment(segment, nextSegment);
		if (walkingRouteSegment != null) {
			return walkingRouteSegment.getWholeDistance();
		}
		return walkDistPT;
	}

	public void buildRowDivider(@NonNull View view, boolean needMargin) {
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM | Gravity.START;
		if (needMargin) {
			AndroidUtils.setMargins(llHorLineParams, dpToPx(64), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(ColorUtilities.getDividerColor(app, isNightMode()));
		((LinearLayout) view).addView(horizontalLine);
	}

	private void makeGpx() {
		gpxFile = GpxUiHelper.makeGpxFromRoute(app.getRoutingHelper().getRoute(), app);
		gpxItem = GpxUiHelper.makeGpxDisplayItem(app, gpxFile, ROUTE, null);
	}

	void openDetails() {
		if (gpxItem != null && elevationDataSet != null) {
			LatLon location = null;
			WptPt wpt = null;
			gpxItem.chartTypes = new GPXDataSetType[]{GPXDataSetType.ALTITUDE, GPXDataSetType.SLOPE};
			if (gpxItem.chartHighlightPos != -1) {
				TrkSegment segment = gpxFile.getTracks().get(0).getSegments().get(0);
				if (segment != null) {
					float distance = gpxItem.chartHighlightPos * elevationDataSet.getDivX();
					for (WptPt p : segment.getPoints()) {
						if (p.getDistance() >= distance) {
							wpt = p;
							break;
						}
					}
					if (wpt != null) {
						location = new LatLon(wpt.getLat(), wpt.getLon());
					}
				}
			}

			if (location == null) {
				location = new LatLon(gpxItem.locationStart.getLat(), gpxItem.locationStart.getLon());
			}
			if (wpt != null) {
				gpxItem.locationOnMap = wpt;
			} else {
				gpxItem.locationOnMap = gpxItem.locationStart;
			}

			ChooseRouteFragment parent = (ChooseRouteFragment) getParentFragment();
			if (parent != null) {
				parent.analyseOnMap(location, gpxItem);
			}
		}
	}

	private void showDirectionsInfo(int directionInfoIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		RoutingHelper helper = mapActivity.getRoutingHelper();
		List<RouteDirectionInfo> routeDirections = helper.getRouteDirections();
		if (routeDirections.size() > directionInfoIndex) {
			RouteDirectionInfo routeDirectionInfo = routeDirections.get(directionInfoIndex);
			Location loc = helper.getLocationFromRouteDirection(routeDirectionInfo);
			if (loc != null) {
				MapRouteInfoMenu.directionInfo = directionInfoIndex;
				OsmandSettings settings = mapActivity.getMyApplication().getSettings();
				settings.setMapLocationToShow(loc.getLatitude(), loc.getLongitude(),
						Math.max(13, settings.getLastKnownMapZoom()),
						new PointDescription(PointDescription.POINT_TYPE_MARKER,
								routeDirectionInfo.getDescriptionRoutePart() + " " + getTimeDescription(mapActivity.getMyApplication(), routeDirectionInfo)),
						false, null);
				MapActivity.launchMapActivityMoveToTop(mapActivity);
				dismiss();
			}
		}
	}

	@Override
	public void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull TransportRouteResultSegment segment) {
		showRouteSegmentOnMap(segment);
	}

	@Override
	public void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull RouteCalculationResult result) {
		showRouteOnMap(result);
	}

	@Override
	public void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull LatLon start, @NonNull LatLon end) {
		showOnMap(start, end);
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
		runLayoutListener();
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		if (card instanceof PublicTransportCard) {
			switch (buttonIndex) {
				case PublicTransportCard.DETAILS_BUTTON_INDEX:
					openMenuFullScreen();
					break;
				case PublicTransportCard.SHOW_BUTTON_INDEX:
					RouteDetailsFragmentListener listener = getRouteDetailsListener();
					if (listener != null) {
						listener.onNavigationRequested();
					}
					break;
			}
		} else if (card instanceof RouteDirectionsCard) {
			if (buttonIndex >= 0) {
				showDirectionsInfo(buttonIndex);
			}
		} else if (card instanceof RouteStatisticCard) {
			switch (buttonIndex) {
				case RouteStatisticCard.DETAILS_BUTTON_INDEX:
					openMenuFullScreen();
					break;
				case RouteStatisticCard.START_BUTTON_INDEX:
					RouteDetailsFragmentListener listener = getRouteDetailsListener();
					if (listener != null) {
						listener.onNavigationRequested();
					}
					break;
			}
		} else if (card instanceof AttachTrackToRoadsBannerCard) {
			if (MeasurementToolFragment.showSnapToRoadsDialog(requireMapActivity(), true)) {
				dismiss();
			}
		}
	}

	public static class CumulativeInfo {
		public int distance;
		public int time;

		CumulativeInfo() {
			distance = 0;
			time = 0;
		}
	}

	public static CumulativeInfo getRouteDirectionCumulativeInfo(int position, List<
			RouteDirectionInfo> routeDirections) {
		CumulativeInfo cumulativeInfo = new CumulativeInfo();
		if (position >= routeDirections.size()) {
			return cumulativeInfo;
		}
		for (int i = 0; i < position; i++) {
			RouteDirectionInfo routeDirectionInfo = routeDirections.get(i);
			cumulativeInfo.time += routeDirectionInfo.getExpectedTime();
			cumulativeInfo.distance += routeDirectionInfo.distance;
		}
		return cumulativeInfo;
	}

	public static String getTimeDescription(OsmandApplication app, RouteDirectionInfo model) {
		int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}
}
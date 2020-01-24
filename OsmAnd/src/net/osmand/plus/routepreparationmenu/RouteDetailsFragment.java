package net.osmand.plus.routepreparationmenu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture;
import com.github.mikephil.charting.renderer.HorizontalBarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.mapcontextmenu.InterceptorLinearLayout;
import net.osmand.plus.mapcontextmenu.MenuBuilder.CollapsableView;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.CardChartListener;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard.PublicTransportCardListener;
import net.osmand.plus.routepreparationmenu.cards.RouteDirectionsCard;
import net.osmand.plus.routepreparationmenu.cards.RouteInfoCard;
import net.osmand.plus.routepreparationmenu.cards.RouteStatisticCard;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RouteDetailsFragment extends ContextMenuFragment implements PublicTransportCardListener,
		CardListener, CardChartListener {

	public static final String ROUTE_ID_KEY = "route_id_key";
	private static final float PAGE_MARGIN = 5f;

	private int routeId = -1;
	private String destinationStreetStr = "";
	private int pageMarginPx;
	private int toolbarHeightPx;

	private GPXFile gpx;
	@Nullable
	private OrderedLineDataSet slopeDataSet;
	@Nullable
	private OrderedLineDataSet elevationDataSet;
	private GpxDisplayItem gpxItem;
	private List<BaseCard> menuCards = new ArrayList<>();
	@Nullable
	private PublicTransportCard transportCard;
	private RouteDetailsFragmentListener routeDetailsListener;
	private RouteStatisticCard statisticCard;
	private List<RouteInfoCard> routeInfoCards = new ArrayList<>();
	private RouteDetailsMenu routeDetailsMenu;

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
				view.findViewById(getBottomScrollViewId()).setBackgroundDrawable(null);
				LinearLayout cardsContainer = getCardsContainer();
				FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) cardsContainer.getLayoutParams();
				layoutParams.setMargins(pageMarginPx, 0, pageMarginPx, 0);
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
			if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
				topShadow.setVisibility(View.INVISIBLE);
				bottomContainer.setBackgroundDrawable(null);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.drawable.travel_card_bg_light, R.drawable.travel_card_bg_dark);
			} else {
				topShadow.setVisibility(View.VISIBLE);
				AndroidUtils.setBackground(mainView.getContext(), bottomContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
				AndroidUtils.setBackground(mainView.getContext(), cardsContainer, isNightMode(), R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
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
		statisticCard = new RouteStatisticCard(mapActivity, gpx, new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent ev) {
				InterceptorLinearLayout mainView = getMainView();
				if (mainView != null) {
					mainView.requestDisallowInterceptTouchEvent(true);
				}
				for (RouteInfoCard card : routeInfoCards) {
					final HorizontalBarChart ch = card.getChart();
					if (ch != null) {
						final MotionEvent event = MotionEvent.obtainNoHistory(ev);
						event.setSource(0);
						ch.dispatchTouchEvent(event);
					}
				}
				return false;
			}
		}, new OnClickListener() {
			@Override
			public void onClick(View v) {
				openDetails();
			}
		});
		statisticCard.setTransparentBackground(true);
		statisticCard.setListener(this);
		statisticCard.setChartListener(this);
		menuCards.add(statisticCard);
		cardsContainer.addView(statisticCard.build(mapActivity));
		buildRowDivider(cardsContainer, false);
		slopeDataSet = statisticCard.getSlopeDataSet();
		elevationDataSet = statisticCard.getElevationDataSet();

		List<RouteSegmentResult> route = app.getRoutingHelper().getRoute().getOriginalRoute();
		if (route != null) {
			RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
			RenderingRulesStorage defaultRender = app.getRendererRegistry().defaultRender();

			MapRenderRepositories maps = app.getResourceManager().getRenderer();
			RenderingRuleSearchRequest currentSearchRequest = maps.getSearchRequestWithAppliedCustomRules(currentRenderer, isNightMode());
			RenderingRuleSearchRequest defaultSearchRequest = maps.getSearchRequestWithAppliedCustomRules(defaultRender, isNightMode());

			List<RouteStatistics> routeStatistics = RouteStatisticsHelper.calculateRouteStatistic(route,
					currentRenderer, defaultRender, currentSearchRequest, defaultSearchRequest);
			GPXTrackAnalysis analysis = gpx.getAnalysis(0);


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
		LineChart chart = statisticCard.getChart();
		if (chart != null) {
			chart.setExtraRightOffset(16);
			chart.setExtraLeftOffset(16);
		}
	}

	@Override
	protected void calculateLayout(View view, boolean initLayout) {
		super.calculateLayout(view, initLayout);
		if (!initLayout && getCurrentMenuState() != MenuState.FULL_SCREEN) {
			refreshChart(false);
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

	private OnTouchListener getChartTouchListener() {
		return new OnTouchListener() {
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View v, MotionEvent ev) {
				if (ev.getSource() != 0 && v instanceof HorizontalBarChart) {
					if (statisticCard != null) {
						LineChart ch = statisticCard.getChart();
						if (ch != null) {
							final MotionEvent event = MotionEvent.obtainNoHistory(ev);
							event.setSource(0);
							ch.dispatchTouchEvent(event);
						}
					}
					return true;
				}
				return false;
			}
		};
	}

	@SuppressLint("ClickableViewAccessibility")
	private void addRouteCard(final LinearLayout cardsContainer, RouteInfoCard routeInfoCard) {
		OsmandApplication app = requireMyApplication();
		menuCards.add(routeInfoCard);
		routeInfoCard.setListener(this);
		cardsContainer.addView(routeInfoCard.build(app));
		buildRowDivider(cardsContainer, false);

		routeInfoCards.add(routeInfoCard);
		HorizontalBarChart chart = routeInfoCard.getChart();
		if (chart != null) {
			LineChart mainChart = statisticCard.getChart();
			if (mainChart != null) {
				chart.getAxisRight().setAxisMinimum(mainChart.getXChartMin());
				chart.getAxisRight().setAxisMaximum(mainChart.getXChartMax());
			}
			chart.setRenderer(new CustomBarChartRenderer(chart, chart.getAnimator(), chart.getViewPortHandler(), AndroidUtils.dpToPx(app, 1f) / 2f));
			chart.setHighlightPerDragEnabled(false);
			chart.setHighlightPerTapEnabled(false);
			chart.setOnTouchListener(getChartTouchListener());
		}
	}

	public Drawable getCollapseIcon(boolean collapsed) {
		OsmandApplication app = requireMyApplication();
		return app.getUIUtilities().getIcon(collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up, R.color.description_font_and_bottom_sheet_icons);
	}

	private void buildSegmentItem(View view, final TransportRouteResultSegment segment,
								  final TransportRouteResultSegment nextSegment, int[] startTime, double walkSpeed, double boardingTime) {
		OsmandApplication app = requireMyApplication();
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
		int bgColor = transportStopRoute.getColor(app, isNightMode());
		routeLine.setBackgroundColor(bgColor);
		baseContainer.addView(routeLine);

		LinearLayout stopsContainer = new LinearLayout(view.getContext());
		stopsContainer.setOrientation(LinearLayout.VERTICAL);
		baseContainer.addView(stopsContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus : transportStopRoute.type.getResourceId();
		Drawable icon = getContentIcon(drawableResId);

		Typeface typeface = FontCache.getRobotoMedium(app);
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
			spannable.append(String.valueOf(stops.size())).append(" ").append(getString(R.string.transport_stops));
			spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (spannable.length() > 0) {
			startIndex = spannable.length();
			spannable.append(" • ");
			spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, startIndex + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		spannable.append(OsmAndFormatter.getFormattedDistance((float) segment.getTravelDist(), app));
		spannable.setSpan(new CustomTypefaceSpan(typeface), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		String type = getString(transportStopRoute.getTypeStrRes()).toLowerCase();
		SpannableString textType = new SpannableString(getString(R.string.by_transport_type, type));
		buildCollapsableRow(stopsContainer, spannable, textType, true, collapsableView, null);

		final TransportStop endStop = stops.get(stops.size() - 1);
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
				spannable.append(getString(R.string.shared_string_walk)).append(", ").append(OsmAndFormatter.getFormattedDistance((float) walkDist, app));
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
		imagesContainer.setPadding(dpToPx(16), dpToPx(12), dpToPx(24), 0);
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
		OsmandApplication app = requireMyApplication();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TargetPoint startPoint = targetPointsHelper.getPointToStart();
		TargetPoint endPoint = targetPointsHelper.getPointToNavigate();
		int[] startTime = {0};
		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		for (int i = 0; i < segments.size(); i++) {
			boolean first = i == 0;
			boolean last = i == segments.size() - 1;
			final TransportRouteResultSegment segment = segments.get(i);
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

	private void buildStartItem(@NonNull View view, final TargetPoint start, int[] startTime,
								final TransportRouteResultSegment segment, double walkSpeed) {
		OsmandApplication app = requireMyApplication();
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
		title.setSpan(new CustomTypefaceSpan(FontCache.getRobotoMedium(app)), startIndex, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		startIndex = title.length();
		title.append(", ").append(OsmAndFormatter.getFormattedDistance((float) walkDist, app));
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
		OsmandApplication app = requireMyApplication();
		if (transportCard == null) {
			RouteCalculationResult route = app.getRoutingHelper().getRoute();
			if (route != null) {
				showRouteOnMap(route);
			}
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
		OsmandApplication app = requireMyApplication();
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
		OsmandApplication app = requireMyApplication();
		QuadRect rect = app.getTransportRoutingHelper().getTransportRouteRect(result);
		if (rect != null) {
			openMenuHeaderOnly();
			fitRectOnMap(rect);
		}
	}

	private void addWalkRouteIcon(LinearLayout container) {
		OsmandApplication app = requireMyApplication();
		ImageView walkLineImage = new ImageView(container.getContext());
		walkLineImage.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.walk_route_item_light));
		LinearLayout.LayoutParams walkImageLayoutParams = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(14));
		walkImageLayoutParams.setMargins(dpToPx(7), dpToPx(6), 0, dpToPx(6));
		walkLineImage.setLayoutParams(walkImageLayoutParams);
		container.addView(walkLineImage);
	}

	private void buildDestinationItem(@NonNull View view, final TargetPoint destination, int[] startTime,
									  final TransportRouteResultSegment segment, double walkSpeed) {
		OsmandApplication app = requireMyApplication();
		Typeface typeface = FontCache.getRobotoMedium(app);
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
		spannable.append(", ").append(OsmAndFormatter.getFormattedDistance((float) walkDist, app));
		spannable.setSpan(new ForegroundColorSpan(getSecondaryColor()), startIndex, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		buildWalkRow(infoContainer, spannable, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showWalkingRouteOnMap(segment, null);
			}
		});
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

		buildDestinationRow(infoContainer, timeStr, title, secondaryText, destination.point, imagesContainer, new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLocationOnMap(destination.point);
			}
		});

		((ViewGroup) view).addView(baseItemView);
	}

	@ColorInt
	private int getActiveColor() {
		OsmandApplication app = requireMyApplication();
		return ContextCompat.getColor(app, isNightMode() ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
	}

	@ColorInt
	protected int getMainFontColor() {
		OsmandApplication app = requireMyApplication();
		return ContextCompat.getColor(app, isNightMode() ? R.color.text_color_primary_dark : R.color.text_color_primary_light);
	}

	@ColorInt
	protected int getSecondaryColor() {
		OsmandApplication app = requireMyApplication();
		return ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons);
	}

	public void buildCollapsableRow(@NonNull View view, final Spannable title, Spannable secondaryText, boolean collapsable,
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

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		LinearLayout llText = buildTextContainerView(view.getContext());
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

	public void buildStartStopRow(@NonNull View view, Drawable icon, String timeText, final Spannable title,
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

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
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
			iconView.setBackgroundResource(R.drawable.border_round_solid_light);
			iconView.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
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
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
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

		final String routeDescription = transportStopRoute.getDescription(app);
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 36, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(routeDescription, v.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);

		View routeBadge = createRouteBadge(mapActivity, transportStopRoute);
		LinearLayout.LayoutParams routeBadgeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		routeBadgeParams.setMargins(0, dpToPx(6), 0, dpToPx(8));
		routeBadge.setLayoutParams(routeBadgeParams);
		llText.addView(routeBadge);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildEndStopRow(@NonNull View view, Drawable icon, String timeText, final Spannable title, Spannable secondaryText, OnClickListener onClickListener) {
		OsmandApplication app = requireMyApplication();
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
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
			iconView.setBackgroundResource(R.drawable.border_round_solid_light);
			iconView.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
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
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
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
		OsmandApplication app = requireMyApplication();
		final WeakReference<RouteDetailsFragment> fragmentRef = new WeakReference<>(RouteDetailsFragment.this);
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

	public void buildWalkRow(@NonNull View view, final Spannable title, LinearLayout imagesContainer, OnClickListener onClickListener) {
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
				return true;
			}
		});
		baseView.addView(ll);

		Drawable icon = getPaintedContentIcon(R.drawable.ic_action_pedestrian, getActiveColor());
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

		LinearLayout llText = buildTextContainerView(view.getContext());
		ll.addView(llText);
		buildTitleView(title, llText);

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildStartRow(@NonNull View view, Drawable icon, String timeText, final Spannable title, LinearLayout imagesContainer, OnClickListener onClickListener) {
		OsmandApplication app = requireMyApplication();
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
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

		LinearLayout llText = buildTextContainerView(view.getContext());
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
			timeView.setTextColor(getMainFontColor());

			timeView.setText(timeText);
			baseItemView.addView(timeView);
		}

		if (onClickListener != null) {
			ll.setOnClickListener(onClickListener);
		}

		((LinearLayout) view).addView(baseItemView);
	}

	public void buildDestinationRow(@NonNull View view, String timeText, final Spannable title, Spannable secondaryText,
									LatLon location, LinearLayout imagesContainer, OnClickListener onClickListener) {
		OsmandApplication app = requireMyApplication();
		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 48, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
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
			timeViewParams.setMargins(0, dpToPx(8), 0, 0);
			timeView.setLayoutParams(timeViewParams);
			timeView.setPadding(0, 0, dpToPx(16), 0);
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

	private void buildIntermediateRow(@NonNull View view, Drawable icon, final Spannable title, OnClickListener onClickListener) {

		FrameLayout baseItemView = new FrameLayout(view.getContext());
		FrameLayout.LayoutParams baseViewLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		baseItemView.setLayoutParams(baseViewLayoutParams);

		LinearLayout baseView = new LinearLayout(view.getContext());
		baseView.setOrientation(LinearLayout.VERTICAL);
		baseView.setLayoutParams(baseViewLayoutParams);
		baseView.setGravity(Gravity.END);
		baseView.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		baseItemView.addView(baseView);

		LinearLayout ll = buildHorizontalContainerView(view.getContext(), 36, new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(title.toString(), v.getContext());
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
			iconView.setBackgroundResource(R.drawable.border_round_solid_light_small);
			iconView.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
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

	private CollapsableView getCollapsableTransportStopRoutesView(@NonNull Context context, TransportStopRoute transportStopRoute, final List<TransportStop> stops) {
		LinearLayout view = (LinearLayout) buildCollapsableContentView(context, false);
		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus : transportStopRoute.type.getResourceId();
		Drawable icon = getContentIcon(drawableResId);
		for (int i = 0; i < stops.size(); i++) {
			final TransportStop stop = stops.get(i);
			buildIntermediateRow(view, icon, new SpannableString(stop.getName(getPreferredMapLang(), isTransliterateNames())), new OnClickListener() {
				@Override
				public void onClick(View v) {
					showLocationOnMap(stop.getLocation());
				}
			});
		}
		return new CollapsableView(view, null, true);
	}

	protected LinearLayout buildCollapsableContentView(@NonNull Context context, boolean collapsed) {
		final LinearLayout view = new LinearLayout(context);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setVisibility(collapsed ? View.GONE : View.VISIBLE);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		view.setLayoutParams(llParams);
		return view;
	}

	private void buildTitleView(Spannable title, LinearLayout container) {
		OsmandApplication app = requireMyApplication();
		TextViewEx titleView = new TextViewEx(container.getContext());
		FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		titleParams.gravity = Gravity.CENTER_VERTICAL;
		titleView.setTypeface(FontCache.getRobotoRegular(container.getContext()));
		titleView.setLayoutParams(titleParams);
		titleView.setTextSize(16);
		titleView.setTextColor(getMainFontColor());

		titleView.setText(title);
		container.addView(titleView);
	}

	private void buildDescriptionView(Spannable description, LinearLayout container, int paddingTop, int paddingBottom) {
		OsmandApplication app = requireMyApplication();
		TextViewEx textViewDescription = new TextViewEx(container.getContext());
		LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		descriptionParams.setMargins(0, dpToPx(paddingTop), 0, dpToPx(paddingBottom));
		textViewDescription.setLayoutParams(descriptionParams);
		textViewDescription.setTypeface(FontCache.getRobotoRegular(container.getContext()));
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
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
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
		ll.setPadding(dpToPx(64f), 0, dpToPx(16f), 0);
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

			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			ImageView transportStopRouteImageView = (ImageView) convertView.findViewById(R.id.transport_stop_route_icon);

			int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus : transportStopRoute.type.getResourceId();
			transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, UiUtilities.getContrastColor(mapActivity, bgColor, true)));
			transportStopRouteTextView.setText(routeRef + ": " + routeDescription);
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

	private double getWalkTime(@Nullable TransportRouteResultSegment segment,
							   @Nullable TransportRouteResultSegment nextSegment, double walkDistPT, double walkSpeedPT) {
		OsmandApplication app = requireMyApplication();
		RouteCalculationResult walkingRouteSegment = app.getTransportRoutingHelper().getWalkingRouteSegment(segment, nextSegment);
		if (walkingRouteSegment != null) {
			return walkingRouteSegment.getRoutingTime();
		}
		return walkDistPT / walkSpeedPT;
	}

	private double getWalkDistance(@Nullable TransportRouteResultSegment segment,
								   @Nullable TransportRouteResultSegment nextSegment, double walkDistPT) {
		OsmandApplication app = requireMyApplication();
		RouteCalculationResult walkingRouteSegment = app.getTransportRoutingHelper().getWalkingRouteSegment(segment, nextSegment);
		if (walkingRouteSegment != null) {
			return walkingRouteSegment.getWholeDistance();
		}
		return walkDistPT;
	}

	public void buildRowDivider(@NonNull View view, boolean needMargin) {
		OsmandApplication app = requireMyApplication();
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (needMargin) {
			llHorLineParams.setMargins(dpToPx(64), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(ContextCompat.getColor(app, isNightMode() ? R.color.divider_color_dark : R.color.divider_color_light));
		((LinearLayout) view).addView(horizontalLine);
	}

	private void makeGpx() {
		OsmandApplication app = requireMyApplication();
		gpx = GpxUiHelper.makeGpxFromRoute(app.getRoutingHelper().getRoute(), app);
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
		if (gpxItem != null && elevationDataSet != null) {
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
		}
	}

	private void refreshChart(boolean forceFit) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && routeDetailsMenu != null && statisticCard != null &&
				!mapActivity.getMyApplication().getRoutingHelper().isFollowingMode()) {
			LineChart chart = statisticCard.getChart();
			if (chart != null) {
				routeDetailsMenu.refreshChart(chart, forceFit);
				mapActivity.refreshMap();
			}
		}
	}

	private void highlightRouteInfoCharts(@Nullable Highlight h) {
		for (RouteInfoCard rc : routeInfoCards) {
			HorizontalBarChart chart = rc.getChart();
			if (chart != null) {
				Highlight bh = h != null ? chart.getHighlighter().getHighlight(1, h.getXPx()) : null;
				if (bh != null) {
					bh.setDraw(h.getXPx(), 0);
				}
				chart.highlightValue(bh, true);
			}
		}
	}

	@Override
	public void onValueSelected(BaseCard card, Entry e, Highlight h) {
		refreshChart(false);
		highlightRouteInfoCharts(h);
	}

	@Override
	public void onNothingSelected(BaseCard card) {
		highlightRouteInfoCharts(null);
	}

	@Override
	public void onChartGestureStart(BaseCard card, MotionEvent me, ChartGesture lastPerformedGesture) {
	}

	@Override
	public void onChartGestureEnd(BaseCard card, MotionEvent me, ChartGesture lastPerformedGesture, boolean hasTranslated) {
		if ((lastPerformedGesture == ChartGesture.DRAG && hasTranslated) ||
				lastPerformedGesture == ChartGesture.X_ZOOM ||
				lastPerformedGesture == ChartGesture.Y_ZOOM ||
				lastPerformedGesture == ChartGesture.PINCH_ZOOM ||
				lastPerformedGesture == ChartGesture.DOUBLE_TAP ||
				lastPerformedGesture == ChartGesture.ROTATE) {

			refreshChart(true);
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
		final int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}

	private static class CustomBarChartRenderer extends HorizontalBarChartRenderer {

		private float highlightHalfWidth;

		CustomBarChartRenderer(BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler, float highlightHalfWidth) {
			super(chart, animator, viewPortHandler);
			this.highlightHalfWidth = highlightHalfWidth;
		}

		@Override
		protected void setHighlightDrawPos(Highlight high, RectF bar) {
			bar.left = high.getDrawX() - highlightHalfWidth;
			bar.right = high.getDrawX() + highlightHalfWidth;
		}
	}
}
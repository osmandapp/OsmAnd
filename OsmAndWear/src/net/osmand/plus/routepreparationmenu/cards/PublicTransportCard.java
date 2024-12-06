package net.osmand.plus.routepreparationmenu.cards;


import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.router.TransportRouteResult;
import net.osmand.util.Algorithms;

import java.util.Iterator;
import java.util.List;

public class PublicTransportCard extends MapBaseCard {

	private static final int MIN_WALK_TIME = 120;
	public static final int DETAILS_BUTTON_INDEX = 0;
	public static final int SHOW_BUTTON_INDEX = 1;

	private final LatLon startLocation;
	private final LatLon endLocation;
	private final TransportRouteResult routeResult;
	private PublicTransportCardListener transportCardListener;

	private final int routeId;
	private String showButtonCustomTitle;
	private boolean routeInfoVisible = true;
	private boolean routeButtonsVisible = true;

	public interface PublicTransportCardListener {
		void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull TransportRouteResultSegment segment);
		void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull RouteCalculationResult result);
		void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull LatLon start, @NonNull LatLon end);
	}

	public PublicTransportCard(@NonNull MapActivity mapActivity, LatLon startLocation, LatLon endLocation,
							   @NonNull TransportRouteResult routeResult, int routeId) {
		super(mapActivity);
		this.startLocation = startLocation;
		this.endLocation = endLocation;
		this.routeResult = routeResult;
		this.routeId = routeId;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.transport_route_card;
	}

	public PublicTransportCardListener getTransportCardListener() {
		return transportCardListener;
	}

	public void setTransportCardListener(PublicTransportCardListener listener) {
		this.transportCardListener = listener;
	}

	public String getShowButtonCustomTitle() {
		return showButtonCustomTitle;
	}

	public void setShowButtonCustomTitle(String showButtonCustomTitle) {
		this.showButtonCustomTitle = showButtonCustomTitle;
	}

	public boolean isRouteInfoVisible() {
		return routeInfoVisible;
	}

	public void setRouteInfoVisible(boolean routeInfoVisible) {
		this.routeInfoVisible = routeInfoVisible;
	}

	public boolean isRouteButtonsVisible() {
		return routeButtonsVisible;
	}

	public void setRouteButtonsVisible(boolean routeButtonsVisible) {
		this.routeButtonsVisible = routeButtonsVisible;
	}

	@Override
	protected void updateContent() {
		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		boolean badgesRowClickable = !routeInfoVisible && !routeButtonsVisible;
		createRouteBadges(segments, badgesRowClickable);

		view.findViewById(R.id.route_info).setVisibility(routeInfoVisible ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.route_buttons).setVisibility(routeButtonsVisible ? View.VISIBLE : View.GONE);
		if (badgesRowClickable) {
			view.findViewById(R.id.badges_padding).setVisibility(View.VISIBLE);
			view.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), R.attr.card_and_list_background_basic));
			View info = view.findViewById(R.id.routes_info_container);
			int paddingLeft = info.getPaddingLeft();
			int paddingTop = info.getPaddingTop();
			int paddingRight = info.getPaddingRight();
			int paddingBottom = info.getPaddingBottom();
			info.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
			info.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
			info.setOnClickListener(v -> notifyCardPressed());
		} else {
			view.findViewById(R.id.badges_padding).setVisibility(View.GONE);
		}

		TextView fromLine = view.findViewById(R.id.from_line);
		TextView wayLine = view.findViewById(R.id.way_line);
		TextView intervalLine = view.findViewById(R.id.interval_line);

		fromLine.setText(getFirstLineDescrSpan());
		wayLine.setText(getSecondLineDescrSpan(segments));
		if (hasInterval(segments)) {
			intervalLine.setText(getIntervalDescr(segments));
			intervalLine.setVisibility(View.VISIBLE);
		}

		updateButtons();

		view.findViewById(R.id.bottom_shadow).setVisibility(showBottomShadow ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.card_divider).setVisibility(showTopShadow ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.top_divider).setVisibility(!showTopShadow && showDivider ? View.VISIBLE : View.GONE);
		if (transparentBackground) {
			view.findViewById(R.id.routes_info_container).setBackground(null);
		}
	}

	private boolean hasInterval(List<TransportRouteResultSegment> segments) {
		for (TransportRouteResultSegment segment : segments) {
			if (segment.route.hasInterval()) {
				return true;
			}
		}
		return false;
	}

	public void updateButtons() {
		int color = getActiveColor();
		FrameLayout detailsButton = view.findViewById(R.id.details_button);
		TextView detailsButtonDescr = view.findViewById(R.id.details_button_descr);

		AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		AndroidUtils.setBackground(app, detailsButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		detailsButton.setOnClickListener(v -> {
			app.getTransportRoutingHelper().setCurrentRoute(routeId);
			getMapActivity().refreshMap();
			notifyButtonPressed(DETAILS_BUTTON_INDEX);
		});

		FrameLayout showButton = view.findViewById(R.id.show_button);
		TextView showButtonDescr = view.findViewById(R.id.show_button_descr);
		if (isCurrentRoute()) {
			color = ContextCompat.getColor(app, R.color.card_and_list_background_light);
			AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_active_light, R.drawable.btn_active_dark);
			if (!Algorithms.isEmpty(showButtonCustomTitle)) {
				showButtonDescr.setText(showButtonCustomTitle);
			} else {
				showButtonDescr.setText(R.string.shared_string_control_start);
			}
			showButton.setOnClickListener(v -> notifyButtonPressed(SHOW_BUTTON_INDEX));
		} else {
			AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(app, showButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			if (!Algorithms.isEmpty(showButtonCustomTitle)) {
				showButtonDescr.setText(showButtonCustomTitle);
			} else {
				showButtonDescr.setText(R.string.shared_string_show_on_map);
			}
			showButton.setOnClickListener(v -> {
				app.getTransportRoutingHelper().setCurrentRoute(routeId);
				getMapActivity().refreshMap();
				notifyButtonPressed(SHOW_BUTTON_INDEX);
			});
		}
		showButtonDescr.setTextColor(color);
	}

	public int getRouteId() {
		return routeId;
	}

	public boolean isCurrentRoute() {
		return routeId == app.getTransportRoutingHelper().getCurrentRoute();
	}

	private SpannableString getFirstLineDescrSpan() {
		String preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		boolean transliterateNames = app.getSettings().MAP_TRANSLITERATE_NAMES.get();

		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		String name = segments.get(0).getStart().getName(preferredMapLang, transliterateNames);
		String firstLine = Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_from)) + " " + name;

		if (segments.size() > 1) {
			firstLine += "  •  " + app.getString(R.string.transfers_size, (segments.size() - 1));
		}

		SpannableString firstLineDesc = new SpannableString(firstLine);
		firstLineDesc.setSpan(new CustomTypefaceSpan(FontCache.getMediumFont()), firstLine.indexOf(name), firstLine.indexOf(name) + name.length(), 0);
		firstLineDesc.setSpan(new ForegroundColorSpan(getMainFontColor()), firstLine.indexOf(name), firstLine.indexOf(name) + name.length(), 0);

		return firstLineDesc;
	}

	private SpannableString getSecondLineDescrSpan(List<TransportRouteResultSegment> segments) {
		TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
		Typeface typeface = FontCache.getMediumFont();
		int walkTimeReal = transportRoutingHelper.getWalkingTime(segments);
		int walkTimePT = (int) routeResult.getWalkTime();
		int walkTime = walkTimeReal > 0 ? walkTimeReal : walkTimePT;
		String walkTimeStr = OsmAndFormatter.getFormattedDuration(walkTime, app);
		int walkDistanceReal = transportRoutingHelper.getWalkingDistance(segments);
		int walkDistancePT = (int) routeResult.getWalkDist();
		int walkDistance = walkDistanceReal > 0 ? walkDistanceReal : walkDistancePT;
		String walkDistanceStr = OsmAndFormatter.getFormattedDistance(walkDistance, app);
		int travelTime = (int) routeResult.getTravelTime() + walkTime;
		String travelTimeStr = OsmAndFormatter.getFormattedDuration(travelTime, app);
		int travelDist = (int) routeResult.getTravelDist() + walkDistance;
		String travelDistStr = OsmAndFormatter.getFormattedDistance(travelDist, app);

		String secondLine = travelTimeStr + ", " + travelDistStr + "  •  " + app.getString(R.string.shared_string_walk) + " " + walkTimeStr + ", " + walkDistanceStr;

		SpannableString secondLineDesc = new SpannableString(secondLine);

		int mainFontColor = getMainFontColor();
		int startTravelTime = secondLine.indexOf(travelTimeStr);
		secondLineDesc.setSpan(new ForegroundColorSpan(mainFontColor), startTravelTime, startTravelTime + travelTimeStr.length(), 0);
		secondLineDesc.setSpan(new CustomTypefaceSpan(typeface), startTravelTime, startTravelTime + travelTimeStr.length(), 0);

		int startWalkTime = secondLine.lastIndexOf(walkTimeStr);
		secondLineDesc.setSpan(new ForegroundColorSpan(mainFontColor), startWalkTime, startWalkTime + walkTimeStr.length(), 0);
		secondLineDesc.setSpan(new CustomTypefaceSpan(typeface), startWalkTime, startWalkTime + walkTimeStr.length(), 0);

		return secondLineDesc;
	}

	private String getIntervalDescr(List<TransportRouteResultSegment> segments) {
		String interval = Algorithms.capitalizeFirstLetter(app.getString(R.string.shared_string_interval));
		Iterator<TransportRouteResultSegment> iterator = segments.iterator();
		boolean firstInterval = true;
		while (iterator.hasNext()) {
			TransportRouteResultSegment segment = iterator.next();
			if (segment.route.hasInterval()) {
				if (firstInterval) {
					interval = app.getString(R.string.ltr_or_rtl_combine_via_space, interval,
							app.getString(R.string.ltr_or_rtl_combine_via_dash,
									segment.route.getRef(), segment.route.getInterval()));
					firstInterval = false;
				} else {
					interval = app.getString(R.string.ltr_or_rtl_combine_via_comma, interval,
							app.getString(R.string.ltr_or_rtl_combine_via_dash,
									segment.route.getRef(), segment.route.getInterval()));
				}
			}
		}
		return interval;
	}

	private void createRouteBadges(List<TransportRouteResultSegment> segments, boolean badgesRowClickable) {
		int itemsSpacing = AndroidUtils.dpToPx(app, 6);
		FlowLayout routesBadges = view.findViewById(R.id.routes_badges);
		routesBadges.removeAllViews();

		TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
		Iterator<TransportRouteResultSegment> iterator = segments.iterator();
		TransportRouteResultSegment prevSegment = null;
		while (iterator.hasNext()) {
			TransportRouteResultSegment s = iterator.next();
			RouteCalculationResult walkingSegment = transportRoutingHelper.getWalkingRouteSegment(prevSegment, s);
			if (walkingSegment != null) {
				double walkTime = walkingSegment.getRoutingTime();
				if (walkTime > MIN_WALK_TIME) {
					routesBadges.addView(createWalkRouteBadge(walkingSegment, badgesRowClickable), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
					routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
				}
			} else if (s.walkDist > 0) {
				double walkTime = getWalkTime(s.walkDist, routeResult.getWalkSpeed());
				if (walkTime > MIN_WALK_TIME) {
					LatLon start;
					LatLon end = s.getStart().getLocation();
					if (prevSegment != null) {
						start = prevSegment.getEnd().getLocation();
					} else {
						start = this.startLocation;
					}
					routesBadges.addView(createWalkRouteBadge(walkTime, start, end, badgesRowClickable), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
					routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
				}
			}
			routesBadges.addView(createRouteBadge(s, badgesRowClickable), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			if (iterator.hasNext()) {
				routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			} else {
				walkingSegment = transportRoutingHelper.getWalkingRouteSegment(s, null);
				if (walkingSegment != null) {
					double walkTime = walkingSegment.getRoutingTime();
					if (walkTime > MIN_WALK_TIME) {
						routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
						routesBadges.addView(createWalkRouteBadge(walkingSegment, badgesRowClickable), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
					}
				} else {
					double finishWalkDist = routeResult.getFinishWalkDist();
					if (finishWalkDist > 0) {
						double walkTime = getWalkTime(finishWalkDist, routeResult.getWalkSpeed());
						if (walkTime > MIN_WALK_TIME) {
							LatLon start = s.getEnd().getLocation();
							LatLon end = this.endLocation;
							routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
							routesBadges.addView(createWalkRouteBadge(walkTime, start, end, badgesRowClickable));
						}
					}
				}
			}
			prevSegment = s;
		}
	}

	private View createRouteBadge(@NonNull TransportRouteResultSegment segment, boolean badgesRowClickable) {
		LinearLayout bageView = (LinearLayout) themedInflater.inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		TransportRoute transportRoute = segment.route;
		TransportStopRoute transportStopRoute = TransportStopRoute.getTransportStopRoute(transportRoute, segment.getStart());

		String routeRef = segment.route.getAdjustedRouteRef(true);
		int bgColor = transportStopRoute.getColor(app, nightMode);

		TextView transportStopRouteTextView = bageView.findViewById(R.id.transport_stop_route_text);
		ImageView transportStopRouteImageView = bageView.findViewById(R.id.transport_stop_route_icon);

		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
		transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, ColorUtilities.getContrastColor(app, bgColor, true)));
		transportStopRouteTextView.setText(routeRef);
		GradientDrawable gradientDrawableBg = (GradientDrawable) bageView.getBackground();
		gradientDrawableBg.setColor(bgColor);
		transportStopRouteTextView.setTextColor(ColorUtilities.getContrastColor(app, bgColor, true));

		if (transportCardListener != null && !badgesRowClickable) {
			bageView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					transportCardListener.onPublicTransportCardBadgePressed(PublicTransportCard.this, segment);
				}
			});
		} else {
			bageView.findViewById(R.id.button).setBackground(null);
		}
		return bageView;
	}

	private View createWalkRouteBadge(@NonNull RouteCalculationResult result, boolean badgesRowClickable) {
		View v = createWalkRouteBadge(result.getRoutingTime(), null, null, badgesRowClickable);
		if (transportCardListener != null && !badgesRowClickable) {
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					transportCardListener.onPublicTransportCardBadgePressed(PublicTransportCard.this, result);
				}
			});
		}
		return v;
	}

	private View createWalkRouteBadge(double walkTime, @Nullable LatLon start, @Nullable LatLon end, boolean badgesRowClickable) {
		LinearLayout bageView = (LinearLayout) getMapActivity().getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		int activeColor = getActiveColor();

		TextView transportStopRouteTextView = bageView.findViewById(R.id.transport_stop_route_text);
		ImageView transportStopRouteImageView = bageView.findViewById(R.id.transport_stop_route_icon);

		Drawable icPedestrian = getActiveIcon(R.drawable.ic_action_pedestrian_dark);
		transportStopRouteImageView.setImageDrawable(AndroidUtils.getDrawableForDirection(app, icPedestrian));
		transportStopRouteTextView.setText(OsmAndFormatter.getFormattedDuration((int) walkTime, app));
		GradientDrawable gradientDrawableBg = (GradientDrawable) bageView.getBackground();
		gradientDrawableBg.setColor(activeColor);
		transportStopRouteTextView.setTextColor(activeColor);

		AndroidUtils.setBackground(app, bageView, nightMode, R.drawable.btn_border_active_light, R.drawable.btn_border_active_dark);

		if (transportCardListener != null && start != null && end != null && !badgesRowClickable) {
			bageView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					transportCardListener.onPublicTransportCardBadgePressed(PublicTransportCard.this, start, end);
				}
			});
		} else {
			bageView.findViewById(R.id.button).setBackground(null);
		}
		return bageView;
	}

	private View createArrow() {
		LinearLayout container = new LinearLayout(app);
		ImageView arrow = new ImageView(app);
		Drawable icArrow = getContentIcon(R.drawable.ic_action_arrow_forward_16);
		arrow.setImageDrawable(AndroidUtils.getDrawableForDirection(app, icArrow));
		container.addView(arrow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtils.dpToPx(app, 28)));
		return container;
	}

	private double getWalkTime(double walkDist, double walkSpeed) {
		return walkDist / walkSpeed;
	}
}
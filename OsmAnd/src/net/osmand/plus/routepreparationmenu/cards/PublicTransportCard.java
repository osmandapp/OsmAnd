package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.routepreparationmenu.ShowRouteInfoDialogFragment;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.Algorithms;

import java.util.Iterator;
import java.util.List;

public class PublicTransportCard extends BaseCard {

	private static final int MIN_WALK_TIME = 120;
	public static final int DETAILS_BUTTON_INDEX = 0;
	public static final int SHOW_BUTTON_INDEX = 1;

	private LatLon startLocation;
	private LatLon endLocation;
	private TransportRouteResult routeResult;
	private PublicTransportCardListener transportCardListener;

	private int routeId;

	public interface PublicTransportCardListener {
		void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull TransportRouteResultSegment segment);
		void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull RouteCalculationResult result);
		void onPublicTransportCardBadgePressed(@NonNull PublicTransportCard card, @NonNull LatLon start, @NonNull LatLon end);
	}

	public PublicTransportCard(MapActivity mapActivity, LatLon startLocation, LatLon endLocation, TransportRouteResult routeResult, int routeId) {
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

	@Override
	protected void updateContent() {
		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		createRouteBadges(segments);

		TextView fromLine = (TextView) view.findViewById(R.id.from_line);
		TextView wayLine = (TextView) view.findViewById(R.id.way_line);

		fromLine.setText(getFirstLineDescrSpan());
		wayLine.setText(getSecondLineDescrSpan());

		FrameLayout detailsButton = (FrameLayout) view.findViewById(R.id.details_button);
		TextView detailsButtonDescr = (TextView) view.findViewById(R.id.details_button_descr);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(app, detailsButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}
		int color = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		detailsButtonDescr.setTextColor(color);
		detailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				app.getTransportRoutingHelper().setCurrentRoute(routeId);
				getMapActivity().refreshMap();
				ShowRouteInfoDialogFragment.showInstance(mapActivity, routeId);
			}
		});

		FrameLayout showButton = (FrameLayout) view.findViewById(R.id.show_button);
		TextView showButtonDescr = (TextView) view.findViewById(R.id.show_button_descr);
		if (isCurrentRoute()) {
			color = ContextCompat.getColor(app, R.color.color_white);
			AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_active_light, R.drawable.btn_active_dark);
			showButtonDescr.setText(R.string.shared_string_selected);
			showButton.setOnClickListener(null);
		} else {
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
				AndroidUtils.setBackground(app, showButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			} else {
				AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
			}
			showButtonDescr.setText(R.string.shared_string_show_on_map);
			showButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getTransportRoutingHelper().setCurrentRoute(routeId);
					getMapActivity().refreshMap();
					CardListener listener = getListener();
					if (listener != null) {
						listener.onCardButtonPressed(PublicTransportCard.this, SHOW_BUTTON_INDEX);
					}
				}
			});
		}
		showButtonDescr.setTextColor(color);

		view.findViewById(R.id.bottom_shadow).setVisibility(showBottomShadow ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.card_divider).setVisibility(showTopShadow ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.top_divider).setVisibility(!showTopShadow ? View.VISIBLE : View.GONE);
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
		Typeface typeface = FontCache.getRobotoMedium(app);
		firstLineDesc.setSpan(new CustomTypefaceSpan(typeface),
				firstLine.indexOf(name), firstLine.indexOf(name) + name.length(), 0);
		firstLineDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				firstLine.indexOf(name), firstLine.indexOf(name) + name.length(), 0);

		return firstLineDesc;
	}

	private SpannableString getSecondLineDescrSpan() {
		Typeface typeface = FontCache.getRobotoMedium(app);
		String travelTime = OsmAndFormatter.getFormattedDuration((int) routeResult.getTravelTime(), app);
		String walkTime = OsmAndFormatter.getFormattedDuration((int) routeResult.getWalkTime(), app);
		String walkDistance = OsmAndFormatter.getFormattedDistance((int) routeResult.getWalkDist(), app);

		String secondLine = travelTime + "  •  " + app.getString(R.string.on_foot) + " " + walkTime + ", " + walkDistance;

		SpannableString secondLineDesc = new SpannableString(secondLine);

		int startTravelTime = secondLine.indexOf(travelTime);
		secondLineDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				startTravelTime, startTravelTime + travelTime.length(), 0);
		secondLineDesc.setSpan(new CustomTypefaceSpan(typeface),
				startTravelTime, startTravelTime + travelTime.length(), 0);

		int startWalkTime = secondLine.lastIndexOf(walkTime);
		secondLineDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				startWalkTime, startWalkTime + walkTime.length(), 0);
		secondLineDesc.setSpan(new CustomTypefaceSpan(typeface),
				startWalkTime, startWalkTime + walkTime.length(), 0);

		return secondLineDesc;
	}

	private void createRouteBadges(List<TransportRouteResultSegment> segments) {
		int itemsSpacing = AndroidUtils.dpToPx(app, 6);
		FlowLayout routesBadges = (FlowLayout) view.findViewById(R.id.routes_badges);
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
					routesBadges.addView(createWalkRouteBadge(walkingSegment), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
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
					routesBadges.addView(createWalkRouteBadge(walkTime, start, end), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
					routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
				}
			}
			routesBadges.addView(createRouteBadge(s), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			if (iterator.hasNext()) {
				routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			} else {
				walkingSegment = transportRoutingHelper.getWalkingRouteSegment(s, null);
				if (walkingSegment != null) {
					double walkTime = walkingSegment.getRoutingTime();
					if (walkTime > MIN_WALK_TIME) {
						routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
						routesBadges.addView(createWalkRouteBadge(walkingSegment), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
					}
				} else {
					double finishWalkDist = routeResult.getFinishWalkDist();
					if (finishWalkDist > 0) {
						double walkTime = getWalkTime(finishWalkDist, routeResult.getWalkSpeed());
						if (walkTime > MIN_WALK_TIME) {
							LatLon start = s.getEnd().getLocation();
							LatLon end = this.endLocation;
							routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
							routesBadges.addView(createWalkRouteBadge(walkTime, start, end));
						}
					}
				}
			}
			prevSegment = s;
		}
	}

	private View createRouteBadge(@NonNull final TransportRouteResultSegment segment) {
		LinearLayout bageView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		TransportRoute transportRoute = segment.route;
		TransportStopRoute transportStopRoute = TransportStopRoute.getTransportStopRoute(transportRoute, segment.getStart());

		String routeRef = segment.route.getAdjustedRouteRef();
		int bgColor = transportStopRoute.getColor(app, nightMode);

		TextView transportStopRouteTextView = (TextView) bageView.findViewById(R.id.transport_stop_route_text);
		ImageView transportStopRouteImageView = (ImageView) bageView.findViewById(R.id.transport_stop_route_icon);

		int drawableResId = transportStopRoute.type == null ? R.drawable.ic_action_bus_dark : transportStopRoute.type.getResourceId();
		transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(drawableResId, UiUtilities.getContrastColor(app, bgColor, true)));
		transportStopRouteTextView.setText(routeRef);
		GradientDrawable gradientDrawableBg = (GradientDrawable) bageView.getBackground();
		gradientDrawableBg.setColor(bgColor);
		transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(app, bgColor, true));

		if (transportCardListener != null) {
			bageView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					transportCardListener.onPublicTransportCardBadgePressed(PublicTransportCard.this, segment);
				}
			});
		}
		return bageView;
	}

	private View createWalkRouteBadge(@NonNull final RouteCalculationResult result) {
		View v = createWalkRouteBadge(result.getRoutingTime(), null, null);
		if (transportCardListener != null) {
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					transportCardListener.onPublicTransportCardBadgePressed(PublicTransportCard.this, result);
				}
			});
		}
		return v;
	}

	private View createWalkRouteBadge(double walkTime, @Nullable final LatLon start, @Nullable final LatLon end) {
		LinearLayout bageView = (LinearLayout) getMapActivity().getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		int bgColor = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

		TextView transportStopRouteTextView = (TextView) bageView.findViewById(R.id.transport_stop_route_text);
		ImageView transportStopRouteImageView = (ImageView) bageView.findViewById(R.id.transport_stop_route_icon);

		transportStopRouteImageView.setImageDrawable(getColoredIcon(R.drawable.ic_action_pedestrian_dark, nightMode ? R.color.ctx_menu_bottom_view_url_color_dark : R.color.ctx_menu_bottom_view_url_color_light));
		transportStopRouteTextView.setText(OsmAndFormatter.getFormattedDuration((int) walkTime, app));
		GradientDrawable gradientDrawableBg = (GradientDrawable) bageView.getBackground();
		gradientDrawableBg.setColor(bgColor);
		transportStopRouteTextView.setTextColor(ContextCompat.getColor(app, nightMode ? R.color.ctx_menu_bottom_view_url_color_dark : R.color.ctx_menu_bottom_view_url_color_light));

		AndroidUtils.setBackground(app, bageView, nightMode, R.drawable.btn_border_active_light, R.drawable.btn_border_active_dark);

		if (transportCardListener != null && start != null && end != null) {
			bageView.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					transportCardListener.onPublicTransportCardBadgePressed(PublicTransportCard.this, start, end);
				}
			});
		}
		return bageView;
	}

	private View createArrow() {
		LinearLayout container = new LinearLayout(app);
		ImageView arrow = new ImageView(app);
		arrow.setImageDrawable(getContentIcon(R.drawable.ic_action_arrow_forward_16));
		container.addView(arrow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtils.dpToPx(app, 28)));
		return container;
	}

	private double getWalkTime(double walkDist, double walkSpeed) {
		return walkDist / walkSpeed;
	}
}
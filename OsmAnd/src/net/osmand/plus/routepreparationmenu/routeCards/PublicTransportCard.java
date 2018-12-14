package net.osmand.plus.routepreparationmenu.routeCards;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.FlowLayout;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.router.TransportRoutePlanner;

import java.util.Iterator;

public class PublicTransportCard extends BaseRouteCard {


	private MapActivity mapActivity;
	private TransportRoutePlanner.TransportRouteResult routeResult;
	private final TransportRoutingHelper transportHelper;
	private int routeId;

	public PublicTransportCard(MapActivity mapActivity, boolean nightMode, TransportRoutePlanner.TransportRouteResult routeResult, int routeId) {
		super(mapActivity.getMyApplication(), nightMode);
		this.transportHelper = app.getTransportRoutingHelper();
		this.mapActivity = mapActivity;
		this.routeResult = routeResult;
		this.routeId = routeId;
	}

	@Override
	public void bindViewHolder() {
		view = mapActivity.getLayoutInflater().inflate(R.layout.transport_route_card, null);
		view.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));

		int itemsSpacing = AndroidUtils.dpToPx(app, 6);

		FlowLayout routesBadges = (FlowLayout) view.findViewById(R.id.routes_badges);

		Iterator<TransportRoutePlanner.TransportRouteResultSegment> iterator = routeResult.getSegments().iterator();

		while (iterator.hasNext()) {
			TransportRoutePlanner.TransportRouteResultSegment s = iterator.next();
			if (s.walkDist != 0) {
				double walkTime = getWalkTime(s.walkDist, routeResult.getWalkSpeed());
				if (walkTime > 120) {
					String walkTimeS = OsmAndFormatter.getFormattedDuration((int) walkTime, app);
					routesBadges.addView(createWalkRouteBadge(walkTimeS), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
					routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
				}
			}
			routesBadges.addView(createRouteBadge(s), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			if (iterator.hasNext()) {
				routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			} else {
				double finishWalkDist = routeResult.getFinishWalkDist();
				if (finishWalkDist > 0) {
					double walkTime2 = getWalkTime(finishWalkDist, routeResult.getWalkSpeed());
					if (walkTime2 > 120) {
						String walkTimeS = OsmAndFormatter.getFormattedDuration((int) walkTime2, app);
						routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
						routesBadges.addView(createWalkRouteBadge(walkTimeS));
					}
				}
			}
		}

		TextView fromLine = (TextView) view.findViewById(R.id.from_line);
		TextView wayLine = (TextView) view.findViewById(R.id.way_line);

		String name = routeResult.getSegments().get(0).getStart().getName();
		String firstLine = app.getString(R.string.route_from) + " " + name;
		if (routeResult.getSegments().size() > 1) {
			firstLine += ", transfers: " + (routeResult.getSegments().size() - 1);
		}
		SpannableString spannableDesc1 = new SpannableString(firstLine);

		spannableDesc1.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
				firstLine.indexOf(name), firstLine.indexOf(name) + name.length(), 0);

		fromLine.setText(spannableDesc1);
		String travelTime = OsmAndFormatter.getFormattedDuration((int) routeResult.getTravelTime(), app);
		String walkTime = OsmAndFormatter.getFormattedDuration((int) routeResult.getWalkTime(), app);
		String walkDistance = OsmAndFormatter.getFormattedDistance((int) routeResult.getTravelDist(), app);

		String secondLine = app.getString(R.string.route_way, travelTime) + " • " + app.getString(R.string.on_foot, walkTime) + " • " + walkDistance;

		SpannableString spannableDesc = new SpannableString(secondLine);

		spannableDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				secondLine.indexOf(travelTime), secondLine.indexOf(travelTime) + travelTime.length(), 0);
		spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
				secondLine.indexOf(travelTime), secondLine.indexOf(travelTime) + travelTime.length(), 0);

		spannableDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				secondLine.indexOf(walkTime), secondLine.indexOf(walkTime) + walkTime.length(), 0);
		spannableDesc.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
				secondLine.indexOf(walkTime), secondLine.indexOf(walkTime) + walkTime.length(), 0);

		wayLine.setText(spannableDesc);
		wayLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		FrameLayout detailsButton = view.findViewById(R.id.details_button);
		detailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				transportHelper.setCurrentRoute(routeId);
			}
		});

		applyDayNightMode();
		if (isLastItem) {
			view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom() + AndroidUtils.dpToPx(app, 60));
		}
	}

	private void applyDayNightMode() {
		TextView fromLine = (TextView) view.findViewById(R.id.from_line);
		TextView wayLine = (TextView) view.findViewById(R.id.way_line);
		AndroidUtils.setTextSecondaryColor(app, fromLine, nightMode);
		AndroidUtils.setTextSecondaryColor(app, wayLine, nightMode);

		FrameLayout detailsButton = view.findViewById(R.id.details_button);

		AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		}

		AndroidUtils.setBackground(app, view, nightMode, R.color.activity_background_light, R.color.activity_background_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.top_divider), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.routes_info_container), nightMode,
				R.color.route_info_bg_light, R.color.route_info_bg_dark);

		int color = ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

		((TextView) view.findViewById(R.id.details_button_descr)).setTextColor(color);

	}

	private View createRouteBadge(TransportRoutePlanner.TransportRouteResultSegment segment) {
		LinearLayout convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		if (segment != null) {
			TransportRoute transportRoute = segment.route;
			TransportStopRoute transportStopRoute = getTransportStopRoute(transportRoute, segment.getStart());

			String routeRef = getAdjustedRouteRef(segment.route.getRef());
			int bgColor = transportStopRoute.getColor(app, nightMode);

			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			ImageView transportStopRouteImageView = (ImageView) convertView.findViewById(R.id.transport_stop_route_icon);

			transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_bus_dark, UiUtilities.getContrastColor(app, bgColor, true)));
			transportStopRouteTextView.setText(routeRef);
			GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(app, bgColor, true));
		}

		return convertView;
	}

	private View createWalkRouteBadge(String walkTime) {
		LinearLayout convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		if (walkTime != null) {
			int bgColor = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			ImageView transportStopRouteImageView = (ImageView) convertView.findViewById(R.id.transport_stop_route_icon);

			transportStopRouteImageView.setImageDrawable(getActiveIcon(R.drawable.ic_action_pedestrian_dark));
			transportStopRouteTextView.setText(walkTime);
			GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(bgColor);

			AndroidUtils.setBackground(app, convertView, nightMode, R.drawable.btn_border_active_light, R.drawable.btn_border_active_dark);
		}

		return convertView;
	}

	private View createArrow() {
		LinearLayout container = new LinearLayout(app);
		ImageView arrow = new ImageView(app);
		arrow.setImageDrawable(getContentIcon(R.drawable.ic_arrow_forward));
		container.addView(arrow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, AndroidUtils.dpToPx(app, 28)));
		return container;
	}

	private TransportStopRoute getTransportStopRoute(TransportRoute rs, TransportStop s) {
		TransportStopType type = TransportStopType.findType(rs.getType());
		TransportStopRoute r = new TransportStopRoute();
		r.type = type;
		r.desc = rs.getName();
		r.route = rs;
		r.stop = s;
		r.refStop = s;
		return r;
	}

	private String getAdjustedRouteRef(String ref) {
		if (ref != null) {
			int charPos = ref.lastIndexOf(':');
			if (charPos != -1) {
				ref = ref.substring(0, charPos);
			}
			if (ref.length() > 4) {
				ref = ref.substring(0, 4);
			}
		}
		return ref;
	}

	private double getWalkTime(double walkDist, double walkSpeed) {
		return walkDist / walkSpeed;
	}
}

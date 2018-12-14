package net.osmand.plus.routepreparationmenu.routeCards;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.FlowLayout;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.router.TransportRoutePlanner;

import java.util.Iterator;

public class PublicTransportCard extends BaseRouteCard {


	private MapActivity mapActivity;
	private TransportRoutePlanner.TransportRouteResult routeResult;
	private final RoutingHelper routingHelper;
	private final TransportRoutingHelper transportHelper;
	private int routeId;

	public PublicTransportCard(MapActivity mapActivity, boolean nightMode, TransportRoutePlanner.TransportRouteResult routeResult, int routeId) {
		super(mapActivity.getMyApplication(), nightMode);
		this.mapActivity = mapActivity;
		this.routeResult = routeResult;
		routingHelper = mapActivity.getRoutingHelper();
		this.transportHelper = routingHelper.getTransportRoutingHelper();
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
//				if (walkTime > 120) {
				String walkTimeS = OsmAndFormatter.getFormattedDuration((int) walkTime, app);
				routesBadges.addView(createWalkRouteBadge(walkTimeS), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
				routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
//				}
			}
			routesBadges.addView(createRouteBadge(s), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			if (iterator.hasNext()) {
				routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			}
		}

		double finishWalkDist = routeResult.getFinishWalkDist();
		if (finishWalkDist > 0) {
			double walkTime2 = getWalkTime(finishWalkDist, routeResult.getWalkSpeed());
//		if (walkTime2 > 120) {
			String walkTimeS = OsmAndFormatter.getFormattedDuration((int) walkTime2, app);
			routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
			routesBadges.addView(createWalkRouteBadge(walkTimeS));
//		}
		}

		TextView fromLine = (TextView) view.findViewById(R.id.from_line);
		TextView wayLine = (TextView) view.findViewById(R.id.way_line);

		fromLine.setText(app.getString(R.string.route_from) + " " + routeResult.getSegments().get(0).getStart().getName());
		String travelTime = OsmAndFormatter.getFormattedDuration((int) routeResult.getTravelTime(), app);
		String walkTime = OsmAndFormatter.getFormattedDuration((int) routeResult.getWalkTime(), app);
		String walkDistance = OsmAndFormatter.getFormattedDistance((int) routeResult.getTravelDist(), app);

		wayLine.setText(app.getString(R.string.route_way) + " " + travelTime + " *  walking - " + walkTime + " * " + walkDistance);
		wayLine.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		AndroidUtils.setTextSecondaryColor(app, fromLine, nightMode);
		AndroidUtils.setTextSecondaryColor(app, wayLine, nightMode);
		FrameLayout detailsButton = view.findViewById(R.id.details_button);

		AndroidUtils.setBackground(app, detailsButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, view.findViewById(R.id.details_button_descr), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		}

		AndroidUtils.setBackground(app, view.findViewById(R.id.top_divider), nightMode,
				R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.routes_info_container), nightMode,
				R.color.route_info_bg_light, R.color.route_info_bg_dark);

		int color = ContextCompat.getColor(mapActivity, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

		((TextView) view.findViewById(R.id.details_button_descr)).setTextColor(color);

		detailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				transportHelper.setCurrentRoute(routeId);
			}
		});

		if (isLastItem) {
			view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom() + AndroidUtils.dpToPx(app, 60));
		}
	}

	private View createRouteBadge(final Object object) {
		LinearLayout convertView = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		if (object != null) {
			String routeRef = "";
			int bgColor = 0;
			if (object instanceof TransportRoutePlanner.TransportRouteResultSegment) {
				TransportRoutePlanner.TransportRouteResultSegment segment = (TransportRoutePlanner.TransportRouteResultSegment) object;
				TransportRoute transportRoute = segment.route;

				TransportStopRoute transportStopRoute = getTransportStopRoute(transportRoute, segment.getStart());
				routeRef = getAdjustedRouteRef(transportStopRoute.route.getRef());
				bgColor = transportStopRoute.getColor(app, nightMode);
			}
			TextView transportStopRouteTextView = (TextView) convertView.findViewById(R.id.transport_stop_route_text);
			ImageView transportStopRouteImageView = (ImageView) convertView.findViewById(R.id.transport_stop_route_icon);

			transportStopRouteImageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_bus_dark, UiUtilities.getContrastColor(app, bgColor, true)));
			transportStopRouteTextView.setText(routeRef);
			GradientDrawable gradientDrawableBg = (GradientDrawable) convertView.getBackground();
			gradientDrawableBg.setColor(bgColor);
			transportStopRouteTextView.setTextColor(UiUtilities.getContrastColor(app, bgColor, true));

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Toast.makeText(app, object.toString(), Toast.LENGTH_LONG).show();
				}
			});
		}

		return convertView;
	}

	private View createWalkRouteBadge(final String walkTime) {
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

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setForeground(app, convertView, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			} else {
				AndroidUtils.setForeground(app, convertView, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
			}
			AndroidUtils.setBackground(app, convertView, nightMode, R.drawable.btn_border_trans_light, R.drawable.ripple_dark);

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Toast.makeText(app, walkTime, Toast.LENGTH_LONG).show();
				}
			});
		}

		return convertView;
	}

	private View createArrow() {
		ImageView arrow = new ImageView(app);
		arrow.setImageDrawable(getContentIcon(R.drawable.ic_arrow_forward));
		return arrow;
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

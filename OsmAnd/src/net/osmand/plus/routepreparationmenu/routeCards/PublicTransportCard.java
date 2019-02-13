package net.osmand.plus.routepreparationmenu.routeCards;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
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
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.FlowLayout;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.plus.widgets.style.CustomTypefaceSpan;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;

import java.util.Iterator;
import java.util.List;

public class PublicTransportCard extends BaseRouteCard {

	private static final int MIN_WALK_TIME = 120;

	private TransportRouteResult routeResult;

	private int routeId;
	private boolean secondButtonVisible;

	public PublicTransportCard(MapActivity mapActivity, TransportRouteResult routeResult, int routeId) {
		super(mapActivity);
		this.routeResult = routeResult;
		this.routeId = routeId;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.transport_route_card;
	}

	@Override
	public void update() {
		if (view != null) {
			view.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));

			List<TransportRouteResultSegment> segments = routeResult.getSegments();
			createRouteBadges(segments);

			TextView fromLine = (TextView) view.findViewById(R.id.from_line);
			TextView wayLine = (TextView) view.findViewById(R.id.way_line);

			fromLine.setText(getFirstLineDescrSpan());
			wayLine.setText(getSecondLineDescrSpan());

			view.findViewById(R.id.details_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					app.getTransportRoutingHelper().setCurrentRoute(routeId);
					getMapActivity().refreshMap();
				}
			});
			view.findViewById(R.id.bottom_shadow).setVisibility(showBottomShadow ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.card_divider).setVisibility(showTopShadow ? View.VISIBLE : View.GONE);

			applyDayNightMode();
		}
	}

	public int getRouteId() {
		return routeId;
	}

	public void setSecondButtonVisible(boolean secondButtonVisible) {
		this.secondButtonVisible = secondButtonVisible;
	}

	protected void applyDayNightMode() {
		TextView fromLine = (TextView) view.findViewById(R.id.from_line);
		TextView wayLine = (TextView) view.findViewById(R.id.way_line);
		AndroidUtils.setTextSecondaryColor(app, fromLine, nightMode);
		AndroidUtils.setTextSecondaryColor(app, wayLine, nightMode);

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

		FrameLayout showButton = (FrameLayout) view.findViewById(R.id.show_button);
		if (secondButtonVisible) {
			TextView showButtonDescr = (TextView) view.findViewById(R.id.show_button_descr);
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
				AndroidUtils.setBackground(app, showButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			} else {
				AndroidUtils.setBackground(app, showButton, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
			}
			showButtonDescr.setTextColor(color);
			showButton.setVisibility(View.VISIBLE);
		} else {
			showButton.setVisibility(View.GONE);
		}

		AndroidUtils.setBackground(app, view, nightMode, R.color.activity_background_light, R.color.activity_background_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.top_divider), nightMode, R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(app, view.findViewById(R.id.routes_info_container), nightMode, R.color.route_info_bg_light, R.color.route_info_bg_dark);
	}

	private SpannableString getFirstLineDescrSpan() {
		List<TransportRouteResultSegment> segments = routeResult.getSegments();
		String name = segments.get(0).getStart().getName();
		String firstLine = app.getString(R.string.route_from) + " " + name;

		if (segments.size() > 1) {
			firstLine += ", " + app.getString(R.string.transfers, (segments.size() - 1));
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
		String walkDistance = OsmAndFormatter.getFormattedDistance((int) routeResult.getTravelDist(), app);

		String secondLine = app.getString(R.string.route_way, travelTime) + "  •  " + app.getString(R.string.on_foot, walkTime) + "  •  " + walkDistance;

		SpannableString secondLineDesc = new SpannableString(secondLine);

		secondLineDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				secondLine.indexOf(travelTime), secondLine.indexOf(travelTime) + travelTime.length(), 0);
		secondLineDesc.setSpan(new CustomTypefaceSpan(typeface),
				secondLine.indexOf(travelTime), secondLine.indexOf(travelTime) + travelTime.length(), 0);

		secondLineDesc.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, nightMode ? R.color.primary_text_dark : R.color.primary_text_light)),
				secondLine.indexOf(walkTime), secondLine.indexOf(walkTime) + walkTime.length(), 0);
		secondLineDesc.setSpan(new CustomTypefaceSpan(typeface),
				secondLine.indexOf(walkTime), secondLine.indexOf(walkTime) + walkTime.length(), 0);

		return secondLineDesc;
	}

	private void createRouteBadges(List<TransportRouteResultSegment> segments) {
		int itemsSpacing = AndroidUtils.dpToPx(app, 6);
		FlowLayout routesBadges = (FlowLayout) view.findViewById(R.id.routes_badges);

		Iterator<TransportRouteResultSegment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			TransportRouteResultSegment s = iterator.next();
			if (s.walkDist != 0) {
				double walkTime = getWalkTime(s.walkDist, routeResult.getWalkSpeed());
				if (walkTime > MIN_WALK_TIME) {
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
					if (walkTime2 > MIN_WALK_TIME) {
						String walkTimeS = OsmAndFormatter.getFormattedDuration((int) walkTime2, app);
						routesBadges.addView(createArrow(), new FlowLayout.LayoutParams(itemsSpacing, itemsSpacing));
						routesBadges.addView(createWalkRouteBadge(walkTimeS));
					}
				}
			}
		}
	}

	private View createRouteBadge(TransportRouteResultSegment segment) {
		LinearLayout convertView = (LinearLayout) getMapActivity().getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
		if (segment != null) {
			TransportRoute transportRoute = segment.route;
			TransportStopRoute transportStopRoute = getTransportStopRoute(transportRoute, segment.getStart());

			String routeRef = segment.route.getAdjustedRouteRef();
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
		LinearLayout convertView = (LinearLayout) getMapActivity().getLayoutInflater().inflate(R.layout.transport_stop_route_item_with_icon, null, false);
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

	private double getWalkTime(double walkDist, double walkSpeed) {
		return walkDist / walkSpeed;
	}
}
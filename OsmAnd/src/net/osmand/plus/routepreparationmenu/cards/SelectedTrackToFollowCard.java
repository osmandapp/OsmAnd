package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherLocalRoutingParameter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.FollowTrackFragment;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class SelectedTrackToFollowCard extends MapBaseCard {

	final FollowTrackFragment target;
	final GpxFile gpxFile;

	public SelectedTrackToFollowCard(@NonNull MapActivity mapActivity, @NonNull FollowTrackFragment target,
	                                 @NonNull GpxFile gpxFile) {
		super(mapActivity);
		this.target = target;
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.selected_track_to_follow_card;
	}

	@Override
	protected void updateContent() {
		view.getViewTreeObserver().addOnScrollChangedListener(() -> {
			boolean bottomScrollAvailable = view.canScrollVertically(1);
			if (bottomScrollAvailable) {
				target.showShadowButton();
			} else {
				target.hideShadowButton();
			}
		});

		ViewGroup cardsContainer = view.findViewById(R.id.cards_container);
		cardsContainer.removeAllViews();

		TrackEditCard importTrackCard = new TrackEditCard(mapActivity, gpxFile);
		importTrackCard.setListener(target);
		cardsContainer.addView(importTrackCard.build(mapActivity));

		SelectTrackCard selectTrackCard = new SelectTrackCard(mapActivity);
		selectTrackCard.setListener(target);
		cardsContainer.addView(selectTrackCard.build(mapActivity));

		ApplicationMode mode = app.getRoutingHelper().getAppMode();

		RoutingHelper routingHelper = app.getRoutingHelper();
		GPXRouteParamsBuilder rparams = routingHelper.getCurrentGPXRoute();
		boolean osmandRouter = mode.getRouteService() == RouteService.OSMAND;
		if (rparams != null && osmandRouter) {
			cardsContainer.addView(buildDividerView(cardsContainer, false));

			ReverseTrackCard reverseTrackCard = new ReverseTrackCard(mapActivity, rparams.isReverse());
			reverseTrackCard.setListener(target);
			cardsContainer.addView(reverseTrackCard.build(mapActivity));

			cardsContainer.addView(buildDividerView(cardsContainer, true));

			AttachTrackToRoadsCard attachTrackCard = new AttachTrackToRoadsCard(mapActivity);
			attachTrackCard.setListener(target);
			cardsContainer.addView(attachTrackCard.build(mapActivity));

			setupNavigateOptionsCard(cardsContainer, rparams);
		}
	}

	private View buildDividerView(@NonNull ViewGroup view, boolean needMargin) {
		View divider = themedInflater.inflate(R.layout.simple_divider_item, view, false);

		ViewGroup.LayoutParams params = divider.getLayoutParams();
		if (needMargin && params instanceof ViewGroup.MarginLayoutParams) {
			AndroidUtils.setMargins((ViewGroup.MarginLayoutParams) params,
					AndroidUtils.dpToPx(mapActivity, 64), 0, 0, 0);
			divider.setLayoutParams(params);
		}

		return divider;
	}

	private void setupNavigateOptionsCard(ViewGroup cardsContainer, GPXRouteParamsBuilder routeParamsBuilder) {
		int passRouteId = R.string.gpx_option_from_start_point;
		LocalRoutingParameter passWholeRoute = new OtherLocalRoutingParameter(passRouteId,
				app.getString(passRouteId), routeParamsBuilder.isPassWholeRoute());

		int navigationTypeId = R.string.gpx_option_calculate_first_last_segment;
		LocalRoutingParameter navigationType = new OtherLocalRoutingParameter(navigationTypeId,
				app.getString(navigationTypeId), routeParamsBuilder.isCalculateOsmAndRouteParts());


		int connectTrackPointsId = R.string.connect_track_points_as;
		LocalRoutingParameter connectTrackPointStraightly = new OtherLocalRoutingParameter(connectTrackPointsId,
				app.getString(connectTrackPointsId), routeParamsBuilder.shouldConnectPointsStraightly());

		NavigateTrackOptionsCard navigateTrackCard = new NavigateTrackOptionsCard(mapActivity,
				passWholeRoute, navigationType, connectTrackPointStraightly,
				routeParamsBuilder.shouldUseIntermediateRtePoints());
		navigateTrackCard.setListener(target);
		cardsContainer.addView(navigateTrackCard.build(mapActivity));
	}
}
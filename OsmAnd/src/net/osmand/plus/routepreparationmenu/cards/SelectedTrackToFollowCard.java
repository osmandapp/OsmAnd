package net.osmand.plus.routepreparationmenu.cards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.FollowTrackFragment;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.GPXRouteParams;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;

import androidx.annotation.NonNull;

public class SelectedTrackToFollowCard extends MapBaseCard {

	final FollowTrackFragment target;
	final GPXFile gpxFile;

	public SelectedTrackToFollowCard(@NonNull MapActivity mapActivity, @NonNull FollowTrackFragment target, @NonNull GPXFile gpxFile) {
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
		view.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
				boolean bottomScrollAvailable = view.canScrollVertically(1);
				if (bottomScrollAvailable) {
					target.showShadowButton();
				} else {
					target.hideShadowButton();
				}
			}
		});

		ViewGroup cardsContainer = ((ViewGroup) view.findViewById(R.id.cards_container));
		cardsContainer.removeAllViews();

		TrackEditCard importTrackCard = new TrackEditCard(mapActivity, gpxFile);
		importTrackCard.setListener(target);
		cardsContainer.addView(importTrackCard.build(mapActivity));

		SelectTrackCard selectTrackCard = new SelectTrackCard(mapActivity);
		selectTrackCard.setListener(target);
		cardsContainer.addView(selectTrackCard.build(mapActivity));

		ApplicationMode mode = app.getRoutingHelper().getAppMode();

		RoutingHelper routingHelper = app.getRoutingHelper();
		GPXRouteParams.GPXRouteParamsBuilder rparams = routingHelper.getCurrentGPXRoute();
		boolean osmandRouter = mode.getRouteService() == RouteService.OSMAND;
		if (rparams != null && osmandRouter) {
			cardsContainer.addView(buildDividerView(cardsContainer, false));

			ReverseTrackCard reverseTrackCard = new ReverseTrackCard(mapActivity, rparams.isReverse());
			reverseTrackCard.setListener(target);
			cardsContainer.addView(reverseTrackCard.build(mapActivity));

			if (!gpxFile.hasRtePt() && !gpxFile.hasRoute()) {
				cardsContainer.addView(buildDividerView(cardsContainer, true));

				AttachTrackToRoadsCard attachTrackCard = new AttachTrackToRoadsCard(mapActivity);
				attachTrackCard.setListener(target);
				cardsContainer.addView(attachTrackCard.build(mapActivity));
			}
			if (!rparams.isUseIntermediatePointsRTE()) {
				setupNavigateOptionsCard(cardsContainer, rparams);
			}
		}
	}

	private View buildDividerView(@NonNull ViewGroup view, boolean needMargin) {
		LayoutInflater themedInflater = UiUtilities.getInflater(view.getContext(), nightMode);
		View divider = themedInflater.inflate(R.layout.simple_divider_item, view, false);

		ViewGroup.LayoutParams params = divider.getLayoutParams();
		if (needMargin && params instanceof ViewGroup.MarginLayoutParams) {
			AndroidUtils.setMargins((ViewGroup.MarginLayoutParams) params, AndroidUtils.dpToPx(mapActivity, 64), 0, 0, 0);
			divider.setLayoutParams(params);
		}

		return divider;
	}

	private void setupNavigateOptionsCard(ViewGroup cardsContainer, GPXRouteParams.GPXRouteParamsBuilder rparams) {
		int passRouteId = R.string.gpx_option_from_start_point;
		RoutingOptionsHelper.LocalRoutingParameter passWholeRoute = new RoutingOptionsHelper.OtherLocalRoutingParameter(passRouteId,
				app.getString(passRouteId), rparams.isPassWholeRoute());

		int navigationTypeId = R.string.gpx_option_calculate_first_last_segment;
		RoutingOptionsHelper.LocalRoutingParameter navigationType = new RoutingOptionsHelper.OtherLocalRoutingParameter(navigationTypeId,
				app.getString(navigationTypeId), rparams.isCalculateOsmAndRouteParts());

		NavigateTrackOptionsCard navigateTrackCard = new NavigateTrackOptionsCard(mapActivity, passWholeRoute, navigationType);
		navigateTrackCard.setListener(target);
		cardsContainer.addView(navigateTrackCard.build(mapActivity));
	}
}
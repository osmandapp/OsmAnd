package net.osmand.plus.development;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;

public class DashSimulateFragment extends DashBaseFragment {

	private static final String TAG = "DASH_SIMULATE_FRAGMENT";
	private static final int TITLE_ID = R.string.simulate_your_location;

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(DashSimulateFragment.TAG,
			DashSimulateFragment.class,
			SHOULD_SHOW_FUNCTION, 150, null);

	@Override
	public void onOpenDash() {
		OsmAndLocationProvider loc = getMyApplication().getLocationProvider();
		boolean routeAnimating = loc.getLocationSimulation().isRouteAnimating();
		((TextView) getView().findViewById(R.id.name)).setText(routeAnimating ? R.string.animate_route_off
				: R.string.animate_route);
		ImageButton actionButton = (ImageButton) getView().findViewById(R.id.stop);
		actionButton.setImageDrawable(
				!routeAnimating ? getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_play_dark)
						: getMyApplication().getUIUtilities().getThemedIcon(R.drawable.ic_action_rec_stop));
		actionButton.setContentDescription(getString(routeAnimating ? R.string.animate_route_off : R.string.animate_route));

	}

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		TextView header = (TextView) view.findViewById(R.id.fav_text);
		header.setText(TITLE_ID);
		((Button) view.findViewById(R.id.show_all)).setVisibility(View.GONE);
		LinearLayout tracks = (LinearLayout) view.findViewById(R.id.items);
		View item = inflater.inflate(R.layout.dash_simulate_item, null, false);
		tracks.addView(item);
		final OsmAndLocationProvider loc = getMyApplication().getLocationProvider();
		OnClickListener listener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				loc.getLocationSimulation().startStopGpxAnimation(getActivity());
				dashboard.hideDashboard();
			}
		};
		item.setOnClickListener(listener);
		ImageButton actionButton = (ImageButton) item.findViewById(R.id.stop);
		actionButton.setOnClickListener(listener);
		actionButton.setContentDescription(getString(R.string.animate_route));
		((TextView) item.findViewById(R.id.name)).setText(R.string.animate_route);
		item.findViewById(R.id.divider).setVisibility(View.VISIBLE);

		return view;
	}
}

package net.osmand.plus.development;

import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.helpers.FontCache;
import android.graphics.Typeface;
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

/**
 */
public class DashSimulateFragment extends DashBaseFragment {

	public static final String TAG = "DASH_SIMULATE_FRAGMENT";

	@Override
	public void onOpenDash() {
		OsmAndLocationProvider loc = getMyApplication().getLocationProvider();
		boolean routeAnimating = loc.getLocationSimulation().isRouteAnimating();
		((TextView) getView().findViewById(R.id.name)).setText(routeAnimating ? R.string.animate_route_off
				: R.string.animate_route);
		((ImageButton) getView().findViewById(R.id.stop)).setImageDrawable(
				!routeAnimating ? getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_play_dark)
						: getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_rec_stop));

	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		TextView header = (TextView) view.findViewById(R.id.fav_text);
		header.setText(R.string.simulate_your_location);
		((Button) view.findViewById(R.id.show_all)).setVisibility(View.GONE);
		LinearLayout tracks = (LinearLayout) view.findViewById(R.id.items);
		View item = inflater.inflate(R.layout.dash_simulate_item, null, false);
		tracks.addView(item);
		final OsmAndLocationProvider loc = getMyApplication().getLocationProvider();
		OnClickListener listener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (getActivity() instanceof MapActivity) {
					loc.getLocationSimulation().startStopRouteAnimation((MapActivity) getActivity());
					dashboard.hideDashboard();
				}
			}
		};
		item.setOnClickListener(listener);
		((ImageButton) item.findViewById(R.id.stop)).setOnClickListener(listener);
		((TextView) item.findViewById(R.id.name)).setText(R.string.animate_route);
		item.findViewById(R.id.divider).setVisibility(View.VISIBLE);

		return view;
	}

}

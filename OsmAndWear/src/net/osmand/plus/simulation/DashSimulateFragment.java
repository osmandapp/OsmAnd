package net.osmand.plus.simulation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;

public class DashSimulateFragment extends DashBaseFragment {

	private static final String TAG = "DASH_SIMULATE_FRAGMENT";
	private static final int TITLE_ID = R.string.shared_string_navigation;

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	public static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(TAG,
			DashSimulateFragment.class,
			SHOULD_SHOW_FUNCTION, 150, null);

	@Override
	public void onOpenDash() {
		((TextView) getView().findViewById(R.id.name)).setText(R.string.simulate_your_location);
	}

	@Override
	public View initView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_common_fragment, container, false);
		TextView header = view.findViewById(R.id.fav_text);
		header.setText(TITLE_ID);
		view.findViewById(R.id.show_all).setVisibility(View.GONE);
		LinearLayout tracks = view.findViewById(R.id.items);
		View item = inflater.inflate(R.layout.dash_simulate_item, null, false);
		tracks.addView(item);
		item.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				SimulateLocationFragment.showInstance(activity.getSupportFragmentManager(), null, true);
				dashboard.hideDashboard();
			}
		});
		((TextView) item.findViewById(R.id.name)).setText(R.string.simulate_your_location);

		item.findViewById(R.id.divider).setVisibility(View.VISIBLE);

		return view;
	}
}

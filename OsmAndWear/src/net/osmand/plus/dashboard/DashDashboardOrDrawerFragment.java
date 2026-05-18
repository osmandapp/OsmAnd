package net.osmand.plus.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;

public class DashDashboardOrDrawerFragment extends DashBaseFragment {

	public static final String TAG = "DASH_DASHBOARD_OR_DRAWER_FRAGMENT";
	public static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashFragmentData.ShouldShowFunction() {
				// If settings null. No changes in setting will be made.
				@Override
				public boolean shouldShow(OsmandSettings settings, MapActivity activity, String tag) {
					return settings.SHOW_CARD_TO_CHOOSE_DRAWER.get();
				}
			};

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_dashboard_or_drawer_fragment, container, false);
		view.findViewById(R.id.useDashboardButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandSettings settings = getMyApplication().getSettings();
				settings.SHOW_DASHBOARD_ON_START.set(true);
				settings.SHOW_DASHBOARD_ON_MAP_SCREEN.set(true);
				settings.SHOW_CARD_TO_CHOOSE_DRAWER.set(false);
				dashboard.hideFragmentByTag(TAG);
			}
		});
		view.findViewById(R.id.useDrawerButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OsmandSettings settings = getMyApplication().getSettings();
				settings.SHOW_DASHBOARD_ON_START.set(false);
				settings.SHOW_DASHBOARD_ON_MAP_SCREEN.set(false);
				settings.SHOW_CARD_TO_CHOOSE_DRAWER.set(false);
				dashboard.hideDashboard();
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
	}
}

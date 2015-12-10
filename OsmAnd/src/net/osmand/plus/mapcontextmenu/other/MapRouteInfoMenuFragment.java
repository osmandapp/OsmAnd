package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public class MapRouteInfoMenuFragment extends Fragment {
	public static final String TAG = "MapRouteInfoMenuFragment";

	private MapRouteInfoMenu menu;
	private View mainView;

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		MapActivity mapActivity = getMapActivity();

		menu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
		View view = inflater.inflate(R.layout.plan_route_info, container, false);
		if (menu == null) {
			return view;
		}

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		mainView = view.findViewById(R.id.main_view);
		updateInfo();

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (menu == null) {
			dismiss();
		}
	}

	public void updateInfo() {
		menu.updateInfo(mainView);
	}

	public void show(MapActivity mapActivity) {
		int slideInAnim = R.anim.slide_in_bottom;
		int slideOutAnim = R.anim.slide_out_bottom;

		mapActivity.getSupportFragmentManager().beginTransaction()
				.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
				.add(R.id.routeMenuContainer, this, TAG)
				.addToBackStack(TAG)
				.commitAllowingStateLoss();
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStack(TAG,
						FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				//
			}
		}
	}

	public static boolean showInstance(final MapActivity mapActivity) {
		try {
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			int slideInAnim;
			int slideOutAnim;
			if (portrait) {
				slideInAnim = R.anim.slide_in_bottom;
				slideOutAnim = R.anim.slide_out_bottom;
			} else {
				slideInAnim = R.anim.slide_in_left;
				slideOutAnim = R.anim.slide_out_left;
			}

			MapRouteInfoMenuFragment fragment = new MapRouteInfoMenuFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}
}

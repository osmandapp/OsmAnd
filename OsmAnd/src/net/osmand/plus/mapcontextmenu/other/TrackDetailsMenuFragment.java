package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;

public class TrackDetailsMenuFragment extends Fragment {
	public static final String TAG = "TrackDetailsMenuFragment";

	private TrackDetailsMenu menu;
	private View mainView;

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		MapActivity mapActivity = getMapActivity();

		menu = mapActivity.getMapLayers().getMapControlsLayer().getTrackDetailsMenu();
		View view = inflater.inflate(R.layout.track_details, container, false);
		if (menu == null) {
			return view;
		}

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

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (menu != null) {
			menu.onDismiss();
		}
	}

	public int getHeight() {
		if (mainView != null) {
			return mainView.getHeight();
		} else {
			return 0;
		}
	}

	public int getWidth() {
		if (mainView != null) {
			return mainView.getWidth();
		} else {
			return 0;
		}
	}

	public void updateInfo() {
		menu.updateInfo(mainView);
		applyDayNightMode();
	}

	public void show(MapActivity mapActivity) {
		mapActivity.getSupportFragmentManager().beginTransaction()
				.add(R.id.routeMenuContainer, this, TAG)
				.addToBackStack(TAG)
				.commitAllowingStateLoss();
	}

	public void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			try {
				activity.getSupportFragmentManager().popBackStackImmediate(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	public void applyDayNightMode() {
		MapActivity ctx = getMapActivity();
		boolean portraitMode = AndroidUiHelper.isOrientationPortrait(ctx);
		boolean landscapeLayout = !portraitMode;
		boolean nightMode = ctx.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		if (!landscapeLayout) {
			AndroidUtils.setBackground(ctx, mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		} else {
			AndroidUtils.setBackground(ctx, mainView, nightMode, R.drawable.bg_left_menu_light, R.drawable.bg_left_menu_dark);
		}

		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.y_axis_title), nightMode);
		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.x_axis_title), nightMode);

		ImageView yAxisArrow = (ImageView) mainView.findViewById(R.id.y_axis_arrow);
		ImageView xAxisArrow = (ImageView) mainView.findViewById(R.id.x_axis_arrow);
		yAxisArrow.setImageDrawable(ctx.getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_arrow_drop_down));
		xAxisArrow.setImageDrawable(ctx.getMyApplication().getIconsCache().getThemedIcon(R.drawable.ic_action_arrow_drop_down));
	}

	public static boolean showInstance(final MapActivity mapActivity) {
		try {
			TrackDetailsMenuFragment fragment = new TrackDetailsMenuFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(R.id.bottomFragmentContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}
}

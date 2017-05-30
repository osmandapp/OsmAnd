package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.AndroidUtils;
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
		if (menu != null) {
			menu.updateInfo(mainView);
		}
		applyDayNightMode();
	}

	public void updateFromIcon() {
		if (menu != null) {
			menu.updateFromIcon(mainView);
		}
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
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerModesLayout), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerFromDropDown), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.viaLayoutDivider), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerToDropDown), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerButtons), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);

		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerBtn1), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerBtn2), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerBtn3), nightMode,
				R.color.dashboard_divider_light, R.color.dashboard_divider_dark);

		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.ViaView), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.ViaSubView), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.toTitle), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.fromTitle), nightMode);
		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.InfoTextView), nightMode);

		AndroidUtils.setDashButtonBackground(ctx, mainView.findViewById(R.id.FromLayout), nightMode);
		AndroidUtils.setDashButtonBackground(ctx, mainView.findViewById(R.id.ViaLayout), nightMode);
		AndroidUtils.setDashButtonBackground(ctx, mainView.findViewById(R.id.ToLayout), nightMode);
		AndroidUtils.setDashButtonBackground(ctx, mainView.findViewById(R.id.Info), nightMode);

		AndroidUtils.setDashButtonBackground(ctx, mainView.findViewById(R.id.Next), nightMode);
		AndroidUtils.setDashButtonBackground(ctx, mainView.findViewById(R.id.Prev), nightMode);

		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.DistanceText), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.DistanceTitle), nightMode);
		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.DurationText), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.DurationTitle), nightMode);
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

			mapActivity.getContextMenu().hideMenues();

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

package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class MapRouteInfoMenuFragment extends BaseOsmAndFragment {
	public static final String TAG = "MapRouteInfoMenuFragment";

	private MapRouteInfoMenu menu;
	private View mainView;

	private boolean portrait;

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		MapActivity mapActivity = getMapActivity();

		menu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
		View view = inflater.inflate(R.layout.plan_route_info, container, false);
		portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		if (!portrait) {
			AndroidUtils.addStatusBarPadding21v(getActivity(), view);
		}
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
		getMapActivity().getMapLayers().getMapControlsLayer().showMapControlsIfHidden();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (menu != null) {
			menu.onDismiss();
		}
	}

	@Override
	public int getStatusBarColorId() {
		return portrait ? -1 : R.color.status_bar_transparent_gradient;
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
			applyDayNightMode();
		}
	}

	public void updateFromIcon() {
		if (menu != null) {
			menu.updateFromIcon(mainView);
		}
	}

	public void show(MapActivity mapActivity) {
		int slideInAnim = 0;
		int slideOutAnim = 0;
		if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			slideInAnim = R.anim.slide_in_bottom;
			slideOutAnim = R.anim.slide_out_bottom;
		}

		mapActivity.getSupportFragmentManager()
				.beginTransaction()
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
			AndroidUtils.setBackground(ctx, mainView, nightMode, R.drawable.route_info_menu_bg_light, R.drawable.route_info_menu_bg_dark);
		} else {
			AndroidUtils.setBackground(ctx, mainView, nightMode, R.drawable.route_info_menu_bg_left_light, R.drawable.route_info_menu_bg_left_dark);
		}
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.map_route_prepare_bottom_view), nightMode,
				R.color.route_info_bottom_view_bg_light, R.color.route_info_bottom_view_bg_dark);

		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerModesLayout), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerFromDropDown), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.viaLayoutDivider), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerToDropDown), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerButtons), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);

		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerBtn1), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.dividerBtn2), nightMode,
				R.color.route_info_divider_light, R.color.route_info_divider_dark);

		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.ViaView), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.ViaSubView), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.toTitle), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.fromTitle), nightMode);
		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.InfoTextView), nightMode);

		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.DistanceText), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.DistanceTitle), nightMode);
		AndroidUtils.setTextPrimaryColor(ctx, (TextView) mainView.findViewById(R.id.DurationText), nightMode);
		AndroidUtils.setTextSecondaryColor(ctx, (TextView) mainView.findViewById(R.id.DurationTitle), nightMode);
	}

	public static boolean showInstance(final MapActivity mapActivity) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		int slideInAnim = 0;
		int slideOutAnim = 0;
		if (!mapActivity.getMyApplication().getSettings().DO_NOT_USE_ANIMATIONS.get()) {
			if (portrait) {
				slideInAnim = R.anim.slide_in_bottom;
				slideOutAnim = R.anim.slide_out_bottom;
			} else {
				slideInAnim = R.anim.slide_in_left;
				slideOutAnim = R.anim.slide_out_left;
			}
		}

		try {
			mapActivity.getContextMenu().hideMenues();

			MapRouteInfoMenuFragment fragment = new MapRouteInfoMenuFragment();
			mapActivity.getSupportFragmentManager()
					.beginTransaction()
					.setCustomAnimations(slideInAnim, slideOutAnim, slideInAnim, slideOutAnim)
					.add(R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}
}

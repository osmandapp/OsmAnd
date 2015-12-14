package net.osmand.plus.mapcontextmenu.other;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
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

	public void updateInfo() {
		menu.updateInfo(mainView);
		applyDayNightMode();
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
		boolean portrait = AndroidUiHelper.isOrientationPortrait(ctx);
		boolean nightMode = ctx.getMyApplication().getDaynightHelper().isNightMode();
		if (portrait) {
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

		((TextView) mainView.findViewById(R.id.ViaView)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.primary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.primary_text_light));
		((TextView) mainView.findViewById(R.id.ViaSubView)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_light));

		((TextView) mainView.findViewById(R.id.toTitle)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_light));

		((TextView) mainView.findViewById(R.id.fromTitle)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_light));

		((TextView) mainView.findViewById(R.id.InfoTextView)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_light));

		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.FromLayout), nightMode,
				R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.ViaLayout), nightMode,
				R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.ToLayout), nightMode,
				R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.Info), nightMode,
				R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);

		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.Next), nightMode,
				R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);
		AndroidUtils.setBackground(ctx, mainView.findViewById(R.id.Prev), nightMode,
				R.drawable.dashboard_button_light, R.drawable.dashboard_button_dark);

		((TextView) mainView.findViewById(R.id.DistanceText)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.primary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.primary_text_light));
		((TextView) mainView.findViewById(R.id.DistanceTitle)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_light));
		((TextView) mainView.findViewById(R.id.DurationText)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.primary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.primary_text_light));
		((TextView) mainView.findViewById(R.id.DurationTitle)).setTextColor(nightMode ?
				ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_dark)
				: ContextCompat.getColorStateList(ctx, android.R.color.secondary_text_light));

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

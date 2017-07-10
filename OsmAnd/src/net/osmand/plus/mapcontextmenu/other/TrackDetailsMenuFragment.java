package net.osmand.plus.mapcontextmenu.other;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
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
		if (menu == null || menu.getGpxItem() == null) {
			return view;
		}

		mainView = view.findViewById(R.id.main_view);

		TextView topBarTitle = (TextView) mainView.findViewById(R.id.top_bar_title);
		if (topBarTitle != null) {
			if (menu.getGpxItem().group != null) {
				topBarTitle.setText(menu.getGpxItem().group.getGpxName());
			} else {
				topBarTitle.setText(mapActivity.getString(R.string.rendering_category_details));
			}
		}

		ImageButton backButton = (ImageButton) mainView.findViewById(R.id.top_bar_back_button);
		ImageButton closeButton = (ImageButton) mainView.findViewById(R.id.top_bar_close_button);
		if (backButton != null) {
			backButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getActivity().onBackPressed();
				}
			});
		}
		if (closeButton != null) {
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dismiss();
				}
			});
		}

		updateInfo();

		ViewTreeObserver vto = mainView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

			@Override
			public void onGlobalLayout() {

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
				if (getMapActivity() != null) {
					updateInfo();
				}
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (menu == null || menu.getGpxItem() == null) {
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

		ImageButton backButton = (ImageButton) mainView.findViewById(R.id.top_bar_back_button);
		if (backButton != null) {
			backButton.setImageDrawable(ctx.getMyApplication().getIconsCache().getIcon(R.drawable.ic_arrow_back, R.color.color_white));
		}

	}

	public static boolean showInstance(final MapActivity mapActivity) {
		try {
			boolean portrait = mapActivity.findViewById(R.id.bottomFragmentContainer) != null;
			TrackDetailsMenuFragment fragment = new TrackDetailsMenuFragment();
			mapActivity.getSupportFragmentManager().beginTransaction()
					.add(portrait ? R.id.bottomFragmentContainer : R.id.routeMenuContainer, fragment, TAG)
					.addToBackStack(TAG).commitAllowingStateLoss();

			return true;

		} catch (RuntimeException e) {
			return false;
		}
	}
}

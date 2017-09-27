package net.osmand.plus.mapmarkers;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

public class OptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

	public final static String TAG = "OptionsBottomSheetDialogFragment";

	private MarkerOptionsFragmentListener listener;
	private boolean portrait;

	public void setListener(MarkerOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) getActivity();
		portrait = AndroidUiHelper.isOrientationPortrait(getActivity());
		boolean nightMode = !getMyApplication().getSettings().isLightContent();
		final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;

		final View mainView = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_marker_options_bottom_sheet_dialog, container);
		if (portrait) {
			AndroidUtils.setBackground(getActivity(), mainView, nightMode, R.drawable.bg_bottom_menu_light, R.drawable.bg_bottom_menu_dark);
		}

		((ImageView) mainView.findViewById(R.id.sort_by_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_sort_waypoint_dark));
		OsmandSettings.MapMarkersMode mode = getMyApplication().getSettings().MAP_MARKERS_MODE.get();
		ImageView showDirectionIcon = (ImageView) mainView.findViewById(R.id.show_direction_icon);
		int imageResId = 0;
		switch (mode) {
			case TOOLBAR:
				imageResId = R.drawable.ic_action_device_topbar;
				break;
			case WIDGETS:
				imageResId = R.drawable.ic_action_device_widget;
				break;
		}
		showDirectionIcon.setBackgroundDrawable(getContentIcon(R.drawable.ic_action_device_top));
		if (imageResId != 0) {
			showDirectionIcon.setImageDrawable(getIcon(imageResId, R.color.dashboard_blue));
		}
		((ImageView) mainView.findViewById(R.id.coordinate_input_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_action_coordinates_longitude));
		((ImageView) mainView.findViewById(R.id.build_route_icon))
				.setImageDrawable(getContentIcon(R.drawable.map_directions));
		((ImageView) mainView.findViewById(R.id.save_as_new_track_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
		((ImageView) mainView.findViewById(R.id.move_all_to_history_icon))
				.setImageDrawable(getContentIcon(R.drawable.ic_action_history2));

		((TextView) mainView.findViewById(R.id.show_direction_text_view)).setTextColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.color_dialog_buttons_dark : R.color.map_widget_blue_pressed));
		((TextView) mainView.findViewById(R.id.show_direction_text_view)).setText(getMyApplication().getSettings().MAP_MARKERS_MODE.get().toHumanString(getActivity()));

		mainView.findViewById(R.id.sort_by_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.sortByOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.show_direction_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.showDirectionOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.coordinate_input_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.coordinateInputOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.build_route_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.buildRouteOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.save_as_new_track_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.saveAsNewTrackOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.move_all_to_history_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.moveAllToHistoryOnClick();
				}
				dismiss();
			}
		});
		mainView.findViewById(R.id.cancel_row).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		final int screenHeight = AndroidUtils.getScreenHeight(getActivity());
		final int statusBarHeight = AndroidUtils.getStatusBarHeight(getActivity());
		final int navBarHeight = AndroidUtils.getNavBarHeight(getActivity());

		mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				final View scrollView = mainView.findViewById(R.id.marker_options_scroll_view);
				int scrollViewHeight = scrollView.getHeight();
				int dividerHeight = AndroidUtils.dpToPx(getContext(), 1);
				int cancelButtonHeight = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_sheet_cancel_button_height);
				int spaceForScrollView = screenHeight - statusBarHeight - navBarHeight - dividerHeight - cancelButtonHeight;
				if (scrollViewHeight > spaceForScrollView) {
					scrollView.getLayoutParams().height = spaceForScrollView;
					scrollView.requestLayout();
				}

				if (!portrait) {
					if (screenHeight - statusBarHeight - mainView.getHeight()
							>= AndroidUtils.dpToPx(getActivity(), 8)) {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_topsides_landscape_light, R.drawable.bg_bottom_sheet_topsides_landscape_dark);
					} else {
						AndroidUtils.setBackground(getActivity(), mainView, false,
								R.drawable.bg_bottom_sheet_sides_landscape_light, R.drawable.bg_bottom_sheet_sides_landscape_dark);
					}
				}

				ViewTreeObserver obs = mainView.getViewTreeObserver();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					obs.removeOnGlobalLayoutListener(this);
				} else {
					obs.removeGlobalOnLayoutListener(this);
				}
			}
		});

		return mainView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!portrait) {
			final Window window = getDialog().getWindow();
			WindowManager.LayoutParams params = window.getAttributes();
			params.width = getActivity().getResources().getDimensionPixelSize(R.dimen.landscape_bottom_sheet_dialog_fragment_width);
			window.setAttributes(params);
		}
	}

	@Override
	protected Drawable getContentIcon(@DrawableRes int id) {
		return getIcon(id, getMyApplication().getSettings().isLightContent() ? R.color.on_map_icon_color : R.color.ctx_menu_info_text_dark);
	}

	interface MarkerOptionsFragmentListener {

		void sortByOnClick();

		void showDirectionOnClick();

		void coordinateInputOnClick();

		void buildRouteOnClick();

		void saveAsNewTrackOnClick();

		void moveAllToHistoryOnClick();
	}
}
